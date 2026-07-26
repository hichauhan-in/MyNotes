package com.example.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalNeuColors
import com.example.ui.theme.neumorphicRaised
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt

private enum class WbMode { DRAW, MOVE, TEXT }

private data class WbText(val id: String, val x: Float, val y: Float, val text: String)

/** A single freehand stroke. [color] 0 means "follow the theme" (onSurface); else a fixed ARGB. */
private data class WbStroke(val points: List<Offset>, val color: Int, val width: Float)

private data class WbModel(
    val strokes: List<WbStroke>,
    val texts: List<WbText>,
    val panX: Float,
    val panY: Float,
    val scale: Float,
)

private data class PenPreset(val label: String, val width: Float)

private val penPresets = listOf(
    PenPreset("Fine", 2f),
    PenPreset("Medium", 4f),
    PenPreset("Bold", 7f),
    PenPreset("Marker", 12f),
)

// 0 == follow the current theme (onSurface); everything else is a fixed ARGB colour.
private val penColors = listOf(
    0,
    0xFFE53935.toInt(),
    0xFFFB8C00.toInt(),
    0xFFFDD835.toInt(),
    0xFF43A047.toInt(),
    0xFF1E88E5.toInt(),
    0xFF8E24AA.toInt(),
    0xFFEC407A.toInt(),
)

private fun emptyWb() = WbModel(emptyList(), emptyList(), 0f, 0f, 1f)

private fun parseWb(content: String): WbModel = runCatching {
    if (content.isBlank()) return@runCatching emptyWb()
    val o = JSONObject(content)
    val sArr = o.optJSONArray("s") ?: JSONArray()
    val strokes = (0 until sArr.length()).mapNotNull { i ->
        when (val el = sArr.get(i)) {
            is JSONObject -> {
                val ptsArr = el.optJSONArray("p") ?: JSONArray()
                val pts = (0 until ptsArr.length()).map { j ->
                    val p = ptsArr.getJSONArray(j)
                    Offset(p.getDouble(0).toFloat(), p.getDouble(1).toFloat())
                }
                WbStroke(pts, el.optInt("c", 0), el.optDouble("w", 3.0).toFloat())
            }
            is JSONArray -> {
                // Legacy format: a stroke was just an array of [x, y] points.
                val pts = (0 until el.length()).map { j ->
                    val p = el.getJSONArray(j)
                    Offset(p.getDouble(0).toFloat(), p.getDouble(1).toFloat())
                }
                WbStroke(pts, 0, 3f)
            }
            else -> null
        }
    }
    val tArr = o.optJSONArray("t") ?: JSONArray()
    val texts = (0 until tArr.length()).map { i ->
        val t = tArr.getJSONObject(i)
        WbText(
            t.optString("id", UUID.randomUUID().toString()),
            t.optDouble("x", 0.0).toFloat(),
            t.optDouble("y", 0.0).toFloat(),
            t.optString("t"),
        )
    }
    WbModel(
        strokes = strokes,
        texts = texts,
        panX = o.optDouble("px", 0.0).toFloat(),
        panY = o.optDouble("py", 0.0).toFloat(),
        scale = o.optDouble("sc", 1.0).toFloat().coerceIn(0.3f, 4f),
    )
}.getOrDefault(emptyWb())

private fun serializeWb(m: WbModel): String {
    val o = JSONObject()
    o.put("px", m.panX.toDouble())
    o.put("py", m.panY.toDouble())
    o.put("sc", m.scale.toDouble())
    val sArr = JSONArray()
    m.strokes.forEach { stroke ->
        val pts = JSONArray()
        stroke.points.forEach { p -> pts.put(JSONArray().put(p.x.toDouble()).put(p.y.toDouble())) }
        sArr.put(JSONObject().put("c", stroke.color).put("w", stroke.width.toDouble()).put("p", pts))
    }
    o.put("s", sArr)
    val tArr = JSONArray()
    m.texts.forEach { t ->
        tArr.put(JSONObject().put("id", t.id).put("x", t.x.toDouble()).put("y", t.y.toDouble()).put("t", t.text))
    }
    o.put("t", tArr)
    return o.toString()
}

