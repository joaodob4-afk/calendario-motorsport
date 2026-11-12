package com.motorsport.calendario

/**
 * Escolhe entre o cache baixado e o JSON embutido no APK, valendo sempre o mais recente:
 * - cache de temporada anterior à do APK é ignorado (não esconde o calendário do APK);
 * - cache da mesma temporada ou de uma mais nova é usado, para a atualização online
 *   funcionar também na virada de temporada;
 * - exceção: se o cache é da mesma temporada mas foi baixado ANTES da última instalação/atualização
 *   do APK, o JSON do APK (mais novo) vence até um novo download.
 */
internal object CalendarioJsonTemporada {
    private val temporadaNoJson =
        Regex("\"temporada\"\\s*:\\s*\"?(\\d{4})(?:/(\\d{2,4}))?")

    /**
     * Usa o ano final como referência para temporadas que atravessam dois anos:
     * "2026/27" equivale a 2027 e "2027/28" equivale a 2028.
     * Calendários anuais, como "2027", continuam equivalendo ao próprio ano.
     */
    private fun anoDaTemporada(json: String): Int? {
        val grupos = temporadaNoJson.find(json)?.groupValues ?: return null
        val anoInicial = grupos.getOrNull(1)?.toIntOrNull() ?: return null
        val sufixo = grupos.getOrNull(2).orEmpty()
        if (sufixo.isBlank()) return anoInicial

        val anoFinal = if (sufixo.length == 2) {
            (anoInicial / 100) * 100 + (sufixo.toIntOrNull() ?: return null)
        } else {
            sufixo.toIntOrNull() ?: return null
        }
        return if (anoFinal < anoInicial) anoFinal + 100 else anoFinal
    }

    fun selecionar(cache: String?, asset: String?, cacheAnteriorAoApk: Boolean = false): String? {
        if (cache.isNullOrBlank()) return asset
        if (asset.isNullOrBlank()) return cache

        val anoCache = anoDaTemporada(cache)
        val anoAsset = anoDaTemporada(asset)

        return when {
            anoCache == null -> asset
            anoAsset == null -> cache
            anoCache == anoAsset && cacheAnteriorAoApk -> asset
            anoCache >= anoAsset -> cache
            else -> asset
        }
    }
}
