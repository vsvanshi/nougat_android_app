package app.nougat

import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.nougat.design.Accent
import app.nougat.playback.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import app.nougat.design.NougatTheme
import app.nougat.screens.AppShell

class MainActivity : ComponentActivity() {
    private var controller: ListenableFuture<MediaController>? = null

    /** Connecting to the media service starts it, so playback can carry on in the background. */
    override fun onStart() {
        super.onStart()
        controller = MediaController.Builder(this, SessionToken(this, ComponentName(this, PlaybackService::class.java))).buildAsync()
    }

    override fun onStop() {
        controller?.let(MediaController::releaseFuture)
        controller = null
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Light status-bar icons over the header colour; navigation-bar icons follow the theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            var accent by remember { mutableStateOf(Accent.load(this)) }
            NougatTheme(accent) {
                AppShell(accent) { accent = it; Accent.save(this, it) }
            }
        }
    }
}
