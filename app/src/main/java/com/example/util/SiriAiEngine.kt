package com.example.util

import android.content.Context
import com.example.BuildConfig
import com.example.data.IOSPersistenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

typealias SiriMessage = SiriAiEngine.SiriMessage

object SiriAiEngine {

    data class SiriMessage(
        val role: String, // "user" or "model"
        val content: String
    )

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun getActiveApiKey(context: Context?): String {
        if (context != null) {
            val userKey = IOSPersistenceManager.getGeminiApiKey(context).trim()
            if (userKey.isNotBlank()) return userKey
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
        if (!buildKey.isNullOrBlank() && buildKey != "DEFAULT_KEY" && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey.trim()
        }
        return ""
    }

    val CANDIDATE_MODELS = listOf("gemini-flash-latest", "gemini-3.1-flash-lite-preview", "gemini-3.5-flash", "gemini-3.8-flash")

    fun isGeminiConfigured(context: Context?): Boolean {
        return getActiveApiKey(context).isNotBlank()
    }

    suspend fun testApiKey(apiKey: String, context: Context? = null): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            return@withContext Pair(false, "API Key tidak boleh kosong.")
        }

        val preferredModel = if (context != null) IOSPersistenceManager.getGeminiModel(context) else "gemini-flash-latest"
        val modelsToTry = (listOf(preferredModel) + CANDIDATE_MODELS).distinct()

