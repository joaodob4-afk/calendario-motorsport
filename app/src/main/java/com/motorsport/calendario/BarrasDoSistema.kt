package com.motorsport.calendario

import android.app.Activity
import android.view.View
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
