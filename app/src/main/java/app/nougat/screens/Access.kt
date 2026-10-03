package app.nougat.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.nougat.design.EmptyState
import app.nougat.library.MediaLibrary
import app.nougat.library.audioPermission
import kotlinx.coroutines.launch

val LocalLibrary = staticCompositionLocalOf<MediaLibrary> { error("No library") }

/**
 * Shown where the music would be while Nougat may not see it. It explains first and asks only when
 * tapped; after a refusal the app keeps working, and once Android refuses without asking, the button
 * opens Nougat's page in Settings instead.
 */
@Composable
fun AccessRequest(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val library = LocalLibrary.current
    val scope = rememberCoroutineScope()
    val activity = context as android.app.Activity
    // Set when Android refuses without asking (after two refusals, or "Don't ask again"); then only
    // Settings can grant it. Worked out from the answer, so a permission Android reset itself is asked again.
    var blocked by rememberSaveable { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        blocked = !granted && !activity.shouldShowRequestPermissionRationale(audioPermission)
        scope.launch { library.refresh() }
    }
    EmptyState(
        title = "Allow access to your music",
        message = if (blocked) "Nougat plays the songs already on this phone. Turn on Music and audio for Nougat in Settings, under Permissions."
            else "Nougat plays the songs already on this phone. Android will ask whether it may see your audio files; nothing is copied or changed.",
        actionTitle = if (blocked) "Open settings" else "Allow",
        onAction = {
            if (blocked) context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
            else ask.launch(audioPermission)
        },
        modifier = modifier.fillMaxWidth(),
    )
}
