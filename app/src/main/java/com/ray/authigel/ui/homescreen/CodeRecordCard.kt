package com.ray.authigel.ui.homescreen

import android.content.ClipData
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ray.authigel.vault.CodeRecord
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableCollectionItemScope
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReorderableCodeRecordItem(
    record: CodeRecord,
    code: String,
    scope: ReorderableCollectionItemScope,
    interactionSource: MutableInteractionSource,
    isDragging: Boolean,
    hapticFeedback: HapticFeedback,
    onDelete: () -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val alpha by animateFloatAsState(
        targetValue = if (isDragging) 0.9f else 1f,
        animationSpec = tween(120)
    )

    Surface(
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        },
        shadowElevation = elevation
    ) {
        CodeRecordCard(
            record = record,
            code = code,
            onDelete = onDelete,
            dragHandle = {
                DragHandle(
                    scope = scope,
                    interactionSource = interactionSource,
                    hapticFeedback = hapticFeedback,
                )
            }
        )
    }
}

@Composable
fun CodeRecordCard(
    record: CodeRecord,
    code: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: @Composable () -> Unit = {}
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 32.dp)
            ) {
                Text(
                    record.issuer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    record.holder,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    formatOtp(code),
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(
                                    ClipData.newPlainText("OTP Code", code)
                                )
                            )
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copy"
                    )
                }

                dragHandle()

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete"
                    )
                }
            }

            Text(
                text = "Added at: " +
                        DateTimeFormatter
                            .ofPattern("yyyy-MM-dd HH:mm")
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(record.addedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun DragHandle(
    scope: ReorderableCollectionItemScope,
    interactionSource: MutableInteractionSource,
    hapticFeedback: HapticFeedback
) {
    IconButton(
        modifier = with(scope) {
            Modifier
                .size(36.dp)
                .draggableHandle(
                interactionSource = interactionSource,
                onDragStarted = {
                    hapticFeedback.performHapticFeedback(
                        HapticFeedbackType.LongPress
                    )
                },
                onDragStopped = {
                    hapticFeedback.performHapticFeedback(
                        HapticFeedbackType.GestureEnd
                    )
                }
            )
        },
        onClick = {}
    ) {
        Icon(
            Icons.Rounded.DragIndicator,
            contentDescription = "Reorder"
        )
    }
}

private fun formatOtp(code: String): String =
    if (code.length == 6) code.chunked(3).joinToString(" ") else code