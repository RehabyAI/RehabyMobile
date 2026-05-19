package com.rehaby.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rehaby.app.databinding.ActivityMainBinding
import com.rehaby.app.ui.HistoryFragment
import com.rehaby.app.ui.HomeFragment
import com.rehaby.app.ui.PatientErrorsFragment
import com.rehaby.app.ui.TrendsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchToTab(TAG_HOME)
                R.id.nav_trends -> switchToTab(TAG_TRENDS)
                R.id.nav_history -> switchToTab(TAG_HISTORY)
                R.id.nav_errors -> switchToTab(TAG_ERRORS)
                else -> return@setOnItemSelectedListener false
            }
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_home
            switchToTab(TAG_HOME)
        }
    }

    private fun switchToTab(tag: String) {
        val fm = supportFragmentManager
        var target = fm.findFragmentByTag(tag)
        if (target == null) {
            target = when (tag) {
                TAG_HOME -> HomeFragment()
                TAG_TRENDS -> TrendsFragment()
                TAG_HISTORY -> HistoryFragment()
                TAG_ERRORS -> PatientErrorsFragment()
                else -> return
            }
        }
        fm.beginTransaction().apply {
            for (t in TAB_TAGS) {
                val f = fm.findFragmentByTag(t)
                if (f != null && f !== target) {
                    hide(f)
                }
            }
            if (!target.isAdded) {
                add(R.id.fragmentContainer, target, tag)
            }
            show(target)
        }.commit()
    }

    companion object {
        private const val TAG_HOME = "tab_home"
        private const val TAG_TRENDS = "tab_trends"
        private const val TAG_HISTORY = "tab_history"
        private const val TAG_ERRORS = "tab_errors"
        private val TAB_TAGS = listOf(TAG_HOME, TAG_TRENDS, TAG_HISTORY, TAG_ERRORS)
    }
}
