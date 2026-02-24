package com.example.centraledellebolle.data

import com.google.gson.annotations.SerializedName

data class Bolla(
    val id: Long,
    val numero: Int,
    val data: String,
    @SerializedName("cliente_nome")
    val clienteNome: String,
    @SerializedName("cliente_via")
    val clienteVia: String,
    @SerializedName("tipo_documento_nome")
    val tipoDocumentoNome: String
)