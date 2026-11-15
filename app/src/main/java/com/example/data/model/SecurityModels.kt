package com.example.data.model

enum class ThreatSeverity(val label: String, val weight: Int) {
    CRITICAL("Critical", 95),
    HIGH("High", 78),
    MEDIUM("Medium", 52),
    LOW("Low", 24);

    companion object {
        fun fromString(value: String): ThreatSeverity {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}

enum class ContainmentStatus(val label: String) {
    DETECTED("Detected"),
    AI_ASSESSED("AI Assessed"),
    PENDING_HUMAN_APPROVAL("Pending Approval"),
    AUTO_CONTAINED("Auto-Contained"),
    APPROVED_CONTAINED("Approved Contained"),
    DISMISSED("Dismissed");

    companion object {
        fun fromString(value: String): ContainmentStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DETECTED
        }
    }
}

enum class SecurityAuditEventType(val label: String) {
    THREAT_DETECTED("Threat Detected"),
    FIREWALL_BREACH("Firewall Breach"),
    AI_RISK_ASSESSMENT("AI Risk Assessment"),
    AUTO_CONTAINMENT("Auto-Containment"),
    HUMAN_APPROVAL("Human Approval"),
    HUMAN_DISMISSAL("Human Dismissal"),
    POLICY_CHANGE("Policy Update");

    companion object {
        fun fromString(value: String): SecurityAuditEventType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: THREAT_DETECTED
        }
    }
}

