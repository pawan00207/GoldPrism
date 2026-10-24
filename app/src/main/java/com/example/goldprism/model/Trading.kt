package com.example.goldprism.model

/**
 * Vehicle comparison and ranking for a target exposure.
 */
data class VehicleRanking(
    val contractType: GoldContractType,
    val targetExposureGrams: Double,
    val requiredLots: Int,
    val actualExposureGrams: Double,
    val trackingErrorGrams: Double,
    val notionalValueInr: Double,
    val marginRequirementInr: Double,
    val estimatedRoundtripCostsInr: Double,
    val costBasisBps: Double,
    val liquidityScore: Int,      // 0-100 based on volume & OI
    val slippageEstimateBps: Double,
    val overallScore: Double,      // Composite weighted score
    val rank: Int,
    val justification: String
)

/**
 * Order side.
 */
enum class OrderSide {
    BUY, SELL
}

/**
 * Order type.
 */
enum class OrderType {
    MARKET, LIMIT
}

/**
 * Order execution status.
 */
enum class OrderStatus {
    PENDING, FILLED, PARTIALLY_FILLED, REJECTED, CANCELLED
}

/**
 * Execution Mode: PAPER vs LIVE MOCK vs LIVE BROKER.
 */
enum class ExecutionMode(val label: String) {
    PAPER("PAPER TRADING"),
    LIVE_MOCK("LIVE (MOCK BROKER)"),
    LIVE_AUTHORIZED("LIVE (BROKER CONNECTED)")
}

/**
 * Order record.
 */
data class OrderItem(
    val orderId: String,
    val timestamp: Long,
    val symbol: String,
    val side: OrderSide,
    val orderType: OrderType,
    val quantityLots: Int,
    val exposureGrams: Double,
    val limitPrice: Double,
    val executedPrice: Double,
    val status: OrderStatus,
    val mode: ExecutionMode,
    val brokerageInr: Double,
    val taxesAndChargesInr: Double,
    val rejectionReason: String? = null,
    val brokerOrderId: String? = null
)

/**
 * Result of an atomic 2-leg spread execution.
 */
data class AtomicSpreadOrderResult(
    val success: Boolean,
    val spreadId: String,
    val leg1Order: OrderItem,
    val leg2Order: OrderItem,
    val rolledBack: Boolean = false,
    val message: String
)

/**
 * Open Position record.
 */
data class PositionItem(
    val positionId: String,
    val symbol: String,
    val contractType: GoldContractType,
    val side: OrderSide,
    val quantityLots: Int,
    val exposureGrams: Double,
    val averageEntryPrice: Double,
    val currentLtp: Double,
    val unrealizedPnlInr: Double,
    val unrealizedPnlPct: Double,
    val realizedPnlInr: Double,
    val marginBlockedInr: Double,
    val mode: ExecutionMode,
    val openTimestamp: Long
)

/**
 * Paper Trading / Demo Account State.
 */
data class AccountState(
    val cashBalance: Double = 1_000_000.0, // ₹10,00,000 initial capital
    val marginBlocked: Double = 0.0,
    val realizedPnl: Double = 0.0,
    val unrealizedPnl: Double = 0.0,
    val totalRoundtripFeesPaid: Double = 0.0,
    val activeMode: ExecutionMode = ExecutionMode.PAPER
) {
    val totalEquity: Double
        get() = cashBalance + unrealizedPnl
}

/**
 * A backtest trade point.
 */
data class BacktestTrade(
    val tradeId: String,
    val entryDate: String,
    val exitDate: String,
    val pair: String,
    val action: String,
    val spreadEntry: Double,
    val spreadExit: Double,
    val holdingPeriodDays: Int,
    val grossReturnInr: Double,
    val netReturnInr: Double,
    val feesInr: Double,
    val returnPct: Double,
    val isWin: Boolean
)

/**
 * Equity point for backtest curve.
 */
data class BacktestEquityPoint(
    val dateLabel: String,
    val grossEquity: Double,
    val netEquity: Double,
    val drawdownPct: Double
)

/**
 * Comprehensive Backtest Result.
 */
data class BacktestResult(
    val strategyName: String,
    val pair: String,
    val dateRange: String,
    val initialCapital: Double,
    val finalGrossEquity: Double,
    val finalNetEquity: Double,
    val totalGrossReturnPct: Double,
    val totalNetReturnPct: Double,
    val annualizedSharpe: Double,
    val maxDrawdownPct: Double,
    val winRatePct: Double,
    val profitFactor: Double,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val averageTradePnlInr: Double,
    val totalFeesPaidInr: Double,
    val bootstrapConfidence95Low: Double,
    val bootstrapConfidence95High: Double,
    val minimumDetectableEffectPct: Double,
    val frozenConfigHash: String,
    val equityCurve: List<BacktestEquityPoint>,
    val tradeLogs: List<BacktestTrade>,
    val isHoldoutBurned: Boolean = false,
    val holdoutSummary: String? = null,
    val hasEdge: Boolean = true,
    val verdictLabel: String = "TRADABLE",
    val isInsufficientRealData: Boolean = false,
    val lookAheadViolationsCount: Int = 0,
    val provenance: String = "SIMULATED"
)

/**
 * Saved Strategy Configuration in the Strategy Lab.
 */
data class StrategyConfig(
    val id: String,
    val name: String,
    val baseContract: GoldContractType,
    val targetContract: GoldContractType,
    val entryZScore: Double = 2.0,
    val exitZScore: Double = 0.5,
    val stopLossPerGram: Double = 45.0,
    val maxHoldingDays: Int = 10,
    val slippageAssumptionBps: Double = 3.0,
    val targetExposureGrams: Double = 100.0,
    val fillPriceBasis: String = "OPEN", // "OPEN" or "SETTLE"
    val version: String = "v1.2",
    val configHash: String = "sha256:7f8a9e2d1c"
) {
    fun computeHash(): String {
        val payload = "$id|$baseContract|$targetContract|$entryZScore|$exitZScore|$stopLossPerGram|$maxHoldingDays|$slippageAssumptionBps|$targetExposureGrams|$fillPriceBasis"
        return "sha256:" + payload.hashCode().toUInt().toString(16)
    }
}
