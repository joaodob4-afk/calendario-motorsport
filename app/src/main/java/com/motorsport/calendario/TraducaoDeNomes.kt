package com.motorsport.calendario

/**
 * Tradução para inglês dos nomes de eventos ([NomesDeEventos] está em português).
 * Só é usada na hora de mostrar o texto; a lógica do app continua com os nomes originais.
 * Nomes desconhecidos (por exemplo os da IndyCar, que já vêm em inglês) ficam como estão.
 */
internal object TraducaoDeNomes {

    private val eventos = mapOf(
        // Fórmula 1, F2 e F3
        "GP da Austrália" to "Australian Grand Prix",
        "GP da China" to "Chinese Grand Prix",
        "GP do Japão" to "Japanese Grand Prix",
        "GP do Bahrein" to "Bahrain Grand Prix",
        "GP do Bahrein na Malásia" to "Bahrain Grand Prix in Malaysia",
        "GP da Arábia Saudita" to "Saudi Arabian Grand Prix",
        "GP de Miami" to "Miami Grand Prix",
        "GP do Canadá" to "Canadian Grand Prix",
        "GP de Mônaco" to "Monaco Grand Prix",
        "GP de Barcelona-Catalunha" to "Barcelona-Catalunya Grand Prix",
        "GP da Áustria" to "Austrian Grand Prix",
        "GP da Grã-Bretanha" to "British Grand Prix",
        "GP da Bélgica" to "Belgian Grand Prix",
        "GP da Hungria" to "Hungarian Grand Prix",
        "GP da Holanda" to "Dutch Grand Prix",
        "GP da Itália" to "Italian Grand Prix",
        "GP da Espanha" to "Spanish Grand Prix",
        "GP do Azerbaijão" to "Azerbaijan Grand Prix",
        "GP de Singapura" to "Singapore Grand Prix",
        "GP dos Estados Unidos" to "United States Grand Prix",
        "GP da Cidade do México" to "Mexico City Grand Prix",
        "GP de São Paulo" to "São Paulo Grand Prix",
        "GP de Las Vegas" to "Las Vegas Grand Prix",
        "GP do Catar" to "Qatar Grand Prix",
        "GP de Abu Dhabi" to "Abu Dhabi Grand Prix",
        // MotoGP, Moto2 e Moto3
        "GP da Tailândia" to "Thai Grand Prix",
        "GP do Brasil" to "Brazilian Grand Prix",
        "GP da França" to "French Grand Prix",
        "GP da Catalunha" to "Catalan Grand Prix",
        "GP da Tchéquia" to "Czech Grand Prix",
        "GP da Alemanha" to "German Grand Prix",
        "GP de Aragão" to "Aragon Grand Prix",
        "GP de San Marino" to "San Marino Grand Prix",
        "GP da Indonésia" to "Indonesian Grand Prix",
        "GP da Malásia" to "Malaysian Grand Prix",
        "GP de Portugal" to "Portuguese Grand Prix",
        "GP de Valência" to "Valencia Grand Prix",
        // WEC
        "6 Horas de Imola" to "6 Hours of Imola",
        "6 Horas de Spa-Francorchamps" to "6 Hours of Spa-Francorchamps",
        "24 Horas de Le Mans" to "24 Hours of Le Mans",
        "6 Horas de São Paulo" to "6 Hours of São Paulo",
        "6 Horas de Fuji" to "6 Hours of Fuji",
        "6 Horas de Barcelona" to "6 Hours of Barcelona",
        "6 Horas de Monza" to "6 Hours of Monza",
        "1812 km do Catar" to "1812 km of Qatar",
        "6 Horas de Silverstone" to "6 Hours of Silverstone",
        "8 Horas do Bahrein" to "8 Hours of Bahrain"
    )

    /** Países e cidades cujo nome em português é diferente do inglês. */
    private val lugares = mapOf(
        // Países
        "Alemanha" to "Germany",
        "Arábia Saudita" to "Saudi Arabia",
        "Austrália" to "Australia",
        "Azerbaijão" to "Azerbaijan",
        "Brasil" to "Brazil",
        "Bélgica" to "Belgium",
        "Canadá" to "Canada",
        "Catar" to "Qatar",
        "Emirados Árabes Unidos" to "United Arab Emirates",
        "Espanha" to "Spain",
        "Estados Unidos" to "United States",
        "França" to "France",
        "Holanda" to "Netherlands",
        "Hungria" to "Hungary",
        "Indonésia" to "Indonesia",
        "Itália" to "Italy",
        "Japão" to "Japan",
        "Malásia" to "Malaysia",
        "México" to "Mexico",
        "Mônaco" to "Monaco",
        "Reino Unido" to "United Kingdom",
        "República Tcheca" to "Czech Republic",
        "Singapura" to "Singapore",
        "Tailândia" to "Thailand",
        "Áustria" to "Austria",
        "Bahrein" to "Bahrain",
        // Cidades
        "Berlim" to "Berlin",
        "Budapeste" to "Budapest",
        "Cidade do México" to "Mexico City",
        "Goiânia" to "Goiania",
        "Portimão" to "Portimao",
        "Tóquio" to "Tokyo",
        "Xangai" to "Shanghai"
    )

    private val eprix = Regex("^E-Prix d[aeo] (.+)$")

    fun evento(nome: String): String {
        eventos[nome]?.let { return it }

        eprix.matchEntire(nome.trim())?.let {
            val cidade = it.groupValues[1]
            return "${lugar(cidade)} E-Prix"
        }

        return nome
    }

    /** País ou cidade em inglês; nomes que já são iguais ou desconhecidos ficam como estão. */
    fun lugar(nome: String): String = lugares[nome.trim()] ?: nome
}
