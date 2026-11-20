package com.example.centraledellebolle.data

import com.google.gson.annotations.SerializedName

data class QuickCatalogResponse(
    @SerializedName("schema_version") val schemaVersion: Int,
    @SerializedName("generated_at") val generatedAt: String,
    val year: Int,
    val printer: QuickCatalogPrinter,
    val concessionario: QuickCatalogConcessionario,
    val types: List<QuickCatalogDocumentType>,
    val customers: List<QuickCatalogCustomer>,
    val articles: List<QuickCatalogArticle>
)

data class QuickCatalogPrinter(val renderer: String, val width: Int, val encoding: String)

data class QuickCatalogConcessionario(
    val id: Int,
    val nome: String,
    val indirizzo: String,
    @SerializedName("partita_iva") val partitaIva: String?,
    val telefono: String?,
    @SerializedName("consegna_per_conto_di") val consegnaPerContoDi: String?
)

data class QuickCatalogDocumentType(
    val id: Int,
    val nome: String,
    val descrizione: String,
    @SerializedName("next_number") val nextNumber: Int
)

data class QuickCatalogOwner(val nome: String?, val codice: String?)

data class QuickCatalogCustomer(
    val id: Int,
    val nome: String,
    val via: String,
    val cap: String?,
    val citta: String?,
    val piva: String?,
    val codice: String?,
    @SerializedName("tipo_documento_id") val documentTypeId: Int,
    val proprietario: QuickCatalogOwner?,
    @SerializedName("articoli_consentiti") val allowedArticles: List<String>
)

data class QuickCatalogArticle(
    val id: Int,
    val codice: String,
    val descrizione: String,
    @SerializedName("ultimo_lotto") val lastLotto: String?
)

data class QuickSyncLine(val codice: String, val quantita: Int, val lotto: String)
data class QuickPrintedReceipt(val renderer: String, val width: Int, val text: String)

data class QuickSyncRequest(
    @SerializedName("emission_id") val emissionId: String,
    @SerializedName("customer_id") val customerId: Int,
    @SerializedName("tipo_documento_id") val documentTypeId: Int,
    val numero: Int,
    @SerializedName("anno_emissione") val year: Int,
    @SerializedName("data_emissione") val issuedAt: String,
    val righe: List<QuickSyncLine>,
    @SerializedName("printed_receipt") val printedReceipt: QuickPrintedReceipt
)

data class QuickSyncResponse(
    val status: String?,
    @SerializedName("bolla_id") val bollaId: Int?,
    @SerializedName("emission_id") val emissionId: String?,
    val numero: Int?,
    @SerializedName("tipo_documento_id") val documentTypeId: Int?,
    val code: String?,
    val detail: String?
)
