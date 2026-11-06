package com.raj.fix

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Home / Search tab. Searches the live iFixit API — no key, no login.
 * Quick chips are the most common shop jobs, so Raj taps once instead of typing.
 */
class HomeFragment : Fragment() {

    private lateinit var adapter: GuideAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_home, container, false)
        val searchBox = v.findViewById<EditText>(R.id.searchBox)
        val searchBtn = v.findViewById<Button>(R.id.searchButton)
        val progress = v.findViewById<ProgressBar>(R.id.searchProgress)
        val empty = v.findViewById<TextView>(R.id.emptyText)
        val list = v.findViewById<RecyclerView>(R.id.resultsList)
        list.layoutManager = LinearLayoutManager(requireContext())
        adapter = GuideAdapter(emptyList()) { guide ->
            (activity as? MainActivity)?.openGuide(guide)
        }
        list.adapter = adapter

        fun doSearch(query: String) {
            val q = query.trim()
            if (q.isEmpty()) {
                Toast.makeText(requireContext(), "Device ya problem likho, jaise 'iPhone 14 battery'", Toast.LENGTH_SHORT).show()
                return
            }
            progress.visibility = View.VISIBLE
            empty.visibility = View.GONE
            Thread {
                try {
                    val results = IfixitApi.search(q)
                    activity?.runOnUiThread {
                        progress.visibility = View.GONE
                        adapter.setItems(results)
                        empty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
                        if (results.isEmpty()) empty.text = "Koi guide nahi mili. Doosra model ya word try karo."
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        progress.visibility = View.GONE
                        empty.visibility = View.VISIBLE
                        empty.text = e.message ?: "Search nahi ho paya. Internet check karo."
                    }
                }
            }.start()
        }

        searchBtn.setOnClickListener { doSearch(searchBox.text.toString()) }
        searchBox.setOnEditorActionListener { _, _, _ ->
            doSearch(searchBox.text.toString()); true
        }
        val chips = listOf(
            R.id.chip1 to "iPhone 14 Battery Replacement",
            R.id.chip2 to "Samsung Galaxy Screen Replacement",
            R.id.chip3 to "iPhone Battery Replacement",
            R.id.chip4 to "MacBook Battery Replacement"
        )
        for ((id, query) in chips) {
            v.findViewById<Button>(id)?.setOnClickListener {
                searchBox.setText(query)
                doSearch(query)
            }
        }
        return v
    }
}
