package com.raj.fix

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GuideAdapter(
    private var items: List<GuideSummary>,
    private val onClick: (GuideSummary) -> Unit
) : RecyclerView.Adapter<GuideAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val thumb: ImageView = v.findViewById(R.id.guideThumb)
        val title: TextView = v.findViewById(R.id.guideTitle)
        val meta: TextView = v.findViewById(R.id.guideMeta)
        val diff: TextView = v.findViewById(R.id.guideDifficulty)
    }

    fun setItems(newItems: List<GuideSummary>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_guide, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val g = items[position]
        h.title.text = g.title
        val meta = StringBuilder()
        if (g.category.isNotEmpty()) meta.append(g.category)
        if (g.subject.isNotEmpty()) {
            if (meta.isNotEmpty()) meta.append(" • ")
            meta.append(g.subject)
        }
        val time = Models.formatTime(g.timeMaxSec)
        if (time != "—") {
            if (meta.isNotEmpty()) meta.append(" • ")
            meta.append(time)
        }
        if (g.summary.isNotBlank()) {
            meta.append("\n").append(g.summary.take(120))
        }
        h.meta.text = meta.toString()
        h.diff.text = g.difficulty
        ImageLoader.load(g.thumbUrl, h.thumb)
        h.itemView.setOnClickListener { onClick(g) }
    }

    override fun getItemCount(): Int = items.size
}

class CategoryAdapter(
    private var items: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.categoryName)
    }

    fun setItems(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        h.name.text = items[position]
        h.itemView.setOnClickListener { onClick(items[position]) }
    }

    override fun getItemCount(): Int = items.size
}

class StepAdapter(
    private val steps: List<GuideStep>
) : RecyclerView.Adapter<StepAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val number: TextView = v.findViewById(R.id.stepNumber)
        val title: TextView = v.findViewById(R.id.stepTitle)
        val body: TextView = v.findViewById(R.id.stepBody)
        val image: ImageView = v.findViewById(R.id.stepImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_step, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val s = steps[position]
        h.number.text = "Step ${position + 1}"
        h.title.text = if (s.title.isNotBlank()) s.title else "Step ${position + 1}"
        val sb = StringBuilder()
        for (line in s.lines) {
            if (line.text.isBlank()) continue
            val indent = "  ".repeat(line.level.coerceAtMost(3))
            val mark = when (line.bullet) {
                "red" -> "• "
                "black" -> "• "
                "orange" -> "⚠ "
                "blue" -> "ℹ "
                else -> "• "
            }
            sb.append(indent).append(mark).append(line.text).append("\n")
        }
        h.body.text = sb.toString().trim()
        h.body.visibility = if (sb.isEmpty()) View.GONE else View.VISIBLE
        if (s.imageUrls.isNotEmpty()) {
            h.image.visibility = View.VISIBLE
            ImageLoader.load(s.imageUrls[0], h.image)
        } else {
            h.image.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = steps.size
}

class ChatAdapter(
    private val items: MutableList<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val role: TextView = v.findViewById(R.id.chatRole)
        val text: TextView = v.findViewById(R.id.chatText)
    }

    fun add(msg: ChatMessage) {
        items.add(msg)
        notifyItemInserted(items.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val m = items[position]
        h.role.text = when (m.role) {
            "user" -> "Tum"
            "ai" -> "Fix AI"
            else -> "Fix"
        }
        h.text.text = m.text
    }

    override fun getItemCount(): Int = items.size
}
