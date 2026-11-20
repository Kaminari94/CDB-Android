package com.example.centraledellebolle.data

import android.content.Context
import androidx.room.withTransaction
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.centraledellebolle.BolleApplication
import com.example.centraledellebolle.printing.BluetoothPrinterService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit

const val EMISSION_PENDING_SYNC = "PENDING_SYNC"
const val EMISSION_PRINTED_PENDING_SYNC = "PRINTED_PENDING_SYNC"
const val EMISSION_PRINT_FAILED = "PRINT_FAILED"
const val EMISSION_SYNCED = "SYNCED"
const val EMISSION_CONFLICT = "CONFLICT"

data class LocalEmissionLine(val codice: String, val descrizione: String, val quantita: Int, val lotto: String)
data class LocalCustomerSnapshot(
    val id: Int,
    val nome: String,
    val via: String,
    val cap: String?,
    val citta: String?,
    val piva: String?,
    val codice: String?,
    val proprietarioNome: String?,
    val proprietarioCodice: String?
)

data class IssuedBolla(val emission: LocalEmissionEntity, val lines: List<LocalEmissionLine>)

sealed interface OfflineIssueResult {
    data class Success(val issued: IssuedBolla) : OfflineIssueResult
    data class ValidationError(val errors: List<QuickLineError>) : OfflineIssueResult
    data class Error(val message: String) : OfflineIssueResult
}

