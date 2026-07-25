package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalNeuColors
import com.example.ui.theme.neumorphicRaised
import org.json.JSONArray
import org.json.JSONObject

private data class SheetModel(
    val columnWidths: List<Int>,
    val rowHeights: List<Int>,
    val cells: List<List<String>>,
)

private fun defaultSheet(): SheetModel = SheetModel(
    columnWidths = List(4) { 120 },
    rowHeights = List(6) { 46 },
    cells = List(6) { List(4) { "" } },
)

private fun parseSheet(content: String): SheetModel = runCatching {
    if (content.isBlank()) return defaultSheet()
    val obj = JSONObject(content)
    val cwArr = obj.getJSONArray("cw")
    val cw = (0 until cwArr.length()).map { cwArr.getInt(it) }
    val rhArr = obj.getJSONArray("rh")
    val rh = (0 until rhArr.length()).map { rhArr.getInt(it) }
    val cArr = obj.getJSONArray("c")
    val cells = (0 until cArr.length()).map { r ->
        val row = cArr.getJSONArray(r)
        (0 until row.length()).map { row.getString(it) }
    }
    if (cw.isEmpty() || rh.isEmpty() || cells.isEmpty()) defaultSheet() else SheetModel(cw, rh, cells)
}.getOrDefault(defaultSheet())

private fun serializeSheet(model: SheetModel): String {
    val obj = JSONObject()
    obj.put("cw", JSONArray(model.columnWidths))
    obj.put("rh", JSONArray(model.rowHeights))
    val rows = JSONArray()
    model.cells.forEach { rows.put(JSONArray(it)) }
    obj.put("c", rows)
    return obj.toString()
}

private fun SheetModel.setCell(r: Int, c: Int, value: String): SheetModel =
    copy(cells = cells.mapIndexed { ri, row ->
        if (ri == r) row.mapIndexed { ci, cell -> if (ci == c) value else cell } else row
    })

private fun SheetModel.setColWidth(c: Int, w: Int): SheetModel =
    copy(columnWidths = columnWidths.mapIndexed { i, x -> if (i == c) w else x })

private fun SheetModel.setRowHeight(r: Int, h: Int): SheetModel =
    copy(rowHeights = rowHeights.mapIndexed { i, x -> if (i == r) h else x })

private fun SheetModel.addRow(): SheetModel =
    copy(rowHeights = rowHeights + 46, cells = cells + listOf(List(columnWidths.size) { "" }))

private fun SheetModel.removeRow(): SheetModel =
    if (cells.size <= 1) this else copy(rowHeights = rowHeights.dropLast(1), cells = cells.dropLast(1))

private fun SheetModel.addColumn(): SheetModel =
    copy(columnWidths = columnWidths + 120, cells = cells.map { it + "" })

private fun SheetModel.removeColumn(): SheetModel =
    if (columnWidths.size <= 1) this
    else copy(columnWidths = columnWidths.dropLast(1), cells = cells.map { it.dropLast(1) })

private fun columnLabel(c: Int): String {
    var n = c
    val sb = StringBuilder()
    do {
        sb.insert(0, ('A' + (n % 26)))
        n = n / 26 - 1
    } while (n >= 0)
    return sb.toString()
}

/** A full-note spreadsheet: editable cells with drag-resizable columns and rows. */
@Composable
internal fun SheetEditor(
    seedKey: String,
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val neu = LocalNeuColors.current
    var model by remember { mutableStateOf(parseSheet(content)) }
    val current by rememberUpdatedState(model)
    LaunchedEffect(seedKey) { model = parseSheet(content) }

    fun update(newModel: SheetModel) {
        model = newModel
        onContentChange(serializeSheet(newModel))
    }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
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
            decorationBox = { inner ->
                if (title.isEmpty()) {
                    Text(
                        text = "Sheet name",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
                inner()
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SheetCtrl(Icons.Rounded.Add, "Add column") { update(model.addColumn()) }
            SheetCtrl(Icons.Rounded.Remove, "Remove column") { update(model.removeColumn()) }
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .height(20.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)),
            )
            SheetCtrl(Icons.Rounded.Add, "Add row") { update(model.addRow()) }
            SheetCtrl(Icons.Rounded.Remove, "Remove row") { update(model.removeRow()) }
            Spacer(Modifier.weight(1f))
            Text(
                text = "${model.cells.size} × ${model.columnWidths.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        val hScroll = rememberScrollState()
        val vScroll = rememberScrollState()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .neumorphicRaised(14.dp, neu, elevation = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .horizontalScroll(hScroll)
                .verticalScroll(vScroll),
        ) {
            Column {
                Row {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(30.dp)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    model.columnWidths.forEachIndexed { c, w ->
                        SheetColumnHeader(
                            label = columnLabel(c),
                            width = w,
                            onResize = { nw -> update(current.setColWidth(c, nw)) },
                        )
                    }
                }
                model.cells.forEachIndexed { r, row ->
                    Row {
                        SheetRowHeader(
                            number = r + 1,
                            height = model.rowHeights.getOrElse(r) { 46 },
                            onResize = { nh -> update(current.setRowHeight(r, nh)) },
                        )
                        row.forEachIndexed { c, cell ->
                            SheetCell(
                                value = cell,
                                width = model.columnWidths.getOrElse(c) { 120 },
                                height = model.rowHeights.getOrElse(r) { 46 },
                                onChange = { v -> update(current.setCell(r, c, v)) },
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SheetColumnHeader(label: String, width: Int, onResize: (Int) -> Unit) {
    val density = LocalDensity.current
    val currentWidth by rememberUpdatedState(width)
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(30.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(14.dp)
                .pointerInput(Unit) {
                    var start = 0
                    var acc = 0f
                    detectDragGestures(
                        onDragStart = { start = currentWidth; acc = 0f },
                        onDrag = { change, drag ->
                            change.consume()
                            acc += drag.x
                            val delta = with(density) { acc.toDp().value }.toInt()
                            onResize((start + delta).coerceIn(50, 420))
                        },
                    )
                },
        )
    }
}

@Composable
private fun SheetRowHeader(number: Int, height: Int, onResize: (Int) -> Unit) {
    val density = LocalDensity.current
    val currentHeight by rememberUpdatedState(height)
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(height.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$number",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(14.dp)
                .pointerInput(Unit) {
                    var start = 0
                    var acc = 0f
                    detectDragGestures(
                        onDragStart = { start = currentHeight; acc = 0f },
                        onDrag = { change, drag ->
                            change.consume()
                            acc += drag.y
                            val delta = with(density) { acc.toDp().value }.toInt()
                            onResize((start + delta).coerceIn(32, 260))
                        },
                    )
                },
        )
    }
}

@Composable
private fun SheetCell(value: String, width: Int, height: Int, onChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(height.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun SheetCtrl(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
