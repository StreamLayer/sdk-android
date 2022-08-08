package io.streamlayer.demo.compose.ui

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.fragment.app.FragmentActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import io.streamlayer.demo.compose.databinding.FragmentStreamlayerBinding
import io.streamlayer.sdk.main.StreamLayerFragment

class ComposeActivity : FragmentActivity() {

    private val viewModel: ComposeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StreamLayerDemoTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.background)
                ) {
                    val configuration = LocalConfiguration.current
                    val density = LocalDensity.current
                    val playerHeight = 210.dp
                    val playerModifier =
                        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                            Modifier
                                .height(playerHeight)
                                .fillMaxWidth()
                        else Modifier.fillMaxSize()
                    ExoPlayerUI(viewModel.player, playerModifier)
                    StreamLayerUI(
                        (density.density * playerHeight.value).toInt(),
                        Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.player.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        viewModel.player.playWhenReady = false
    }
}

@Composable
fun ExoPlayerUI(exoPlayer: ExoPlayer, modifier: Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = {
            StyledPlayerView(context).apply {
                hideController()
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                player = exoPlayer
            }
        })
}

@Composable
fun StreamLayerUI(playerHeight: Int, modifier: Modifier) {
    AndroidViewBinding(FragmentStreamlayerBinding::inflate, modifier = modifier) {
        with(fragmentContainerView.getFragment<StreamLayerFragment>()) {
            isMenuProfileEnabled = true
            overlayHeight = root.height - playerHeight
        }
    }
}
