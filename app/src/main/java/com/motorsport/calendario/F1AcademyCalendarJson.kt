package com.motorsport.calendario

/** Calendário da F1 Academy. Toda a lógica está em [CalendarioFormulaJunior]. */
object F1AcademyCalendarJson : CalendarioFormulaJunior(
    categoria = "F1 Academy",
    urlJson =
        "https://raw.githubusercontent.com/joaodob4-afk/calendario-motorsport/main/app/src/main/assets/f1academy_calendar.json",
    arquivoAsset = "f1academy_calendar.json",
    arquivoCache = "f1academy_calendar_cache.json"
)
