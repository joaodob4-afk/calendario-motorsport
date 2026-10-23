package com.motorsport.calendario

import android.content.Context
import android.text.format.DateFormat
import java.time.ZonedDateTime

/**
 * Os horários aparecem no formato que o aparelho usa: 24 horas ("14:30") ou 12 horas
 * ("2:30 PM"). Os dados dos calendários continuam sempre em 24 horas; só o texto mostrado muda.
 */
internal object Hora {

    private val padrao = Regex("""^(\d{1,2}):(\d{2})$""")

    fun usa24Horas(context: Context): Boolean = DateFormat.is24HourFormat(context)

    fun formatar(hora: Int, minuto: Int, vinteEQuatroHoras: Boolean): String {
        val minutos = minuto.toString().padStart(2, '0')

        if (vinteEQuatroHoras) return hora.toString().padStart(2, '0') + ":" + minutos

        val horaEm12 = if (hora % 12 == 0) 12 else hora % 12
        val periodo = if (hora < 12) "AM" else "PM"

        return "$horaEm12:$minutos $periodo"
    }

    /** "14:30" (como vem dos calendários) no formato do aparelho; outro texto volta igual. */
    fun deTexto(context: Context, texto: String): String =
        deTexto(texto, usa24Horas(context))

    fun deTexto(texto: String, vinteEQuatroHoras: Boolean): String {
        val achado = padrao.matchEntire(texto.trim()) ?: return texto

        val hora = achado.groupValues[1].toInt()
        val minuto = achado.groupValues[2].toInt()
        if (hora > 23 || minuto > 59) return texto

        return formatar(hora, minuto, vinteEQuatroHoras)
    }

    fun deZonedDateTime(context: Context, horario: ZonedDateTime): String =
        formatar(horario.hour, horario.minute, usa24Horas(context))
}
