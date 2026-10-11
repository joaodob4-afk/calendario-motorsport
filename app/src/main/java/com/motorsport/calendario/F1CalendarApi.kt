package com.motorsport.calendario

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * Monta a lista de etapas da F1 a partir da API Jolpica,
 * guarda em cache e nunca lança exceção.
 */
object F1CalendarApi {

    private const val URL_API =
        "https://api.jolpi.ca/ergast/f1/current/races/?limit=100"

    private const val ARQUIVO_CACHE = "f1_races_cache.json"

    private val MESES = arrayOf(
        "JAN", "FEV", "MAR", "ABR", "MAI", "JUN",
        "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"
    )

    // circuitId -> (país, cidade) nos nomes usados pelo app.
    private val CONHECIDOS = mapOf(
        "albert_park" to Pair("Austrália", "Melbourne"),
        "shanghai" to Pair("China", "Xangai"),
        "suzuka" to Pair("Japão", "Suzuka"),
        "bahrain" to Pair("Bahrein", "Sakhir"),
        "jeddah" to Pair("Arábia Saudita", "Jeddah"),
        "miami" to Pair("Estados Unidos", "Miami"),
        "villeneuve" to Pair("Canadá", "Montreal"),
        "monaco" to Pair("Mônaco", "Monte Carlo"),
        "catalunya" to Pair("Espanha", "Barcelona"),
        "red_bull_ring" to Pair("Áustria", "Spielberg"),
        "silverstone" to Pair("Reino Unido", "Silverstone"),
        "spa" to Pair("Bélgica", "Spa-Francorchamps"),
        "hungaroring" to Pair("Hungria", "Budapeste"),
        "zandvoort" to Pair("Holanda", "Zandvoort"),
        "monza" to Pair("Itália", "