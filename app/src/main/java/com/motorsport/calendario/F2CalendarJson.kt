package com.motorsport.calendario

/** Calendário da F2. Toda a lógica está em [CalendarioFormulaJunior]. */
object F2CalendarJson : CalendarioFormulaJunior(
    categoria = "F2",
    urlJson =
        "https://raw.githubusercontent.com/joaodob4-afk/calendario-motorsport/main/app/src/main/assets/f2_calendar.json",
    arquivoAsset = "f2_calendar.json",
    arquivoCache = "f2_calendar_cache.json",
    exigirIndiceConfirmado = true
)
