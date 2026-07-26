package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalMall
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalNeuColors
import com.example.ui.theme.neumorphicRaised
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

/** ALLOCATION sections spend from income (expenses / savings / …); ACCOUNT sections just track balances. */
private enum class SectionKind { ALLOCATION, ACCOUNT }

private data class ExpenseItem(val id: String, val name: String, val amount: Double)
private data class ExpenseSection(
    val id: String,
    val name: String,
    val iconKey: String,
    val kind: SectionKind,
    val items: List<ExpenseItem>,
)

private data class ExpenseModel(
    val income: Double,
    val sections: List<ExpenseSection>,
)

private fun newItem() = ExpenseItem(UUID.randomUUID().toString(), "", 0.0)

private fun defaultExpense(): ExpenseModel = ExpenseModel(
    income = 0.0,
    sections = listOf(
        ExpenseSection(UUID.randomUUID().toString(), "Expenses", "expense", SectionKind.ALLOCATION, emptyList()),
        ExpenseSection(UUID.randomUUID().toString(), "Savings", "savings", SectionKind.ALLOCATION, emptyList()),
        ExpenseSection(UUID.randomUUID().toString(), "Investments", "invest", SectionKind.ALLOCATION, emptyList()),
        ExpenseSection(UUID.randomUUID().toString(), "Accounts", "account", SectionKind.ACCOUNT, emptyList()),
    ),
)

private fun parseExpense(content: String): ExpenseModel = runCatching {
    if (content.isBlank()) return@runCatching defaultExpense()
    val obj = JSONObject(content)
    val income = obj.optDouble("income", 0.0)
    if (obj.has("sections")) {
        val secArr = obj.getJSONArray("sections")
        val sections = (0 until secArr.length()).map { i ->
            val o = secArr.getJSONObject(i)
            val kind = runCatching { SectionKind.valueOf(o.optString("kind", "ALLOCATION")) }
                .getOrDefault(SectionKind.ALLOCATION)
            val itemsArr = o.optJSONArray("items") ?: JSONArray()
            val items = (0 until itemsArr.length()).map { j ->
                val it = itemsArr.getJSONObject(j)
                ExpenseItem(
                    it.optString("id", UUID.randomUUID().toString()),
                    it.optString("name"),
                    it.optDouble("amount", 0.0),
                )
            }
            ExpenseSection(
                o.optString("id", UUID.randomUUID().toString()),
                o.optString("name"),
                o.optString("icon", "other"),
                kind,
                items,
            )
        }
        ExpenseModel(income, sections)
    } else {
        // Migrate the legacy { accounts, entries } layout into sections.
        val entArr = obj.optJSONArray("entries") ?: JSONArray()
        val entryObjects = (0 until entArr.length()).map { entArr.getJSONObject(it) }
        fun itemsFor(cat: String): List<ExpenseItem> =
            entryObjects
                .filter { it.optString("cat", "EXPENSE") == cat }
                .map {
                    ExpenseItem(
                        it.optString("id", UUID.randomUUID().toString()),
                        it.optString("name"),
                        it.optDouble("amount", 0.0),
                    )
                }
        val accArr = obj.optJSONArray("accounts") ?: JSONArray()
        val accItems = (0 until accArr.length()).map { i ->
            val o = accArr.getJSONObject(i)
            ExpenseItem(o.optString("id", UUID.randomUUID().toString()), o.optString("name"), o.optDouble("balance", 0.0))
        }
        val sections = listOf(
            ExpenseSection(UUID.randomUUID().toString(), "Expenses", "expense", SectionKind.ALLOCATION, itemsFor("EXPENSE")),
            ExpenseSection(UUID.randomUUID().toString(), "Savings", "savings", SectionKind.ALLOCATION, itemsFor("SAVINGS")),
            ExpenseSection(UUID.randomUUID().toString(), "Investments", "invest", SectionKind.ALLOCATION, itemsFor("INVESTMENT")),
            ExpenseSection(UUID.randomUUID().toString(), "Accounts", "account", SectionKind.ACCOUNT, accItems),
        )
        ExpenseModel(income, sections)
    }
}.getOrDefault(defaultExpense())

private fun serializeExpense(model: ExpenseModel): String {
    val obj = JSONObject()
    obj.put("income", model.income)
    val secArr = JSONArray()
    model.sections.forEach { s ->
        val itemsArr = JSONArray()
        s.items.forEach { item ->
            itemsArr.put(JSONObject().put("id", item.id).put("name", item.name).put("amount", item.amount))
        }
        secArr.put(
            JSONObject()
                .put("id", s.id)
                .put("name", s.name)
                .put("icon", s.iconKey)
                .put("kind", s.kind.name)
                .put("items", itemsArr),
        )
    }
    obj.put("sections", secArr)
    return obj.toString()
}

