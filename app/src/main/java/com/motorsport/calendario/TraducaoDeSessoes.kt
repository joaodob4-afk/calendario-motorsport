package com.motorsport.calendario

/**
 * Tradução dos nomes de sessões para os dois idiomas.
 *
 * Os calendários podem fornecer nomes em português ou em inglês, dependendo da categoria
 * e da fonte. A tradução acontece somente na apresentação; os dados originais permanecem
 * intactos. O idioma de destino é decidido uma única vez por Idioma, conforme os recursos
 * localizados pelo Android para o idioma do aparelho.
 */
internal object TraducaoDeSessoes {

    // Expressões compostas precisam vir antes dos termos genéricos.
    private val paraIngles = listOf(
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

    private val paraPortugues = listOf(
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

    /**
     * Substitui termos completos, sem alterar palavras maiores como "Pre-Race".
     * A ordenação preserva expressões específicas antes dos nomes genéricos.
     */
    private fun substituir(nome: String, regras: List<Pair<String, String>>): String =
        regras.fold(nome) { texto, (origem, destino) ->
            val termo = Regex(
                "(?<![\\p{L}\\p{N}-])${Regex.escape(origem)}(?![\\p{L}\\p{N}-])",
                RegexOption.IGNORE_CASE
            )
            termo.replace(texto, destino)
        }

    fun paraIngles(nome: String): String = substituir(nome, paraIngles)

    fun paraPortugues(nome: String): String = substituir(nome, paraPortugues)
}
