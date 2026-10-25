package com.motorsport.calendario

import android.content.Intent

internal data class MotoSession(
    val nome: String,
    val data: String,
    val horario: String
)

internal data class MotoEvent(
    val etapa: Int,
    val circuito: String,
    val pais: String,
    val inicio: String,
    val fim: String,
    val sessoes: List<MotoSession>,
    /** Nome oficial do evento vindo do JSON; vazio usa a tabela [NomesDeEventos]. */
    val nome: String = ""
)

/**
 * Campeonatos que usam as mesmas telas e o mesmo formato de JSON (MotoGP, Moto2, Moto3 e WEC);
 * só mudam o nome e os arquivos. Para incluir outro no futuro, basta uma linha aqui, uma em
 * [Categorias] e o arquivo de dados.
 */
internal enum class ClasseMoto(
    /** Nome da categoria no app (igual ao campo "categoria" do JSON e a [Categorias]). */
    val rotulo: String,
    val tituloGrande: String,
    val arquivoAsset: String,
    val arquivoCache: String
) {
    MOTOGP("MotoGP", "MOTOGP", "motogp_calendar.json", "motogp_calendar_cache.json"),
    MOTO2("Moto2", "MOTO2", "moto2_calendar.json", "moto2_calendar_cache.json"),
    MOTO3("Moto3", "MOTO3", "moto3_calendar.json", "moto3_calendar_cache.json"),
    WEC("WEC", "WEC", "wec_calendar.json", "wec_calendar_cache.json");

    companion object {
        /** Nome do extra da Intent que informa a classe às telas. */
        const val EXTRA = "CLASSE"

        fun porRotulo(rotulo: String?): ClasseMoto? =
            values().firstOrNull {
                Formatacao.normalizar(it.rotulo) == Formatacao.normalizar(rotulo.orEmpty())
            }

        fun doIntent(intent: Intent): ClasseMoto =
            porRotulo(intent.getStringExtra(EXTRA)) ?: MOTOGP
    }
}
