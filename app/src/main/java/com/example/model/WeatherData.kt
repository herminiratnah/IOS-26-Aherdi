package com.example.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HourlyForecastItem(
    val time: String,
    val temp: String,
    val weatherCode: Int,
    val condition: String
)

data class DailyForecastItem(
    val day: String,
    val minTemp: String,
    val maxTemp: String,
    val weatherCode: Int,
    val condition: String
)

data class WeatherData(
    val city: String = "Bandung",
    val currentTemp: Int = 25,
    val condition: String = "Partly Cloudy",
    val highTemp: Int = 29,
    val lowTemp: Int = 21,
    val humidity: Int = 76,
    val weatherCode: Int = 2,
    val hourly: List<HourlyForecastItem> = defaultHourly(),
    val daily: List<DailyForecastItem> = defaultDaily()
)

fun defaultHourly(): List<HourlyForecastItem> = listOf(
    HourlyForecastItem("Now", "25°", 2, "Partly Cloudy"),
    HourlyForecastItem("12 PM", "28°", 1, "Mainly Clear"),
    HourlyForecastItem("1 PM", "29°", 2, "Partly Cloudy"),
    HourlyForecastItem("2 PM", "28°", 61, "Light Rain"),
    HourlyForecastItem("3 PM", "26°", 61, "Rain"),
    HourlyForecastItem("4 PM", "25°", 3, "Overcast"),
    HourlyForecastItem("5 PM", "24°", 2, "Partly Cloudy")
)

fun defaultDaily(): List<DailyForecastItem> = listOf(
    DailyForecastItem("Hari Ini", "21°", "29°", 2, "Partly Cloudy"),
    DailyForecastItem("Sel", "20°", "28°", 61, "Light Rain"),
    DailyForecastItem("Rab", "21°", "29°", 2, "Partly Cloudy"),
    DailyForecastItem("Kam", "21°", "30°", 1, "Mainly Clear"),
    DailyForecastItem("Jum", "20°", "28°", 61, "Rain"),
    DailyForecastItem("Sab", "21°", "29°", 2, "Partly Cloudy"),
    DailyForecastItem("Min", "21°", "29°", 1, "Sunny")
)

object WeatherService {
    // Bandung Coordinates: -6.9175, 107.6191
    private const val API_URL =
        "https://api.open-meteo.com/v1/forecast?latitude=-6.9175&longitude=107.6191&current_weather=true&hourly=temperature_2m,weathercode,relative_humidity_2m&daily=temperature_2m_max,temperature_2m_min,weathercode&timezone=Asia%2FJakarta"

    fun mapWmoCode(code: Int): String = when (code) {
        0 -> "Cerah"
        1 -> "Cerah Sebagian"
        2 -> "Berawan Sebagian"
        3 -> "Mendung (Overcast)"
        45, 48 -> "Berkabut"
        51, 53, 55 -> "Gerimis Halus"
        61, 63, 65 -> "Hujan"
        80, 81, 82 -> "Hujan Deras"
        95, 96, 99 -> "Hujan Badai Petir"
        else -> "Berawan"
    }

    suspend fun fetchWeather(latitude: Double = -6.9175, longitude: Double = 107.6191, cityName: String = "Bandung"): WeatherData = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val urlStr = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current_weather=true&hourly=temperature_2m,weathercode,relative_humidity_2m&daily=temperature_2m_max,temperature_2m_min,weathercode&timezone=Asia%2FJakarta"
            val url = URL(urlStr)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
            }

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val current = json.getJSONObject("current_weather")
                val currentTemp = current.getDouble("temperature").toInt()
                val weatherCode = current.getInt("weathercode")

                val dailyJson = json.optJSONObject("daily")
                val maxTemps = dailyJson?.optJSONArray("temperature_2m_max")
                val minTemps = dailyJson?.optJSONArray("temperature_2m_min")
                val dailyCodes = dailyJson?.optJSONArray("weathercode")
                val dailyTimes = dailyJson?.optJSONArray("time")

                val todayMax = maxTemps?.optDouble(0, 29.0)?.toInt() ?: 29
                val todayMin = minTemps?.optDouble(0, 21.0)?.toInt() ?: 21

                // Parse hourly
                val hourlyJson = json.optJSONObject("hourly")
                val hourlyTimes = hourlyJson?.optJSONArray("time")
                val hourlyTemps = hourlyJson?.optJSONArray("temperature_2m")
                val hourlyCodes = hourlyJson?.optJSONArray("weathercode")

                val hourlyList = mutableListOf<HourlyForecastItem>()
                val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                val sdfOutput = SimpleDateFormat("HH:mm", Locale.getDefault())
                val nowHour = SimpleDateFormat("HH", Locale.getDefault()).format(Date()).toIntOrNull() ?: 12

                if (hourlyTimes != null && hourlyTemps != null && hourlyCodes != null) {
                    val count = Math.min(hourlyTimes.length(), 24)
                    for (i in 0 until count) {
                        val timeStr = hourlyTimes.getString(i)
                        try {
                            val parsedDate = sdfInput.parse(timeStr)
                            val hourNum = SimpleDateFormat("HH", Locale.getDefault()).format(parsedDate).toIntOrNull() ?: 0
                            if (hourNum >= nowHour && hourlyList.size < 8) {
                                val label = if (hourNum == nowHour) "Sekarang" else sdfOutput.format(parsedDate)
                                val t = hourlyTemps.getDouble(i).toInt()
                                val code = hourlyCodes.getInt(i)
                                hourlyList.add(HourlyForecastItem(label, "$t°", code, mapWmoCode(code)))
                            }
                        } catch (e: Exception) {}
                    }
                }

                // Parse daily
                val dailyList = mutableListOf<DailyForecastItem>()
                val sdfDayInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val sdfDayOutput = SimpleDateFormat("EEE", Locale("id", "ID"))
                if (dailyTimes != null && maxTemps != null && minTemps != null && dailyCodes != null) {
                    val dayCount = Math.min(dailyTimes.length(), 7)
                    for (i in 0 until dayCount) {
                        val dateStr = dailyTimes.getString(i)
                        val maxT = maxTemps.getDouble(i).toInt()
                        val minT = minTemps.getDouble(i).toInt()
                        val code = dailyCodes.getInt(i)
                        val dayLabel = if (i == 0) "Hari Ini" else {
                            try {
                                val d = sdfDayInput.parse(dateStr)
                                sdfDayOutput.format(d)
                            } catch (e: Exception) {
                                "H+$i"
                            }
                        }
                        dailyList.add(DailyForecastItem(dayLabel, "$minT°", "$maxT°", code, mapWmoCode(code)))
                    }
                }

                return@withContext WeatherData(
                    city = cityName,
                    currentTemp = currentTemp,
                    condition = mapWmoCode(weatherCode),
                    highTemp = todayMax,
                    lowTemp = todayMin,
                    humidity = 76,
                    weatherCode = weatherCode,
                    hourly = if (hourlyList.isNotEmpty()) hourlyList else defaultHourly(),
                    daily = if (dailyList.isNotEmpty()) dailyList else defaultDaily()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }

        // Return Bandung fallback
        return@withContext WeatherData(
            city = cityName,
            currentTemp = 25,
            condition = "Berawan Sebagian",
            highTemp = 29,
            lowTemp = 21,
            humidity = 75,
            weatherCode = 2,
            hourly = defaultHourly(),
            daily = defaultDaily()
        )
    }
}
