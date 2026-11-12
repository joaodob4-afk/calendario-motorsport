package com.motorsport.calendario

internal data class CategoriaInfo(
    val nome: String,
    val cor: String,
    /** Texto curto para o selo dos cards (opcional), útil para nomes muito longos. */
    val abreviacao: String? = null
)

/**
 * Lista central de categorias. Para adicionar uma nova, basta incluir uma linha em [todas]:
 * o botão do topo, o selo dos cards e a cor passam a existir automaticamente.
 * O nome deve ser igual ao campo "categoria" dos dados (maiúsculas e acentos são ignorados).
 */
internal object Categorias {

    /** Verde neon do app, usado por categorias que ainda não estão na lista. */
    const val COR_PADRAO = "#39FF14"

    val todas: List<CategoriaInfo> = listOf(
        CategoriaInfo("F1", "#FF3B30"),
        CategoriaInfo("F2", "#26A8FF"),
        CategoriaInfo("F3", "#F28124"),
        CategoriaInfo("F1 Academy", "#FF73FF"),
        CategoriaInfo("IndyCar", "#B891F2"),
        CategoriaInfo("Formula E", "#22D3EE"),
        CategoriaInfo("MotoGP", "#FFCC00"),
        CategoriaInfo("Moto2", "#FF5FA2"),
        CategoriaInfo("Moto3", "#41D99C"),
        CategoriaInfo("WEC", "#CBD5E1"),
        // NASCAR: família de verdes (cores livres), longe das demais categorias e distintas entre si
        CategoriaInfo("NASCAR Cup Series", "#81BF43", "NASCAR CUP"),
        CategoriaInfo("NASCAR O'Reilly Auto Parts Series", "#DBFABB", "NASCAR O'REILLY"),
        CategoriaInfo("NASCAR Craftsman Truck Series", "#2FEB2F", "NASCAR TRUCK")
    )

    fun info(nome: String): CategoriaInfo? =
        todas.firstOrNull { Formatacao.normalizar(it.nome) == Formatacao.normalizar(nome) }

    /** Cor da categoria em #RRGGBB; categorias fora da lista usam [COR_PADRAO]. */
    fun corHex(nome: String): String = info(nome)?.cor ?: COR_PADRAO

    /** Texto do selo nos cards: abreviação, quando existir, ou o nome completo. */
    fun rotuloDoSelo(nome: String): String = (info(nome)?.abreviacao ?: nome).uppercase()
}
