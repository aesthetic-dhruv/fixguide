package com.raj.fix

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Fix home: 4 tabs — Search, Browse (devices), Saved (offline + favourites),
 * Fix AI (chat). Settings opens from the top-right gear.
 * App name/branding is Fix only. iFixit is credited on guide pages and
 * in Settings, never as the app's own name.
 */
class MainActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val browseFragment = BrowseFragment()
    private val savedFragment = SavedFragment()
    private val aiFragment = AiFragment()
    private var active: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, aiFragment, "ai").hide(aiFragment)
            .add(R.id.fragmentContainer, savedFragment, "saved").hide(savedFragment)
            .add(R.id.fragmentContainer, browseFragment, "browse").hide(browseFragment)
            .add(R.id.fragmentContainer, homeFragment, "home")
            .commit()

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setOnItemSelectedListener { item ->
            val next = when (item.itemId) {
                R.id.nav_search -> homeFragment
                R.id.nav_browse -> browseFragment
                R.id.nav_saved -> savedFragment
                R.id.nav_ai -> aiFragment
                else -> homeFragment
            }
            if (next != active) {
                supportFragmentManager.beginTransaction().hide(active).show(next).commit()
                active = next
                if (next == savedFragment) savedFragment.refresh()
            }
            true
        }
    }

    /** Open a guide from anywhere (search, browse, saved, AI results). */
    fun openGuide(guide: GuideSummary) {
        val intent = Intent(this, GuideDetailActivity::class.java)
        intent.putExtra(GuideDetailActivity.EXTRA_GUIDE_ID, guide.guideId)
        intent.putExtra(GuideDetailActivity.EXTRA_TITLE, guide.title)
        startActivity(intent)
    }
}
