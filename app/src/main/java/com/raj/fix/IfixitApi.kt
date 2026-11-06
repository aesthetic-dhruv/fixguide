package com.raj.fix

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * iFixit public API 2.0 — NO key needed.
 * Docs: https://www.ifixit.com/api/2.0/docs
 * Verified live 2026-11-06 (no auth):
 *   GET /api/2.0/search/iPhone%2014%20Battery%20Replacement -> totalResults 6
 *   GET /api/2.0/categories/Phone -> children [iPhone, Android Phone, ...]
 *   GET /api/2.0/guides/152966 -> 27 steps, tools, parts, author
 * The API gives guides + device hierarchy ONLY — no parts store, no Answers Q&A DB.
 * All calls run on a background thread; callers must never call these on the UI thread.
 */
object IfixitApi {
    const val BASE = "https://www.ifixit.com/api/2.0"
    private const val TIMEOUT_MS = 20000

    class ApiException(message: String) : Exception(message)

    private fun getJson(urlStr: String): JSONObject {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Fix/1.0 (Android)")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw ApiException("iFixit server ne $code diya. Internet check karo, phir dobara try karo.")
            }
            val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
            val text = reader.use { it.readText() }
            return JSONObject(text)
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw ApiException("iFixit se connect nahi ho paya: ${e.message ?: "network error"}")
        } finally {
            conn?.disconnect()
        }
    }

    /** Search guides by free text, e.g. "iPhone 14 Battery Replacement". */
    fun search(query: String): List<GuideSummary> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val json = getJson("$BASE/search/$q")
        return Models.parseSummaryList(json.optJSONArray("results"))
    }

    /** One category node: its children (sub-models) + its own guides. */
    fun category(name: String): CategoryNode {
        val n = URLEncoder.encode(name, "UTF-8")
        val json = getJson("$BASE/categories/$n")
        return Models.parseCategory(json)
    }

    /** Full guide detail by guideid, e.g. 152966. */
    fun guide(guideId: Int): GuideDetail {
        val json = getJson("$BASE/guides/$guideId")
        return Models.parseDetail(json)
    }

    /** Raw guide JSON (for offline save). Returns the raw text, not parsed. */
    fun guideRawJson(guideId: Int): String {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$BASE/guides/$guideId")
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Fix/1.0 (Android)")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw ApiException("iFixit server ne ${conn.responseCode} diya.")
            }
            return BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
                .use { it.readText() }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw ApiException("iFixit se connect nahi ho paya: ${e.message ?: "network error"}")
        } finally {
            conn?.disconnect()
        }
    }
}
