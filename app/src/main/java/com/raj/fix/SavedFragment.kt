package com.raj.fix

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Saved tab: offline guides (full guide JSON saved on the phone — opens
 * without internet) + favourites (title remembered locally; tap opens the
 * guide online if it is not saved offline yet).
 */
class SavedFragment : Fragment() {

    private lateinit var adapter: GuideAdapter
    private lateinit var empty: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_saved, container, false)
        empty = v.findViewById(R.id.savedEmpty)
        val list = v.findViewById<RecyclerView>(R.id.savedList)
        list.layoutManager = LinearLayoutManager(requireContext())
        adapter = GuideAdapter(emptyList()) { guide ->
            (activity as? MainActivity)?.openGuide(guide)
        }
        list.adapter = adapter
        refresh()
        return v
    }

    fun refresh() {
        if (!::adapter.isInitialized) return
        val ctx = context ?: return
        val items = mutableListOf<GuideSummary>()
        val seen = mutableSetOf<Int>()
        // 1. Offline guides first (full detail is on the phone).
        for (id in OfflineStore.listIds(ctx)) {
            val d = OfflineStore.load(ctx, id) ?: continue
            seen.add(id)
            items.add(
                GuideSummary(
                    guideId = d.guideId, title = "📥 ${d.title}", category = d.category,
                    subject = d.subject, difficulty = d.difficulty,
                    timeMaxSec = d.timeMaxSec, url = d.url, summary = "Offline saved — bina internet ke khulegi.",
                    authorName = d.authorName, thumbUrl = ""
                )
            )
        }
        // 2. Favourites not saved offline yet.
        val titles = Prefs.getFavouriteTitles(ctx)
        for (idStr in Prefs.getFavourites(ctx)) {
            val id = idStr.toIntOrNull() ?: continue
            if (seen.contains(id)) continue
            seen.add(id)
            items.add(
                GuideSummary(
                    guideId = id, title = "⭐ ${titles[id] ?: "Guide #$id"}",
                    category = "", subject = "", difficulty = "—",
                    timeMaxSec = 0, url = "", summary = "Favourite — kholne ke liye internet chahiye.",
                    authorName = "", thumbUrl = ""
                )
            )
        }
        adapter.setItems(items)
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}
