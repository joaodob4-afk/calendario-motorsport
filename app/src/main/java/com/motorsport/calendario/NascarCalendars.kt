package com.motorsport.calendario

/** Os três calendários NASCAR usam o mesmo formato e o mesmo cache seguro de F2/F3. */
object NascarCupCalendarJson : CalendarioFormulaJunior(
    categoria = "NASCAR Cup Series",
    urlJson = "https://raw.githubusercontent.com/joaodob4-afk/calendario-motorsport/main/app/src/main/assets/nascar_cup_calendar.json",
    arquivoAsset = "nascar_cup_calendar.json",
    arquivoCache = "nascar_cup_calendar_cache.json"
)

object NascarXfinityCalendarJson : CalendarioFormulaJunior(
    categoria = "NASCAR O'Reilly Auto Parts Series",
    urlJson = "https://raw.githubusercontent.com/joaodob4-afk/calendario-motorsport/main/app/src/main/assets/nascar_xfinity_calendar.json",
    arquivoAsset = "nascar_xfinity_calendar.json",
    arquivoCache = "nascar_xfinity_calendar_cache.json"
)

object NascarTruckCalendarJson : CalendarioFormulaJunior(
    categoria = "NASCAR Craftsman Truck Series",
    urlJson = "https://raw.githubusercontent.com/joaodob4-afk/calendario-motorsport/main/app/src/main/assets/nascar_truck_calendar.json",
    arquivoAsset = "nascar_truck_calendar.json",
    arquivoCache = "nascar_truck_calendar_cache.json"
)
