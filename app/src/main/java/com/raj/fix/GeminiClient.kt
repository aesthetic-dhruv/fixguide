package com.raj.fix

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Fix AI — Gemini client.
 *
 * The user's Gemini API key is entered IN THE APP (Settings > Gemini API Key)
 * and stored only on the phone (see Prefs.kt). It is NEVER hardcoded here.
 * The key is sent only to Google's Generative Language API, directly from
 * the phone — exactly like Misri / IRIS "Direct Gemini" mode.
 *
 * Flow (see AiFragment): FIRST search iFixit for matching guides, THEN ask
 * Gemini using those guides' titles + steps as context. Gemini never
 * invents a repair procedure from nothing — it explains the iFixit guide
 * in Hinglish. If there is no key, the caller must NOT call this; it must
 * show the guide results and tell the user to add the key in Settings.
 */
object GeminiClient {
    const val MODEL = "gemini-2.0-flash"
    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    class GeminiException(message: String) : Exception(message)

    /** One plain-text answer from Gemini. Throws GeminiException on any failure. */
    fun ask(apiKey: String, prompt: String): String {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$ENDPOINT?key=${URLEncoder.encode(apiKey.trim(), "UTF-8")}")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 30000
            conn.readTimeout = 60000
            val body = JSONObject()
            val contents = JSONArray()
            val content = JSONObject()
            val parts = JSONArray()
            parts.put(JSONObject().put("text", prompt))
            content.put("parts", parts)
            contents.put(content)
            body.put("contents", contents)
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
            val code = conn.responseCode
            val stream = if (code == HttpURLConnection.HTTP_OK) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            if (code != HttpURLConnection.HTTP_OK) {
                if (code == 400 || code == 403) {
                    throw GeminiException("Gemini key galat ya band hai (code $code). Settings me key dobara check karo.")
                }
                throw GeminiException("Gemini se jawab nahi mila (code $code). Baad me try karo.")
            }
            val json = JSONObject(text)
            val candidates = json.optJSONArray("candidates") ?: throw GeminiException("Gemini ne khaali jawab diya.")
            val first = candidates.optJSONObject(0) ?: throw GeminiException("Gemini ne khaali jawab diya.")
            val partsArr = first.optJSONObject("content")?.optJSONArray("parts")
                ?: throw GeminiException("Gemini ne khaali jawab diya.")
            val sb = StringBuilder()
            for (i in 0 until partsArr.length()) {
                sb.append(partsArr.optJSONObject(i)?.optString("text") ?: "")
            }
            val answer = sb.toString().trim()
            if (answer.isEmpty()) throw GeminiException("Gemini ne khaali jawab diya.")
            return answer
        } catch (e: GeminiException) {
            throw e
        } catch (e: Exception) {
            throw GeminiException("Gemini se connect nahi ho paya: ${e.message ?: "network error"}")
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Builds the Fix AI prompt: the iFixit guide context first, then the
     * user's question. Keeps the context short (top 3 guides, first 8 steps
     * each, first 2 lines per step) so the request stays small on a 4 GB phone.
     */
    fun buildPrompt(userQuestion: String, guides: List<GuideSummary>, details: List<GuideDetail>): String {
        val sb = StringBuilder()
        sb.append("Tum Fix ke repair assistant 'Fix AI' ho. Hinglish me jawab do — Roman Hindi + simple English mix, jaise ek mobile repair shop ka experienced technician bolta hai.\n")
        sb.append("Neeche iFixit ke real repair guides ka data hai. Sirf isi data ke basis par jawab do. Agar data me jawab na ho, to saaf bol do 'is guide me ye step nahi hai' — andaza laga kar step mat banao.\n")
        sb.append("Jawab short aur step-wise do. Battery, screen ya garam (hot) parts wale kaam me pehle ek safety warning do: phone band karo, battery puncture mat karo, garam battery ko paani me mat daalo.\n\n")
        sb.append("GUIDES:\n")
        val top = guides.take(3)
        for ((index, g) in top.withIndex()) {
            sb.append("${index + 1}. ${g.title} | Device: ${g.category} | Difficulty: ${g.difficulty} | Time: ${Models.formatTime(g.timeMaxSec)} | ${g.url}\n")
            val d = details.firstOrNull { it.guideId == g.guideId }
            if (d != null) {
                for ((si, step) in d.steps.take(8).withIndex()) {
                    sb.append("   Step ${si + 1}: ${step.title}\n")
                    for (line in step.lines.take(2)) {
                        if (line.text.isNotBlank()) sb.append("     - ${line.text}\n")
                    }
                }
                if (d.tools.isNotEmpty()) {
                    sb.append("   Tools: ${d.tools.take(6).joinToString(", ") { it.text }}\n")
                }
            }
        }
        if (top.isEmpty()) {
            sb.append("(Koi matching iFixit guide nahi mili.)\n")
        }
        sb.append("\nUSER KA SAWAL: $userQuestion\n")
        sb.append("Jawab Hinglish me do, guide ka naam aur link bhi likho, aur last me ye line add karo: 'Poori step-by-step guide ke liye Fix me guide kholo.'")
        return sb.toString()
    }
}
