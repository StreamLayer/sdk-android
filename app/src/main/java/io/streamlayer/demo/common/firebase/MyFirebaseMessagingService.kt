package io.streamlayer.demo.common.firebase

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.streamlayer.sdk.StreamLayer
import javax.inject.Inject

/**
 * Created by tomislav on 27/01/2017.
 */

private const val TAG = "MyFirebaseMessagingServ"

class MyFirebaseMessagingService : FirebaseMessagingService(), HasAndroidInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        Log.d(TAG, "Refreshed token: $s")

        StreamLayer.uploadDeviceFCMToken(s)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        Log.d(TAG, "Notification data: " + remoteMessage.data)

        if (!StreamLayer.handleStreamLayerPush(remoteMessage.data)) {
            // handle host notification
        }
    }

}
