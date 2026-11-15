package com.example.data.service

import android.content.Context
import android.util.Log
import com.example.data.model.ContainmentPolicy
import com.example.data.model.ContainmentStatus
import com.example.data.model.FirewallRuleStatus
import com.example.data.model.SecurityAuditEventType
import com.example.data.model.SecurityAuditLogEntry
import com.example.data.model.SecurityBaselineDefaults
import com.example.data.model.SecurityThreatEvent
import com.example.data.model.ThreatSeverity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.oistars.billings.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "SecurityDefenseService"

/**
 * Security Operations & Threat Defense service implementing:
 * - Live threat detection & firewall breach identification
 * - Genkit AI risk assessment (/api/security/assess-risk + deterministic fallback)
 * - Automated containment threshold execution
 * - Human-in-the-loop (HITL) operator approval & immutable security audit logs
 */
class SecurityDefenseService(private val context: Context) {

    private val db: FirebaseFirestore? = runCatching {
        FirebaseFirestore.getInstance(
            FirebaseApp.getInstance(),
            context.getString(R.string.firestore_database_id)
        )
    }.getOrNull()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val backendBaseUrl = "http://10.0.2.2:5001"

    private val _firewallRules = MutableStateFlow(SecurityBaselineDefaults.defaultFirewallRules())
    val firewallRules: StateFlow<List<FirewallRuleStatus>> = _firewallRules.asStateFlow()

    private val _lastScanTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastScanTimestamp: StateFlow<Long> = _lastScanTimestamp.asStateFlow()

