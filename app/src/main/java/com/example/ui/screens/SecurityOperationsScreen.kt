package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContainmentPolicy
import com.example.data.model.ContainmentStatus
import com.example.data.model.FirewallRuleStatus
import com.example.data.model.SecurityAuditEventType
import com.example.data.model.SecurityAuditLogEntry
import com.example.data.model.SecurityPostureMetrics
import com.example.data.model.SecurityThreatEvent
import com.example.data.model.ThreatSeverity
import com.example.ui.components.Formatters
import com.example.ui.theme.AccentGold
import com.example.ui.theme.NavyDark
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPending
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityOperationsScreen(
    metrics: SecurityPostureMetrics,
    threats: List<SecurityThreatEvent>,
    firewallRules: List<FirewallRuleStatus>,
    auditLogs: List<SecurityAuditLogEntry>,
    policy: ContainmentPolicy,
    isScanning: Boolean,
    operatorName: String,
    onRunPerimeterScan: () -> Unit,
    onAssessThreatWithAi: (SecurityThreatEvent) -> Unit,
    onTriggerBreachProbe: (String) -> Unit,
    onApproveContainment: (SecurityThreatEvent) -> Unit,
    onDismissThreat: (SecurityThreatEvent) -> Unit,
    onUpdatePolicy: (ContainmentPolicy) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember { mutableIntStateOf(0) }
    val pendingApprovals = remember(threats) {
        threats.filter { it.containmentStatus == ContainmentStatus.PENDING_HUMAN_APPROVAL }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("security_operations_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Security Operations Center", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "Live Threat Detection • AI Risk • Auto-Containment",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_security_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Security Posture Banner
            item {
                SecurityPostureHeroCard(
                    metrics = metrics,
                    isScanning = isScanning,
                    onRunPerimeterScan = onRunPerimeterScan,
                    onTriggerBreachProbe = onTriggerBreachProbe
                )
            }

            // Tab Switcher
            item {
                val tabs = listOf(
                    "Threats (${threats.size})",
                    "Firewall & Auto-Contain",
                    "Approvals (${pendingApprovals.size})",
                    "Audit Logs (${auditLogs.size})"
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = OceanBlue
                ) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = label,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.testTag("security_tab_$index")
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    if (threats.isEmpty()) {
                        item {
                            EmptySecurityStateCard(
                                title = "No active threats detected",
                                subtitle = "All WAF rules and settlement webhooks are operating cleanly."
                            )
                        }
                    } else {
                        items(threats, key = { it.id }) { threat ->
                            ThreatDetectionCard(
                                threat = threat,
                                isScanning = isScanning,
                                onAssessAi = { onAssessThreatWithAi(threat) },
                                onApprove = { onApproveContainment(threat) },
                                onDismiss = { onDismissThreat(threat) }
                            )
                        }
                    }
                }

                1 -> {
                    item {
                        ContainmentPolicyCard(
                            policy = policy,
                            onSavePolicy = onUpdatePolicy
                        )
                    }
                    items(firewallRules, key = { it.ruleId }) { rule ->
                        FirewallRuleCard(rule = rule)
                    }
                }

                2 -> {
                    if (pendingApprovals.isEmpty()) {
                        item {
                            EmptySecurityStateCard(
                                title = "Human approval queue is clear",
                                subtitle = "Critical breaches and payout-impacting threats requiring operator sign-off appear here."
                            )
                        }
                    } else {
                        items(pendingApprovals, key = { it.id }) { threat ->
                            HumanApprovalQueueCard(
                                threat = threat,
                                operatorName = operatorName,
                                onApprove = { onApproveContainment(threat) },
                                onDismiss = { onDismissThreat(threat) }
                            )
                        }
                    }
                }

                else -> {
                    items(auditLogs, key = { it.id }) { entry ->
                        SecurityAuditLogCard(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityPostureHeroCard(
    metrics: SecurityPostureMetrics,
    isScanning: Boolean,
    onRunPerimeterScan: () -> Unit,
    onTriggerBreachProbe: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("security_posture_hero"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = NavyDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    Surface(
                        color = StatusPaid.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "SOC Shield",
                            tint = StatusPaid,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PERIMETER POSTURE SCORE",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${metrics.postureScore} / 100",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Surface(
                    color = if (metrics.pendingHumanApprovalsCount > 0) StatusPending.copy(alpha = 0.22f) else StatusPaid.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (metrics.pendingHumanApprovalsCount > 0) {
                            "${metrics.pendingHumanApprovalsCount} HITL Pending"
                        } else {
                            "Perimeter Guarded"
                        },
                        color = if (metrics.pendingHumanApprovalsCount > 0) AccentGold else StatusPaid,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PostureMiniStat(
                    label = "Active Threats",
                    value = "${metrics.activeThreatsCount}",
                    color = if (metrics.activeThreatsCount > 0) AccentGold else StatusPaid,
                    modifier = Modifier.weight(1f)
                )
                PostureMiniStat(
                    label = "WAF Blocked",
                    value = "${metrics.firewallBreachesBlocked}",
                    color = OceanBlue,
                    modifier = Modifier.weight(1f)
                )
                PostureMiniStat(
                    label = "Contained",
                    value = "${metrics.autoContainedCount}",
                    color = StatusPaid,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Last live perimeter inspection: ${Formatters.formatRelativeTime(metrics.lastScanAt)}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRunPerimeterScan,
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_run_perimeter_scan")
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Perimeter Scan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { onTriggerBreachProbe("PAYOUT_SPIKE") },
                    enabled = !isScanning,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.35f))
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_test_breach_probe")
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AccentGold, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Breach", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun PostureMiniStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = label, color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun ThreatDetectionCard(
    threat: SecurityThreatEvent,
    isScanning: Boolean,
    onAssessAi: () -> Unit,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    val severityColor = when (threat.severity) {
        ThreatSeverity.CRITICAL -> StatusOverdue
        ThreatSeverity.HIGH -> StatusPending
        ThreatSeverity.MEDIUM -> OceanBlue
        ThreatSeverity.LOW -> StatusPaid
    }
    val statusColor = when (threat.containmentStatus) {
        ContainmentStatus.AUTO_CONTAINED, ContainmentStatus.APPROVED_CONTAINED -> StatusPaid
        ContainmentStatus.PENDING_HUMAN_APPROVAL -> StatusOverdue
        ContainmentStatus.DISMISSED -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> OceanBlue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("threat_card_${threat.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = severityColor.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = threat.severity.label.uppercase(),
                            color = severityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(color = statusColor.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = threat.containmentStatus.label,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = Formatters.formatRelativeTime(threat.detectedAt),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = threat.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = threat.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Firewall Breach Rule: ${threat.firewallRuleMatched} • Endpoint: ${threat.targetEndpoint}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Source IP: ${threat.sourceIp} • Vector: ${threat.breachType}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // AI Risk Score Block
            if (threat.aiRiskScore > 0) {
                Surface(
                    color = OceanBlue.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI Risk Assessment", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OceanBlue)
                            }
                            Text(
                                text = "${threat.aiRiskScore}/100 (${threat.aiRiskLevel})",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = if (threat.aiRiskScore >= 85) StatusOverdue else OceanBlue
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (threat.aiRiskScore / 100f).coerceIn(0f, 1f) },
                            color = if (threat.aiRiskScore >= 85) StatusOverdue else OceanBlue,
                            trackColor = OceanBlue.copy(alpha = 0.16f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        if (threat.aiRecommendedAction.isNotBlank()) {
                            Text(
                                text = "Action: ${threat.aiRecommendedAction}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (threat.aiRationale.isNotBlank()) {
                            Text(
                                text = threat.aiRationale,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onAssessAi,
                    enabled = !isScanning,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("btn_ai_assess_${threat.id}")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Re-Score", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (threat.containmentStatus == ContainmentStatus.PENDING_HUMAN_APPROVAL ||
                    threat.containmentStatus == ContainmentStatus.AI_ASSESSED ||
                    threat.containmentStatus == ContainmentStatus.DETECTED
                ) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusPaid),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("btn_approve_contain_${threat.id}")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Contain", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_dismiss_threat_${threat.id}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss Threat", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ContainmentPolicyCard(
    policy: ContainmentPolicy,
    onSavePolicy: (ContainmentPolicy) -> Unit
) {
    var autoContain by remember(policy) { mutableStateOf(policy.autoContainEnabled) }
    var threshold by remember(policy) { mutableIntStateOf(policy.autoContainRiskThreshold) }
    var hitlPayoutGate by remember(policy) { mutableStateOf(policy.requireHumanApprovalForPayoutFreeze) }
    var strictWaf by remember(policy) { mutableStateOf(policy.strictWafBlocking) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("containment_policy_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Policy, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Automated Containment & WAF Policy", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Automated Edge Containment", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Auto-block threats when AI risk exceeds threshold", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = autoContain,
                    onCheckedChange = { autoContain = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = OceanBlue)
                )
            }

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Auto-Contain AI Risk Threshold", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("$threshold / 100", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OceanBlue)
                }
                Slider(
                    value = threshold.toFloat(),
                    onValueChange = { threshold = it.roundToInt() },
                    valueRange = 50f..95f,
                    colors = SliderDefaults.colors(thumbColor = OceanBlue, activeTrackColor = OceanBlue)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Human Sign-Off on Payout Freeze", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Require HITL approval before freezing Wero/SEPA draws", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = hitlPayoutGate,
                    onCheckedChange = { hitlPayoutGate = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = OceanBlue)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Strict L7 WAF Enforcement", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Enforce HMAC & SQLi blocking across all endpoints", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = strictWaf,
                    onCheckedChange = { strictWaf = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = OceanBlue)
                )
            }

            Button(
                onClick = {
                    onSavePolicy(
                        ContainmentPolicy(
                            autoContainEnabled = autoContain,
                            autoContainRiskThreshold = threshold,
                            requireHumanApprovalForPayoutFreeze = hitlPayoutGate,
                            strictWafBlocking = strictWaf
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_containment_policy")
            ) {
                Icon(Icons.Default.GppGood, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Containment Policy", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FirewallRuleCard(rule: FirewallRuleStatus) {
    val activeColor = if (rule.status == "ALERTING") StatusPending else StatusPaid
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = CircleShape,
                    color = activeColor.copy(alpha = 0.14f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Block, contentDescription = null, tint = activeColor, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "${rule.ruleId} • ${rule.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        text = "${rule.layer} • Mode: ${rule.mode}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${rule.blockedCount} blocked",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = OceanBlue
                )
                Surface(color = activeColor.copy(alpha = 0.14f), shape = RoundedCornerShape(4.dp)) {
                    Text(
                        text = rule.status,
                        color = activeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HumanApprovalQueueCard(
    threat: SecurityThreatEvent,
    operatorName: String,
    onApprove: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("approval_queue_card_${threat.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StatusOverdue.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = StatusOverdue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("OPERATOR SIGN-OFF REQUIRED", color = StatusOverdue, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
                Text("Risk ${threat.aiRiskScore}/100", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = StatusOverdue)
            }

            Text(text = threat.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = threat.aiRecommendedAction.ifBlank { threat.description }, fontSize = 12.sp)
            Text(
                text = "Assigned Operator: $operatorName • Target: ${threat.targetEndpoint}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = StatusOverdue.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusPaid),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_hitl_approve_${threat.id}")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Approve Containment", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("btn_hitl_dismiss_${threat.id}")
                ) {
                    Text("Dismiss", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SecurityAuditLogCard(entry: SecurityAuditLogEntry) {
    val badgeColor = when (entry.eventType) {
        SecurityAuditEventType.FIREWALL_BREACH, SecurityAuditEventType.THREAT_DETECTED -> StatusOverdue
        SecurityAuditEventType.AUTO_CONTAINMENT, SecurityAuditEventType.HUMAN_APPROVAL -> StatusPaid
        SecurityAuditEventType.AI_RISK_ASSESSMENT -> OceanBlue
        else -> AccentGold
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audit_log_card_${entry.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.14f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.History, contentDescription = null, tint = badgeColor, modifier = Modifier.size(17.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = badgeColor.copy(alpha = 0.14f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = entry.eventType.label,
                            color = badgeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = Formatters.formatRelativeTime(entry.timestamp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(text = entry.summary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(
                    text = entry.details,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Actor: ${entry.actor} • Ref: ${entry.threatId}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun EmptySecurityStateCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.GppGood, contentDescription = null, tint = StatusPaid, modifier = Modifier.size(34.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
