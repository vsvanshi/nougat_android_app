package app.nougat.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import app.nougat.App
import app.nougat.MainActivity

/**
 * Keeps music going in the background as a foreground media service, and gives the system the
 * media session behind the notification, the lock screen and headset, Bluetooth and car buttons.
 */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        session = MediaSession.Builder(this, (application as App).player.sessionPlayer).setSessionActivity(open).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    /** Swiping the app away stops the service unless music is playing. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        // The player belongs to the app and outlives the service; only the session goes.
        session?.release()
        session = null
        super.onDestroy()
    }
}
