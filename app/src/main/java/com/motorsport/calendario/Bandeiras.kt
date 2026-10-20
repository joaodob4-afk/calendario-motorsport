package com.motorsport.calendario

import android.content.Context
import java.util.Locale

/**
 * Único lugar que liga o nome de um país à sua bandeira. Antes, cada tela tinha a sua própria
 * lista (com países diferentes em cada uma). Aceita nomes em português e em inglês, sem
 * diferenciar maiúsculas de minúsculas.
 */
internal object Bandeiras {
    private val ARQUIVOS: Map<String, String> = mapOf(
        "abu dhabi" to "flag_ae",
        "emirados arabes unidos" to "flag_ae",
        "emirados árabes unidos" to "flag_ae",
        "uae" to "flag_ae",
        "united arab emirates" to "flag_ae",
        "austria" to "flag_at",
        "áustria" to "flag_at",
        "australia" to "flag_au",
        "austrália" to "flag_au",
        "azerbaijan" to "flag_az",
        "azerbaijao" to "flag_az",
        "azerbaijão" to "flag_az",
        "belgica" to "flag_be",
        "belgium" to "flag_be",
        "bélgica" to "flag_be",
        "bahrain" to "flag_bh",
        "bahrein" to "flag_bh",
        "brasil" to "flag_br",
        "brazil" to "flag_br",
        "são paulo" to "flag_br",
        "canada" to "flag_ca",
        "canadá" to "flag_ca",
        "switzerland" to "flag_ch",
        "china" to "flag_cn",
        "czech republic" to "flag_cz",
        "czechia" to "flag_cz",
        "república tcheca" to "flag_cz",
        "tchéquia" to "flag_cz",
        "alemanha" to "flag_de",
        "germany" to "flag_de",
        "catalunya" to "flag_es",
        "espanha" to "flag_es",
        "spain" to "flag_es",
        "france" to "flag_fr",
        "frança" to "flag_fr",
        "great britain" to "flag_gb",
        "reino unido" to "flag_gb",
        "uk" to "flag_gb",
        "united kingdom" to "flag_gb",
        "hungary" to "flag_hu",
        "hungria" to "flag_hu",
        "hungría" to "flag_hu",
        "indonesia" to "flag_id",
        "indonésia" to "flag_id",
        "italia" to "flag_it",
        "italy" to "flag_it",
        "itália" to "flag_it",
        "japan" to "flag_jp",
        "japão" to "flag_jp",
        "south korea" to "flag_kr",
        "monaco" to "flag_mc",
        "mônaco" to "flag_mc",
        "mexico" to "flag_mx",
        "méxico" to "flag_mx",
        "malaysia" to "flag_my",
        "malásia" to "flag_my",
        "holanda" to "flag_nl",
        "netherlands" to "flag_nl",
        "paises baixos" to "flag_nl",
        "países baixos" to "flag_nl",
        "portugal" to "flag_pt",
        "catar" to "flag_qa",
        "qatar" to "flag_qa",
        "russia" to "flag_ru",
        "arabia saudita" to "flag_sa",
        "arábia saudita" to "flag_sa",
        "saudi arabia" to "flag_sa",
        "singapore" to "flag_sg",
        "singapura" to "flag_sg",
        "san marino" to "flag_sm",
        "tailândia" to "flag_th",
        "thailand" to "flag_th",
        "turkey" to "flag_tr",
        "estados unidos" to "flag_us",
        "miami" to "flag_us",
        "united states" to "flag_us",
        "usa" to "flag_us",
        "south africa" to "flag_za"
    )

    /** Nome do arquivo (ex.: "flag_br"), ou null se o país não for conhecido. */
    fun arquivo(pais: String): String? = ARQUIVOS[pais.trim().lowercase(Locale.ROOT)]

    /** Id do drawable da bandeira, ou 0 se o país não for conhecido ou o arquivo não existir. */
    fun recurso(context: Context, pais: String): Int {
        val arquivo = arquivo(pais) ?: return 0
        return context.resources.getIdentifier(arquivo, "drawable", context.packageName)
    }

    /** Emoji da bandeira (montado a partir do código de duas letras) ou [padrao]. */
    fun emoji(pais: String, padrao: String): String {
        val codigo = arquivo(pais)?.substringAfter('_')?.uppercase(Locale.ROOT) ?: return padrao
        if (codigo.length != 2 || codigo.any { it !in 'A'..'Z' }) return padrao
        return codigo.map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
    }
}
