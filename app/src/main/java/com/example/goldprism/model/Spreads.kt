package com.example.goldprism.model

/**
 * Signal Verdict enum representing the quantitative decision.
 */
enum class SignalVerdict(val label: String, val badgeColorHex: Long) {
    TRADABLE("TRADABLE", 0xFF10B981), // Emerald Green
    WATCH("WATCH", 0xFFF59E0B),       // Amber Yellow
    NO_EDGE("NO EDGE", 0xFF64748B),   // Slate Neutral
    BLOCKED("BLOCKED", 0xFFEF4444)    // Red
}

/**
 * An X-Ray Decomposition step component in the waterfall.
 */
data class WaterfallComponent(
    val name: String,
    val valuePerGram: Double,
    val runningTotalPerGram: Double,
    val explanation: String,
    val confidencePct: Int,
    val isDeduction: Boolean = false
)

/**
 * Full X-Ray waterfall decomposition breakdown for a spread pair.
 */
data class XRayDecomposition(
    val pairKey: String,
    val baseSymbol: String,
    val targetSymbol: String,
    val rawSpreadPerGram: Double,
    val economicBasis: Double,         // Physical logistics / purity discrepancy / delivery center
    val carryEffect: Double,           // Cost of carry & interest rate adjustment across expiry
    val settlementArtifact: Double,    // Discrepancy from closing mark vs spot auction
    val residualPerGram: Double,        // Gross model edge before transaction costs
    val transactionCostsPerGram: Double,// Round-trip trading friction (taxes, brokerage, slippage)
    val netEstimatedEdgePerGram: Double,// True net economic edge
    val confidenceIntervalLow: Double,
    val confidenceIntervalHigh: Double,
    val reconciliationError: Double,   // Must be ~0.0 (Sum of components == Raw Spread)
    val components: List<WaterfallComponent>,
    val methodologyNote: String
)

/**
 * Signal Intelligence analysis for a pair.
 */
data class SpreadSignal(
    val pairKey: String,
    val baseType: GoldContractType,
    val targetType: GoldContractType,
    val rawSpreadPerGram: Double,
    val netEdgePerGram: Double,
    val zScore: Double,
    val historicalMean: Double,
    val historicalStdDev: Double,
    val percentile: Double,
    val verdict: SignalVerdict,
    val verdictRationale: String,
    val passedCriteria: List<String>,
    val failedCriteria: List<String>,
    val estimatedGrossEdgePct: Double,
    val estimatedNetEdgePct: Double,
    val estimatedRoundtripCostsPerGram: Double,
    val recommendedAction: String, // e.g. "BUY GOLDPETAL / SELL GOLDM" or "STAY CASH"
    val dataFreshnessSeconds: Int,
    val confidenceScorePct: Int
)

/**
 * Historical spread point for charting.
 */
data class SpreadHistoryPoint(
    val timestamp: Long,
    val dateLabel: String,
    val spreadPerGram: Double,
    val upperBand2Sigma: Double,
    val lowerBand2Sigma: Double,
    val meanSpread: Double,
    val zScore: Double
)
