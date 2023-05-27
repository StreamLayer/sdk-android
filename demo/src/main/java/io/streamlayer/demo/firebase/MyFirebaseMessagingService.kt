package io.streamlayer.demo.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.streamlayer.sdk.StreamLayer

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        StreamLayer.uploadDeviceFCMToken(application, s)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        if (!StreamLayer.handlePush(application, remoteMessage.data)) {
            // handle host notification
        }
    }

}
