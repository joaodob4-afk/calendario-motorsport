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
        "sakhir" to "Bahrain International Circuit",
        "buriram" to "Chang International Circuit",
        "goiânia" to "Autódromo Internacional de Goiânia – Ayrton Senna",
        "goiania" to "Autódromo Internacional de Goiânia – Ayrton Senna",
        "austin" to "Circuit of The Americas",
        "jerez" to "Circuito de Jerez – Ángel Nieto",
        "le mans" to "Bugatti Circuit",
        "mugello" to "Autodromo Internazionale del Mugello",
        "balaton park" to "Balaton Park",
        "brno" to "Autodrom Brno",
        "assen" to "TT Circuit Assen",
        "sachsenring" to "Sachsenring",
        "aragón" to "MotorLand Aragón",
        "aragon" to "MotorLand Aragón",
        "misano" to "Misano World Circuit Marco Simoncelli",
        "motegi" to "Mobility Resort Motegi",
        "mandalika" to "Pertamina Mandalika International Circuit",
        "phillip island" to "Phillip Island",
        "sepang" to "Petronas Sepang International Circuit",
        "portimão" to "Autódromo Internacional do Algarve",
        "portimao" to "Autódromo Internacional do Algarve",
        "valencia" to "Circuit Ricardo Tormo"
    )

    fun nome(circuito: String): String {
        return NOMES[circuito.trim().lowercase()] ?: ""
    }
}