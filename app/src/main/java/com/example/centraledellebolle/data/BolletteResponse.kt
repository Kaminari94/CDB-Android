package com.example.centraledellebolle.data

import com.google.gson.annotations.SerializedName

data class BolletteResponse(
    val results: List<Bolla>
)

data class BollaDetail(
    val id: Int,
    val numero: Int,
    val data: String,
    @SerializedName("cliente_nome") val clienteNome: String,
    @SerializedName("tipo_documento_nome") val tipoDocumentoNome: String,
    val righe: List<Riga>
)

data class Riga(
    val id: Int,
    val codice: String,
    val descrizione: String,
    val quantita: String, // La quantità potrebbe essere un numero, ma la ricevo come stringa
    val lotto: String?
)