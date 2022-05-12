package io.streamlayer.demo.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import io.streamlayer.demo.R
import io.streamlayer.demo.common.ext.collectWhenStarted
import io.streamlayer.demo.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    companion object {

        fun open(context: Context) {
            context.startActivity(Intent(context, AuthActivity::class.java))
        }
    }

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
        bind()
    }

    private fun setupUI() {
        with(binding) {
            slLoginButton.setOnClickListener {
                val token = slTokenView.text.toString()
                if (token.isEmpty()) return@setOnClickListener
                viewModel.bypassAuth(token)
            }
            slLogoutButton.setOnClickListener {
                viewModel.logout()
            }
        }
    }

    private fun bind() {
        viewModel.isUserAuthorized.collectWhenStarted(this) {
            binding.slAuthView.text =
                getString(if (it) R.string.user_is_authorized else R.string.user_is_not_authorized)
        }
    }

}