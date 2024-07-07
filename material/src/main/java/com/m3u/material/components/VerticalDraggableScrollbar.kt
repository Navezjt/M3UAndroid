package com.m3u.material.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.m3u.material.model.LocalSpacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun VerticalDraggableScrollbar(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    val visibleCountPercent by remember {
        derivedStateOf {
            val visibleItemsCount = lazyListState.layoutInfo.visibleItemsInfo.size
            val totalItemsCount = lazyListState.layoutInfo.totalItemsCount
            if (totalItemsCount == 0) 0f
            else visibleItemsCount.toFloat() / totalItemsCount
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            lazyListState.scrollBy(delta / visibleCountPercent)
        }
    }
    var isDragging: Boolean by remember { mutableStateOf(false) }
    var isScrolling: Boolean by remember { mutableStateOf(false) }
    val fadedIsDragging: Boolean by produceState(isDragging) {
        snapshotFlow { isDragging }
            .onEach {
                if (!it) delay(800.milliseconds)
                value = it
            }
            .launchIn(this)
    }
    val fadedIsScrolling: Boolean by produceState(isScrolling) {
        snapshotFlow { isScrolling }
            .onEach {
                if (!it) delay(400.milliseconds)
                value = it
            }
            .launchIn(this)
    }
    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }.collectLatest {
            isScrolling = true
            delay(200.milliseconds)
            isScrolling = false
        }
    }
    val onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit =
        { isDragging = true }
    val onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit = { isDragging = false }
    val percent by remember(lazyListState) {
        derivedStateOf {
            val totalItemsCount = lazyListState.layoutInfo.totalItemsCount
            val firstVisibleItemIndex = lazyListState.firstVisibleItemIndex
            if (totalItemsCount == 0) 0f
            else {
                val firstVisiblePercent = with(lazyListState) {
                    firstVisibleItemScrollOffset.toFloat() / layoutInfo.visibleItemsInfo[0].size
                }
                ((firstVisibleItemIndex + firstVisiblePercent) / totalItemsCount).coerceIn(0f, 1f)
            }
        }
    }
    val currentAlpha by animateFloatAsState(
        targetValue = if (fadedIsDragging || fadedIsScrolling) 1f else 0.65f,
        label = "current-alpha"
    )
    val currentPosition by animateFloatAsState(
        targetValue = percent,
        label = "current-position"
    )
    val currentVisibleCountPercent by animateFloatAsState(
        targetValue = visibleCountPercent,
        label = "current-visible-count-percent",
        animationSpec = tween(200, delayMillis = 200)
    )
    val currentWidth by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 12.dp,
        label = "current-width"
    )
    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .requiredWidth(currentWidth)
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStarted = onDragStarted,
                onDragStopped = onDragStopped
            )
    ) {
        drawVerticalDraggableScrollbar(
            color = color,
            currentAlpha = currentAlpha,
            currentPosition = currentPosition,
            currentVisibleCountPercent = currentVisibleCountPercent
        )
    }
}

@Composable
fun VerticalDraggableScrollbar(
    lazyGridState: LazyGridState,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    val visibleCountPercent by remember {
        derivedStateOf {
            val visibleItemsCount = lazyGridState.layoutInfo.visibleItemsInfo.size
            val totalItemsCount = lazyGridState.layoutInfo.totalItemsCount
            if (totalItemsCount == 0) 0f
            else visibleItemsCount.toFloat() / totalItemsCount
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            lazyGridState.scrollBy(delta / visibleCountPercent)
        }
    }
    var isDragging: Boolean by remember { mutableStateOf(false) }
    var isScrolling: Boolean by remember { mutableStateOf(false) }
    val fadedIsDragging: Boolean by produceState(isDragging) {
        snapshotFlow { isDragging }
            .onEach {
                if (!it) delay(800.milliseconds)
                value = it
            }
            .launchIn(this)
    }
    val fadedIsScrolling: Boolean by produceState(isScrolling) {
        snapshotFlow { isScrolling }
            .onEach {
                if (!it) delay(400.milliseconds)
                value = it
            }
            .launchIn(this)
    }
    LaunchedEffect(Unit) {
        snapshotFlow { lazyGridState.firstVisibleItemScrollOffset }.collectLatest {
            isScrolling = true
            delay(200.milliseconds)
            isScrolling = false
        }
    }
    val onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit =
        { isDragging = true }
    val onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit = { isDragging = false }
    val percent by remember(lazyGridState) {
        derivedStateOf {
            val totalItemsCount = lazyGridState.layoutInfo.totalItemsCount
            val firstVisibleItemIndex = lazyGridState.firstVisibleItemIndex
            if (totalItemsCount == 0) 0f
            else {
                val firstVisiblePercent = with(lazyGridState) {
                    firstVisibleItemScrollOffset.toFloat() / layoutInfo.visibleItemsInfo[0].size.height
                }
                ((firstVisibleItemIndex + firstVisiblePercent) / totalItemsCount).coerceIn(0f, 1f)
            }
        }
    }
    val currentAlpha by animateFloatAsState(
        targetValue = if (fadedIsDragging || fadedIsScrolling) 1f else 0.65f,
        label = "current-alpha"
    )
    val currentPosition by animateFloatAsState(
        targetValue = percent,
        label = "current-position"
    )
    val currentVisibleCountPercent by animateFloatAsState(
        targetValue = visibleCountPercent,
        label = "current-visible-count-percent",
        animationSpec = tween(200, delayMillis = 200)
    )
    val currentWidth by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 12.dp,
        label = "current-width"
    )
    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .requiredWidth(currentWidth)
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStarted = onDragStarted,
                onDragStopped = onDragStopped
            )
    ) {
        drawVerticalDraggableScrollbar(
            color = color,
            currentAlpha = currentAlpha,
            currentPosition = currentPosition,
            currentVisibleCountPercent = currentVisibleCountPercent
        )
    }
}