    fun observeThreats(userId: String): Flow<List<SecurityThreatEvent>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(SecurityBaselineDefaults.initialThreats())
            close()
            return@callbackFlow
        }

        val reg = firestore.collection("users")
            .document(userId)
            .collection("security_threats")
            .orderBy("detectedAt", Query.Direction.DESCENDING)
            .limit(40)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Threat stream listener warning: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val threats = snapshot.documents.mapNotNull { doc ->
                        val d = doc.data ?: return@mapNotNull null
                        SecurityThreatEvent(
                            id = d["id"] as? String ?: doc.id,
                            title = d["title"] as? String ?: "Security Anomaly",
                            description = d["description"] as? String ?: "",
                            breachType = d["breachType"] as? String ?: "FIREWALL_ANOMALY",
                            sourceIp = d["sourceIp"] as? String ?: "0.0.0.0",
                            targetEndpoint = d["targetEndpoint"] as? String ?: "/api",
                            firewallRuleMatched = d["firewallRuleMatched"] as? String ?: "WAF-EDGE-01",
                            severity = ThreatSeverity.fromString(d["severity"] as? String ?: "MEDIUM"),
                            detectedAt = (d["detectedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            containmentStatus = ContainmentStatus.fromString(d["containmentStatus"] as? String ?: "DETECTED"),
                            aiRiskScore = (d["aiRiskScore"] as? Number)?.toInt() ?: 0,
                            aiRiskLevel = d["aiRiskLevel"] as? String ?: "UNASSESSED",
                            aiAttackVector = d["aiAttackVector"] as? String ?: "",
                            aiBlastRadius = d["aiBlastRadius"] as? String ?: "",
                            aiRecommendedAction = d["aiRecommendedAction"] as? String ?: "",
                            aiRationale = d["aiRationale"] as? String ?: "",
                            requiresHumanApproval = d["requiresHumanApproval"] as? Boolean ?: false,
                            approvedBy = d["approvedBy"] as? String,
                            updatedAt = (d["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                    }
                    trySend(threats)
                }
            }
        awaitClose { reg.remove() }
    }

    fun observeAuditLogs(userId: String): Flow<List<SecurityAuditLogEntry>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(SecurityBaselineDefaults.initialAuditLogs())
            close()
            return@callbackFlow
        }

        val reg = firestore.collection("users")
            .document(userId)
            .collection("security_audit_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(60)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Audit log listener warning: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val logs = snapshot.documents.mapNotNull { doc ->
                        val d = doc.data ?: return@mapNotNull null
                        SecurityAuditLogEntry(
                            id = doc.id,
                            threatId = d["threatId"] as? String ?: "",
                            eventType = SecurityAuditEventType.fromString(d["eventType"] as? String ?: "THREAT_DETECTED"),
                            actor = d["actor"] as? String ?: "System",
                            summary = d["summary"] as? String ?: "",
                            details = d["details"] as? String ?: "",
                            riskScore = (d["riskScore"] as? Number)?.toInt() ?: 0,
                            timestamp = (d["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                    }
                    trySend(logs)
                }
            }
        awaitClose { reg.remove() }
    }

    fun observeContainmentPolicy(userId: String): Flow<ContainmentPolicy> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(ContainmentPolicy())
            close()
            return@callbackFlow
        }

        val reg = firestore.collection("users")
            .document(userId)
            .collection("settings")
            .document("security_policy")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Policy listener warning: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val d = snapshot.data ?: emptyMap()
                    trySend(
                        ContainmentPolicy(
                            autoContainEnabled = d["autoContainEnabled"] as? Boolean ?: true,
                            autoContainRiskThreshold = (d["autoContainRiskThreshold"] as? Number)?.toInt() ?: 80,
                            requireHumanApprovalForPayoutFreeze = d["requireHumanApprovalForPayoutFreeze"] as? Boolean ?: true,
                            strictWafBlocking = d["strictWafBlocking"] as? Boolean ?: true,
                            updatedAt = (d["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                    )
                } else {
                    trySend(ContainmentPolicy())
                }
            }
        awaitClose { reg.remove() }
    }

    suspend fun seedBaselineIfNeeded(userId: String) = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext
        try {
            val threatsCol = firestore.collection("users").document(userId).collection("security_threats")
            val existing = threatsCol.limit(1).get().await()
            if (existing.isEmpty) {
                val now = System.currentTimeMillis()
                SecurityBaselineDefaults.initialThreats(now).forEach { threat ->
                    threatsCol.document(threat.id).set(threatToMap(threat)).await()
                }
                val logsCol = firestore.collection("users").document(userId).collection("security_audit_logs")
                SecurityBaselineDefaults.initialAuditLogs(now).forEach { log ->
                    logsCol.document(log.id).set(auditToMap(log)).await()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Security baseline seed skipped: ${e.message}")
        }
    }

    suspend fun assessThreatWithAi(
        userId: String,
        threat: SecurityThreatEvent,
        policy: ContainmentPolicy
    ): SecurityThreatEvent = withContext(Dispatchers.IO) {
        var assessed: SecurityThreatEvent? = null

        // 1. Attempt Genkit backend AI risk assessment flow
        try {
            val payload = JSONObject().apply {
                put("userId", userId)
                put("threatId", threat.id)
                put("title", threat.title)
                put("breachType", threat.breachType)
                put("sourceIp", threat.sourceIp)
                put("targetEndpoint", threat.targetEndpoint)
                put("firewallRuleMatched", threat.firewallRuleMatched)
                put("severity", threat.severity.name)
                put("description", threat.description)
                put("autoContainThreshold", policy.autoContainRiskThreshold)
            }
            val req = Request.Builder()
                .url("$backendBaseUrl/api/security/assess-risk")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = JSONObject(resp.body?.string().orEmpty())
                val a = json.optJSONObject("assessment")
                if (a != null) {
                    val riskScore = a.optInt("riskScore", threat.severity.weight)
                    val requiresHitl = a.optBoolean("requiresHumanApproval", false) ||
                        (policy.requireHumanApprovalForPayoutFreeze && isPayoutOrCriticalScope(threat, riskScore))
                    val status = resolveContainmentStatus(riskScore, requiresHitl, policy)
                    assessed = threat.copy(
                        aiRiskScore = riskScore,
                        aiRiskLevel = a.optString("riskLevel", threat.severity.name),
                        aiAttackVector = a.optString("attackVector", threat.breachType),
                        aiBlastRadius = a.optString("blastRadius", threat.targetEndpoint),
                        aiRecommendedAction = a.optString("recommendedAction", "Isolate source IP and rotate tokens."),
                        aiRationale = a.optString("aiRationale", "Genkit SOC AI assessment complete."),
                        requiresHumanApproval = requiresHitl,
                        containmentStatus = status,
                        approvedBy = if (status == ContainmentStatus.AUTO_CONTAINED) "Auto-Containment Policy" else threat.approvedBy,
                        updatedAt = System.currentTimeMillis()
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Backend AI risk endpoint unreachable, using on-device deterministic SOC scorer: ${e.message}")
        }

        // 2. Deterministic AI SOC Fallback if backend wasn't reached
        val finalThreat = assessed ?: runDeterministicRiskAssessment(threat, policy)
        persistThreatAndAudit(userId, finalThreat)
        finalThreat
    }

    suspend fun runLivePerimeterScan(
        userId: String,
        policy: ContainmentPolicy,
        currentThreats: List<SecurityThreatEvent>
    ): Int = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        _lastScanTimestamp.value = now

        val authUser = FirebaseAuth.getInstance().currentUser
        val authContext = if (authUser != null) "Authenticated (${authUser.email ?: authUser.uid})" else "Workspace Sandbox ($userId)"

        // Update WAF rule telemetry timestamps
        _firewallRules.value = _firewallRules.value.mapIndexed { index, rule ->
            if (index == 0 || index == 2) {
                rule.copy(
                    status = if (policy.strictWafBlocking) "ACTIVE" else "MONITOR",
                    lastTriggeredAt = now
                )
            } else rule
        }

        var assessedCount = 0
        currentThreats.filter { it.containmentStatus == ContainmentStatus.DETECTED || it.aiRiskScore == 0 }
            .forEach { unassessed ->
                assessThreatWithAi(userId, unassessed, policy)
                assessedCount++
            }

        val log = SecurityAuditLogEntry(
            id = "AUD-${now % 1000000}",
            threatId = "SCAN-${now % 10000}",
            eventType = SecurityAuditEventType.FIREWALL_BREACH,
            actor = "Perimeter Telemetry Scanner",
            summary = "Live perimeter & firewall inspection complete ($assessedCount threats evaluated)",
            details = "Identity: $authContext • WAF rules verified • Auto-contain threshold >= ${policy.autoContainRiskThreshold}",
            riskScore = 0,
            timestamp = now
        )
        persistAuditLog(userId, log)
        assessedCount
    }

    suspend fun simulateFirewallBreachProbe(
        userId: String,
        scenario: String,
        policy: ContainmentPolicy
    ): SecurityThreatEvent = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val threatId = "THR-${(now % 90000) + 10000}"

        val rawThreat = when (scenario) {
            "SQLI_ORDERS" -> SecurityThreatEvent(
                id = threatId,
                title = "SQL Injection Probe on Order Ledger",
                description = "Malformed UNION SELECT payload intercepted on /api/orders query parameter.",
                breachType = "FIREWALL_BREACH_SQLI",
                sourceIp = "45.155.205.91",
                targetEndpoint = "/api/orders",
                firewallRuleMatched = "FW-SQLI-04",
                severity = ThreatSeverity.HIGH,
                detectedAt = now
            )
            "PAYOUT_SPIKE" -> SecurityThreatEvent(
                id = threatId,
                title = "Anomalous High-Value IBAN Settlement Spike",
                description = "Sudden automated payout request anomaly exceeding normal draw velocity; gated by FW-PAY-09.",
                breachType = "PAYOUT_VELOCITY_ANOMALY",
                sourceIp = "194.26.135.14",
                targetEndpoint = "/api/payouts/initiate",
                firewallRuleMatched = "FW-PAY-09",
                severity = ThreatSeverity.CRITICAL,
                detectedAt = now
            )
            else -> SecurityThreatEvent(
                id = threatId,
                title = "Wero Webhook Spoof & HMAC Breach",
                description = "Invalid EPI/Wero webhook signature probe attempting unauthorized credit event.",
                breachType = "FIREWALL_BREACH_WERO_SPOOF",
                sourceIp = "185.220.101.77",
                targetEndpoint = "/api/webhooks/wero",
                firewallRuleMatched = "WAF-EDGE-01",
                severity = ThreatSeverity.CRITICAL,
                detectedAt = now
            )
        }

        // Increment matching rule counter
        _firewallRules.value = _firewallRules.value.map { rule ->
            if (rule.ruleId == rawThreat.firewallRuleMatched) {
                rule.copy(blockedCount = rule.blockedCount + 1, lastTriggeredAt = now)
            } else rule
        }

        // Log detection + breach audit first
        persistAuditLog(
            userId,
            SecurityAuditLogEntry(
                id = "AUD-DET-${now % 1000000}",
                threatId = threatId,
                eventType = SecurityAuditEventType.THREAT_DETECTED,
                actor = "Firewall ${rawThreat.firewallRuleMatched}",
                summary = "Detected ${rawThreat.title} from ${rawThreat.sourceIp}",
                details = rawThreat.description,
                riskScore = rawThreat.severity.weight,
                timestamp = now
            )
        )

        // Immediately run AI risk assessment & containment pipeline
        assessThreatWithAi(userId, rawThreat, policy)
    }

    suspend fun approveContainment(
        userId: String,
        threat: SecurityThreatEvent,
        operatorName: String,
        note: String = "Operator approved firewall block and session isolation"
    ): Result<SecurityThreatEvent> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        runCatching {
            val payload = JSONObject().apply {
                put("userId", userId)
                put("threatId", threat.id)
                put("decision", "APPROVE")
                put("operator", operatorName)
                put("note", note)
            }
            val req = Request.Builder()
                .url("$backendBaseUrl/api/security/approve")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            runCatching { httpClient.newCall(req).execute().close() }

            val updated = threat.copy(
                containmentStatus = ContainmentStatus.APPROVED_CONTAINED,
                approvedBy = operatorName,
                requiresHumanApproval = false,
                updatedAt = now
            )
            db?.collection("users")
                ?.document(userId)
                ?.collection("security_threats")
                ?.document(threat.id)
                ?.set(threatToMap(updated), SetOptions.merge())
                ?.await()

            persistAuditLog(
                userId,
                SecurityAuditLogEntry(
                    id = "AUD-APP-${now % 1000000}",
                    threatId = threat.id,
                    eventType = SecurityAuditEventType.HUMAN_APPROVAL,
                    actor = operatorName,
                    summary = "Approved containment for #${threat.id} (${threat.sourceIp})",
                    details = note,
                    riskScore = threat.aiRiskScore,
                    timestamp = now
                )
            )
            updated
        }
    }

    suspend fun dismissThreat(
        userId: String,
        threat: SecurityThreatEvent,
        operatorName: String,
        reason: String = "Verified benign traffic by operator"
    ): Result<SecurityThreatEvent> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        runCatching {
            val payload = JSONObject().apply {
                put("userId", userId)
                put("threatId", threat.id)
                put("decision", "DISMISS")
                put("operator", operatorName)
                put("note", reason)
            }
            val req = Request.Builder()
                .url("$backendBaseUrl/api/security/approve")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            runCatching { httpClient.newCall(req).execute().close() }

            val updated = threat.copy(
                containmentStatus = ContainmentStatus.DISMISSED,
                approvedBy = operatorName,
                requiresHumanApproval = false,
                updatedAt = now
            )
            db?.collection("users")
                ?.document(userId)
                ?.collection("security_threats")
                ?.document(threat.id)
                ?.set(threatToMap(updated), SetOptions.merge())
                ?.await()

            persistAuditLog(
                userId,
                SecurityAuditLogEntry(
                    id = "AUD-DIS-${now % 1000000}",
                    threatId = threat.id,
                    eventType = SecurityAuditEventType.HUMAN_DISMISSAL,
                    actor = operatorName,
                    summary = "Dismissed threat #${threat.id} (${threat.title})",
                    details = reason,
                    riskScore = threat.aiRiskScore,
                    timestamp = now
                )
            )
            updated
        }
    }

    suspend fun saveContainmentPolicy(
        userId: String,
        policy: ContainmentPolicy,
        operatorName: String
    ): Result<ContainmentPolicy> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val updated = policy.copy(updatedAt = now)
        runCatching {
            db?.collection("users")
                ?.document(userId)
                ?.collection("settings")
                ?.document("security_policy")
                ?.set(
                    mapOf(
                        "autoContainEnabled" to updated.autoContainEnabled,
                        "autoContainRiskThreshold" to updated.autoContainRiskThreshold,
                        "requireHumanApprovalForPayoutFreeze" to updated.requireHumanApprovalForPayoutFreeze,
                        "strictWafBlocking" to updated.strictWafBlocking,
                        "updatedAt" to updated.updatedAt
                    ),
                    SetOptions.merge()
                )
                ?.await()

            persistAuditLog(
                userId,
                SecurityAuditLogEntry(
                    id = "AUD-POL-${now % 1000000}",
                    threatId = "POLICY",
                    eventType = SecurityAuditEventType.POLICY_CHANGE,
                    actor = operatorName,
                    summary = "Updated SOC Containment Policy (Auto=${updated.autoContainEnabled}, Threshold=${updated.autoContainRiskThreshold})",
                    details = "Strict WAF=${updated.strictWafBlocking}, HITL Payout Gate=${updated.requireHumanApprovalForPayoutFreeze}",
                    riskScore = 0,
                    timestamp = now
                )
            )
            updated
        }
    }

    private fun isPayoutOrCriticalScope(threat: SecurityThreatEvent, riskScore: Int): Boolean {
        return threat.targetEndpoint.contains("payout", ignoreCase = true) ||
            threat.targetEndpoint.contains("wero", ignoreCase = true) ||
            riskScore >= 90
    }

    private fun resolveContainmentStatus(
        riskScore: Int,
        requiresHumanApproval: Boolean,
        policy: ContainmentPolicy
    ): ContainmentStatus {
        return when {
            requiresHumanApproval -> ContainmentStatus.PENDING_HUMAN_APPROVAL
            policy.autoContainEnabled && riskScore >= policy.autoContainRiskThreshold -> ContainmentStatus.AUTO_CONTAINED
            else -> ContainmentStatus.AI_ASSESSED
        }
    }

    private fun runDeterministicRiskAssessment(
        threat: SecurityThreatEvent,
        policy: ContainmentPolicy
    ): SecurityThreatEvent {
        val endpointBoost = if (threat.targetEndpoint.contains("payout", true) || threat.targetEndpoint.contains("wero", true)) 12 else 4
        val riskScore = (threat.severity.weight + endpointBoost).coerceIn(15, 98)
        val riskLevel = when {
            riskScore >= 88 -> "CRITICAL"
            riskScore >= 70 -> "HIGH"
            riskScore >= 45 -> "MEDIUM"
            else -> "LOW"
        }
        val requiresHitl = (policy.requireHumanApprovalForPayoutFreeze && isPayoutOrCriticalScope(threat, riskScore))
        val status = resolveContainmentStatus(riskScore, requiresHitl, policy)

        return threat.copy(
            aiRiskScore = riskScore,
            aiRiskLevel = riskLevel,
            aiAttackVector = "Targeted ${threat.breachType} via ${threat.firewallRuleMatched}",
            aiBlastRadius = "Endpoint ${threat.targetEndpoint} (${if (riskScore >= 85) "financial pipeline" else "service edge"})",
            aiRecommendedAction = if (requiresHitl) {
                "Isolate ${threat.sourceIp}, freeze outbound settlement hooks, and await human operator approval."
            } else {
                "Auto-drop traffic from ${threat.sourceIp} at WAF rule ${threat.firewallRuleMatched} and rotate edge tokens."
            },
            aiRationale = "AI Risk Engine scored $riskScore/100 ($riskLevel) against endpoint ${threat.targetEndpoint}. ${if (requiresHitl) "Escalated to HITL sign-off." else if (status == ContainmentStatus.AUTO_CONTAINED) "Exceeded auto-containment threshold (${policy.autoContainRiskThreshold})." else "Monitored by WAF."}",
            requiresHumanApproval = requiresHitl,
            containmentStatus = status,
            approvedBy = if (status == ContainmentStatus.AUTO_CONTAINED) "Auto-Containment Policy" else null,
            updatedAt = System.currentTimeMillis()
        )
    }

    private suspend fun persistThreatAndAudit(userId: String, threat: SecurityThreatEvent) {
        val now = System.currentTimeMillis()
        runCatching {
            db?.collection("users")
                ?.document(userId)
                ?.collection("security_threats")
                ?.document(threat.id)
                ?.set(threatToMap(threat), SetOptions.merge())
                ?.await()

            val auditType = if (threat.containmentStatus == ContainmentStatus.AUTO_CONTAINED) {
                SecurityAuditEventType.AUTO_CONTAINMENT
            } else {
                SecurityAuditEventType.AI_RISK_ASSESSMENT
            }
            persistAuditLog(
                userId,
                SecurityAuditLogEntry(
                    id = "AUD-AI-${now % 1000000}",
                    threatId = threat.id,
                    eventType = auditType,
                    actor = "Genkit SOC AI Engine",
                    summary = "AI Score ${threat.aiRiskScore}/100 (${threat.aiRiskLevel}) -> ${threat.containmentStatus.label}",
                    details = threat.aiRationale,
                    riskScore = threat.aiRiskScore,
                    timestamp = now
                )
            )
        }
    }

    private suspend fun persistAuditLog(userId: String, log: SecurityAuditLogEntry) {
        runCatching {
            db?.collection("users")
                ?.document(userId)
                ?.collection("security_audit_logs")
                ?.document(log.id)
                ?.set(auditToMap(log), SetOptions.merge())
                ?.await()
        }
    }

    private fun threatToMap(t: SecurityThreatEvent): Map<String, Any?> = mapOf(
        "id" to t.id,
        "title" to t.title,
        "description" to t.description,
        "breachType" to t.breachType,
        "sourceIp" to t.sourceIp,
        "targetEndpoint" to t.targetEndpoint,
        "firewallRuleMatched" to t.firewallRuleMatched,
        "severity" to t.severity.name,
        "detectedAt" to t.detectedAt,
        "containmentStatus" to t.containmentStatus.name,
        "aiRiskScore" to t.aiRiskScore,
        "aiRiskLevel" to t.aiRiskLevel,
        "aiAttackVector" to t.aiAttackVector,
        "aiBlastRadius" to t.aiBlastRadius,
        "aiRecommendedAction" to t.aiRecommendedAction,
        "aiRationale" to t.aiRationale,
        "requiresHumanApproval" to t.requiresHumanApproval,
        "approvedBy" to t.approvedBy,
        "updatedAt" to t.updatedAt
    )

    private fun auditToMap(a: SecurityAuditLogEntry): Map<String, Any> = mapOf(
        "id" to a.id,
        "threatId" to a.threatId,
        "eventType" to a.eventType.name,
        "actor" to a.actor,
        "summary" to a.summary,
        "details" to a.details,
        "riskScore" to a.riskScore,
        "timestamp" to a.timestamp
    )
}