data class SecurityThreatEvent(
    val id: String,
    val title: String,
    val description: String,
    val breachType: String,
    val sourceIp: String,
    val targetEndpoint: String,
    val firewallRuleMatched: String,
    val severity: ThreatSeverity = ThreatSeverity.HIGH,
    val detectedAt: Long = System.currentTimeMillis(),
    val containmentStatus: ContainmentStatus = ContainmentStatus.DETECTED,
    val aiRiskScore: Int = 0,
    val aiRiskLevel: String = "UNASSESSED",
    val aiAttackVector: String = "",
    val aiBlastRadius: String = "",
    val aiRecommendedAction: String = "",
    val aiRationale: String = "",
    val requiresHumanApproval: Boolean = false,
    val approvedBy: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

data class FirewallRuleStatus(
    val ruleId: String,
    val name: String,
    val layer: String,
    val mode: String,
    val status: String,
    val blockedCount: Int,
    val lastTriggeredAt: Long?
)

data class SecurityAuditLogEntry(
    val id: String,
    val threatId: String,
    val eventType: SecurityAuditEventType,
    val actor: String,
    val summary: String,
    val details: String,
    val riskScore: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class ContainmentPolicy(
    val autoContainEnabled: Boolean = true,
    val autoContainRiskThreshold: Int = 80,
    val requireHumanApprovalForPayoutFreeze: Boolean = true,
    val strictWafBlocking: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

data class SecurityPostureMetrics(
    val postureScore: Int = 94,
    val activeThreatsCount: Int = 0,
    val firewallBreachesBlocked: Int = 0,
    val autoContainedCount: Int = 0,
    val pendingHumanApprovalsCount: Int = 0,
    val lastScanAt: Long = System.currentTimeMillis()
)

object SecurityBaselineDefaults {
    fun defaultFirewallRules(): List<FirewallRuleStatus> {
        val now = System.currentTimeMillis()
        return listOf(
            FirewallRuleStatus(
                ruleId = "WAF-EDGE-01",
                name = "Wero Webhook HMAC Signature Guard",
                layer = "L7 Payment Gateway",
                mode = "ENFORCE",
                status = "ACTIVE",
                blockedCount = 14,
                lastTriggeredAt = now - 640_000L
            ),
            FirewallRuleStatus(
                ruleId = "FW-SQLI-04",
                name = "Deep SQLi / Payload Sanitizer",
                layer = "L7 API Perimeter",
                mode = "BLOCK",
                status = "ACTIVE",
                blockedCount = 31,
                lastTriggeredAt = now - 2100_000L
            ),
            FirewallRuleStatus(
                ruleId = "FW-PAY-09",
                name = "Owner IBAN Payout Velocity Limiter",
                layer = "Settlement Engine",
                mode = "HITL_GATE",
                status = "ALERTING",
                blockedCount = 3,
                lastTriggeredAt = now - 320_000L
            ),
            FirewallRuleStatus(
                ruleId = "FW-AUTH-12",
                name = "Credential Stuffing & Token Replay Shield",
                layer = "Identity / Firebase Auth",
                mode = "AUTO_DROP",
                status = "ACTIVE",
                blockedCount = 22,
                lastTriggeredAt = now - 4500_000L
            )
        )
    }

    fun initialThreats(now: Long = System.currentTimeMillis()): List<SecurityThreatEvent> {
        return listOf(
            SecurityThreatEvent(
                id = "THR-40912",
                title = "Unsigned Wero Webhook Replay Attempt",
                description = "External node replayed settlement webhook signature targeting /api/webhooks/wero with mismatched HMAC digest.",
                breachType = "FIREWALL_BREACH_HMAC_REPLAY",
                sourceIp = "185.220.101.42",
                targetEndpoint = "/api/webhooks/wero",
                firewallRuleMatched = "WAF-EDGE-01",
                severity = ThreatSeverity.CRITICAL,
                detectedAt = now - 420_000L,
                containmentStatus = ContainmentStatus.PENDING_HUMAN_APPROVAL,
                aiRiskScore = 92,
                aiRiskLevel = "CRITICAL",
                aiAttackVector = "Settlement Webhook Signature Spoofing / Replay",
                aiBlastRadius = "Wero settlement ledger & automated payout triggers",
                aiRecommendedAction = "Quarantine source IP 185.220.101.42, rotate Wero webhook secret, and freeze outbound payout hooks pending operator sign-off.",
                aiRationale = "Critical severity anomaly on financial webhook endpoint requires explicit human sign-off before freezing live payout settlement pipeline.",
                requiresHumanApproval = true,
                updatedAt = now - 390_000L
            ),
            SecurityThreatEvent(
                id = "THR-40887",
                title = "Credential Brute-Force Burst on Admin API",
                description = "High-frequency token probing blocked by Identity Edge Firewall rule FW-AUTH-12.",
                breachType = "BRUTE_FORCE_PROBE",
                sourceIp = "91.199.212.55",
                targetEndpoint = "/api/orders",
                firewallRuleMatched = "FW-AUTH-12",
                severity = ThreatSeverity.HIGH,
                detectedAt = now - 1900_000L,
                containmentStatus = ContainmentStatus.AUTO_CONTAINED,
                aiRiskScore = 84,
                aiRiskLevel = "HIGH",
                aiAttackVector = "Distributed Auth Token Enumeration",
                aiBlastRadius = "Read-only order listing endpoint",
                aiRecommendedAction = "Automated edge IP drop applied for 24h; no credential compromise detected.",
                aiRationale = "Risk score 84 >= threshold 80 with non-payout scope; automated containment executed immediately.",
                requiresHumanApproval = false,
                approvedBy = "Auto-Containment Policy",
                updatedAt = now - 1880_000L
            )
        )
    }

    fun initialAuditLogs(now: Long = System.currentTimeMillis()): List<SecurityAuditLogEntry> {
        return listOf(
            SecurityAuditLogEntry(
                id = "AUD-9003",
                threatId = "THR-40912",
                eventType = SecurityAuditEventType.AI_RISK_ASSESSMENT,
                actor = "Genkit SOC AI Engine",
                summary = "AI Risk Score 92/100 (CRITICAL) — Escalated to Human Approval",
                details = "Wero webhook replay detected against /api/webhooks/wero. Escalated to operator approval queue.",
                riskScore = 92,
                timestamp = now - 390_000L
            ),
            SecurityAuditLogEntry(
                id = "AUD-9002",
                threatId = "THR-40912",
                eventType = SecurityAuditEventType.FIREWALL_BREACH,
                actor = "Firewall WAF-EDGE-01",
                summary = "Firewall breach signature tripped by 185.220.101.42",
                details = "HMAC replay mismatch blocked at L7 Payment Gateway perimeter.",
                riskScore = 92,
                timestamp = now - 420_000L
            ),
            SecurityAuditLogEntry(
                id = "AUD-9001",
                threatId = "THR-40887",
                eventType = SecurityAuditEventType.AUTO_CONTAINMENT,
                actor = "Auto-Containment Policy",
                summary = "Auto-contained IP 91.199.212.55 (Risk Score 84/100)",
                details = "Credential probing dropped by FW-AUTH-12 under automated threshold >= 80.",
                riskScore = 84,
                timestamp = now - 1880_000L
            )
        )
    }
}