        var lastError = ""
        for (model in modelsToTry) {
            try {
                val jsonBody = JSONObject().apply {
                    val contentsArray = JSONArray()
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Halo, tes koneksi. Balas 'OK'.")
                            })
                        }
                        put("parts", partsArray)
                    }
                    contentsArray.put(contentObj)
                    put("contents", contentsArray)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonBody.toString().toRequestBody(mediaType)
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$trimmed"

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    if (context != null) {
                        IOSPersistenceManager.saveGeminiModel(context, model)
                    }
                    return@withContext Pair(true, "Koneksi Google Gemini API berhasil terhubung ($model)! 🎉")
                } else {
                    val code = response.code
                    val errBody = response.body?.string() ?: ""
                    val errorMsg = try {
                        JSONObject(errBody).optJSONObject("error")?.optString("message") ?: "HTTP $code"
                    } catch (_: Exception) {
                        "HTTP $code"
                    }

                    if (code == 503) {
                        return@withContext Pair(true, "API Key valid! Server model $model sedang sibuk (503), namun key sudah terverifikasi.")
                    } else if (code == 404) {
                        lastError = errorMsg
                        continue // Try next candidate model
                    } else if (code == 400 || code == 403) {
                        return@withContext Pair(false, "Gagal terhubung: $errorMsg. Periksa kembali API Key kamu.")
                    } else {
                        lastError = errorMsg
                    }
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "Network error"
            }
        }
        return@withContext Pair(false, "Gagal menghubungkan Gemini ($lastError).")
    }

    fun buildSystemInstruction(): String {
        val localeId = Locale("id", "ID")
        val now = Date()
        val fullDate = try {
            SimpleDateFormat("EEEE, d MMMM yyyy", localeId).format(now)
        } catch (_: Exception) {
            "Hari ini"
        }
        val timeStr = try {
            SimpleDateFormat("HH:mm", localeId).format(now)
        } catch (_: Exception) {
            ""
        }
        val timeZoneStr = try {
            TimeZone.getDefault().id
        } catch (_: Exception) {
            "WIB"
        }

        return "Kamu adalah Siri, asisten cerdas resmi dari Apple di iOS dengan Apple Intelligence. " +
                "Karaktermu: sangat cerdas, ramah, natural, dan cepat tanggap. " +
                "INFORMASI WAKTU NYATA PERANGKAT SAAT INI: Hari ini adalah $fullDate, waktu saat ini menunjukkan pukul $timeStr ($timeZoneStr). " +
                "PENTING: Gunakan informasi tanggal, hari, bulan, tahun, dan waktu saat ini ($fullDate, $timeStr) sebagai acuan waktu real-time yang mutlak benar untuk menjawab pertanyaan apa pun tentang tanggal, hari apa sekarang, waktu saat ini, kalender, jadwal, atau waktu lampau/mendatang. Jangan gunakan tahun cutoff lama 2024. " +
                "Aturan utama: Jawablah dengan singkat, padat, dan langsung ke intinya (maksimal 1-3 kalimat) layaknya asisten suara Siri asli agar respons instan. " +
                "HANYA berikan jawaban panjang atau terperinci jika pengguna secara spesifik memintanya (seperti 'jelaskan detail', 'buatkan cerita', 'analisis'). " +
                "Pertahankan dan pahami konteks percakapan sebelumnya dalam sesi obrolan ini sehingga kamu bisa menjawab pertanyaan lanjutan dengan nyambung dan akurat. " +
                "Berbicaralah dalam Bahasa Indonesia yang santun dan akrab."
    }

    suspend fun queryAiStreaming(
        prompt: String,
        history: List<SiriMessage> = emptyList(),
        context: Context? = null,
        onChunk: suspend (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val trimmed = prompt.trim()
        if (trimmed.isBlank()) {
            val emptyReply = "Halo! Ada yang bisa Siri bantu hari ini?"
            onChunk(emptyReply)
            return@withContext emptyReply
        }

        val apiKey = getActiveApiKey(context)

        if (apiKey.isNotBlank()) {
            val preferredModel = if (context != null) IOSPersistenceManager.getGeminiModel(context) else "gemini-flash-latest"
            val modelsToTry = (listOf(preferredModel) + CANDIDATE_MODELS).distinct()

            for (model in modelsToTry) {
                try {
                    val jsonBody = JSONObject().apply {
                        val systemInstructionObj = JSONObject().apply {
                            val partsArr = JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", buildSystemInstruction())
                                })
                            }
                            put("parts", partsArr)
                        }
                        put("systemInstruction", systemInstructionObj)

                        val contentsArray = JSONArray()

                        // Prepare valid alternating multi-turn contents:
                        // 1. Take up to last 8 messages to keep payload small & fast on J2 Prime (RAM 1.5GB)
                        val sanitizedTurns = mutableListOf<Pair<String, String>>()
                        var lastRole = ""
                        for (msg in history.takeLast(8)) {
                            val text = msg.content.trim()
                            if (text.isBlank()) continue
                            val role = if (msg.role == "model" || msg.role == "assistant") "model" else "user"
                            if (role != lastRole) {
                                sanitizedTurns.add(Pair(role, text))
                                lastRole = role
                            }
                        }

                        // 2. Ensure last turn before current prompt isn't user (Gemini forbids consecutive user roles)
                        if (sanitizedTurns.isNotEmpty() && sanitizedTurns.last().first == "user") {
                            sanitizedTurns.removeAt(sanitizedTurns.lastIndex)
                        }

                        // 3. Add history turns
                        for ((role, text) in sanitizedTurns) {
                            contentsArray.put(JSONObject().apply {
                                put("role", role)
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("text", text)
                                    })
                                })
                            })
                        }

                        // 4. Add current prompt
                        contentsArray.put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", trimmed)
                                })
                            })
                        })

                        put("contents", contentsArray)

                        val genConfig = JSONObject().apply {
                            put("temperature", 0.7)
                            put("maxOutputTokens", 1024)
                        }
                        put("generationConfig", genConfig)
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val requestBody = jsonBody.toString().toRequestBody(mediaType)
                    // Request SSE stream for ultra-low latency word-by-word streaming
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey"

                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val fullTextBuilder = StringBuilder()
                        response.body?.source()?.let { source ->
                            while (!source.exhausted()) {
                                val line = source.readUtf8Line() ?: break
                                if (line.startsWith("data: ")) {
                                    val jsonStr = line.removePrefix("data: ").trim()
                                    if (jsonStr == "[DONE]") break
                                    try {
                                        val rootJson = JSONObject(jsonStr)
                                        val candidates = rootJson.optJSONArray("candidates")
                                        if (candidates != null && candidates.length() > 0) {
                                            val firstCandidate = candidates.getJSONObject(0)
                                            val content = firstCandidate.optJSONObject("content")
                                            val parts = content?.optJSONArray("parts")
                                            if (parts != null) {
                                                for (p in 0 until parts.length()) {
                                                    val partText = parts.getJSONObject(p).optString("text")
                                                    if (partText.isNotEmpty()) {
                                                        fullTextBuilder.append(partText)
                                                        onChunk(partText)
                                                    }
                                                }
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                        }

                        val finalResult = fullTextBuilder.toString().trim()
                        if (finalResult.isNotBlank()) {
                            if (context != null && model != preferredModel) {
                                IOSPersistenceManager.saveGeminiModel(context, model)
                            }
                            return@withContext finalResult
                        }
                    } else if (response.code == 404 || response.code == 503) {
                        continue
                    }
                } catch (e: Exception) {
                    // Try next model if applicable
                }
            }
        }

        // Offline Smart Assistant Engine (Instant, Indonesian, Zero Data Required)
        val offline = getSmartOfflineAnswer(trimmed)
        onChunk(offline)
        return@withContext offline
    }

    suspend fun queryAi(prompt: String, context: Context? = null): String {
        return queryAiStreaming(prompt, emptyList(), context) {}
    }

    suspend fun queryAi(prompt: String, history: List<SiriMessage>, context: Context? = null): String {
        return queryAiStreaming(prompt, history, context) {}
    }

    private fun getSmartOfflineAnswer(input: String): String {
        val lower = input.lowercase()

        return when {
            lower.contains("siapa kamu") || lower.contains("kamu siapa") ->
                "Saya Siri, asisten cerdas iOS dengan Apple Intelligence. Kamu bisa menghubungkan Google Gemini API Key di Settings untuk mengobrol bebas apa saja dengan saya!"

            lower.contains("halo") || lower.contains("hai") || lower.contains("pagi") || lower.contains("siang") || lower.contains("malam") ->
                "Halo! Semoga harimu menyenangkan. Ada yang bisa Siri bantu untukmu hari ini?"

            lower.contains("terima kasih") || lower.contains("makasih") || lower.contains("thanks") ->
                "Sama-sama! Senang selalu bisa membantumu."

            lower.contains("apa kabar") || lower.contains("gimana kabarmu") ->
                "Kabar saya sangat baik dan siap membantumu kapan saja!"

            lower.contains("gemini") || lower.contains("api key") || lower.contains("masukin api") ->
                "Kamu bisa memasukkan Gemini API Key di Settings -> Siri & Apple Intelligence. Dapatkan key gratis di aistudio.google.com/apikey!"

            lower.contains("j2 prime") || lower.contains("lineageos") ->
                "Samsung Galaxy J2 Prime dengan LineageOS 15 berjalan lancar. Kamu bisa gunakan Quick RAM Boost di Control Center untuk performa maksimal."

            lower.contains("tips") && lower.contains("ram") || lower.contains("hemat baterai") ->
                "Untuk menghemat baterai & RAM: aktifkan Low Power Mode di Settings -> Battery, dan bersihkan cache RAM via Control Center."

            lower.contains("jam berapa") || lower.contains("pukul berapa") || lower.contains("waktu") -> {
                val now = SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date())
                "Saat ini waktu menunjukkan pukul $now WIB."
            }

            lower.contains("tanggal") || lower.contains("hari apa") || lower.contains("hari ini") || lower.contains("tahun") || lower.contains("bulan") -> {
                val today = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date())
                val year = SimpleDateFormat("yyyy", Locale("id", "ID")).format(Date())
                if (lower.contains("tahun")) {
                    "Saat ini kita berada di tahun $year."
                } else {
                    "Hari ini adalah $today."
                }
            }

            lower.contains("hitung") || lower.contains("+") || lower.contains("-") || lower.contains("*") || lower.contains("/") -> {
                try {
                    val cleaned = lower.replace("hitung", "").trim()
                    "Hasil perhitungan $cleaned bisa langsung kamu lihat di aplikasi Kalkulator iOS."
                } catch (_: Exception) {
                    "Kalkulator iOS siap membantumu menghitung dengan cepat."
                }
            }

            lower.contains("motivasi") || lower.contains("kata bijak") || lower.contains("quotes") ->
                "\"Langkah kecil yang konsisten setiap hari akan membawamu mencapai hal-hal luar biasa.\" Tetap semangat!"

            lower.contains("buka") || lower.contains("jalankan") ->
                "Kamu bisa langsung ketuk ikon aplikasi di Home Screen atau gunakan Spotlight Search untuk menemukannya secepat kilat."

            else ->
                "Saya mengerti pertanyaanmu tentang \"$input\". Untuk menjawab pertanyaan pengetahuan luas dan mengobrol bebas, masukkan Google Gemini API Key di Settings -> Siri & Apple Intelligence!"
        }
    }
}