private val sectionIconOptions: List<Pair<String, ImageVector>> = listOf(
    "expense" to Icons.Rounded.ShoppingCart,
    "savings" to Icons.Rounded.Savings,
    "invest" to Icons.Rounded.TrendingUp,
    "account" to Icons.Rounded.AccountBalance,
    "salary" to Icons.Rounded.Payments,
    "home" to Icons.Rounded.Home,
    "bills" to Icons.Rounded.ReceiptLong,
    "food" to Icons.Rounded.Restaurant,
    "car" to Icons.Rounded.DirectionsCar,
    "health" to Icons.Rounded.FavoriteBorder,
    "education" to Icons.Rounded.School,
    "travel" to Icons.Rounded.Flight,
    "shopping" to Icons.Rounded.LocalMall,
    "fun" to Icons.Rounded.Movie,
    "gift" to Icons.Rounded.CardGiftcard,
    "phone" to Icons.Rounded.Smartphone,
    "pet" to Icons.Rounded.Pets,
    "other" to Icons.Rounded.Category,
)

private fun sectionIcon(key: String): ImageVector =
    sectionIconOptions.firstOrNull { it.first == key }?.second ?: Icons.Rounded.Category

private fun formatMoney(v: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.US)
    nf.maximumFractionDigits = 2
    return "₹" + nf.format(v)
}

private fun plainAmount(v: Double): String =
    if (v == 0.0) "" else if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

/**
 * A fully customisable budgeting dashboard: income, then any number of sections the user can
 * add, rename and remove. "Allocation" sections spend from income and feed the live unallocated
 * total and bar; "balances" sections (like accounts) simply track amounts.
 */
