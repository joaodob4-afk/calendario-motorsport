package com.motorsport.calendario

/**
 * Nome oficial do autódromo a partir do nome da cidade/etapa usado no app.
 * Se o circuito não estiver na lista, devolve texto vazio.
 */
object Autodromos {

    private val NOMES = mapOf(
        "melbourne" to "Albert Park Circuit",
        "miami" to "Miami International Autodrome",
        "montreal" to "Circuit Gilles-Villeneuve",
        "monte carlo" to "Circuit de Monaco",
        "monaco" to "Circuit de Monaco",
        "barcelona" to "Circuit de Barcelona-Catalunya",
        "spielberg" to "Red Bull Ring",
        "silverstone" to "Silverstone Circuit",
        "spa-francorchamps" to "Circuit de Spa-Francorchamps",
        "budapeste" to "Hungaroring",
        "budapest" to "Hungaroring",
        "monza" to "Autodromo Nazionale Monza",
        "madrid" to "Madring",
        "baku city circuit" to "Baku City Circuit",
        "baku" to "Baku City Circuit",
        "lusail" to "Lusail International Circuit",
        "yas marina" to "Yas Marina Circuit",
        "sakhir" to "Bahrain International Circuit"
    )

    fun nome(circuito: String): String {
        return NOMES[circuito.trim().lowercase()] ?: ""
    }
}