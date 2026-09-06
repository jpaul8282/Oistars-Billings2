package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.InvoiceEntity
import com.example.data.model.InvoiceStatus
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyLight
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.StatusDraft
import com.example.ui.theme.StatusDraftBg
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val shortDateFormat = SimpleDateFormat("dd MMM", Locale.US)

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val minuteMs = 60 * 1000L
        val hourMs = 60 * minuteMs
        val dayMs = 24 * hourMs

        return when {
            diff < 0 -> formatShortDate(timestamp)
            diff < minuteMs -> "Just now"
            diff < hourMs -> "${(diff / minuteMs).coerceAtLeast(1)}m ago"
            diff < dayMs -> "${diff / hourMs}h ago"
            diff < 2 * dayMs -> "Yesterday"
            diff < 7 * dayMs -> "${diff / dayMs}d ago"
            else -> formatShortDate(timestamp)
        }
    }

    fun formatCurrency(amount: Double, currency: String = "EUR"): String {
        val symbol = when (currency.uppercase()) {
            "USD" -> "$"
            "GBP" -> "£"
            else -> "€"
        }
        return String.format(Locale.US, "%s%,.2f", symbol, amount)
    }
}

@Composable
fun StatusBadge(
    status: InvoiceStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        InvoiceStatus.PAID -> StatusPaidBg to StatusPaid
        InvoiceStatus.PENDING -> StatusPendingBg to StatusPending
        InvoiceStatus.OVERDUE -> StatusOverdueBg to StatusOverdue
        InvoiceStatus.DRAFT -> StatusDraftBg to StatusDraft
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = status.label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MetricStatCard(
    title: String,
    amount: Double,
    currency: String = "EUR",
    icon: ImageVector,
    iconColor: Color,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Formatters.formatCurrency(amount, currency),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search invoices or clients...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("search_bar")
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentDialog(
    invoice: InvoiceEntity,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, method: String, ref: String, notes: String) -> Unit
) {
    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", invoice.balanceDue)) }
    var selectedMethod by remember { mutableStateOf("iDEAL") }
    var refText by remember { mutableStateOf("TRX-${(System.currentTimeMillis() % 1000000).toString().padStart(6, '0')}") }
    var notesText by remember { mutableStateOf("Settlement for ${invoice.id}") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val methods = listOf("iDEAL", "Bank Transfer (SEPA)", "Credit Card", "Stripe", "PayPal", "Cash / Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Payment",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Invoice: ${invoice.id} (${invoice.clientName})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Balance Due: ${Formatters.formatCurrency(invoice.balanceDue, invoice.currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )

                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        methods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    selectedMethod = method
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = refText,
                    onValueChange = { refText = it },
                    label = { Text("Transaction Reference") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: invoice.balanceDue
                    onConfirm(amount, selectedMethod, refText, notesText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusPaid),
                modifier = Modifier.testTag("confirm_payment_button")
            ) {
                Text("Confirm Payment", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
