package io.streamlayer.demo.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import io.branch.referral.Branch
import io.branch.referral.Branch.BranchReferralInitListener
import io.streamlayer.demo.R
import io.streamlayer.demo.databinding.ActivityMainBinding
import io.streamlayer.demo.databinding.LayoutMainContentBinding
import io.streamlayer.sdk.StreamLayer

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val branchReferralInitListener =
        BranchReferralInitListener { linkProperties, error ->
            if (error == null) linkProperties?.let {
                if (StreamLayer.isReferralLink(it.toString())) {
                    // do host logic if needed
                    StreamLayer.handleReferralLink(it.toString(), this)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun setupBottomNavView() {
        val merged = LayoutMainContentBinding.bind(binding.root)
        merged.bnv.setupWithNavController(findNavController(R.id.nav_host_fragment))
        merged.bnv.setOnNavigationItemSelectedListener(BottomNavHandler(merged))
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onStart() {
        super.onStart()
        Branch.sessionBuilder(this)
            .withCallback(branchReferralInitListener)
            .withData(this.intent?.data).init()
    }

    override fun onResume() {
        super.onResume()
        setupBottomNavView()
        if (!StreamLayer.handleDeepLink(intent, this)) {
            // do host logic if needed
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        this.intent = intent
        Branch.sessionBuilder(this)
            .withCallback(branchReferralInitListener)
            .reInit()
        if (!StreamLayer.handleDeepLink(intent, this)) {
            // do host logic if needed
        }
    }
}