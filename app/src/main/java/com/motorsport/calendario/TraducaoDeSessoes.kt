package com.motorsport.calendario

/**
 * Tradução dos nomes de sessões para os dois idiomas.
 *
 * Os calendários podem fornecer nomes em português ou em inglês, dependendo da categoria
 * e da fonte. Por isso, a tradução é bidirecional e acontece somente na apresentação:
 * a lógica e os dados originais permanecem intactos.
 */
internal object TraducaoDeSessoes {

    // Ordem importa: expressões mais longas vêm antes das palavras soltas.
    private val trocas = listOf(
        "Classificação Sprint" to "Sprint Qualifying",
        "Corrida de Abertura" to "Opening Race",
        "Corrida Grid Invertido" to "Reverse Grid Race",
        "Corrida Sprint" to "Sprint Race",
        "Corrida Feature" to "Feature Race",
        "Competição de Pit Stop" to "Pit Stop Competition",
        "Treino Livre" to "Free Practice",
        "Treino Final" to "Final Practice",
        "Classificação" to "Qualifying",
        "Corrida" to "Race",
        "Rodada" to "Round",
        "Treino" to "Practice"
    )

    // Nomes adicionais encontrados em calendários internacionais.
    private val nomesEmIngles = listOf(
        "Warm-up" to "Aquecimento",
        "Warm Up" to "Aquecimento",
        "Pit Stop Competition" to "Competição de Pit Stop",
        "Reverse Grid Race" to "Corrida Grid Invertido",
        "Opening Race" to "Corrida de Abertura",
        "Feature Race" to "Corrida Feature",
        "Sprint Qualifying" to "Classificação Sprint",
        "Sprint Race" to "Corrida Sprint",
        "Final Practice" to "Treino Final",
        "Free Practice" to "Treino Livre",
        "Qualifying" to "Classificação",
        "Practice" to "Treino",
        "Race" to "Corrida",
        "Round" to "Rodada"
    )

    fun paraIngles(nome: String): String =
        trocas.fold(nome) { texto, (de, para) -> texto.replace(de, para) }

    fun paraPortugues(nome: String): String =
        nomesEmIngles.fold(nome) { texto, (de, para) ->
            texto.replace(de, para, ignoreCase = true)
        }
}
