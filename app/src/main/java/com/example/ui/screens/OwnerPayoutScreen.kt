package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PayoutEntity
import com.example.data.local.PayoutSpeed
import com.example.data.local.PayoutStatus
import com.example.ui.components.Formatters
import com.example.ui.theme.AccentGold
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyLight
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.StatusDraft
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPending
import com.example.ui.viewmodel.OwnerPayoutMetrics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerPayoutScreen(
    metrics: OwnerPayoutMetrics,
    payouts: List<PayoutEntity>,
    onRequestPayout: (
        amount: Double,
        ownerId: String,
        ownerName: String,
        destinationBank: String,
        destinationIban: String,
        destinationBic: String,
        speed: PayoutSpeed,
        reference: String,
        notes: String
    ) -> Unit,
    onCancelPayout: (PayoutEntity) -> Unit,
    onDeletePayout: (PayoutEntity) -> Unit,
    onSyncPayouts: (() -> Unit)? = null,
    syncStatusMessage: String? = null,
    ownerName: String = "Jurgen Paul Westerveld",
    ownerEmail: String = "westerveldjp@gmail.com",
    modifier: Modifier = Modifier
) {
    var showRequestDialog by remember { mutableStateOf(false) }
    var selectedPayoutForDetail by remember { mutableStateOf<PayoutEntity?>(null) }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    // Auto payout configuration states
    var autoPayoutEnabled by remember { mutableStateOf(true) }
    var autoPayoutSchedule by remember { mutableStateOf("Weekly (Every Friday at 17:00 CET)") }
    var minThreshold by remember { mutableStateOf("500.00") }
    var reserveAmount by remember { mutableStateOf("250.00") }
    var scheduleExpanded by remember { mutableStateOf(false) }
    val schedules = listOf(
        "Instant on Clearance",
        "Weekly (Every Friday at 17:00 CET)",
        "Bi-weekly (15th and End of Month)",
        "Monthly (1st of month)"
    )

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val filteredPayouts = remember(payouts, selectedStatusFilter) {
        when (selectedStatusFilter) {
            "COMPLETED" -> payouts.filter { it.status.equals(PayoutStatus.COMPLETED.name, ignoreCase = true) }
            "PROCESSING" -> payouts.filter {
                it.status.equals(PayoutStatus.PROCESSING.name, ignoreCase = true) ||
                it.status.equals(PayoutStatus.SCHEDULED.name, ignoreCase = true)
            }
            "CANCELLED" -> payouts.filter { it.status.equals(PayoutStatus.CANCELLED.name, ignoreCase = true) }
            else -> payouts
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("owner_payout_screen")
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Owner Profile & Verified Settlement Account Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("owner_profile_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(NavyDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ownerName.take(2).uppercase().ifBlank { "JW" },
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = ownerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verified Owner",
                                    tint = OceanBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Beneficiary Owner • Oistars International B.V.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ownerEmail,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bank Account details row
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = StatusPaid,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "ING Bank N.V. • SEPA Instant",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = StatusPaid.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Active",
                                            color = StatusPaid,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "NL91 INGB 0412 8923 00 • BIC: INGBNL2A",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("NL91 INGB 0412 8923 00"))
                                    Toast.makeText(context, "IBAN copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy IBAN",
                                    tint = OceanBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Primary Available Balance & Hero Payout Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("available_balance_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = AccentGold.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(18.dp)
                                    )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AVAILABLE FOR WITHDRAWAL",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "SEPA Instant Free",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = Formatters.formatCurrency(metrics.availableBalance, metrics.currency),
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Net funds collected from client invoices & retainer subscriptions ready for owner disbursement.",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showRequestDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_request_payout")
                        ) {
                            Icon(
                                Icons.Default.ArrowOutward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Request Payout",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        if (onSyncPayouts != null) {
                            OutlinedButton(
                                onClick = onSyncPayouts,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.35f))
                                ),
                                modifier = Modifier
                                    .height(46.dp)
                                    .testTag("btn_sync_payouts")
                            ) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = "Sync Payouts",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Cloud", fontSize = 13.sp)
                            }
                        }
                    }

                    if (!syncStatusMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = syncStatusMessage,
                            color = AccentGold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Secondary Sub-metrics: In Transit & Lifetime Withdrawn
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "In-Transit / Pending",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = Formatters.formatCurrency(metrics.pendingPayoutsAmount, metrics.currency),
                                color = if (metrics.pendingPayoutsAmount > 0) AccentGold else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Lifetime Draws",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = Formatters.formatCurrency(metrics.totalPaidOutLifetime, metrics.currency),
                                color = StatusPaid,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Automated Payout Rules & Working Capital Settings
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auto_payout_settings_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = OceanBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Automated Settlement Rules",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Switch(
                            checked = autoPayoutEnabled,
                            onCheckedChange = { autoPayoutEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = OceanBlue
                            )
                        )
                    }

                    Text(
                        text = if (autoPayoutEnabled)
                            "Auto-settlement is active. Collected client payments will automatically transfer to your beneficiary IBAN on schedule."
                        else
                            "Auto-settlement is paused. Payouts must be requested manually above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (autoPayoutEnabled) {
                        ExposedDropdownMenuBox(
                            expanded = scheduleExpanded,
                            onExpandedChange = { scheduleExpanded = !scheduleExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = autoPayoutSchedule,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Settlement Frequency") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scheduleExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = scheduleExpanded,
                                onDismissRequest = { scheduleExpanded = false }
                            ) {
                                schedules.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s) },
                                        onClick = {
                                            autoPayoutSchedule = s
                                            scheduleExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = minThreshold,
                                onValueChange = { minThreshold = it },
                                label = { Text("Min Threshold (€)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = reserveAmount,
                                onValueChange = { reserveAmount = it },
                                label = { Text("Reserve Float (€)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Payout History Header & Filter Chips
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Payout History & Draws",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        color = OceanBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${filteredPayouts.size} records",
                            color = OceanBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatusFilter == "ALL",
                        onClick = { selectedStatusFilter = "ALL" },
                        label = { Text("All (${payouts.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OceanBlue.copy(alpha = 0.15f),
                            selectedLabelColor = OceanBlue
                        )
                    )
                    FilterChip(
                        selected = selectedStatusFilter == "COMPLETED",
                        onClick = { selectedStatusFilter = "COMPLETED" },
                        label = { Text("Completed (${metrics.completedPayoutsCount})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusPaid.copy(alpha = 0.15f),
                            selectedLabelColor = StatusPaid
                        )
                    )
                    FilterChip(
                        selected = selectedStatusFilter == "PROCESSING",
                        onClick = { selectedStatusFilter = "PROCESSING" },
                        label = { Text("In Transit (${metrics.inTransitPayoutsCount})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusPending.copy(alpha = 0.15f),
                            selectedLabelColor = StatusPending
                        )
                    )
                }
            }
        }

        // Section 5: Payout Records List
        if (filteredPayouts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No payout records in this filter",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Initiate a new withdrawal using 'Request Payout' above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredPayouts, key = { it.id }) { payout ->
                PayoutListItemCard(
                    payout = payout,
                    onClick = { selectedPayoutForDetail = payout }
                )
            }
        }
    }

    // Modal 1: Request Owner Payout Dialog
    if (showRequestDialog) {
        RequestPayoutDialog(
            availableBalance = metrics.availableBalance,
            currency = metrics.currency,
            beneficiaryName = ownerName,
            beneficiaryBank = "ING Bank N.V.",
            beneficiaryIban = "NL91 INGB 0412 8923 00",
            beneficiaryBic = "INGBNL2A",
            onDismiss = { showRequestDialog = false },
            onConfirm = { amount, speed, reference, notes ->
                onRequestPayout(
                    amount,
                    "jurgen-westerveld",
                    ownerName,
                    "ING Bank N.V.",
                    "NL91 INGB 0412 8923 00",
                    "INGBNL2A",
                    speed,
                    reference,
                    notes
                )
                showRequestDialog = false
                Toast.makeText(context, "Payout of €%.2f initiated successfully!".format(amount), Toast.LENGTH_LONG).show()
            }
        )
    }

    // Modal 2: Payout Detail / Receipt Dialog
    selectedPayoutForDetail?.let { payout ->
        PayoutDetailDialog(
            payout = payout,
            onDismiss = { selectedPayoutForDetail = null },
            onCancelPayout = {
                onCancelPayout(payout)
                selectedPayoutForDetail = null
                Toast.makeText(context, "Payout ${payout.id} cancelled", Toast.LENGTH_SHORT).show()
            },
            onDeletePayout = {
                onDeletePayout(payout)
                selectedPayoutForDetail = null
            }
        )
    }
}

@Composable
fun PayoutListItemCard(
    payout: PayoutEntity,
    onClick: () -> Unit
) {
    val isCompleted = payout.status.equals(PayoutStatus.COMPLETED.name, ignoreCase = true)
    val isProcessing = payout.status.equals(PayoutStatus.PROCESSING.name, ignoreCase = true) ||
                       payout.status.equals(PayoutStatus.SCHEDULED.name, ignoreCase = true)

    val statusColor = when {
        isCompleted -> StatusPaid
        isProcessing -> StatusPending
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusBg = when {
        isCompleted -> StatusPaid.copy(alpha = 0.12f)
        isProcessing -> StatusPending.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payout_item_${payout.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = statusBg,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (payout.payoutSpeed == PayoutSpeed.INSTANT.name) Icons.Default.Bolt else Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = payout.reference.ifBlank { "Owner Settlement" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${payout.destinationBank} • •••• ${payout.destinationIban.takeLast(4)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = Formatters.formatDate(payout.initiatedAt),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatCurrency(payout.amount, payout.currency),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = payout.payoutStatus.label,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestPayoutDialog(
    availableBalance: Double,
    currency: String,
    beneficiaryName: String,
    beneficiaryBank: String,
    beneficiaryIban: String,
    beneficiaryBic: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, speed: PayoutSpeed, reference: String, notes: String) -> Unit
) {
    var amountInput by remember { mutableStateOf(if (availableBalance > 0) "%.2f".format(availableBalance) else "500.00") }
    var selectedSpeed by remember { mutableStateOf(PayoutSpeed.INSTANT) }
    var referenceText by remember { mutableStateOf("Owner Draw - Oistars International") }
    var notesText by remember { mutableStateOf("") }

    val enteredAmount = amountInput.toDoubleOrNull() ?: 0.0
    val isAmountValid = enteredAmount > 0 && enteredAmount <= (availableBalance + 0.01)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = OceanBlue.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Payments,
                        contentDescription = null,
                        tint = OceanBlue,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Request Owner Payout", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Instant SEPA Direct Settlement", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("request_payout_dialog"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Balance Banner
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Available to Withdraw:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            Formatters.formatCurrency(availableBalance, currency),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = OceanBlue
                        )
                    }
                }

                // Quick percentage chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.25 to "25%", 0.50 to "50%", 0.75 to "75%", 1.0 to "100% (All)").forEach { (fraction, label) ->
                        OutlinedButton(
                            onClick = {
                                val calc = (availableBalance * fraction)
                                amountInput = "%.2f".format(calc)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Amount input field
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Payout Amount (€)") },
                    prefix = { Text("€ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = !isAmountValid && amountInput.isNotBlank(),
                    supportingText = {
                        if (enteredAmount > availableBalance) {
                            Text("Amount exceeds available balance", color = StatusOverdue)
                        } else if (enteredAmount <= 0 && amountInput.isNotBlank()) {
                            Text("Amount must be greater than €0", color = StatusOverdue)
                        } else {
                            Text("Transfer fee: €0.00 (Free)")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payout_amount_input")
                )

                // Speed Selector Cards
                Text("Settlement Method", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSpeed = PayoutSpeed.INSTANT },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedSpeed == PayoutSpeed.INSTANT) OceanBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (selectedSpeed == PayoutSpeed.INSTANT) ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(OceanBlue)
                        ) else null
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Instant SEPA", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text("Within seconds", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Fee: €0.00", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = StatusPaid)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSpeed = PayoutSpeed.STANDARD },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedSpeed == PayoutSpeed.STANDARD) OceanBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (selectedSpeed == PayoutSpeed.STANDARD) ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(OceanBlue)
                        ) else null
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Standard SEPA", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text("1-2 business days", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Fee: €0.00", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = StatusPaid)
                        }
                    }
                }

                // Beneficiary Target Summary
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Destination: $beneficiaryBank",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$beneficiaryIban • $beneficiaryName",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = referenceText,
                    onValueChange = { referenceText = it },
                    label = { Text("Reference Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(enteredAmount, selectedSpeed, referenceText, notesText)
                },
                enabled = isAmountValid,
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                modifier = Modifier.testTag("confirm_payout_button")
            ) {
                Text("Confirm Transfer", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PayoutDetailDialog(
    payout: PayoutEntity,
    onDismiss: () -> Unit,
    onCancelPayout: () -> Unit,
    onDeletePayout: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val isCompleted = payout.status.equals(PayoutStatus.COMPLETED.name, ignoreCase = true)
    val isCancellable = payout.status.equals(PayoutStatus.PROCESSING.name, ignoreCase = true) ||
                        payout.status.equals(PayoutStatus.SCHEDULED.name, ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Payout Receipt", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("ID: #${payout.id}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Surface(
                    color = if (isCompleted) StatusPaid.copy(alpha = 0.15f) else StatusPending.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = payout.payoutStatus.label,
                        color = if (isCompleted) StatusPaid else StatusPending,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payout_detail_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Large Amount Banner
                Surface(
                    color = NavyDark,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Disbursed Amount",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatCurrency(payout.amount, payout.currency),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentGold, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${payout.speed.label} • Zero Fee (€0.00)",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Beneficiary & Bank Details
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Beneficiary:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(payout.ownerName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Destination Bank:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(payout.destinationBank, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("IBAN:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(payout.destinationIban, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("BIC / SWIFT:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(payout.destinationBic, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Initiated Date:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatDate(payout.initiatedAt), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (payout.completedAt != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Completed Date:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Formatters.formatDate(payout.completedAt), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Reference:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(payout.reference, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isCancellable) {
                    OutlinedButton(
                        onClick = onCancelPayout,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusOverdue)
                    ) {
                        Text("Cancel Payout")
                    }
                }

                Button(
                    onClick = {
                        clipboardManager.setText(
                            AnnotatedString("Payout ID: ${payout.id}\nAmount: €${payout.amount}\nIBAN: ${payout.destinationIban}\nRef: ${payout.reference}")
                        )
                        Toast.makeText(context, "Receipt copied to clipboard", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                ) {
                    Text("Copy Receipt")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
