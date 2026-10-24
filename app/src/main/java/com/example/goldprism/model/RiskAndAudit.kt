package com.example.goldprism.model

/**
 * Risk Limits & Configuration.
 */
data class RiskSettings(
    val maxOrderQtyLots: Int = 20,
    val maxExposureGrams: Double = 1000.0,
    val dailyLossLimitInr: Double = 50_000.0,
    val maxAllowedSlippageBps: Double = 15.0,
    val blockOnStaleData: Boolean = true,
    val blockOnDataWarning: Boolean = false,
    val requireManualApprovalForLive: Boolean = true,
    val killSwitchEngaged: Boolean = false
)

/**
 * Pre-trade Risk Check outcome.
 */
data class PreTradeRiskCheckResult(
    val passed: Boolean,
    val checksRun: Int,
    val checksPassed: Int,
    val violations: List<String>,
    val estimatedMarginInr: Double,
    val availableCashInr: Double,
    val marginSufficient: Boolean,
    val exposureSafe: Boolean,
    val dataHealthSafe: Boolean,
    val killSwitchSafe: Boolean,
    val isTenderPeriod: Boolean = false,
    val effectiveMarginPercentage: Double = 0.0,
    val tenderPeriodNotice: String? = null
)

/**
 * Severity of Alerts.
 */
enum class AlertSeverity {
    CRITICAL, WARNING, INFO
}

/**
 * Category of Alert.
 */
enum class AlertCategory {
    SIGNAL, DATA_HEALTH, RISK, EXECUTION, SYSTEM
}

/**
 * In-app Notification Alert.
 */
data class AlertNotification(
    val id: String,
    val timestamp: Long,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val category: AlertCategory,
    val isRead: Boolean = false
)

/**
 * Data Health Integrity Check Record.
 */
data class DataIntegrityCheck(
    val checkName: String,
    val description: String,
    val status: DataTrustStatus,
    val details: String,
    val checkedTimestamp: Long
)

/**
 * Immutable Audit Log Entry.
 */
data class AuditLogEntry(
    val id: String,
    val timestamp: Long,
    val eventType: String, // e.g. "ORDER_SUBMITTED", "RISK_CHECK_PASSED", "KILL_SWITCH_ENGAGED"
    val actor: String,     // e.g. "USER", "ENGINE_AUTO", "RISK_DAEMON"
    val description: String,
    val payloadJson: String,
    val hashSignature: String // SHA-256 integrity simulation
)
