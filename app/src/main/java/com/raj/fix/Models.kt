package com.raj.fix

import org.json.JSONArray
import org.json.JSONObject

/**
 * Data models for the iFixit public API 2.0.
 * Field names match the LIVE JSON verified 2026-11-06, e.g.
 * search "iPhone 14 Battery Replacement" -> totalResults 6, guideid 152966,
 * title "iPhone 14 Battery Replacement", difficulty "Moderate",
 * time_required_max 7200. Guide 152966 -> 27 steps, tools[], parts[],
 * author { username "Tobias Isakeit" }, url, conclusion_raw.
 * Every parser uses opt* so one missing field never crashes the app.
 */
data class GuideSummary(
    val guideId: Int,
    val title: String,
    val category: String,
    val subject: String,
    val difficulty: String,
    val timeMaxSec: Int,
    val url: String,
    val summary: String,
    val authorName: String,
    val thumbUrl: String
)

data class StepLine(val text: String, val bullet: String, val level: Int)

data class GuideStep(
    val title: String,
    val lines: List<StepLine>,
    val imageUrls: List<String>
)

data class ToolItem(
    val text: String,
    val quantity: Int,
    val optional: Boolean,
    val url: String,
    val thumbUrl: String
)

data class GuideDetail(
    val guideId: Int,
    val title: String,
    val category: String,
    val subject: String,
    val difficulty: String,
    val timeMinSec: Int,
    val timeMaxSec: Int,
    val url: String,
    val summary: String,
    val introRaw: String,
    val conclusionRaw: String,
    val authorName: String,
    val authorUrl: String,
    val tools: List<ToolItem>,
    val parts: List<ToolItem>,
    val steps: List<GuideStep>
)

data class CategoryNode(
    val name: String,
    val description: String,
    val children: List<String>,
    val guides: List<GuideSummary>
)

data class ChatMessage(val role: String, val text: String) // role: "user" | "ai" | "note"

object Models {

    fun formatTime(seconds: Int): String {
        if (seconds <= 0) return "—"
        val mins = seconds / 60
        if (mins < 60) return "$mins min"
        val h = mins / 60
        val m = mins % 60
        return if (m == 0) "$h hr" else "$h hr $m min"
    }

    fun imageUrl(obj: JSONObject?): String {
        if (obj == null) return ""
        // Prefer "standard", fall back to "medium", then "thumbnail".
        return obj.optString("standard").ifEmpty { obj.optString("medium") }
            .ifEmpty { obj.optString("thumbnail") }
    }

    fun parseSummary(obj: JSONObject): GuideSummary {
        return GuideSummary(
            guideId = obj.optInt("guideid"),
            title = obj.optString("title"),
            category = obj.optString("category"),
            subject = obj.optString("subject"),
            difficulty = obj.optString("difficulty").ifEmpty { "—" },
            timeMaxSec = obj.optInt("time_required_max"),
            url = obj.optString("url"),
            summary = obj.optString("summary"),
            authorName = obj.optString("username"),
            thumbUrl = imageUrl(obj.optJSONObject("image"))
        )
    }

    fun parseSummaryList(arr: JSONArray?): List<GuideSummary> {
        val out = mutableListOf<GuideSummary>()
        if (arr == null) return out
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            // Search results can contain non-guide items; keep guides only.
            val type = o.optString("dataType")
            if (type.isNotEmpty() && type != "guide") continue
            out.add(parseSummary(o))
        }
        return out
    }

    private fun parseTool(obj: JSONObject): ToolItem {
        return ToolItem(
            text = obj.optString("text"),
            quantity = obj.optInt("quantity", 1),
            optional = obj.optBoolean("isoptional", false),
            url = obj.optString("url"),
            thumbUrl = obj.optString("thumbnail")
        )
    }

    fun parseDetail(obj: JSONObject): GuideDetail {
        val steps = mutableListOf<GuideStep>()
        val stepsArr = obj.optJSONArray("steps")
        if (stepsArr != null) {
            for (i in 0 until stepsArr.length()) {
                val s = stepsArr.optJSONObject(i) ?: continue
                val lines = mutableListOf<StepLine>()
                val linesArr = s.optJSONArray("lines")
                if (linesArr != null) {
                    for (j in 0 until linesArr.length()) {
                        val l = linesArr.optJSONObject(j) ?: continue
                        lines.add(
                            StepLine(
                                text = l.optString("text_raw").ifEmpty { l.optString("text_rendered") },
                                bullet = l.optString("bullet"),
                                level = l.optInt("level", 0)
                            )
                        )
                    }
                }
                val images = mutableListOf<String>()
                val media = s.optJSONObject("media")
                if (media != null && media.optString("type") == "image") {
                    val data = media.optJSONArray("data")
                    if (data != null) {
                        for (j in 0 until data.length()) {
                            val u = imageUrl(data.optJSONObject(j))
                            if (u.isNotEmpty()) images.add(u)
                        }
                    }
                }
                steps.add(GuideStep(title = s.optString("title"), lines = lines, imageUrls = images))
            }
        }
        val tools = mutableListOf<ToolItem>()
        val toolsArr = obj.optJSONArray("tools")
        if (toolsArr != null) {
            for (i in 0 until toolsArr.length()) {
                toolsArr.optJSONObject(i)?.let { tools.add(parseTool(it)) }
            }
        }
        val parts = mutableListOf<ToolItem>()
        val partsArr = obj.optJSONArray("parts")
        if (partsArr != null) {
            for (i in 0 until partsArr.length()) {
                partsArr.optJSONObject(i)?.let { parts.add(parseTool(it)) }
            }
        }
        val author = obj.optJSONObject("author")
        return GuideDetail(
            guideId = obj.optInt("guideid"),
            title = obj.optString("title"),
            category = obj.optString("category"),
            subject = obj.optString("subject"),
            difficulty = obj.optString("difficulty").ifEmpty { "—" },
            timeMinSec = obj.optInt("time_required_min"),
            timeMaxSec = obj.optInt("time_required_max"),
            url = obj.optString("url"),
            summary = obj.optString("summary"),
            introRaw = obj.optString("introduction_raw"),
            conclusionRaw = obj.optString("conclusion_raw"),
            authorName = author?.optString("username") ?: "",
            authorUrl = author?.optString("url") ?: "",
            tools = tools,
            parts = parts,
            steps = steps
        )
    }

    fun parseCategory(obj: JSONObject): CategoryNode {
        val children = mutableListOf<String>()
        val ch = obj.optJSONArray("children")
        if (ch != null) {
            for (i in 0 until ch.length()) children.add(ch.optString(i))
        }
        return CategoryNode(
            name = obj.optString("wiki_title").ifEmpty { obj.optString("title") },
            description = obj.optString("description"),
            children = children,
            guides = parseSummaryList(obj.optJSONArray("guides"))
        )
    }
}
