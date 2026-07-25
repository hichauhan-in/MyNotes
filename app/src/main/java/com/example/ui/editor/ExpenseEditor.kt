package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

private enum class ExpenseCategory { EXPENSE, SAVINGS, INVESTMENT }

private data class ExpenseAccount(val id: String, val name: String, val balance: Double)
private data class ExpenseEntry(val id: String, val name: String, val amount: Double, val category: ExpenseCategory)
private data class ExpenseModel(
    val income: Double,
    val accounts: List<ExpenseAccount>,
    val entries: List<ExpenseEntry>,
)

private fun defaultExpense(): ExpenseModel = ExpenseModel(0.0, emptyList(), emptyList())

private fun parseExpense(content: String): ExpenseModel = runCatching {
    if (content.isBlank()) return defaultExpense()
    val obj = JSONObject(content)
    val income = obj.optDouble("income", 0.0)
    val accArr = obj.optJSONArray("accounts") ?: JSONArray()
    val accounts = (0 until accArr.length()).map { i ->
        val o = accArr.getJSONObject(i)
        ExpenseAccount(o.optString("id", UUID.randomUUID().toString()), o.optString("name"), o.optDouble("balance", 0.0))
    }
    val entArr = obj.optJSONArray("entries") ?: JSONArray()
    val entries = (0 until entArr.length()).map { i ->
        val o = entArr.getJSONObject(i)
        val cat = runCatching { ExpenseCategory.valueOf(o.optString("cat", "EXPENSE")) }.getOrDefault(ExpenseCategory.EXPENSE)
        ExpenseEntry(o.optString("id", UUID.randomUUID().toString()), o.optString("name"), o.optDouble("amount", 0.0), cat)
    }
    ExpenseModel(income, accounts, entries)
}.getOrDefault(defaultExpense())

private fun serializeExpense(model: ExpenseModel): String {
    val obj = JSONObject()
    obj.put("income", model.income)
    val accArr = JSONArray()
    model.accounts.forEach { a ->
        accArr.put(JSONObject().put("id", a.id).put("name", a.name).put("balance", a.balance))
    }
    obj.put("accounts", accArr)
    val entArr = JSONArray()
    model.entries.forEach { e ->
        entArr.put(JSONObject().put("id", e.id).put("name", e.name).put("amount", e.amount).put("cat", e.category.name))
    }
    obj.put("entries", entArr)
    return obj.toString()
}

private fun formatMoney(v: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.US)
    nf.maximumFractionDigits = 2
    return "₹" + nf.format(v)
}

private fun plainAmount(v: Double): String =
    if (v == 0.0) "" else if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