@Composable
fun VerticalDraggableScrollbar(
    lazyStaggeredGridState: LazyStaggeredGridState,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    alwaysShow: Boolean = false
) {
    val visibleCountPercent by remember {
        derivedStateOf {
            val visibleItemsCount = lazyStaggeredGridState.layoutInfo.visibleItemsInfo.size
            val totalItemsCount = lazyStaggeredGridState.layoutInfo.totalItemsCount
            if (totalItemsCount == 0) 0f
            else visibleItemsCount.toFloat() / totalItemsCount
        }
    }
    val shouldShow by remember(alwaysShow) {
        derivedStateOf {
            alwaysShow || (visibleCountPercent > 0f && visibleCountPercent <= 0.85f)
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            if (!shouldShow) return@launch
            lazyStaggeredGridState.scrollBy(delta / visibleCountPercent)
        }
    }
    var isDragging: Boolean by remember { mutableStateOf(false) }
    var isScrolling: Boolean by remember { mutableStateOf(false) }
    val fadedIsDragging: Boolean by produceState(isDragging) {
        snapshotFlow { isDragging }
            .onEach {
                if (!it) delay(800.milliseconds)
                value = it
            }
            .launchIn(this)
    }
    val fadedIsScrolling: Boolean by produceState(isScrolling) {
        snapshotFlow { isScrolling }
            .onEach {
                if (!it) delay(400.milliseconds)
                value = it
            }
            .launchIn(this)
    }
    LaunchedEffect(Unit) {
        snapshotFlow { lazyStaggeredGridState.firstVisibleItemScrollOffset }.collectLatest {
            isScrolling = true
            delay(200.milliseconds)
            isScrolling = false
        }
    }
    val onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit =
        { isDragging = true }
    val onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit = { isDragging = false }
    val percent by remember(lazyStaggeredGridState) {
        derivedStateOf {
            val totalItemsCount = lazyStaggeredGridState.layoutInfo.totalItemsCount
            val firstVisibleItemIndex = lazyStaggeredGridState.firstVisibleItemIndex
            if (totalItemsCount == 0) 0f
            else {
                val firstVisiblePercent = with(lazyStaggeredGridState) {
                    firstVisibleItemScrollOffset.toFloat() / layoutInfo.visibleItemsInfo[0].size.height
                }
                ((firstVisibleItemIndex + firstVisiblePercent) / totalItemsCount).coerceIn(0f, 1f)
            }
        }
    }
    val currentAlpha by animateFloatAsState(
        targetValue = if (fadedIsDragging || fadedIsScrolling) 1f else 0.65f,
        label = "current-alpha"
    )
    val currentPosition by animateFloatAsState(
        targetValue = percent,
        label = "current-position"
    )
    val currentVisibleCountPercent by animateFloatAsState(
        targetValue = visibleCountPercent,
        label = "current-visible-count-percent",
        animationSpec = tween(200, delayMillis = 200)
    )
    val currentWidth by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 12.dp,
        label = "current-width"
    )
    if (shouldShow) {
        Canvas(
            modifier = modifier
                .fillMaxHeight()
                .requiredWidth(currentWidth)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStarted = onDragStarted,
                    onDragStopped = onDragStopped
                )
        ) {
            drawVerticalDraggableScrollbar(
                color = color,
                currentAlpha = currentAlpha,
                currentPosition = currentPosition,
                currentVisibleCountPercent = currentVisibleCountPercent
            )
        }
    } else {
        Spacer(modifier = Modifier.width(LocalSpacing.current.medium))
    }

}

private fun DrawScope.drawVerticalDraggableScrollbar(
    color: Color,
    currentAlpha: Float,
    currentPosition: Float,
    currentVisibleCountPercent: Float
) {
    val maxHeight = size.height
    val minDimension = size.minDimension
    drawRoundRect(
        color = color,
        alpha = currentAlpha,
        topLeft = Offset(
            x = 0f,
            y = currentPosition * maxHeight
        ),
        size = size.copy(
            height = (maxHeight * currentVisibleCountPercent).coerceAtLeast(minDimension)
        ),
        cornerRadius = CornerRadius(minDimension)
    )
}