package com.raj.fix

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Fix AI tab — the FixBot-like assistant, built legally:
 *   1. FIRST search the iFixit API for guides matching the user's words.
 *   2. Show those guides in the list below the chat (tap any to open it).
 *   3. THEN, only if a Gemini key is saved in Settings, ask Gemini with
 *      those guides' titles + steps as context, and show the Hinglish answer.
 *   4. If there is NO key: say so clearly ("Settings me Gemini key daalo")
 *      and NEVER pretend the AI answered — the guide results are still shown.
 */
class AiFragment : Fragment() {

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var guideAdapter: GuideAdapter
    private lateinit var chatList: RecyclerView
    private var busy = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_ai, container, false)
        val input = v.findViewById<EditText>(R.id.aiInput)
        val send = v.findViewById<Button>(R.id.aiSend)
        chatList = v.findViewById(R.id.chatList)
        chatList.layoutManager = LinearLayoutManager(requireContext())
        chatAdapter = ChatAdapter(messages)
        chatList.adapter = chatAdapter
        val results = v.findViewById<RecyclerView>(R.id.aiResults)
        results.layoutManager = LinearLayoutManager(requireContext())
        guideAdapter = GuideAdapter(emptyList()) { guide ->
            (activity as? MainActivity)?.openGuide(guide)
        }
        results.adapter = guideAdapter

        if (messages.isEmpty()) {
            chatAdapter.add(
                ChatMessage(
                    "ai",
                    "Namaste, main Fix AI hoon. Device + problem likho, jaise 'iPhone 14 battery drain' ya 'Samsung screen replacement'. Pehle main iFixit guides dhoondhunga, phir unke steps se Hinglish me samjhaunga.\n\nAI jawab ke liye Settings me apni Gemini key daalni hogi — bina key ke bhi guides dikha dunga."
                )
            )
        }

        fun ask(question: String) {
            val q = question.trim()
            if (q.isEmpty() || busy) return
            busy = true
            chatAdapter.add(ChatMessage("user", q))
            chatAdapter.add(ChatMessage("note", "iFixit me matching guides dhoondh raha hoon…"))
            chatList.scrollToPosition(messages.size - 1)
            input.setText("")
            Thread {
                try {
                    val guides = IfixitApi.search(q)
                    activity?.runOnUiThread {
                        guideAdapter.setItems(guides)
                        messages.removeAt(messages.size - 1) // remove "searching" note
                        chatAdapter.notifyItemRemoved(messages.size)
                    }
                    if (guides.isEmpty()) {
                        activity?.runOnUiThread {
                            chatAdapter.add(
                                ChatMessage(
                                    "ai",
                                    "Iske liye koi iFixit guide nahi mili. Model ka naam + part sahi likh kar dobara try karo, jaise 'iPhone 13 screen replacement'."
                                )
                            )
                            chatList.scrollToPosition(messages.size - 1)
                            busy = false
                        }
                        return@Thread
                    }
                    val ctx = context
                    if (ctx == null || !Prefs.hasGeminiKey(ctx)) {
                        activity?.runOnUiThread {
                            chatAdapter.add(
                                ChatMessage(
                                    "ai",
                                    "Neeche ${guides.size} matching guide(s) mil gayi hain — kisi par tap karke poori steps dekho.\n\nAI se Hinglish me step-wise samajhna hai to Settings me apni Gemini key daalo, phir dobara poochho. (Key sirf tumhare phone me rehti hai.)"
                                )
                            )
                            chatList.scrollToPosition(messages.size - 1)
                            busy = false
                        }
                        return@Thread
                    }
                    // Key exists: fetch top guide detail as context, then ask Gemini.
                    val details = mutableListOf<GuideDetail>()
                    for (g in guides.take(2)) {
                        try { details.add(IfixitApi.guide(g.guideId)) } catch (_: Exception) { }
                    }
                    val prompt = GeminiClient.buildPrompt(q, guides, details)
                    val answer = GeminiClient.ask(Prefs.getGeminiKey(ctx), prompt)
                    activity?.runOnUiThread {
                        chatAdapter.add(ChatMessage("ai", answer))
                        chatList.scrollToPosition(messages.size - 1)
                        busy = false
                    }
                } catch (e: Exception) {
                    activity?.runOnUiThread {
                        if (messages.isNotEmpty() && messages.last().role == "note") {
                            messages.removeAt(messages.size - 1)
                            chatAdapter.notifyItemRemoved(messages.size)
                        }
                        chatAdapter.add(
                            ChatMessage(
                                "ai",
                                e.message ?: "Kuch gadbad hui. Internet check karke dobara try karo."
                            )
                        )
                        chatList.scrollToPosition(messages.size - 1)
                        busy = false
                    }
                }
            }.start()
        }

        send.setOnClickListener { ask(input.text.toString()) }
        input.setOnEditorActionListener { _, _, _ -> ask(input.text.toString()); true }
        val suggestions = listOf(
            R.id.aiChip1 to "iPhone 14 battery drain",
            R.id.aiChip2 to "Samsung screen replacement",
            R.id.aiChip3 to "Phone garam ho raha hai"
        )
        for ((id, q) in suggestions) {
            v.findViewById<Button>(id)?.setOnClickListener { ask(q) }
        }
        return v
    }
}
