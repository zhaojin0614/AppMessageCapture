@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.aifactory.appmessagecapture.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Global coordinator to ensure only one SwipeableItem is open at a time. */
object SwipeableItemCoordinator {
    private val flow = MutableSharedFlow<String?>(replay = 1, extraBufferCapacity = 1)

    /** Exposed for internal collect only. */
    internal val openedItem = flow

    fun open(key: String) {
        flow.tryEmit(key)
    }

    /** 关闭所有已滑开的 SwipeableItem（删除时调用）。 */
    fun reset() {
        flow.tryEmit(null)
    }
}

/**
 * A reusable swipeable list item container.
 *
 * Features:
 * - Left-swipe reveals a delete button.
 * - Delete background is hidden while in selection mode.
 * - Automatically snaps back to origin when selection mode is entered.
 * - When [itemKey] changes (e.g. after deletion), swipe offset resets automatically.
 *
 * @param isSelectionMode Whether the parent list is in multi-select mode.
 * @param onDelete Called when the user taps the revealed delete button.
 *                 The caller is responsible for showing a confirmation dialog.
 * @param modifier Modifier to be applied to the outer container.
 * @param itemKey 唯一标识当前数据项的 key。当数据项因列表变化而改变时，
 *                 滑动偏移会自动重置，防止删除后下一条记录继承旧状态。
 * @param content The actual card/content composable.
 */
@Composable
fun SwipeableItem(
    isSelectionMode: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    itemKey: Any? = null,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val maxSwipe = with(LocalDensity.current) { 60.dp.toPx() }
    val swipeKey = remember { java.util.UUID.randomUUID().toString() }

    // Swipe reveal progress: 0 = hidden, 1 = fully swiped open.
    // Drives the delete button's fade/scale. The button sits BEHIND the
    // frosted card, so without this it would show through the translucent
    // glass surface even when the item is at rest.
    val revealProgress = (offsetX.value / -maxSwipe).coerceIn(0f, 1f)

    // 当数据项标识变化时（如删除导致列表缩短、槽位复用），重置滑动偏移
    LaunchedEffect(itemKey) {
        if (offsetX.value != 0f) {
            offsetX.animateTo(0f)
        }
    }

    // Auto-reset swipe offset when entering selection mode
    LaunchedEffect(isSelectionMode) {
        if (isSelectionMode && offsetX.value != 0f) {
            offsetX.animateTo(0f)
        }
    }

    // Auto-reset when another SwipeableItem opens or coordinator resets
    LaunchedEffect(swipeKey) {
        SwipeableItemCoordinator.openedItem.collect { openedKey ->
            if (openedKey != swipeKey && offsetX.value != 0f) {
                offsetX.animateTo(0f)
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Delete button — glass styled, fades in only as the item is
        // swiped open (invisible at rest so it never shows through the
        // frosted card). Hidden entirely in selection mode.
        if (!isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .size(40.dp)
                    .graphicsLayer {
                        alpha = revealProgress
                        scaleX = 0.85f + 0.15f * revealProgress
                        scaleY = 0.85f + 0.15f * revealProgress
                    }
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.error.copy(
                            alpha = if (isDarkTheme()) 0.32f else 0.45f
                        )
                    )
                    .border(glassBorder(), CircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(isSelectionMode) {
                    if (!isSelectionMode) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    val target = if (offsetX.value < -maxSwipe * 0.5f) -maxSwipe else 0f
                                    if (target == -maxSwipe) {
                                        SwipeableItemCoordinator.open(swipeKey)
                                    }
                                    offsetX.animateTo(target)
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val newValue = (offsetX.value + dragAmount).coerceIn(-maxSwipe, 0f)
                                scope.launch { offsetX.snapTo(newValue) }
                            }
                        )
                    }
                }
        ) {
            content()
        }
    }
}
