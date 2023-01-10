package io.streamlayer.demo.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.branch.referral.Branch
import io.streamlayer.demo.auth.AuthActivity
import io.streamlayer.demo.databinding.ActivityMainBinding
import io.streamlayer.demo.live.LiveActivity
import io.streamlayer.demo.managed.ManagedGroupActivity
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.model.deeplink.InviteData

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val branchReferralInitListener =
        Branch.BranchReferralInitListener { linkProperties, error ->
            if (error == null) linkProperties?.let {
                StreamLayer.getInvite(it.toString())?.let {
                    // 1. remember InviteData link here - it's an entry point to StreamLayer Invite flow
                    // 2. forward user to logic/registration flow first or check if user is authorized
                    // 3. show some ui dialog/screen which contains additional info about StreamLayer Invite
                    // or call StreamLayer.handleInvite(it, this) here to start StreamLayer Invite flow.
                    if (StreamLayer.isUserAuthorized()) StreamLayer.handleInvite(it, this)
                    else openInviteDescription(it)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.authBtn.setOnClickListener { AuthActivity.open(this) }
        binding.liveBtn.setOnClickListener { LiveActivity.open(this) }
        binding.managedBtn.setOnClickListener { ManagedGroupActivity.open(this) }
    }

    override fun onStart() {
        super.onStart()
        Branch.sessionBuilder(this)
            .withCallback(branchReferralInitListener)
            .withData(this.intent?.data).init()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        this.intent = intent
        Branch.sessionBuilder(this)
            .withCallback(branchReferralInitListener)
            .reInit()
    }

    private fun openInviteDescription(inviteData: InviteData) {
        val fragment = InviteWelcomeFragment.newInstance(inviteData)
        fragment.show(supportFragmentManager, "InviteWelcomeFragment")
    }
}