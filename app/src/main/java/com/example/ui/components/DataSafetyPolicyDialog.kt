package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AccentGold
import com.example.ui.theme.NavyDark
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPending

@Composable
fun DataSafetyPolicyDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
            .testTag("data_safety_policy_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StatusPaid.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Data Safety Verified",
                        tint = StatusPaid,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Data Safety Policy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Google Play & GDPR Transparency Standards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Key Highlight Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SafetyPill(
                        label = "0% Shared",
                        icon = Icons.Default.NoAccounts,
                        color = StatusPaid,
                        modifier = Modifier.weight(1f)
                    )
                    SafetyPill(
                        label = "Local Sandbox",
                        icon = Icons.Default.Lock,
                        color = OceanBlue,
                        modifier = Modifier.weight(1f)
                    )
                    SafetyPill(
                        label = "No Ads / Trackers",
                        icon = Icons.Default.CheckCircle,
                        color = AccentGold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Oistars Billings is committed to absolute data privacy. All merchant records, client details, and financial transactions are kept entirely on your device with zero cloud synchronization or third-party sharing.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Section 1: Data Collection & Handling
                PolicySectionCard(
                    title = "1. Data Collection & Purpose",
                    icon = Icons.Default.Policy,
                    items = listOf(
                        PolicyItem(
                            category = "Personal Info (Name, Email, Phone, Address)",
                            detail = "Collected only when entered by the user for client store profiles. Used strictly for generating invoices and delivery headers. Never transmitted off the device."
                        ),
                        PolicyItem(
                            category = "Financial Info (Invoices, Amounts, Bank IBAN/BIC)",
                            detail = "Used solely to compute ledger totals, outstanding balances, and VAT calculations. Kept exclusively in your local database."
                        ),
                        PolicyItem(
                            category = "Identifiers & Telemetry (None)",
                            detail = "No device identifiers, advertising IDs, IP tracking, or analytics beacons are collected or transmitted."
                        )
                    )
                )

                // Section 2: Data Sharing Disclosure
                PolicySectionCard(
                    title = "2. Third-Party Data Sharing",
                    icon = Icons.Default.Security,
                    items = listOf(
                        PolicyItem(
                            category = "Zero Third-Party Sharing",
                            detail = "We do not sell, license, lease, or share your data with ad networks, data brokers, credit bureaus, or marketing partners."
                        ),
                        PolicyItem(
                            category = "No Remote Servers",
                            detail = "The app operates completely offline-first. Your accounting records are not stored on remote servers."
                        )
                    )
                )

                // Section 3: Security & Storage Safeguards
                PolicySectionCard(
                    title = "3. Security & Storage Architecture",
                    icon = Icons.Default.Lock,
                    items = listOf(
                        PolicyItem(
                            category = "Application Sandbox Isolation",
                            detail = "Data is saved in Android's isolated SQLite Room storage (MODE_PRIVATE), strictly inaccessible to other apps installed on your device."
                        ),
                        PolicyItem(
                            category = "Biometric & Passkey Protection",
                            detail = "You can enable biometric or passkey authentication in Account Settings to safeguard deletion actions and sensitive client records."
                        )
                    )
                )

                // Section 4: Data Retention & User Erasure Rights
                PolicySectionCard(
                    title = "4. Data Retention & Right to Erasure",
                    icon = Icons.Default.Delete,
                    items = listOf(
                        PolicyItem(
                            category = "Immediate Deletion",
                            detail = "Deleting a client, invoice, subscription, or payment permanently purges the record from device storage with zero backup remnants."
                        ),
                        PolicyItem(
                            category = "Complete Uninstall Reset",
                            detail = "Uninstalling the application immediately deletes the local database and all associated merchant preferences."
                        )
                    )
                )

                // Section 5: Permissions Transparency
                PolicySectionCard(
                    title = "5. Permissions Transparency",
                    icon = Icons.Default.Info,
                    items = listOf(
                        PolicyItem(
                            category = "Zero Dangerous Permissions",
                            detail = "No camera, GPS location, microphone, or address book permissions are requested or required."
                        ),
                        PolicyItem(
                            category = "Google Play Policy Compliant",
                            detail = "Complies strictly with Google Play zero-permission photo picker and privacy standards."
                        )
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("data_safety_policy_dismiss_btn")
            ) {
                Text("Understood & Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

data class PolicyItem(
    val category: String,
    val detail: String
)

@Composable
private fun PolicySectionCard(
    title: String,
    icon: ImageVector,
    items: List<PolicyItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OceanBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = item.category,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.detail,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SafetyPill(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
