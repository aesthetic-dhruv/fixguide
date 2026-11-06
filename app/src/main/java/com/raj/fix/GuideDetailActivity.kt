package com.raj.fix

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Guide detail: full steps with images, tools, parts, difficulty + time,
 * conclusion, Save Offline, Favourite, and "Open original on iFixit".
 *
 * ATTRIBUTION (CC-BY, required on every guide): author name, "Source: iFixit",
 * and a link to the original guide URL are shown in the footer. This app is
 * NOT iFixit — its name is Fix — it only shows iFixit's openly licensed
 * guide content with credit.
 */
class GuideDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GUIDE_ID = "guide_id"
        const val EXTRA_TITLE = "title"
    }

    private var guideId: Int = 0
    private var detail: GuideDetail? = null
    private var rawJson: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        guideId = intent.getIntExtra(EXTRA_GUIDE_ID, 0)
        val fallbackTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Guide"
        if (guideId == 0) {
            Toast.makeText(this, "Guide id nahi mila.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        title = fallbackTitle

        val progress = findViewById<ProgressBar>(R.id.detailProgress)
        val titleView = findViewById<TextView>(R.id.detailTitle)
        val metaView = findViewById<TextView>(R.id.detailMeta)
        val introView = findViewById<TextView>(R.id.detailIntro)
        val toolsView = findViewById<TextView>(R.id.detailTools)
        val partsView = findViewById<TextView>(R.id.detailParts)
        val conclusionView = findViewById<TextView>(R.id.detailConclusion)
        val creditView = findViewById<TextView>(R.id.detailCredit)
        val stepsList = findViewById<RecyclerView>(R.id.stepsList)
        stepsList.layoutManager = LinearLayoutManager(this)
        stepsList.isNestedScrollingEnabled = false
        val saveBtn = findViewById<Button>(R.id.btnSaveOffline)
        val favBtn = findViewById<Button>(R.id.btnFavourite)
        val openBtn = findViewById<Button>(R.id.btnOpenOriginal)

        fun refreshButtons() {
            saveBtn.text = if (OfflineStore.exists(this, guideId)) "✓ Offline Saved" else "Save Offline"
            favBtn.text = if (Prefs.isFavourite(this, guideId)) "★ Favourite" else "☆ Favourite"
        }
        refreshButtons()

        fun showDetail(d: GuideDetail) {
            detail = d
            title = d.title
            Prefs.setFavouriteTitle(this, guideId, d.title)
            titleView.text = d.title
            val meta = StringBuilder()
            if (d.category.isNotEmpty()) meta.append(d.category)
            if (d.subject.isNotEmpty()) meta.append(" • ").append(d.subject)
            meta.append("\nDifficulty: ${d.difficulty}")
            val time = if (d.timeMinSec > 0 && d.timeMaxSec > d.timeMinSec) {
                "${Models.formatTime(d.timeMinSec)} – ${Models.formatTime(d.timeMaxSec)}"
            } else Models.formatTime(d.timeMaxSec)
            meta.append(" • Time: $time")
            meta.append(" • Steps: ${d.steps.size}")
            metaView.text = meta.toString()
            introView.text = d.introRaw
            introView.visibility = if (d.introRaw.isBlank()) View.GONE else View.VISIBLE
            val toolsText = StringBuilder()
            for (t in d.tools) {
                toolsText.append("• ${t.text}")
                if (t.quantity > 1) toolsText.append(" ×${t.quantity}")
                if (t.optional) toolsText.append(" (optional)")
                toolsText.append("\n")
            }
            toolsView.text = toolsText.toString().trim()
            toolsView.visibility = if (d.tools.isEmpty()) View.GONE else View.VISIBLE
            findViewById<TextView>(R.id.toolsHeading).visibility =
                if (d.tools.isEmpty()) View.GONE else View.VISIBLE
            val partsText = StringBuilder()
            for (p in d.parts) {
                partsText.append("• ${p.text}")
                if (p.quantity > 1) partsText.append(" ×${p.quantity}")
                partsText.append("\n")
            }
            partsView.text = partsText.toString().trim()
            partsView.visibility = if (d.parts.isEmpty()) View.GONE else View.VISIBLE
            findViewById<TextView>(R.id.partsHeading).visibility =
                if (d.parts.isEmpty()) View.GONE else View.VISIBLE
            conclusionView.text = d.conclusionRaw
            conclusionView.visibility = if (d.conclusionRaw.isBlank()) View.GONE else View.VISIBLE
            findViewById<TextView>(R.id.conclusionHeading).visibility =
                if (d.conclusionRaw.isBlank()) View.GONE else View.VISIBLE
            creditView.text = "Guide by ${d.authorName.ifEmpty { "iFixit community" }}  •  Source: iFixit (CC-BY)  •  ${d.url}"
            stepsList.adapter = StepAdapter(d.steps)
            refreshButtons()
        }

        saveBtn.setOnClickListener {
            val d = detail
            if (d == null) {
                Toast.makeText(this, "Pehle guide load hone do.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Thread {
                try {
                    val raw = rawJson ?: IfixitApi.guideRawJson(guideId)
                    OfflineStore.save(this, guideId, raw)
                    runOnUiThread {
                        refreshButtons()
                        Toast.makeText(this, "Offline save ho gayi — bina internet ke khulegi.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Save nahi ho payi: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
        favBtn.setOnClickListener {
            val nowFav = Prefs.toggleFavourite(this, guideId)
            detail?.let { Prefs.setFavouriteTitle(this, guideId, it.title) }
            refreshButtons()
            Toast.makeText(
                this,
                if (nowFav) "Favourite me add ho gayi." else "Favourite se hata di.",
                Toast.LENGTH_SHORT
            ).show()
        }
        openBtn.setOnClickListener {
            val url = detail?.url
            if (url.isNullOrEmpty()) {
                Toast.makeText(this, "Original link abhi nahi mila.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        // Offline copy first if it exists (opens instantly, no internet needed),
        // then refresh from the API in the background when online.
        val offline = OfflineStore.load(this, guideId)
        if (offline != null) {
            progress.visibility = View.GONE
            showDetail(offline)
        }
        Thread {
            try {
                val raw = IfixitApi.guideRawJson(guideId)
                rawJson = raw
                val d = Models.parseDetail(org.json.JSONObject(raw))
                runOnUiThread {
                    progress.visibility = View.GONE
                    showDetail(d)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progress.visibility = View.GONE
                    if (detail == null) {
                        titleView.text = fallbackTitle
                        metaView.text = e.message ?: "Guide load nahi hui. Internet check karo."
                    }
                }
            }
        }.start()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
