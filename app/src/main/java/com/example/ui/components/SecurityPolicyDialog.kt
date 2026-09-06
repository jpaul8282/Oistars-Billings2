package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VpnKey
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

@Composable
fun SecurityPolicyDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
            .testTag("security_policy_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NavyDark.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security Policy",
                        tint = NavyDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Application Security Policy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Standards, Threat Safeguards & Vulnerability Guidelines",
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
                // Security Badges Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecurityPill(
                        label = "Sandbox Isolation",
                        icon = Icons.Default.Lock,
                        color = OceanBlue,
                        modifier = Modifier.weight(1f)
                    )
                    SecurityPill(
                        label = "Zero DCL Execution",
                        icon = Icons.Default.Code,
                        color = StatusPaid,
                        modifier = Modifier.weight(1f)
                    )
                    SecurityPill(
                        label = "SQL Injection Safe",
                        icon = Icons.Default.Storage,
                        color = AccentGold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Oistars Billings implements high-assurance client-side security architecture. We enforce strict data confinement, zero external telemetry, zero dynamic code loading, and safe memory practices.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Pillar 1: Storage & Sandbox Isolation
                SecurityPillarCard(
                    title = "1. Internal Sandbox & Data Confinement",
                    icon = Icons.Default.Storage,
                    points = listOf(
                        "Private App Sandbox: All SQLite Room database files, client records, and invoice caches are restricted to private app-internal directories (/data/data) protected by Linux user ID (UID) isolation.",
                        "Zero World-Readable Files: No data is ever written to unpartitioned shared storage. Scoped storage boundaries are strictly enforced.",
                        "Temporary Cache Purging: Exported PDF invoices are generated inside dedicated private cache storage and granted through ephemeral FileProvider URIs with automatic cleanup."
                    )
                )

                // Pillar 2: System Integrity & Zero DCL
                SecurityPillarCard(
                    title = "2. System Integrity & Zero Dynamic Code Loading",
                    icon = Icons.Default.Code,
                    points = listOf(
                        "Zero DCL (Dynamic Code Loading): In compliance with Google Play System Integrity rules, this application NEVER loads external, remote, or runtime executable code (.dex, .jar, .so).",
                        "Immutable Compiled Binary: All execution paths and business logic are statically compiled with ProGuard/R8 code minification and strict symbol verification.",
                        "Zero Third-Party SDK Trackers: No advertising networks, marketing SDKs, or background analytics engines have execution privileges within the app."
                    )
                )

                // Pillar 3: Injection Prevention & Data Sanitation
                SecurityPillarCard(
                    title = "3. SQL Injection & Memory Safety",
                    icon = Icons.Default.VpnKey,
                    points = listOf(
                        "Parameterized Queries: Jetpack Room SQLite DAO utilizes 100% compile-time verified parameterized queries, completely neutralizing SQL injection risks.",
                        "Type-Safe Navigation: App routing and argument serialization strictly use Kotlinx Serialization type-safe keys without reflective string evaluation.",
                        "Input Normalization: Currency formatting, VAT calculations, and IBAN strings undergo strict regular expression validation prior to persistence."
                    )
                )

                // Pillar 4: Authentication & Authorization
                SecurityPillarCard(
                    title = "4. Authentication & Access Protection",
                    icon = Icons.Default.Lock,
                    points = listOf(
                        "Biometric & PIN Lock: Optional biometric / passkey verification can be enabled in Account Settings for sensitive merchant actions.",
                        "Protected Destructive Actions: Irreversible operations (such as deleting an invoice or wiping merchant settings) require explicit secondary confirmation dialogs.",
                        "Session Cleansing: Logging out purges transient in-memory authentication states and active cache buffers."
                    )
                )

                // Pillar 5: Vulnerability Disclosure & Bug Reporting
                SecurityPillarCard(
                    title = "5. Coordinated Vulnerability Disclosure",
                    icon = Icons.Default.Mail,
                    points = listOf(
                        "Security Contact: For reporting suspected vulnerabilities or security disclosures, email security@oistars.nl directly.",
                        "Response Commitment: The security response team acknowledges received reports within 48 business hours and prioritizes remediation.",
                        "Safe Harbor: Security researchers acting in good faith who avoid privacy violations, data destruction, and service disruption will not face legal action."
                    )
                )

                // Section 6: Regulatory Compliance
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = OceanBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Compliant with Google Play Developer Program Policies, GDPR Data Protection principles, and OWASP Mobile Application Security Verification Standards (MASVS).",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("security_policy_dismiss_btn")
            ) {
                Text("Acknowledge & Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SecurityPill(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun SecurityPillarCard(
    title: String,
    icon: ImageVector,
    points: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OceanBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            points.forEach { point ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OceanBlue
                    )
                    Text(
                        text = point,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