/** Offline-first QuickBolla implementation.  The legacy server-only method remains in QuickBollaRepository. */
class OfflineQuickBollaRepository(
    private val context: Context,
    private val database: OfflineBollaDatabase,
    private val api: com.example.centraledellebolle.network.ApiService,
    private val printer: BluetoothPrinterService
) {
    private val dao = database.quickBollaDao()
    private val gson = Gson()

    val customers: Flow<List<QuickCustomerEntity>> = dao.customersFlow()
    val documentTypes: Flow<List<QuickDocumentTypeEntity>> = dao.documentTypesFlow()
    val catalogMeta: Flow<QuickCatalogMetaEntity?> = dao.catalogMetaFlow()
    val emissions: Flow<List<LocalEmissionEntity>> = dao.emissionsFlow()
    val conflicts: Flow<List<LocalEmissionEntity>> = dao.conflictsFlow()
    val pendingCount: Flow<Int> = dao.pendingCountFlow()

    suspend fun refreshCatalog(year: Int): Result<Unit> = runCatching {
        val catalog = api.quickBollaCatalog(year)
        require(catalog.year == year) { "Il catalogo ricevuto non corrisponde all'anno richiesto." }
        database.withTransaction {
            dao.putCatalogMeta(
                QuickCatalogMetaEntity(
                    schemaVersion = catalog.schemaVersion,
                    generatedAt = catalog.generatedAt,
                    year = catalog.year,
                    renderer = catalog.printer.renderer,
                    receiptWidth = catalog.printer.width,
                    encoding = catalog.printer.encoding,
                    concessionarioJson = gson.toJson(catalog.concessionario)
                )
            )
            dao.clearTypes()
            dao.putTypes(catalog.types.map {
                QuickDocumentTypeEntity(it.id, it.nome, it.descrizione, it.nextNumber)
            })
            dao.clearCustomers()
            dao.putCustomers(catalog.customers.map {
                QuickCustomerEntity(
                    id = it.id, nome = it.nome, via = it.via, cap = it.cap, citta = it.citta,
                    piva = it.piva, codice = it.codice, defaultDocumentTypeId = it.documentTypeId,
                    ownerName = it.proprietario?.nome, ownerCode = it.proprietario?.codice,
                    allowedArticleCodesJson = gson.toJson(it.allowedArticles)
                )
            })
            dao.clearArticles()
            dao.putArticles(catalog.articles.map {
                QuickArticleEntity(it.codice, it.id, it.descrizione, it.lastLotto)
            })
            // Django's next_number is a lower bound only: never move a locally issued sequence back.
            catalog.types.forEach { type ->
                dao.insertSequence(QuickDocumentSequenceEntity(type.id, year, type.nextNumber))
                dao.advanceSequenceAtLeast(type.id, year, type.nextNumber)
            }
        }
    }

    suspend fun issue(rawLines: String, customerId: Int, documentTypeId: Int): OfflineIssueResult {
        return try {
            database.withTransaction {
                val meta = dao.catalogMeta()
                    ?: return@withTransaction OfflineIssueResult.Error("Catalogo non disponibile: collegati una volta per scaricarlo.")
                val customer = dao.customers().firstOrNull { it.id == customerId }
                    ?: return@withTransaction OfflineIssueResult.Error("Cliente non presente nel catalogo locale.")
                val type = dao.documentTypes().firstOrNull { it.id == documentTypeId }
                    ?: return@withTransaction OfflineIssueResult.Error("Tipo documento non presente nel catalogo locale.")

                val parsed = parseAndValidate(rawLines, customer)
                if (parsed is OfflineIssueResult.ValidationError) return@withTransaction parsed
                if (parsed is OfflineIssueResult.Error) return@withTransaction parsed
                val lines = (parsed as OfflineIssueResult.Success).issued.lines

                val now = ZonedDateTime.now()
                val year = now.year
                var sequence = dao.sequence(type.id, year)
                if (sequence == null) {
                    // A fresh year uses the last downloaded server lower bound for that type.
                    sequence = QuickDocumentSequenceEntity(type.id, year, type.nextNumberFromServer)
                    dao.insertSequence(sequence)
                }
                val number = sequence.nextLocal
                dao.setNextSequence(type.id, year, number + 1)

                val customerSnapshot = LocalCustomerSnapshot(
                    customer.id, customer.nome, customer.via, customer.cap, customer.citta,
                    customer.piva, customer.codice, customer.ownerName, customer.ownerCode
                )
                val issuedAt = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                val receipt = LocalReceiptRenderer.render(
                    gson.fromJson(meta.concessionarioJson, QuickCatalogConcessionario::class.java),
                    type, customerSnapshot, number, year, issuedAt, lines, meta.receiptWidth
                )
                val emission = LocalEmissionEntity(
                    emissionId = UUID.randomUUID().toString(), customerId = customer.id,
                    documentTypeId = type.id, documentTypeName = type.name, number = number, year = year,
                    issuedAt = issuedAt, customerJson = gson.toJson(customerSnapshot),
                    linesJson = gson.toJson(lines), renderer = meta.renderer, receiptWidth = meta.receiptWidth,
                    receiptText = receipt, status = EMISSION_PENDING_SYNC
                )
                dao.insertEmission(emission)
                OfflineIssueResult.Success(IssuedBolla(emission, lines))
            }
        } catch (e: Exception) {
            OfflineIssueResult.Error(e.message ?: "Impossibile salvare la bolla locale")
        }
    }

    /** Validates business data against the saved catalogue and freezes the current article lot. */
    private suspend fun parseAndValidate(rawLines: String, customer: QuickCustomerEntity): OfflineIssueResult {
        val allowedList: List<String> = gson.fromJson(
            customer.allowedArticleCodesJson,
            object : TypeToken<List<String>>() {}.type
        )
        val allowed: Set<String> = allowedList.map { it.trim() }.toSet()
        val errors = mutableListOf<QuickLineError>()
        val lines = mutableListOf<LocalEmissionLine>()
        rawLines.lines().forEachIndexed { index, original ->
            val trimmed = original.trim()
            if (trimmed.isBlank() || trimmed.startsWith("#")) return@forEachIndexed
            val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (parts.size != 2) {
                errors += QuickLineError(index + 1, "Formato non valido: usa CODICE QUANTITÀ")
                return@forEachIndexed
            }
            val quantity = parts[1].toIntOrNull()
            if (quantity == null || quantity == 0) {
                errors += QuickLineError(index + 1, "Quantità non valida")
                return@forEachIndexed
            }
            val article = dao.article(parts[0])
            when {
                article == null -> errors += QuickLineError(index + 1, "Articolo ${parts[0]} assente dal catalogo offline")
                !allowed.contains(parts[0]) -> errors += QuickLineError(index + 1, "Articolo ${parts[0]} non consentito per questo cliente")
                // Django represents an allowed article without stock loads with the literal placeholder.
                else -> lines += LocalEmissionLine(article.codice, article.description, quantity, article.lastLotto?.takeIf { it.isNotBlank() } ?: "---")
            }
        }
        return when {
            errors.isNotEmpty() -> OfflineIssueResult.ValidationError(errors)
            lines.isEmpty() -> OfflineIssueResult.Error("Inserisci almeno una riga con quantità diversa da zero")
            else -> OfflineIssueResult.Success(IssuedBolla(
                // Temporary placeholder; the definitive entity is created atomically by issue().
                LocalEmissionEntity(
                    emissionId = "", customerId = 0, documentTypeId = 0, documentTypeName = "",
                    number = 0, year = 0, issuedAt = "", customerJson = "", linesJson = "",
                    renderer = "", receiptWidth = 0, receiptText = "", status = ""
                ), lines
            ))
        }
    }

    suspend fun print(emissionId: String, macAddress: String): Result<Unit> {
        val emission = dao.emission(emissionId) ?: return Result.failure(Exception("Bolla locale non trovata"))
        val result = printer.printText(macAddress, emission.receiptText)
        if (result.isSuccess) {
            dao.markPrinted(emissionId, EMISSION_PRINTED_PENDING_SYNC,
                ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        } else {
            dao.markPrintFailed(emissionId, result.exceptionOrNull()?.message ?: "Errore di stampa")
        }
        return result
    }

    suspend fun syncPending(): SyncSummary {
        var synced = 0
        var retryableFailure = false
        dao.pendingSync().forEach { emission ->
            when (syncOne(emission)) {
                SyncOneResult.Synced -> synced++
                SyncOneResult.RetryableFailure -> retryableFailure = true
                SyncOneResult.Conflict -> Unit
            }
        }
        return SyncSummary(synced, retryableFailure)
    }

    private suspend fun syncOne(emission: LocalEmissionEntity): SyncOneResult {
        return try {
            val lines: List<LocalEmissionLine> = gson.fromJson(emission.linesJson,
                object : TypeToken<List<LocalEmissionLine>>() {}.type)
            val response = api.syncQuickBolla(
                QuickSyncRequest(
                    emission.emissionId, emission.customerId, emission.documentTypeId, emission.number,
                    emission.year, emission.issuedAt,
                    lines.map { QuickSyncLine(it.codice, it.quantita, it.lotto) },
                    QuickPrintedReceipt(emission.renderer, emission.receiptWidth, emission.receiptText)
                )
            )
            val body = response.body()
            if (response.isSuccessful && body?.bollaId != null && (body.status == "created" || body.status == "already_synced")) {
                dao.markSynced(emission.emissionId, body.bollaId)
                SyncOneResult.Synced
            } else {
                val error = response.errorBody()?.string()
                val errorBody = error?.let { runCatching { gson.fromJson(it, QuickSyncResponse::class.java) }.getOrNull() }
                if (response.code() == 409) {
                    dao.markConflict(emission.emissionId, errorBody?.code ?: errorBody?.detail ?: "Conflitto di sincronizzazione")
                    SyncOneResult.Conflict
                } else {
                    dao.markSyncFailure(emission.emissionId, errorBody?.detail ?: "Errore sync HTTP ${response.code()}")
                    SyncOneResult.RetryableFailure
                }
            }
        } catch (e: Exception) {
            dao.markSyncFailure(emission.emissionId, e.message ?: "Errore di rete")
            SyncOneResult.RetryableFailure
        }
    }

    fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<QuickBollaSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("quick-bolla-sync", ExistingWorkPolicy.KEEP, request)
    }
}

