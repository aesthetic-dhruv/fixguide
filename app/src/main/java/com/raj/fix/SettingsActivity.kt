package com.raj.fix

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Settings: Gemini API key (in-app only, stored locally on this phone —
 * never in the app code, never uploaded), offline cache clear, and the
 * attribution / about page.
 *
 * How to get a key (free, user does this himself): Google AI Studio
 * (aistudio.google.com) > Get API key > Create. Paste it here.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Settings"

        val keyBox = findViewById<EditText>(R.id.geminiKeyBox)
        keyBox.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val status = findViewById<TextView>(R.id.keyStatus)
        val about = findViewById<TextView>(R.id.aboutText)

        fun refreshStatus() {
            val has = Prefs.hasGeminiKey(this)
            status.text = if (has) {
                "Gemini key saved hai ✓ — Fix AI ab AI jawab de sakta hai."
            } else {
                "Gemini key nahi hai — Fix AI sirf guides dikhayega, AI jawab nahi dega."
            }
            if (!has) keyBox.setText("")
        }
        refreshStatus()

        about.text = "Fix v1.0 — Raj ki apni repair-guide app.\n\n" +
            "Guides: iFixit public API 2.0 se (CC-BY). Har guide par author, 'Source: iFixit' aur original link dikhaya jata hai. Ye app iFixit ki official app NAHI hai aur iFixit se affiliated nahi hai.\n\n" +
            "Fix AI: tumhari Gemini key se chalta hai. Key sirf is phone me save hoti hai aur sirf Google Gemini ko bheji jati hai — kahin aur nahi.\n\n" +
            "Safety: Battery ya screen kholne se pehle phone band karo. Battery puncture mat karo, garam battery ko paani me mat daalo. Agar sure nahi ho to guide ke steps follow karo ya experienced technician se poochho."

        findViewById<Button>(R.id.btnSaveKey).setOnClickListener {
            val key = keyBox.text.toString().trim()
            if (key.length < 10) {
                Toast.makeText(this, "Key bahut chhoti hai — poori key paste karo.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Prefs.setGeminiKey(this, key)
            keyBox.setText("")
            refreshStatus()
            Toast.makeText(this, "Gemini key save ho gayi (sirf is phone me).", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnClearKey).setOnClickListener {
            Prefs.clearGeminiKey(this)
            refreshStatus()
            Toast.makeText(this, "Gemini key hata di.", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnClearCache).setOnClickListener {
            val n = OfflineStore.clearAll(this)
            Toast.makeText(this, "$n offline guide(s) delete ho gayi.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
