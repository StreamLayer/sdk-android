package io.streamlayer.demo.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.streamlayer.demo.R
import io.streamlayer.demo.common.ext.bindingDelegate
import io.streamlayer.demo.databinding.FragmentInviteWelcomeBinding
import io.streamlayer.sdk.StreamLayer
import io.streamlayer.sdk.model.deeplink.InviteData

// different types of invites - probably you want to show different ui content
internal enum class InviteType { WATCH_PARTY, CHAT, DEFAULT }

// ui class which show example of StreamLayer Invite link description
class InviteWelcomeFragment : BottomSheetDialogFragment() {

    companion object {

        private const val INVITE_DATA_EXTRA = "INVITE_DATA_EXTRA"

        fun newInstance(inviteData: InviteData) = InviteWelcomeFragment().apply {
            arguments = Bundle().apply {
                putSerializable(INVITE_DATA_EXTRA, inviteData)
            }
        }
    }

    private val binding: FragmentInviteWelcomeBinding by bindingDelegate(
        FragmentInviteWelcomeBinding::bind
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_invite_welcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        bind(getInviteData())
    }

    private fun getInviteData(): InviteData =
        arguments?.getSerializable(INVITE_DATA_EXTRA) as InviteData

    private fun setupUI() {
        with(binding) {
            actionButton.setOnClickListener {
                dismiss()
                // start StreamLayer Invite flow based on your AndroidManifest intent-filter configuration
                StreamLayer.handleInvite(getInviteData(), requireContext())
            }
            closeIcon.setOnClickListener { dismiss() }
        }
    }

    private fun bind(inviteData: InviteData) {
        // get invite type based on invite data
        val type =
            if (inviteData.groupId != null && inviteData.eventId != null) InviteType.WATCH_PARTY
            else if (inviteData.groupId != null) InviteType.CHAT
            else InviteType.DEFAULT
        with(binding) {
            // show data based on invite type
            titleView.setText(
                when (type) {
                    InviteType.WATCH_PARTY -> R.string.invite_to_wp_title
                    InviteType.CHAT -> R.string.invite_to_chat_title
                    InviteType.DEFAULT -> R.string.invite_title
                }
            )
            iconView.setImageResource(
                when (type) {
                    InviteType.WATCH_PARTY -> io.streamlayer.sdk.R.drawable.slr_ic_watch_party
                    InviteType.CHAT -> io.streamlayer.sdk.R.drawable.slr_ic_chat
                    InviteType.DEFAULT -> io.streamlayer.sdk.R.drawable.slr_ic_chat
                }
            )
            textView.text = when (type) {
                InviteType.CHAT -> getString(
                    io.streamlayer.sdk.R.string.slr_is_waiting_to_chat,
                    inviteData.user.name
                )
                else -> getString(
                    io.streamlayer.sdk.R.string.slr_is_waiting_to_hang_out,
                    inviteData.user.name
                )
            }

            inviteData.user.avatar?.let {
                // load user avatar using your image loading library here
            } ?: kotlin.runCatching {
                // or set some default value
            }

            actionButton.setText(
                when (type) {
                    InviteType.WATCH_PARTY -> io.streamlayer.sdk.R.string.slr_join_watch_party
                    InviteType.CHAT -> io.streamlayer.sdk.R.string.slr_join_chat
                    else -> io.streamlayer.sdk.R.string.slr_start_now
                }
            )
        }
    }
}