package com.motorsport.calendario

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var raceInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        raceInfo = findViewById(R.id.raceInfo)

        raceInfo.text = "Carregando calendário da F1..."

        Thread {
            try {
                val url = URL("https://api.jolpi.ca/ergast/f1/current.json")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val resposta = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                connection.disconnect()

                val json = JSONObject(resposta)
                val races = json
                    .getJSONObject("MRData")
                    .getJSONObject("RaceTable")
                    .getJSONArray("Races")

                val texto = StringBuilder()
                texto.append("🏎️ FÓRMULA 1\n\n")

                for (i in 0 until races.length()) {
                    val race = races.getJSONObject(i)

                    val nome = race.getString("raceName")
                    val data = race.getString("date")

                    texto.append("🏁 $nome\n")
                    texto.append("📅 $data\n\n")
                }

                runOnUiThread {
                    raceInfo.text = texto.toString()
                }

            } catch (e: Exception) {

                runOnUiThread {
                    raceInfo.text =
                        "Não foi possível carregar o calendário.\n\n" +
                        "${e.javaClass.simpleName}\n" +
                        "${e.message}"
                }
            }
        }.start()
    }
}
