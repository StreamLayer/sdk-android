package io.streamlayer.demo.common.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import io.streamlayer.demo.R
import io.streamlayer.demo.common.firebase.NotificationChannelsHelper.VERSION

/**
 *
 * This is a helper class which keeps configuration of notification channels on Android versions [Build.VERSION_CODES.O] and newer.
 * If creating a new channel, just update the [VERSION] so the new channels are registered to the system.
 */

object NotificationChannelsHelper {

    private const val PREFS_NAME = "NOTIF_CHANNELS"
    private const val CHANNELS_VERSION = "channels_version"

    /**
     * Version of channels setup. If added new channel, bump the version
     */
    private const val VERSION = 1

    /**
     * When adding new channel, update VERSION and getChannelName/Description methods.
     * Use [Channel.name] as channelId in [android.support.v4.app.NotificationCompat.Builder] constructor
     *
     * @see [com.bornfight.tasteatlas.firebase.MyFirebaseMessagingService.showNotification]
     */
    enum class Channel {
        general
    }

    /**
     * Needs to be called before pushing the notification, best in [App.onCreate] method
     * @param context
     */
    fun initChannels(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && VERSION > sharedPreferences.getInt(
                CHANNELS_VERSION,
                0
            )
        ) {
            sharedPreferences.edit().putInt(CHANNELS_VERSION, VERSION).apply()
            setupChannels(context)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setupChannels(context: Context) {
        val mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        for (channel in Channel.values()) {
            val notificationChannel = NotificationChannel(
                channel.name,
                getChannelName(context, channel),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description = getChannelDescription(context, channel)
            mNotificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun getChannelName(context: Context, channel: Channel): String {
        return when (channel) {
            NotificationChannelsHelper.Channel.general -> context.getString(R.string.general)
        }
    }

    private fun getChannelDescription(context: Context, channel: Channel): String {
        return when (channel) {
            NotificationChannelsHelper.Channel.general -> context.getString(R.string.general_notif_info)
        }
    }

}
