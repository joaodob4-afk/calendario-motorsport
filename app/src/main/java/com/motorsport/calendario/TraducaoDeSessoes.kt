package com.motorsport.calendario

/**
 * Os nomes das sessões vêm dos calendários em português. Para quem usa o app em inglês,
 * esta tradução é aplicada só na hora de mostrar o texto. Nomes desconhecidos (Hyperpole,
 * Warm-up, Practice...) ficam como estão. A lógica do app continua usando o nome original.
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

    fun paraIngles(nome: String): String =
        trocas.fold(nome) { texto, (de, para) -> texto.replace(de, para) }
}