package app.nougat.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import app.nougat.design.LetterTile
import app.nougat.design.Metrics
import app.nougat.design.Radius
import app.nougat.library.Track

/** A song's thumbnail: its cover art once loaded, a letter tile until then or if it has none. */
@Composable
fun TrackThumbnail(track: Track, size: Dp = Metrics.thumbnail) {
    val store = LocalArtwork.current
    val image by produceState<ImageBitmap?>(store.cached(track)?.asImageBitmap(), track.path, track.modified) {
        value = store.thumbnail(track)?.asImageBitmap()
    }
    val art = image
    if (art == null) LetterTile(track.title)
    else Image(
        art, contentDescription = null, contentScale = ContentScale.Crop,
        modifier = Modifier.size(size).clip(RoundedCornerShape(Radius.thumbnail)).clearAndSetSemantics {},
    )
}