private fun DrawScope.drawWbStroke(points: List<Offset>, color: Color, width: Float) {
    when {
        points.size == 1 -> drawCircle(color, radius = width.dp.toPx() / 2f, center = points[0])
        points.size > 1 -> {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = width.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/**
 * A pan/zoomable "whiteboard" note: freehand scribbling is the default, and the user can drop
 * draggable text notes and move around the infinite canvas. The strokes and the text notes share
 * one graphics-layer transform, so everything pans and zooms together (a text note keeps its place
 * and scales with the drawing).
 */
@Composable
internal fun ScribbleEditor(
    seedKey: String,
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    meta: @Composable () -> Unit = {},
) {
    val neu = LocalNeuColors.current
    var model by remember { mutableStateOf(parseWb(content)) }
    LaunchedEffect(seedKey) { model = parseWb(content) }
    var mode by remember { mutableStateOf(WbMode.DRAW) }
    var penWidth by remember { mutableStateOf(4f) }
    var penColor by remember { mutableStateOf(0) }
    val livePoints = remember { mutableStateListOf<Offset>() }
    val defaultStrokeColor = MaterialTheme.colorScheme.onSurface
    val liveColor = if (penColor == 0) defaultStrokeColor else Color(penColor)
    val readOnly = LocalReadOnly.current

    fun update(m: WbModel) {
        model = m
        onContentChange(serializeWb(m))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            readOnly = readOnly,
            decorationBox = { inner ->
                if (title.isEmpty()) {
                    Text(
                        text = "Whiteboard",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
                inner()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(10.dp))
        Box(Modifier.padding(horizontal = 20.dp)) { meta() }
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .neumorphicRaised(18.dp, neu, elevation = 4.dp)
                .background(MaterialTheme.colorScheme.surface)
                .clipToBounds(),
        ) {
            val gestureModifier = when (if (readOnly) WbMode.MOVE else mode) {
                WbMode.DRAW -> Modifier.pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        fun toCanvas(p: Offset) = Offset((p.x - model.panX) / model.scale, (p.y - model.panY) / model.scale)
                        livePoints.clear()
                        livePoints.add(toCanvas(down.position))
                        var active = true
                        while (active) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                active = false
                            } else {
                                livePoints.add(toCanvas(change.position))
                                change.consume()
                            }
                        }
                        if (livePoints.isNotEmpty()) {
                            update(model.copy(strokes = model.strokes + WbStroke(livePoints.toList(), penColor, penWidth)))
                        }
                        livePoints.clear()
                    }
                }

                WbMode.MOVE -> Modifier.pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (model.scale * zoom).coerceIn(0.3f, 4f)
                        update(model.copy(panX = model.panX + pan.x, panY = model.panY + pan.y, scale = newScale))
                    }
                }

                WbMode.TEXT -> Modifier.pointerInput(Unit) {
                    detectTapGestures { pos ->
                        val cx = (pos.x - model.panX) / model.scale
                        val cy = (pos.y - model.panY) / model.scale
                        update(model.copy(texts = model.texts + WbText(UUID.randomUUID().toString(), cx, cy, "")))
                        mode = WbMode.MOVE
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize().then(gestureModifier)) {
                // Strokes + text notes share this transform, so they pan/zoom together.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = model.panX
                            translationY = model.panY
                            scaleX = model.scale
                            scaleY = model.scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        },
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        model.strokes.forEach { stroke ->
                            val c = if (stroke.color == 0) defaultStrokeColor else Color(stroke.color)
                            drawWbStroke(stroke.points, c, stroke.width)
                        }
                        if (livePoints.isNotEmpty()) drawWbStroke(livePoints.toList(), liveColor, penWidth)
                    }
                    model.texts.forEach { textNote ->
                        key(textNote.id) {
                            WbTextNote(
                                note = textNote,
                                onMove = { dx, dy ->
                                    update(
                                        model.copy(
                                            texts = model.texts.map {
                                                if (it.id == textNote.id) it.copy(x = it.x + dx, y = it.y + dy) else it
                                            },
                                        ),
                                    )
                                },
                                onText = { value ->
                                    update(
                                        model.copy(
                                            texts = model.texts.map { if (it.id == textNote.id) it.copy(text = value) else it },
                                        ),
                                    )
                                },
                                onDelete = { update(model.copy(texts = model.texts.filterNot { it.id == textNote.id })) },
                            )
                        }
                    }
                }
            }

            if (model.strokes.isEmpty() && model.texts.isEmpty() && livePoints.isEmpty()) {
                Text(
                    text = "Scribble anywhere. Switch tools below to pan, zoom, or drop a text note.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
            }

            if (!readOnly) {
                WbToolbar(
                    mode = mode,
                    penColor = penColor,
                    penWidth = penWidth,
                    onMode = { mode = it },
                    onPenWidth = { penWidth = it; mode = WbMode.DRAW },
                    onPenColor = { penColor = it; mode = WbMode.DRAW },
                    onUndo = { if (model.strokes.isNotEmpty()) update(model.copy(strokes = model.strokes.dropLast(1))) },
                    onClear = {
                        if (model.strokes.isNotEmpty() || model.texts.isNotEmpty()) {
                            update(model.copy(strokes = emptyList(), texts = emptyList()))
                        }
                    },
                    onResetView = { update(model.copy(panX = 0f, panY = 0f, scale = 1f)) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun WbTextNote(
    note: WbText,
    onMove: (Float, Float) -> Unit,
    onText: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val neu = LocalNeuColors.current
    val readOnly = LocalReadOnly.current
    Row(
        modifier = Modifier
            .offset { IntOffset(note.x.roundToInt(), note.y.roundToInt()) }
            .widthIn(max = 220.dp)
            .neumorphicRaised(12.dp, neu, elevation = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .then(
                    if (readOnly) Modifier
                    else Modifier.pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            onMove(drag.x, drag.y)
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.DragIndicator,
                contentDescription = "Move note",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        BasicTextField(
            value = note.text,
            onValueChange = onText,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            readOnly = readOnly,
            decorationBox = { inner ->
                if (note.text.isEmpty()) {
                    Text(
                        text = "Note",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
                inner()
            },
            modifier = Modifier
                .widthIn(min = 40.dp, max = 150.dp)
                .padding(horizontal = 6.dp),
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable(enabled = !readOnly, onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Delete note",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun WbToolbar(
    mode: WbMode,
    penColor: Int,
    penWidth: Float,
    onMode: (WbMode) -> Unit,
    onPenWidth: (Float) -> Unit,
    onPenColor: (Int) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onResetView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val neu = LocalNeuColors.current
    Row(
        modifier = modifier
            .neumorphicRaised(24.dp, neu, elevation = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        WbPenButton(
            selected = mode == WbMode.DRAW,
            penWidth = penWidth,
            onSelect = { onMode(WbMode.DRAW) },
            onPickWidth = onPenWidth,
        )
        WbColorButton(penColor = penColor, onPick = onPenColor)
        WbToolButton(Icons.Rounded.PanTool, "Move", selected = mode == WbMode.MOVE) { onMode(WbMode.MOVE) }
        WbToolButton(Icons.Rounded.TextFields, "Add text", selected = mode == WbMode.TEXT) { onMode(WbMode.TEXT) }
        WbDivider()
        WbToolButton(Icons.Rounded.Undo, "Undo", selected = false, onClick = onUndo)
        WbToolButton(Icons.Rounded.CenterFocusStrong, "Reset view", selected = false, onClick = onResetView)
        WbToolButton(Icons.Rounded.DeleteSweep, "Clear", selected = false, onClick = onClear)
    }
}

/** Draw tool: tap to draw, long-press to choose the pen thickness. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WbPenButton(
    selected: Boolean,
    penWidth: Float,
    onSelect: () -> Unit,
    onPickWidth: (Float) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSelect,
                    onLongClick = { menu = true },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Draw,
                contentDescription = "Draw",
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            penPresets.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.label) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(preset.width.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.onSurface),
                        )
                    },
                    trailingIcon = {
                        if (preset.width == penWidth) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    onClick = {
                        menu = false
                        onPickWidth(preset.width)
                    },
                )
            }
        }
    }
}

/** Pen colour: shows the current colour and opens a swatch row. */
@Composable
private fun WbColorButton(penColor: Int, onPick: (Int) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val current = if (penColor == 0) MaterialTheme.colorScheme.onSurface else Color(penColor)
    Box {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { menu = true },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(current)
                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                penColors.forEach { c ->
                    val swatch = if (c == 0) MaterialTheme.colorScheme.onSurface else Color(c)
                    val isSelected = c == penColor
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape,
                            )
                            .clickable {
                                menu = false
                                onPick(c)
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun WbDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .height(24.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)),
    )
}

@Composable
private fun WbToolButton(icon: ImageVector, description: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}