/** A budgeting dashboard: income, where it goes (expenses / savings / investments) and account balances. */
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

    fun update(newModel: ExpenseModel) {
        model = newModel
        onContentChange(serializeExpense(newModel))
    }

    val expenses = model.entries.filter { it.category == ExpenseCategory.EXPENSE }
    val savings = model.entries.filter { it.category == ExpenseCategory.SAVINGS }
    val investments = model.entries.filter { it.category == ExpenseCategory.INVESTMENT }
    val expenseTotal = expenses.sumOf { it.amount }
    val savingTotal = savings.sumOf { it.amount }
    val investTotal = investments.sumOf { it.amount }
    val allocated = expenseTotal + savingTotal + investTotal
    val remaining = model.income - allocated

    val expenseColor = MaterialTheme.colorScheme.error
    val savingColor = MaterialTheme.colorScheme.tertiary
    val investColor = MaterialTheme.colorScheme.secondary

    fun updateEntry(id: String, transform: (ExpenseEntry) -> ExpenseEntry) =
        update(model.copy(entries = model.entries.map { if (it.id == id) transform(it) else it }))

    fun addEntry(category: ExpenseCategory) =
        update(model.copy(entries = model.entries + ExpenseEntry(UUID.randomUUID().toString(), "", 0.0, category)))

    fun deleteEntry(id: String) = update(model.copy(entries = model.entries.filterNot { it.id == id }))

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
                allocated = allocated,
                segments = listOf(
                    expenseTotal to expenseColor,
                    savingTotal to savingColor,
                    investTotal to investColor,
                ),
            )
            Spacer(Modifier.height(12.dp))
            LegendRow("Expenses", expenseTotal, expenseColor)
            LegendRow("Savings", savingTotal, savingColor)
            LegendRow("Investments", investTotal, investColor)
        }

        Spacer(Modifier.height(14.dp))
        EntrySection(
            title = "Expenses",
            icon = Icons.Rounded.ShoppingCart,
            accent = expenseColor,
            entries = expenses,
            total = expenseTotal,
            onAdd = { addEntry(ExpenseCategory.EXPENSE) },
            onName = { id, v -> updateEntry(id) { it.copy(name = v) } },
            onAmount = { id, v -> updateEntry(id) { it.copy(amount = v) } },
            onDelete = { deleteEntry(it) },
        )
        Spacer(Modifier.height(14.dp))
        EntrySection(
            title = "Savings",
            icon = Icons.Rounded.Savings,
            accent = savingColor,
            entries = savings,
            total = savingTotal,
            onAdd = { addEntry(ExpenseCategory.SAVINGS) },
            onName = { id, v -> updateEntry(id) { it.copy(name = v) } },
            onAmount = { id, v -> updateEntry(id) { it.copy(amount = v) } },
            onDelete = { deleteEntry(it) },
        )
        Spacer(Modifier.height(14.dp))
        EntrySection(
            title = "Investments",
            icon = Icons.Rounded.TrendingUp,
            accent = investColor,
            entries = investments,
            total = investTotal,
            onAdd = { addEntry(ExpenseCategory.INVESTMENT) },
            onName = { id, v -> updateEntry(id) { it.copy(name = v) } },
            onAmount = { id, v -> updateEntry(id) { it.copy(amount = v) } },
            onDelete = { deleteEntry(it) },
        )

        Spacer(Modifier.height(14.dp))
        AccountsSection(
            accounts = model.accounts,
            onAdd = { update(model.copy(accounts = model.accounts + ExpenseAccount(UUID.randomUUID().toString(), "", 0.0))) },
            onName = { id, v -> update(model.copy(accounts = model.accounts.map { if (it.id == id) it.copy(name = v) else it })) },
            onBalance = { id, v -> update(model.copy(accounts = model.accounts.map { if (it.id == id) it.copy(balance = v) else it })) },
            onDelete = { id -> update(model.copy(accounts = model.accounts.filterNot { it.id == id })) },
        )
        Spacer(Modifier.height(28.dp))
    }
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
private fun EntrySection(
    title: String,
    icon: ImageVector,
    accent: Color,
    entries: List<ExpenseEntry>,
    total: Double,
    onAdd: () -> Unit,
    onName: (String, String) -> Unit,
    onAmount: (String, Double) -> Unit,
    onDelete: (String) -> Unit,
) {
    ExpenseCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatMoney(total),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
        if (entries.isNotEmpty()) Spacer(Modifier.height(8.dp))
        entries.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlainField(
                    value = entry.name,
                    placeholder = "Name",
                    onChange = { onName(entry.id, it) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                MoneyField(
                    value = entry.amount,
                    onChange = { onAmount(entry.id, it) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.width(110.dp),
                )
                DeleteDot { onDelete(entry.id) }
            }
        }
        Spacer(Modifier.height(6.dp))
        AddRow(onAdd)
    }
}

@Composable
private fun AccountsSection(
    accounts: List<ExpenseAccount>,
    onAdd: () -> Unit,
    onName: (String, String) -> Unit,
    onBalance: (String, Double) -> Unit,
    onDelete: (String) -> Unit,
) {
    val total = accounts.sumOf { it.balance }
    ExpenseCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Accounts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatMoney(total),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (accounts.isNotEmpty()) Spacer(Modifier.height(8.dp))
        accounts.forEach { account ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlainField(
                    value = account.name,
                    placeholder = "Account",
                    onChange = { onName(account.id, it) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                MoneyField(
                    value = account.balance,
                    onChange = { onBalance(account.id, it) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.width(110.dp),
                )
                DeleteDot { onDelete(account.id) }
            }
        }
        Spacer(Modifier.height(6.dp))
        AddRow(onAdd)
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
