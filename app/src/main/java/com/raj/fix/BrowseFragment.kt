package com.raj.fix

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Device browse: Phone > iPhone / Android Phone > Samsung / Xiaomi / OnePlus...
 * Uses /api/2.0/categories/<name> — children = sub-models, guides = that
 * category's own guides. Back goes one level up, not out of the app.
 */
class BrowseFragment : Fragment() {

    private val path = mutableListOf("Phone")
    private lateinit var catAdapter: CategoryAdapter
    private lateinit var guideAdapter: GuideAdapter
    private lateinit var crumb: TextView
    private lateinit var desc: TextView
    private lateinit var progress: ProgressBar
    private lateinit var empty: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_browse, container, false)
        crumb = v.findViewById(R.id.breadcrumb)
        desc = v.findViewById(R.id.categoryDesc)
        progress = v.findViewById<ProgressBar>(R.id.browseProgress)
        empty = v.findViewById<TextView>(R.id.browseEmpty)
        val catList = v.findViewById<RecyclerView>(R.id.categoryList)
        val guideList = v.findViewById<RecyclerView>(R.id.categoryGuides)
        catList.layoutManager = LinearLayoutManager(requireContext())
        guideList.layoutManager = LinearLayoutManager(requireContext())
        catAdapter = CategoryAdapter(emptyList()) { child ->
            path.add(child)
            loadCurrent()
        }
        guideAdapter = GuideAdapter(emptyList()) { guide ->
            (activity as? MainActivity)?.openGuide(guide)
        }
        catList.adapter = catAdapter
        guideList.adapter = guideAdapter

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (path.size > 1) {
                        path.removeAt(path.size - 1)
                        loadCurrent()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        )
        loadCurrent()
        return v
    }

    private fun loadCurrent() {
        val name = path.last()
        crumb.text = path.joinToString("  >  ")
        progress.visibility = View.VISIBLE
        empty.visibility = View.GONE
        Thread {
            try {
                val node = IfixitApi.category(name)
                activity?.runOnUiThread {
                    progress.visibility = View.GONE
                    desc.text = node.description
                    desc.visibility = if (node.description.isBlank()) View.GONE else View.VISIBLE
                    catAdapter.setItems(node.children)
                    guideAdapter.setItems(node.guides)
                    empty.visibility =
                        if (node.children.isEmpty() && node.guides.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    progress.visibility = View.GONE
                    empty.visibility = View.VISIBLE
                    empty.text = e.message ?: "Category load nahi hui. Internet check karo."
                }
            }
        }.start()
    }
}
