package app.nougat.design

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Long-press-and-drag reordering for a lazy list (Compose has none of its own). `first` is the list
 * index of the first movable row, for lists that open with a header. The drag lives on each row, so
 * the row has the finger before the list can scroll.
 */
class ReorderState(val list: LazyListState, val first: Int, private val count: () -> Int) {
    /** List index of the row being dragged. */
    var dragged by mutableStateOf<Int?>(null)
        private set
    var offset by mutableFloatStateOf(0f)
        private set

    internal fun start(index: Int) {
        dragged = index
        offset = 0f
    }

    internal fun drag(by: Float, onMove: (from: Int, to: Int) -> Unit) {
        val from = dragged ?: return
        offset += by
        val visible = list.layoutInfo.visibleItemsInfo
        val info = visible.firstOrNull { it.index == from } ?: return
        val middle = (info.offset + info.size / 2 + offset).toInt()
        val target = visible.firstOrNull {
            it.index != from && it.index in first until first + count() && middle in it.offset until it.offset + it.size
        } ?: return
        onMove(from - first, target.index - first)
        offset += info.offset - target.offset
        dragged = target.index
    }

    internal fun end() {
        dragged = null
        offset = 0f
    }
}

@Composable
fun rememberReorderState(list: LazyListState, first: Int = 0, count: () -> Int) = remember(list, first) { ReorderState(list, first, count) }

/** Makes a row draggable and lifts it while dragged. `index` is the row's position among the movable rows. */
fun Modifier.reorderable(state: ReorderState, key: Any, index: () -> Int, onMove: (from: Int, to: Int) -> Unit): Modifier {
    val lifted = state.dragged == index() + state.first
    return pointerInput(key) {
        detectDragGesturesAfterLongPress(
            onDragStart = { state.start(index() + state.first) },
            onDrag = { change, amount -> change.consume(); state.drag(amount.y, onMove) },
            onDragEnd = { state.end() },
            onDragCancel = { state.end() },
        )
    }
        .zIndex(if (lifted) 1f else 0f)
        .graphicsLayer { if (lifted) translationY = state.offset }
        .then(if (lifted) Modifier.shadow(6.dp) else Modifier)
}
