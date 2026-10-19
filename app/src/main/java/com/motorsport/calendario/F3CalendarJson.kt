package com.motorsport.calendario

/** Calendário da F3. Toda a lógica está em [CalendarioFormulaJunior]. */
object F3CalendarJson : CalendarioFormulaJunior(
    categoria = "F3",
    urlJson =
        "https://raw.githubusercontent.com/joaodob4-afk/calendario-motorsport/main/app/src/main/assets/f3_calendar.json",
    arquivoAsset = "f3_calendar.json",
    arquivoCache = "f3_calendar_cache.json"
)
