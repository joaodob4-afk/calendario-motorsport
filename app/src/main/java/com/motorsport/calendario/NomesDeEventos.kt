package com.motorsport.calendario

/**
 * Nome (em português) de cada evento, mostrado na tela inicial e nos detalhes.
 *
 * Os nomes ficam todos aqui, num lugar só: para trocar um nome, ou para escrever em inglês,
 * basta mexer neste arquivo. A busca é feita pelo nome do circuito/cidade que já aparece no
 * app (sem acento e sem diferença de maiúsculas). Quando não há nome conhecido, as telas
 * mostram o país no lugar, como antes. A IndyCar não está aqui porque o próprio JSON dela
 * já traz o nome oficial de cada prova.
 */
internal object NomesDeEventos {

    /** F1: pelo identificador do circuito na API, que não muda de ano para ano. */
    private val formula1PorCircuito = mapOf(
        "albert_park" to "GP da Austrália",
        "shanghai" to "GP da China",
        "suzuka" to "GP do Japão",
        "bahrain" to "GP do Bahrein",
        "jeddah" to "GP da Arábia Saudita",
        "miami" to "GP de Miami",
        "villeneuve" to "GP do Canadá",
        "monaco" to "GP de Mônaco",
        "catalunya" to "GP de Barcelona-Catalunha",
        "red_bull_ring" to "GP da Áustria",
        "silverstone" to "GP da Grã-Bretanha",
        "spa" to "GP da Bélgica",
        "hungaroring" to "GP da Hungria",
        "zandvoort" to "GP da Holanda",
        "monza" to "GP da Itália",
        "madring" to "GP da Espanha",
        "baku" to "GP do Azerbaijão",
        "marina_bay" to "GP de Singapura",
        "americas" to "GP dos Estados Unidos",
        "rodriguez" to "GP da Cidade do México",
        "interlagos" to "GP de São Paulo",
        "vegas" to "GP de Las Vegas",
        "losail" to "GP do Catar",
        "yas_marina" to "GP de Abu Dhabi"
    )

    /** F1, F2 e F3 (as categorias de apoio correm no mesmo fim de semana), pela cidade. */
    private val formulaPorCidade = mapOf(
        "melbourne" to "GP da Austrália",
        "xangai" to "GP da China",
        "shanghai" to "GP da China",
        "suzuka" to "GP do Japão",
        "sakhir" to "GP do Bahrein",
        "jeddah" to "GP da Arábia Saudita",
        "miami" to "GP de Miami",
        "montreal" to "GP do Canadá",
        "monte carlo" to "GP de Mônaco",
        "barcelona" to "GP de Barcelona-Catalunha",
        "spielberg" to "GP da Áustria",
        "silverstone" to "GP da Grã-Bretanha",
        "spa-francorchamps" to "GP da Bélgica",
        "budapeste" to "GP da Hungria",
        "zandvoort" to "GP da Holanda",
        "monza" to "GP da Itália",
        "madrid" to "GP da Espanha",
        "baku" to "GP do Azerbaijão",
        "baku city circuit" to "GP do Azerbaijão",
        "marina bay" to "GP de Singapura",
        "austin" to "GP dos Estados Unidos",
        "cidade do mexico" to "GP da Cidade do México",
        "interlagos" to "GP de São Paulo",
        "las vegas" to "GP de Las Vegas",
        "lusail" to "GP do Catar",
        "yas marina" to "GP de Abu Dhabi",
        // Nomes de cidade como a API da F1 também escreve (em inglês ou com outra grafia)
        "mexico city" to "GP da Cidade do México",
        "ciudad de mexico" to "GP da Cidade do México",
        "sao paulo" to "GP de São Paulo",
        "singapore" to "GP de Singapura",
        "budapest" to "GP da Hungria",
        "monaco" to "GP de Mônaco",
        "monte-carlo" to "GP de Mônaco",
        "abu dhabi" to "GP de Abu Dhabi",
        "yas island" to "GP de Abu Dhabi",
        "miami gardens" to "GP de Miami",
        "lusail city" to "GP do Catar",
        "doha" to "GP do Catar",
        "spa" to "GP da Bélgica"
    )

