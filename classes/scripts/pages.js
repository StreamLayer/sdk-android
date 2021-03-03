var pages = [{'name': 'object StreamLayer', 'description':'io.streamlayer.sdk.StreamLayer', 'location':'sdk/io.streamlayer.sdk/-stream-layer/index.html', 'searchKey':'StreamLayer'},
{'name': 'interface AudioDuckingListener', 'description':'io.streamlayer.sdk.StreamLayer.AudioDuckingListener', 'location':'sdk/io.streamlayer.sdk/-stream-layer/-audio-ducking-listener/index.html', 'searchKey':'AudioDuckingListener'},
{'name': 'abstract fun disableAudioDucking()', 'description':'io.streamlayer.sdk.StreamLayer.AudioDuckingListener.disableAudioDucking', 'location':'sdk/io.streamlayer.sdk/-stream-layer/-audio-ducking-listener/disable-audio-ducking.html', 'searchKey':'disableAudioDucking'},
{'name': 'abstract fun requestAudioDucking()', 'description':'io.streamlayer.sdk.StreamLayer.AudioDuckingListener.requestAudioDucking', 'location':'sdk/io.streamlayer.sdk/-stream-layer/-audio-ducking-listener/request-audio-ducking.html', 'searchKey':'requestAudioDucking'},
{'name': 'enum LogLevel : Enum<StreamLayer.LogLevel> ', 'description':'io.streamlayer.sdk.StreamLayer.LogLevel', 'location':'sdk/io.streamlayer.sdk/-stream-layer/-log-level/index.html', 'searchKey':'LogLevel'},
{'name': 'DEBUG()', 'description':'io.streamlayer.sdk.StreamLayer.LogLevel.DEBUG', 'location':'sdk/io.streamlayer.sdk/-stream-layer/-log-level/-d-e-b-u-g/index.html', 'searchKey':'DEBUG'},
{'name': 'ERROR()', 'description':'io.streamlayer.sdk.StreamLayer.LogLevel.ERROR', 'location':'sdk/io.streamlayer.sdk/-stream-layer/-log-level/-e-r-r-o-r/index.html', 'searchKey':'ERROR'},
{'name': 'INFO()', 'description':'io.streamlayer.sdk.StreamLayer.LogLevel.INFO', 'location':'sdk/io.streamlayer.sdk/-stream-layer/-log-level/-i-n-f-o/index.html', 'searchKey':'INFO'},
{'name': 'VERBOSE()', 'description':'io.streamlayer.sdk.StreamLayer.LogLevel.VERBOSE', 'location':'sdk/io.streamlayer.sdk/-stream-layer/-log-level/-v-e-r-b-o-s-e/index.html', 'searchKey':'VERBOSE'},
{'name': 'WARNING()', 'description':'io.streamlayer.sdk.StreamLayer.LogLevel.WARNING', 'location':'sdk/io.streamlayer.sdk/-stream-layer/-log-level/-w-a-r-n-i-n-g/index.html', 'searchKey':'WARNING'},
{'name': 'interface LogListener', 'description':'io.streamlayer.sdk.StreamLayer.LogListener', 'location':'sdk/io.streamlayer.sdk/-stream-layer/-log-listener/index.html', 'searchKey':'LogListener'},
{'name': 'abstract fun log(level: StreamLayer.LogLevel, msg: String)', 'description':'io.streamlayer.sdk.StreamLayer.LogListener.log', 'location':'sdk/io.streamlayer.sdk/-stream-layer/-log-listener/log.html', 'searchKey':'log'},
{'name': 'interface StreamEventChangeListener', 'description':'io.streamlayer.sdk.StreamLayer.StreamEventChangeListener', 'location':'sdk/io.streamlayer.sdk/-stream-layer/-stream-event-change-listener/index.html', 'searchKey':'StreamEventChangeListener'},
{'name': 'abstract fun onStreamChanged(id: String)', 'description':'io.streamlayer.sdk.StreamLayer.StreamEventChangeListener.onStreamChanged', 'location':'sdk/io.streamlayer.sdk/-stream-layer/-stream-event-change-listener/on-stream-changed.html', 'searchKey':'onStreamChanged'},
{'name': 'fun <T : CharSequence> changeStreamEvent(event: T)', 'description':'io.streamlayer.sdk.StreamLayer.changeStreamEvent', 'location':'sdk/io.streamlayer.sdk/-stream-layer/change-stream-event.html', 'searchKey':'changeStreamEvent'},
{'name': 'fun handleDeepLink(intent: Intent, activity: AppCompatActivity): Boolean', 'description':'io.streamlayer.sdk.StreamLayer.handleDeepLink', 'location':'sdk/io.streamlayer.sdk/-stream-layer/handle-deep-link.html', 'searchKey':'handleDeepLink'},
{'name': 'fun handlePush(context: Context, data: Map<String, String>): Boolean', 'description':'io.streamlayer.sdk.StreamLayer.handlePush', 'location':'sdk/io.streamlayer.sdk/-stream-layer/handle-push.html', 'searchKey':'handlePush'},
{'name': 'fun handleReferralLink(json: String, activity: AppCompatActivity): Boolean', 'description':'io.streamlayer.sdk.StreamLayer.handleReferralLink', 'location':'sdk/io.streamlayer.sdk/-stream-layer/handle-referral-link.html', 'searchKey':'handleReferralLink'},
{'name': 'fun initializeApp(context: Context, sdkKey: String)', 'description':'io.streamlayer.sdk.StreamLayer.initializeApp', 'location':'sdk/io.streamlayer.sdk/-stream-layer/initialize-app.html', 'searchKey':'initializeApp'},
{'name': 'fun setAudioDuckingListener(listener: StreamLayer.AudioDuckingListener?)', 'description':'io.streamlayer.sdk.StreamLayer.setAudioDuckingListener', 'location':'sdk/io.streamlayer.sdk/-stream-layer/set-audio-ducking-listener.html', 'searchKey':'setAudioDuckingListener'},
{'name': 'fun setLogListener(listener: StreamLayer.LogListener?)', 'description':'io.streamlayer.sdk.StreamLayer.setLogListener', 'location':'sdk/io.streamlayer.sdk/-stream-layer/set-log-listener.html', 'searchKey':'setLogListener'},
{'name': 'fun setLogcatLoggingEnabled(isEnabled: Boolean)', 'description':'io.streamlayer.sdk.StreamLayer.setLogcatLoggingEnabled', 'location':'sdk/io.streamlayer.sdk/-stream-layer/set-logcat-logging-enabled.html', 'searchKey':'setLogcatLoggingEnabled'},
{'name': 'fun setStreamEventChangeListener(listener: StreamLayer.StreamEventChangeListener?)', 'description':'io.streamlayer.sdk.StreamLayer.setStreamEventChangeListener', 'location':'sdk/io.streamlayer.sdk/-stream-layer/set-stream-event-change-listener.html', 'searchKey':'setStreamEventChangeListener'},
{'name': 'fun uploadDeviceFCMToken(context: Context, token: String)', 'description':'io.streamlayer.sdk.StreamLayer.uploadDeviceFCMToken', 'location':'sdk/io.streamlayer.sdk/-stream-layer/upload-device-f-c-m-token.html', 'searchKey':'uploadDeviceFCMToken'},
{'name': 'object StreamLayerAuth', 'description':'io.streamlayer.sdk.StreamLayerAuth', 'location':'sdk/io.streamlayer.sdk/-stream-layer-auth/index.html', 'searchKey':'StreamLayerAuth'},
{'name': 'fun authorizationBypass(schema: String, token: String): Completable', 'description':'io.streamlayer.sdk.StreamLayerAuth.authorizationBypass', 'location':'sdk/io.streamlayer.sdk/-stream-layer-auth/authorization-bypass.html', 'searchKey':'authorizationBypass'},
{'name': 'fun logout(): Completable', 'description':'io.streamlayer.sdk.StreamLayerAuth.logout', 'location':'sdk/io.streamlayer.sdk/-stream-layer-auth/logout.html', 'searchKey':'logout'},
{'name': 'fun setExternalAuthEnabled(value: Boolean)', 'description':'io.streamlayer.sdk.StreamLayerAuth.setExternalAuthEnabled', 'location':'sdk/io.streamlayer.sdk/-stream-layer-auth/set-external-auth-enabled.html', 'searchKey':'setExternalAuthEnabled'},
{'name': 'object StreamLayerUI', 'description':'io.streamlayer.sdk.StreamLayerUI', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/index.html', 'searchKey':'StreamLayerUI'},
{'name': 'open class CustomNotification(title: String?, description: String?, layoutId: Int?, actionId: Int?, iconBackgroundColor: Int?, iconUrl: String?) : NotificationBuilder', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomNotification', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-notification/index.html', 'searchKey':'CustomNotification'},
{'name': 'fun CustomNotification(title: String?, description: String?, layoutId: Int?, actionId: Int?, iconBackgroundColor: Int?, iconUrl: String?)', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomNotification.CustomNotification', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-notification/-custom-notification.html', 'searchKey':'CustomNotification'},
{'name': 'data class CustomOverlay(actionId: Int, titleId: Int, iconId: Int, fullClassName: String)', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomOverlay', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-overlay/index.html', 'searchKey':'CustomOverlay'},
{'name': 'fun CustomOverlay(actionId: Int, titleId: Int, iconId: Int, fullClassName: String)', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomOverlay.CustomOverlay', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-overlay/-custom-overlay.html', 'searchKey':'CustomOverlay'},
{'name': 'operator fun component1(): Int', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomOverlay.component1', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-overlay/component1.html', 'searchKey':'component1'},
{'name': 'operator fun component1(): Int?', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomTheme.component1', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-theme/component1.html', 'searchKey':'component1'},
{'name': 'operator fun component2(): Int', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomOverlay.component2', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-overlay/component2.html', 'searchKey':'component2'},
{'name': 'operator fun component2(): Int?', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomTheme.component2', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-theme/component2.html', 'searchKey':'component2'},
{'name': 'operator fun component3(): Int', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomOverlay.component3', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-overlay/component3.html', 'searchKey':'component3'},
{'name': 'operator fun component3(): Int?', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomTheme.component3', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-theme/component3.html', 'searchKey':'component3'},
{'name': 'operator fun component4(): String', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomOverlay.component4', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-overlay/component4.html', 'searchKey':'component4'},
{'name': 'operator fun component4(): Int?', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomTheme.component4', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-theme/component4.html', 'searchKey':'component4'},
{'name': 'fun copy(actionId: Int, titleId: Int, iconId: Int, fullClassName: String): StreamLayerUI.CustomOverlay', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomOverlay.copy', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-overlay/copy.html', 'searchKey':'copy'},
{'name': 'fun copy(authTheme: Int?, mainTheme: Int?, profileTheme: Int?, baseTheme: Int?, watchPartyTheme: Int?, inviteTheme: Int?): StreamLayerUI.CustomTheme', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomTheme.copy', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-theme/copy.html', 'searchKey':'copy'},
{'name': 'fun position(position: Int): StreamLayerUI.CustomOverlay', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomOverlay.position', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-overlay/position.html', 'searchKey':'position'},
{'name': 'fun showOnBottomMenu(show: Boolean): StreamLayerUI.CustomOverlay', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomOverlay.showOnBottomMenu', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-overlay/show-on-bottom-menu.html', 'searchKey':'showOnBottomMenu'},
{'name': 'data class CustomTheme constructor(authTheme: Int?, mainTheme: Int?, profileTheme: Int?, baseTheme: Int?, watchPartyTheme: Int?, inviteTheme: Int?)', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomTheme', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-theme/index.html', 'searchKey':'CustomTheme'},
{'name': 'fun CustomTheme(authTheme: Int?, mainTheme: Int?, profileTheme: Int?, baseTheme: Int?, watchPartyTheme: Int?, inviteTheme: Int?)', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomTheme.CustomTheme', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-theme/-custom-theme.html', 'searchKey':'CustomTheme'},
{'name': 'operator fun component5(): Int?', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomTheme.component5', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-theme/component5.html', 'searchKey':'component5'},
{'name': 'operator fun component6(): Int?', 'description':'io.streamlayer.sdk.StreamLayerUI.CustomTheme.component6', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/-custom-theme/component6.html', 'searchKey':'component6'},
{'name': 'fun addCustomOverlays(overlay: Array<StreamLayerUI.CustomOverlay>): StreamLayerUI', 'description':'io.streamlayer.sdk.StreamLayerUI.addCustomOverlays', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/add-custom-overlays.html', 'searchKey':'addCustomOverlays'},
{'name': 'fun hideLaunchButton(context: Context)', 'description':'io.streamlayer.sdk.StreamLayerUI.hideLaunchButton', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/hide-launch-button.html', 'searchKey':'hideLaunchButton'},
{'name': 'fun sendCustomNotification(notification: StreamLayerUI.CustomNotification): StreamLayerUI', 'description':'io.streamlayer.sdk.StreamLayerUI.sendCustomNotification', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/send-custom-notification.html', 'searchKey':'sendCustomNotification'},
{'name': 'fun setCustomTheme(theme: StreamLayerUI.CustomTheme): StreamLayerUI', 'description':'io.streamlayer.sdk.StreamLayerUI.setCustomTheme', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/set-custom-theme.html', 'searchKey':'setCustomTheme'},
{'name': 'fun setShareMessage(message: String?): StreamLayerUI', 'description':'io.streamlayer.sdk.StreamLayerUI.setShareMessage', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/set-share-message.html', 'searchKey':'setShareMessage'},
{'name': 'fun setWaveMessage(message: String?): StreamLayerUI', 'description':'io.streamlayer.sdk.StreamLayerUI.setWaveMessage', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/set-wave-message.html', 'searchKey':'setWaveMessage'},
{'name': 'fun showLaunchButton(context: Context)', 'description':'io.streamlayer.sdk.StreamLayerUI.showLaunchButton', 'location':'sdk/io.streamlayer.sdk/-stream-layer-u-i/show-launch-button.html', 'searchKey':'showLaunchButton'}]