@Composable
internal fun ExpenseEditor(
    seedKey: String,
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var model by remember { mutableStateOf(parseExpense(content)) }
    LaunchedEffect(seedKey) { model = parseExpense(content) }
    var showAddSection by remember { mutableStateOf(false) }

    fun update(newModel: ExpenseModel) {
        model = newModel
        onContentChange(serializeExpense(newModel))
    }

    fun updateSection(id: String, transform: (ExpenseSection) -> ExpenseSection) =
        update(model.copy(sections = model.sections.map { if (it.id == id) transform(it) else it }))

    val allocationSections = model.sections.filter { it.kind == SectionKind.ALLOCATION }
    val allocationTotal = allocationSections.sumOf { s -> s.items.sumOf { it.amount } }
    val remaining = model.income - allocationTotal

    val accountColor = MaterialTheme.colorScheme.primary
    val palette = listOf(
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primary,
        Color(0xFF7E57C2),
        Color(0xFF26A69A),
        Color(0xFFEF6C00),
        Color(0xFFEC407A),
    )
    val allocColorById = allocationSections
        .mapIndexed { i, s -> s.id to palette[i % palette.size] }
        .toMap()
    fun colorFor(section: ExpenseSection): Color = allocColorById[section.id] ?: accountColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
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
                        text = "e.g. July salary",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
                inner()
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        // Income
        ExpenseCard {
            Text(
                text = "Income this month",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            MoneyField(
                value = model.income,
                onChange = { update(model.copy(income = it)) },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )
        }

        Spacer(Modifier.height(14.dp))

        // Summary
        ExpenseCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (remaining >= 0) "Unallocated" else "Over-allocated",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatMoney(kotlin.math.abs(remaining)),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = "of ${formatMoney(model.income)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            AllocationBar(
                income = model.income,
                allocated = allocationTotal,
                segments = allocationSections.map { s -> s.items.sumOf { it.amount } to colorFor(s) },
            )
            if (allocationSections.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                allocationSections.forEach { s ->
                    LegendRow(
                        label = s.name.ifBlank { "Untitled" },
                        amount = s.items.sumOf { it.amount },
                        color = colorFor(s),
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        model.sections.forEach { section ->
            key(section.id) {
                SectionCard(
                    section = section,
                    accent = colorFor(section),
                    onRename = { newName -> updateSection(section.id) { it.copy(name = newName) } },
                    onAddItem = { updateSection(section.id) { it.copy(items = it.items + newItem()) } },
                    onItemName = { itemId, v ->
                        updateSection(section.id) { s ->
                            s.copy(items = s.items.map { if (it.id == itemId) it.copy(name = v) else it })
                        }
                    },
                    onItemAmount = { itemId, v ->
                        updateSection(section.id) { s ->
                            s.copy(items = s.items.map { if (it.id == itemId) it.copy(amount = v) else it })
                        }
                    },
                    onDeleteItem = { itemId ->
                        updateSection(section.id) { s -> s.copy(items = s.items.filterNot { it.id == itemId }) }
                    },
                    onRemoveSection = {
                        update(model.copy(sections = model.sections.filterNot { it.id == section.id }))
                    },
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        AddSectionButton(onClick = { showAddSection = true })
        Spacer(Modifier.height(28.dp))
    }

    if (showAddSection) {
        AddSectionDialog(
            onAdd = { name, iconKey, kind ->
                update(
                    model.copy(
                        sections = model.sections + ExpenseSection(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            iconKey = iconKey,
                            kind = kind,
                            items = emptyList(),
                        ),
                    ),
                )
                showAddSection = false
            },
            onDismiss = { showAddSection = false },
        )
    }
}

@Composable
private fun SectionCard(
    section: ExpenseSection,
    accent: Color,
    onRename: (String) -> Unit,
    onAddItem: () -> Unit,
    onItemName: (String, String) -> Unit,
    onItemAmount: (String, Double) -> Unit,
    onDeleteItem: (String) -> Unit,
    onRemoveSection: () -> Unit,
) {
    val total = section.items.sumOf { it.amount }
    ExpenseCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(sectionIcon(section.iconKey), contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            SectionNameField(
                value = section.name,
                onChange = onRename,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = formatMoney(total),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            SectionMenu(onRemove = onRemoveSection)
        }
        if (section.items.isNotEmpty()) Spacer(Modifier.height(8.dp))
        section.items.forEach { item ->
            key(item.id) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlainField(
                        value = item.name,
                        placeholder = "Name",
                        onChange = { onItemName(item.id, it) },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    MoneyField(
                        value = item.amount,
                        onChange = { onItemAmount(item.id, it) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.width(110.dp),
                    )
                    DeleteDot { onDeleteItem(item.id) }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        AddRow(onAddItem)
    }
}

@Composable
private fun SectionNameField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        if (value.isEmpty()) {
            Text(
                text = "Section name",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SectionMenu(onRemove: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable { open = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = "Section options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Remove section") },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    open = false
                    onRemove()
                },
            )
        }
    }
}

@Composable
private fun AddSectionButton(onClick: () -> Unit) {
    val neu = LocalNeuColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .neumorphicRaised(26.dp, neu, elevation = 6.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .padding(start = 8.dp, end = 22.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Add section",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AddSectionDialog(
    onAdd: (name: String, iconKey: String, kind: SectionKind) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf("other") }
    var kind by remember { mutableStateOf(SectionKind.ALLOCATION) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New section") },
        text = {
            Column {
                DialogField(value = name, onChange = { name = it }, placeholder = "Section name")
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    sectionIconOptions.forEach { (key, icon) ->
                        IconChoice(icon = icon, selected = key == iconKey, onClick = { iconKey = key })
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KindChip("Spending", kind == SectionKind.ALLOCATION) { kind = SectionKind.ALLOCATION }
                    KindChip("Balances", kind == SectionKind.ACCOUNT) { kind = SectionKind.ACCOUNT }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (kind == SectionKind.ALLOCATION) {
                        "Counts against your income and shows in the allocation bar (like expenses or savings)."
                    } else {
                        "Just tracks balances (like accounts) — it doesn't change what's unallocated."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onAdd(name.trim(), iconKey, kind) }) {
                Text("Add", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DialogField(value: String, onChange: (String) -> Unit, placeholder: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun IconChoice(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun KindChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ExpenseCard(content: @Composable () -> Unit) {
    val neu = LocalNeuColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neumorphicRaised(18.dp, neu, elevation = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun AllocationBar(income: Double, allocated: Double, segments: List<Pair<Double, Color>>) {
    val denom = maxOf(income, allocated)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (denom > 0.0) {
            segments.forEach { (amount, color) ->
                val fraction = (amount / denom).toFloat()
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(fraction)
                            .fillMaxHeight()
                            .background(color),
                    )
                }
            }
            val remainingFraction = ((income - allocated) / denom).toFloat()
            if (remainingFraction > 0f) {
                Box(modifier = Modifier.weight(remainingFraction).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun LegendRow(label: String, amount: Double, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatMoney(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AddRow(onAdd: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onAdd)
            .padding(vertical = 6.dp, horizontal = 2.dp),
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = "Add",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Add",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun DeleteDot(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Close,
            contentDescription = "Remove",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun PlainField(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MoneyField(
    value: Double,
    onChange: (Double) -> Unit,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(plainAmount(value)) }
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = "₹",
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(2.dp))
        Box(modifier = Modifier.weight(1f, fill = false)) {
            if (text.isEmpty()) {
                Text(text = "0", style = textStyle, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            }
            BasicTextField(
                value = text,
                onValueChange = { raw ->
                    val filtered = raw.filter { it.isDigit() || it == '.' }
                    text = filtered
                    onChange(filtered.toDoubleOrNull() ?: 0.0)
                },
                singleLine = true,
                textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
    }
}