    /**
     * Último recurso da F1, quando nem o circuito nem a cidade são conhecidos: o país, só nos
     * casos em que ele tem uma única corrida (EUA e Espanha têm mais de uma, ficam de fora).
     * Aceita o país em português ou em inglês.
     */
    private val formula1PorPais = mapOf(
        "mexico" to "GP da Cidade do México",
        "brasil" to "GP de São Paulo", "brazil" to "GP de São Paulo",
        "australia" to "GP da Austrália",
        "china" to "GP da China",
        "japao" to "GP do Japão", "japan" to "GP do Japão",
        "bahrein" to "GP do Bahrein", "bahrain" to "GP do Bahrein",
        "arabia saudita" to "GP da Arábia Saudita", "saudi arabia" to "GP da Arábia Saudita",
        "canada" to "GP do Canadá",
        "monaco" to "GP de Mônaco",
        "austria" to "GP da Áustria",
        "reino unido" to "GP da Grã-Bretanha", "uk" to "GP da Grã-Bretanha",
        "united kingdom" to "GP da Grã-Bretanha", "great britain" to "GP da Grã-Bretanha",
        "belgica" to "GP da Bélgica", "belgium" to "GP da Bélgica",
        "hungria" to "GP da Hungria", "hungary" to "GP da Hungria",
        "holanda" to "GP da Holanda", "netherlands" to "GP da Holanda",
        "italia" to "GP da Itália", "italy" to "GP da Itália",
        "azerbaijao" to "GP do Azerbaijão", "azerbaijan" to "GP do Azerbaijão",
        "singapura" to "GP de Singapura", "singapore" to "GP de Singapura",
        "catar" to "GP do Catar", "qatar" to "GP do Catar",
        "abu dhabi" to "GP de Abu Dhabi", "uae" to "GP de Abu Dhabi",
        "emirados arabes unidos" to "GP de Abu Dhabi",
        "united arab emirates" to "GP de Abu Dhabi"
    )

    /** MotoGP, Moto2 e Moto3 correm juntas, então usam os mesmos nomes. */
    private val motos = mapOf(
        "buriram" to "GP da Tailândia",
        "goiania" to "GP do Brasil",
        "austin" to "GP dos Estados Unidos",
        "jerez" to "GP da Espanha",
        "le mans" to "GP da França",
        "barcelona" to "GP da Catalunha",
        "mugello" to "GP da Itália",
        "balaton park" to "GP da Hungria",
        "brno" to "GP da Tchéquia",
        "assen" to "GP da Holanda",
        "sachsenring" to "GP da Alemanha",
        "silverstone" to "GP da Grã-Bretanha",
        "aragon" to "GP de Aragão",
        "misano" to "GP de San Marino",
        "spielberg" to "GP da Áustria",
        "motegi" to "GP do Japão",
        "mandalika" to "GP da Indonésia",
        "phillip island" to "GP da Austrália",
        "sepang" to "GP da Malásia",
        "lusail" to "GP do Catar",
        "portimao" to "GP de Portugal",
        "valencia" to "GP de Valência"
    )

    private val wec = mapOf(
        "imola" to "6 Horas de Imola",
        "spa-francorchamps" to "6 Horas de Spa-Francorchamps",
        "circuit de la sarthe" to "24 Horas de Le Mans",
        "interlagos" to "6 Horas de São Paulo",
        "austin" to "Lone Star Le Mans",
        "fuji" to "6 Horas de Fuji",
        "barcelona" to "6 Horas de Barcelona",
        "monza" to "6 Horas de Monza",
        "lusail" to "1812 km do Catar",
        "silverstone" to "6 Horas de Silverstone",
        "sakhir" to "8 Horas do Bahrein"
    )

    /**
     * Nome do evento, ou null quando não há um conhecido. [circuitId] e [pais] só valem para a
     * F1 (os dados dela vêm da API, que pode trazer circuitos e cidades que ainda não conhecemos).
     */
    fun nome(
        categoria: String,
        circuito: String,
        circuitId: String = "",
        pais: String = ""
    ): String? {
        val cidade = Formatacao.normalizar(circuito)

        return when (Formatacao.normalizar(categoria)) {
            "f1" -> formula1PorCircuito[circuitId.trim().lowercase()]
                ?: formulaPorCidade[cidade]
                ?: formula1PorPais[Formatacao.normalizar(pais)]
            "f2", "f3" -> formulaPorCidade[cidade]
            "formula e" -> eprix(circuito)
            "motogp", "moto2", "moto3" -> motos[cidade]
            "wec" -> wec[cidade]
            else -> null
        }
    }

    /** "E-Prix de Berlim", "E-Prix da Cidade do México". */
    private fun eprix(cidade: String): String? {
        val nome = cidade.trim()
        if (nome.isBlank()) return null

        val preposicao = if (nome.startsWith("Cidade ", ignoreCase = true)) "da" else "de"
        return "E-Prix $preposicao $nome"
    }
}
