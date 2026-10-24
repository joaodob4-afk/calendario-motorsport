package com.motorsport.calendario

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Com targetSdk 35, o Android 15+ desenha o app por baixo das barras de status e de
 * navegação. Este ajuste mantém o conteúdo na área livre (e acima do teclado) e se
 * comporta igual em todas as versões do Android.
 *
 * Chame logo depois de setContentView(...). Pode ser chamado mais de uma vez.
 */
fun Activity.aplicarInsetsDasBarras() {
    WindowCompat.setDecorFitsSystemWindows(window, false)

    val raiz = findViewById<View>(android.R.id.content)
    ViewCompat.setOnApplyWindowInsetsListener(raiz) { view, insets ->
        val area = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout() or
                WindowInsetsCompat.Type.ime()
        )
        view.setPadding(area.left, area.top, area.right, area.bottom)
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.requestApplyInsets(raiz)
}

/**
 * Pinta a janela e as barras de status e de navegação com [cor]. Se [corDoTopo] for outra, só a
 * faixa de cima (atrás da barra de status) usa essa cor, como nas telas com cabeçalho azul-marinho.
 * Os ícones das barras ficam escuros sobre cor clara e claros sobre cor escura, para sempre aparecerem.
 */
internal fun Activity.pintarBarrasDoSistema(cor: Int, corDoTopo: Int = cor) {
    val fundoDaJanela: android.graphics.drawable.Drawable =
        if (corDoTopo == cor) {
            ColorDrawable(cor)
        } else {
            android.graphics.drawable.LayerDrawable(arrayOf(ColorDrawable(cor), ColorDrawable(corDoTopo))).apply {
                setLayerHeight(1, (resources.displayMetrics.density * 160f).toInt())
                setLayerGravity(1, android.view.Gravity.TOP)
            }
        }

    window.setBackgroundDrawable(fundoDaJanela)
    window.statusBarColor = corDoTopo
    window.navigationBarColor = cor

    // A linha fina que o Android desenha entre o app e a barra de navegação (aparece preta no claro).
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        window.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
    }

    val controle = WindowCompat.getInsetsController(window, window.decorView)
    controle.isAppearanceLightStatusBars = ColorUtils.calculateLuminance(corDoTopo) > 0.5
    controle.isAppearanceLightNavigationBars = ColorUtils.calculateLuminance(cor) > 0.5
}
