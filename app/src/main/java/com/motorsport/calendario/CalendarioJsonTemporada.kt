package com.motorsport.calendario

/**
 * Evita que um cache de uma temporada anterior esconda o calendário atualizado do APK.
 * Se o cache for da mesma temporada ou de uma mais nova, ele continua sendo priorizado.
 */
internal object CalendarioJsonTemporada {
    private val anoNoJson = Regex("\"temporada\"\\s*:\\s*\"?(\\d{4})")

    fun selecionar(
        cache: String?,
        asset: String?,
        permitirCacheDeTemporadaMaisNova: Boolean = true
    ): String? {
        if (cache.isNullOrBlank()) return asset
        if (asset.isNullOrBlank()) return cache

        val anoCache = anoNoJson.find(cache)?.groupValues?.get(1)?.toIntOrNull()
        val anoAsset = anoNoJson.find(asset)?.groupValues?.get(1)?.toIntOrNull()

        return when {
            anoCache == null -> asset
            anoAsset == null -> cache
            anoCache > anoAsset && !permitirCacheDeTemporadaMaisNova -> asset
            anoCache >= anoAsset -> cache
            else -> asset
        }
    }
}
