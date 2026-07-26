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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalNeuColors
import com.example.ui.theme.neumorphicRaised
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private data class SheetModel(
    val columnWidths: List<Int>,
    val rowHeights: List<Int>,
    val cells: List<List<String>>,
)

private const val DEFAULT_COL_W = 150
private const val DEFAULT_ROW_H = 34

private fun defaultSheet(): SheetModel = SheetModel(
    columnWidths = List(2) { DEFAULT_COL_W },
    rowHeights = List(10) { DEFAULT_ROW_H },
    cells = List(10) { List(2) { "" } },
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

private fun serializeGrid(grid: SheetGrid): String {
    val obj = JSONObject()
    obj.put("cw", JSONArray(grid.columnWidths.toList()))
    obj.put("rh", JSONArray(grid.rowHeights.toList()))
    val rows = JSONArray()
    grid.cells.forEach { rows.put(JSONArray(it.toList())) }
    obj.put("c", rows)
    return obj.toString()
}

private fun parseSheetGrid(content: String): SheetGrid {
    val m = parseSheet(content)
    return SheetGrid(m.columnWidths, m.rowHeights, m.cells)
}

/**
 * A snapshot-backed spreadsheet grid. Editing one cell, or resizing one column/row, mutates a
 * single state entry (O(1)) instead of copying the whole grid, and only the affected cell/row
 * recomposes - so even a very large sheet edits and scrolls smoothly. JSON serialization is done
 * separately (off the main thread, debounced), never on every keystroke or drag frame.
 */
@Stable
private class SheetGrid(
    cols: List<Int>,
    rows: List<Int>,
    cellRows: List<List<String>>,
) {
    val columnWidths: SnapshotStateList<Int> = cols.toMutableStateList()
    val rowHeights: SnapshotStateList<Int> = rows.toMutableStateList()
    val cells: SnapshotStateList<SnapshotStateList<String>> =
        cellRows.map { it.toMutableStateList() }.toMutableStateList()

    val rowCount: Int get() = cells.size
    val colCount: Int get() = columnWidths.size

    fun setCell(r: Int, c: Int, value: String) {
        cells.getOrNull(r)?.let { if (c in it.indices) it[c] = value }
    }

    fun setColWidth(c: Int, w: Int) { if (c in columnWidths.indices) columnWidths[c] = w }
    fun setRowHeight(r: Int, h: Int) { if (r in rowHeights.indices) rowHeights[r] = h }

    fun addRow() {
        rowHeights.add(DEFAULT_ROW_H)
        cells.add(MutableList(colCount) { "" }.toMutableStateList())
    }

    fun removeRow() {
        if (rowCount > 1) {
            rowHeights.removeAt(rowHeights.lastIndex)
            cells.removeAt(cells.lastIndex)
        }
    }

    fun addColumn() {
        columnWidths.add(DEFAULT_COL_W)
        cells.forEach { it.add("") }
    }

    fun removeColumn() {
        if (colCount > 1) {
            columnWidths.removeAt(columnWidths.lastIndex)
            cells.forEach { if (it.isNotEmpty()) it.removeAt(it.lastIndex) }
        }
    }
}

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
    meta: @Composable () -> Unit = {},
) {
    val neu = LocalNeuColors.current
    val grid = remember(seedKey) { parseSheetGrid(content) }
    // Which cell (row to col) is currently being edited. Only that one cell is a real text field;
    // every other cell is a cheap Text, so even a huge sheet stays light.
    var activeCell by remember(seedKey) { mutableStateOf<Pair<Int, Int>?>(null) }
    val readOnly = LocalReadOnly.current

    // Serialize the grid to JSON OFF the main thread and only after edits settle (debounced), so
    // typing and dragging never block on rebuilding a big JSON string every keystroke/frame.
    var revision by remember(seedKey) { mutableStateOf(0) }
    val flushedRev = remember(seedKey) { mutableStateOf(0) }
    LaunchedEffect(grid) {
        snapshotFlow { revision }
            .drop(1)
            .collectLatest {
                delay(350) // collectLatest cancels this when another edit lands -> debounce
                val rev = revision
                val json = withContext(Dispatchers.Default) { serializeGrid(grid) }
                onContentChange(json)
                flushedRev.value = rev
            }
    }
    // If the user leaves during the debounce window, flush the final state so no edit is lost.
    DisposableEffect(grid) {
        onDispose {
            if (revision != flushedRev.value) onContentChange(serializeGrid(grid))
        }
    }
    fun markDirty() { revision++ }

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
            readOnly = readOnly,
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
        Spacer(Modifier.height(12.dp))
        meta()
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SheetAxisLabel("R")
            SheetCtrl(Icons.Rounded.Add, "Add row") { grid.addRow(); markDirty() }
            SheetCtrl(Icons.Rounded.Remove, "Remove row") { grid.removeRow(); markDirty() }
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .height(20.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)),
            )
            SheetAxisLabel("C")
            SheetCtrl(Icons.Rounded.Add, "Add column") { grid.addColumn(); markDirty() }
            SheetCtrl(Icons.Rounded.Remove, "Remove column") { grid.removeColumn(); markDirty() }
            Spacer(Modifier.weight(1f))
            Text(
                text = "${grid.rowCount} × ${grid.colCount}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        val hScroll = rememberScrollState()
        // LazyColumn virtualises the rows: only the handful actually on screen are composed, so the
        // sheet can be arbitrarily tall without ever composing thousands of cells at once. Every row
        // shares one horizontal scroll state so the columns stay aligned as you scroll sideways.
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .neumorphicRaised(14.dp, neu, elevation = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            item(key = "sheet-header") {
                Row(Modifier.horizontalScroll(hScroll)) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(30.dp)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    for (c in 0 until grid.colCount) {
                        SheetColumnHeader(
                            label = columnLabel(c),
                            width = grid.columnWidths[c],
                            onResize = { nw -> grid.setColWidth(c, nw) },
                            onResizeEnd = { markDirty() },
                        )
                    }
                }
            }
            items(count = grid.rowCount, key = { it }) { r ->
                val rowH = grid.rowHeights.getOrElse(r) { DEFAULT_ROW_H }
                Row(Modifier.horizontalScroll(hScroll)) {
                    SheetRowHeader(
                        number = r + 1,
                        height = rowH,
                        onResize = { nh -> grid.setRowHeight(r, nh) },
                        onResizeEnd = { markDirty() },
                    )
                    val cols = grid.colCount
                    for (c in 0 until cols) {
                        SheetCell(
                            value = grid.cells[r].getOrElse(c) { "" },
                            width = grid.columnWidths.getOrElse(c) { DEFAULT_COL_W },
                            height = rowH,
                            active = activeCell == (r to c),
                            onActivate = { activeCell = r to c },
                            onDeactivate = { if (activeCell == (r to c)) activeCell = null },
                            onChange = { v -> grid.setCell(r, c, v); markDirty() },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SheetColumnHeader(label: String, width: Int, onResize: (Int) -> Unit, onResizeEnd: () -> Unit) {
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
                .width(16.dp)
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
                        onDragEnd = { onResizeEnd() },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            ColumnResizeHandle()
        }
    }
}

/** Four dots at a column's right edge, hinting that the boundary can be dragged to resize. */
@Composable
private fun ColumnResizeHandle() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .size(2.5.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetAxisLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(end = 2.dp),
    )
}

@Composable
private fun SheetRowHeader(number: Int, height: Int, onResize: (Int) -> Unit, onResizeEnd: () -> Unit) {
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
                        onDragEnd = { onResizeEnd() },
                    )
                },
        )
    }
}

@Composable
private fun SheetCell(
    value: String,
    width: Int,
    height: Int,
    active: Boolean,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onChange: (String) -> Unit,
) {
    val readOnly = LocalReadOnly.current
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(height.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .then(if (!readOnly && !active) Modifier.clickable { onActivate() } else Modifier)
            .clipToBounds()
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        if (active && !readOnly) {
            // Real editing cell: a single focused text field. Tapping elsewhere (or scrolling it
            // off-screen) drops focus and turns it back into a plain Text.
            val focusRequester = remember { FocusRequester() }
            var tfv by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
            BasicTextField(
                value = tfv,
                onValueChange = { tfv = it; onChange(it.text) },
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .onFocusChanged { if (!it.isFocused) onDeactivate() },
            )
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun SheetCtrl(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(enabled = !LocalReadOnly.current, onClick = onClick),
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
