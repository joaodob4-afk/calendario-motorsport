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
    private val anoNoJson = Regex("\"temporada\"\\s*:\\s*\"?(\\d{4})")

    fun selecionar(cache: String?, asset: String?, cacheAnteriorAoApk: Boolean = false): String? {
        if (cache.isNullOrBlank()) return asset
        if (asset.isNullOrBlank()) return cache

        val anoCache = anoNoJson.find(cache)?.groupValues?.get(1)?.toIntOrNull()
        val anoAsset = anoNoJson.find(asset)?.groupValues?.get(1)?.toIntOrNull()

        return when {
            anoCache == null -> asset
            anoAsset == null -> cache
            anoCache == anoAsset && cacheAnteriorAoApk -> asset
            anoCache >= anoAsset -> cache
            else -> asset
        }
    }
}
