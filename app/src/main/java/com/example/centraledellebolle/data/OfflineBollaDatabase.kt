package com.example.centraledellebolle.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Local, append-only archive for documents issued by this device.  An issued
 * document is deliberately independent from the Django id: it can be printed
 * and restored before it has ever reached the server.
 */
@Entity(tableName = "quick_catalog_meta")
data class QuickCatalogMetaEntity(
    @PrimaryKey val id: Int = 0,
    val schemaVersion: Int,
    val generatedAt: String,
    val year: Int,
    val renderer: String,
    val receiptWidth: Int,
    val encoding: String,
    val concessionarioJson: String
)

@Entity(tableName = "quick_document_types")
data class QuickDocumentTypeEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val nextNumberFromServer: Int
)

@Entity(tableName = "quick_customers")
data class QuickCustomerEntity(
    @PrimaryKey val id: Int,
    val nome: String,
    val via: String,
    val cap: String?,
    val citta: String?,
    val piva: String?,
    val codice: String?,
    val defaultDocumentTypeId: Int,
    val ownerName: String?,
    val ownerCode: String?,
    /** JSON array: kept as a snapshot of the server catalogue. */
    val allowedArticleCodesJson: String
)

@Entity(tableName = "quick_articles")
data class QuickArticleEntity(
    @PrimaryKey val codice: String,
    val remoteId: Int,
    val description: String,
    val lastLotto: String?
)

@Entity(tableName = "quick_document_sequences", primaryKeys = ["documentTypeId", "year"])
data class QuickDocumentSequenceEntity(
    val documentTypeId: Int,
    val year: Int,
    /** The next number available to this device. Numbers are never rolled back. */
    val nextLocal: Int
)

@Entity(tableName = "quick_local_emissions")
data class LocalEmissionEntity(
    @PrimaryKey val emissionId: String,
    val customerId: Int,
    val documentTypeId: Int,
    val documentTypeName: String,
    val number: Int,
    val year: Int,
    val issuedAt: String,
    /** Frozen customer snapshot, so reprints never depend on a changed catalogue. */
    val customerJson: String,
    /** Frozen lines including the lot selected at issuance. */
    val linesJson: String,
    val renderer: String,
    val receiptWidth: Int,
    /** Exact receipt bytes source (UTF-8 text) used for first print and every reprint. */
    val receiptText: String,
    val status: String,
    val printedAt: String? = null,
    val printCount: Int = 0,
    val djangoBollaId: Int? = null,
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null
)

@Dao
interface QuickBollaDao {
    @Query("SELECT * FROM quick_catalog_meta WHERE id = 0")
    suspend fun catalogMeta(): QuickCatalogMetaEntity?

    @Query("SELECT * FROM quick_catalog_meta WHERE id = 0")
    fun catalogMetaFlow(): Flow<QuickCatalogMetaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCatalogMeta(value: QuickCatalogMetaEntity)

    @Query("DELETE FROM quick_document_types")
    suspend fun clearTypes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putTypes(values: List<QuickDocumentTypeEntity>)

    @Query("SELECT * FROM quick_document_types ORDER BY name")
    suspend fun documentTypes(): List<QuickDocumentTypeEntity>

    @Query("SELECT * FROM quick_document_types ORDER BY name")
    fun documentTypesFlow(): Flow<List<QuickDocumentTypeEntity>>

    @Query("DELETE FROM quick_customers")
    suspend fun clearCustomers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCustomers(values: List<QuickCustomerEntity>)

    @Query("SELECT * FROM quick_customers ORDER BY codice, nome")
    suspend fun customers(): List<QuickCustomerEntity>

    @Query("SELECT * FROM quick_customers ORDER BY codice, nome")
    fun customersFlow(): Flow<List<QuickCustomerEntity>>

    @Query("DELETE FROM quick_articles")
    suspend fun clearArticles()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putArticles(values: List<QuickArticleEntity>)

    @Query("SELECT * FROM quick_articles WHERE codice = :code")
    suspend fun article(code: String): QuickArticleEntity?

    @Query("SELECT * FROM quick_document_sequences WHERE documentTypeId = :typeId AND year = :year")
    suspend fun sequence(typeId: Int, year: Int): QuickDocumentSequenceEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSequence(value: QuickDocumentSequenceEntity)

    @Query("UPDATE quick_document_sequences SET nextLocal = CASE WHEN nextLocal < :candidate THEN :candidate ELSE nextLocal END WHERE documentTypeId = :typeId AND year = :year")
    suspend fun advanceSequenceAtLeast(typeId: Int, year: Int, candidate: Int)

    @Query("UPDATE quick_document_sequences SET nextLocal = :next WHERE documentTypeId = :typeId AND year = :year")
    suspend fun setNextSequence(typeId: Int, year: Int, next: Int)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEmission(value: LocalEmissionEntity)

    @Query("SELECT * FROM quick_local_emissions WHERE emissionId = :emissionId")
    suspend fun emission(emissionId: String): LocalEmissionEntity?

    @Query("SELECT * FROM quick_local_emissions WHERE status = 'PRINTED_PENDING_SYNC' ORDER BY issuedAt")
    suspend fun pendingSync(): List<LocalEmissionEntity>

    @Query("SELECT * FROM quick_local_emissions WHERE status = 'CONFLICT' ORDER BY issuedAt DESC")
    fun conflictsFlow(): Flow<List<LocalEmissionEntity>>

    @Query("SELECT * FROM quick_local_emissions ORDER BY issuedAt DESC")
    fun emissionsFlow(): Flow<List<LocalEmissionEntity>>

    @Query("SELECT COUNT(*) FROM quick_local_emissions WHERE status = 'PRINTED_PENDING_SYNC'")
    fun pendingCountFlow(): Flow<Int>

    @Query("UPDATE quick_local_emissions SET status = CASE WHEN status IN ('SYNCED', 'CONFLICT') THEN status ELSE :status END, printedAt = :printedAt, printCount = printCount + 1 WHERE emissionId = :emissionId")
    suspend fun markPrinted(emissionId: String, status: String, printedAt: String)

    @Query("UPDATE quick_local_emissions SET status = 'PRINT_FAILED', lastSyncError = :error WHERE emissionId = :emissionId")
    suspend fun markPrintFailed(emissionId: String, error: String)

    @Query("UPDATE quick_local_emissions SET status = 'SYNCED', djangoBollaId = :djangoId, lastSyncError = NULL WHERE emissionId = :emissionId")
    suspend fun markSynced(emissionId: String, djangoId: Int)

    @Query("UPDATE quick_local_emissions SET status = 'CONFLICT', syncAttempts = syncAttempts + 1, lastSyncError = :error WHERE emissionId = :emissionId")
    suspend fun markConflict(emissionId: String, error: String)

    @Query("UPDATE quick_local_emissions SET syncAttempts = syncAttempts + 1, lastSyncError = :error WHERE emissionId = :emissionId")
    suspend fun markSyncFailure(emissionId: String, error: String)
}

@Database(
    entities = [
        QuickCatalogMetaEntity::class,
        QuickDocumentTypeEntity::class,
        QuickCustomerEntity::class,
        QuickArticleEntity::class,
        QuickDocumentSequenceEntity::class,
        LocalEmissionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class OfflineBollaDatabase : RoomDatabase() {
    abstract fun quickBollaDao(): QuickBollaDao
}
