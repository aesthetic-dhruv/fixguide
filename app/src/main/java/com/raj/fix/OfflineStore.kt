package com.raj.fix

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Offline guide storage: the FULL raw guide JSON from the iFixit API is
 * saved as one file per guide in the app's private storage
 * (filesDir/offline/<guideid>.json). No Room/database dependency, so the
 * build has one less failure point. Files are private to Fix.
 */
object OfflineStore {
    private fun dir(context: Context): File {
        val d = File(context.filesDir, "offline")
        if (!d.exists()) d.mkdirs()
        return d
    }

    private fun file(context: Context, guideId: Int): File =
        File(dir(context), "$guideId.json")

    fun save(context: Context, guideId: Int, rawJson: String) {
        file(context, guideId).writeText(rawJson, Charsets.UTF_8)
    }

    fun exists(context: Context, guideId: Int): Boolean = file(context, guideId).exists()

    fun load(context: Context, guideId: Int): GuideDetail? {
        return try {
            val text = file(context, guideId).readText(Charsets.UTF_8)
            Models.parseDetail(JSONObject(text))
        } catch (_: Exception) {
            null
        }
    }

    /** Saved guide ids, newest first. */
    fun listIds(context: Context): List<Int> {
        val files = dir(context).listFiles() ?: return emptyList()
        return files.mapNotNull { it.nameWithoutExtension.toIntOrNull() }
            .sortedByDescending { file(context, it).lastModified() }
    }

    fun delete(context: Context, guideId: Int) {
        file(context, guideId).delete()
    }

    fun clearAll(context: Context): Int {
        val files = dir(context).listFiles() ?: return 0
        var n = 0
        for (f in files) if (f.delete()) n++
        return n
    }
}