data class SyncSummary(val synced: Int, val hasRetryableFailure: Boolean)
private enum class SyncOneResult { Synced, RetryableFailure, Conflict }

class QuickBollaSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repository = (applicationContext as BolleApplication).container.offlineQuickBollaRepository
        val summary = repository.syncPending()
        return if (summary.hasRetryableFailure) Result.retry() else Result.success()
    }
}

/** Renderer kept client-side so a reprint uses exactly the text committed before Bluetooth starts. */
object LocalReceiptRenderer {
    fun render(
        issuer: QuickCatalogConcessionario,
        type: QuickDocumentTypeEntity,
        customer: LocalCustomerSnapshot,
        number: Int,
        year: Int,
        issuedAt: String,
        lines: List<LocalEmissionLine>,
        width: Int
    ): String = buildString {
        val separator = "*".repeat(width.coerceAtLeast(16))
        appendLine("[b]$separator[/b]")
        appendLine("[b]${issuer.nome}[/b]")
        appendLine(issuer.indirizzo)
        issuer.partitaIva?.let { appendLine("P.IVA $it") }
        issuer.telefono?.let { appendLine("Tel. $it") }
        appendLine("[b]${type.description} ${type.name} n. $number/$year[/b]")
        appendLine("Data $issuedAt")
        appendLine("Cliente: ${customer.nome}")
        appendLine(customer.via)
        listOfNotNull(customer.cap, customer.citta).joinToString(" ").takeIf { it.isNotBlank() }?.let { appendLine(it) }
        customer.codice?.let { appendLine("Cod. cliente: $it") }
        appendLine("-".repeat(width.coerceAtLeast(16)))
        lines.forEach { line ->
            appendLine("${line.codice} x${line.quantita}")
            appendLine(line.descrizione)
            appendLine("Lotto: ${line.lotto}")
        }
        appendLine("[b]$separator[/b]")
    }
}
