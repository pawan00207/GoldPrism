package com.example.goldprism.engine

import com.example.goldprism.model.ContractSnapshot
import com.example.goldprism.model.DataTrustStatus
import com.example.goldprism.model.SignalVerdict
import com.example.goldprism.model.SpreadSignal
import kotlin.math.abs
import kotlin.math.max

object SignalEngine {

    /**
     * Evaluates a candidate pair and produces an explainable quantitative signal.
     */
    fun evaluate(
        baseSnap: ContractSnapshot,
        targetSnap: ContractSnapshot,
        killSwitchEngaged: Boolean = false
    ): SpreadSignal {
        val pairKey = "${targetSnap.contractType.symbol} - ${baseSnap.contractType.symbol}"
        val decomp = XRayEngine.decompose(baseSnap, targetSnap)

        val rawSpread = decomp.rawSpreadPerGram
        val residual = decomp.residualPerGram
        val costs = decomp.transactionCostsPerGram
        val netEdge = decomp.netEstimatedEdgePerGram

        // Statistical parameters (historical distribution baseline)
        val histMean = 28.50 // Baseline historical spread in ₹/g
        val histStdDev = 8.20
        val zScore = (rawSpread - histMean) / histStdDev
        val percentile = (50.0 + (zScore * 19.1)).coerceIn(1.0, 99.0)

        val passedCriteria = mutableListOf<String>()
        val failedCriteria = mutableListOf<String>()

        // 1. Data Integrity Gate
        val isDataHealthClean = baseSnap.trustStatus == DataTrustStatus.TRUSTED &&
                targetSnap.trustStatus == DataTrustStatus.TRUSTED

        if (isDataHealthClean) {
            passedCriteria.add("Data Health Check: Both contracts feed status is TRUSTED.")
        } else {
            failedCriteria.add("Data Health Gate: Base (${baseSnap.trustStatus}) or Target (${targetSnap.trustStatus}) is degraded.")
        }

        // 2. Kill-switch check
        if (!killSwitchEngaged) {
            passedCriteria.add("System Safety Gate: Kill Switch is disengaged.")
        } else {
            failedCriteria.add("System Safety Gate: Emergency Kill Switch is ACTIVE.")
        }

        // 3. Statistical Dislocation Gate (Z-Score threshold >= 1.75)
        val hasStatisticalEdge = abs(zScore) >= 1.75
        if (hasStatisticalEdge) {
            passedCriteria.add("Statistical Significance: |Z-Score| (${String.format("%.2f", abs(zScore))}) exceeds 1.75 threshold.")
        } else {
            failedCriteria.add("Statistical Edge: |Z-Score| (${String.format("%.2f", abs(zScore))}) is within normal noise (|Z| < 1.75).")
        }

        // 4. Net Economic Edge Gate (Net Edge must exceed costs by at least 1.5x)
        val hasNetEconomicEdge = abs(netEdge) > (costs * 1.5) && abs(netEdge) > 12.0
        if (hasNetEconomicEdge) {
            passedCriteria.add("Cost-Benefit Hurdle: Net edge (₹${String.format("%.2f", abs(netEdge))}/g) exceeds cost buffer.")
        } else {
            failedCriteria.add("Cost-Benefit Hurdle: Net edge (₹${String.format("%.2f", abs(netEdge))}/g) is eroded by transaction frictions (₹${String.format("%.2f", costs)}/g).")
        }

        // 5. Liquidity & Days to Expiry Gate
        val hasSufficientLiquidity = baseSnap.volumeLots >= 1000 && targetSnap.volumeLots >= 500 &&
                baseSnap.daysToExpiry >= 3 && targetSnap.daysToExpiry >= 3
        if (hasSufficientLiquidity) {
            passedCriteria.add("Liquidity & Expiry: Sufficient volume and >3 days remaining to rollover.")
        } else {
            failedCriteria.add("Liquidity/Rollover Risk: Contract near expiry or volume thin.")
        }

        // 0. Settle-Price Trust Gate (Phase 2b: Thin or stale settles are EXCLUDED FROM SIGNALS, not just warned)
        val isSettleTrusted = baseSnap.isTrustedForSignals && targetSnap.isTrustedForSignals &&
                !baseSnap.isThinSettle && !targetSnap.isThinSettle &&
                !baseSnap.isStaleSettle && !targetSnap.isStaleSettle

        if (isSettleTrusted) {
            passedCriteria.add("Settle Price Trust: Official settlement marks verified for signal generation.")
        } else {
            failedCriteria.add("Settle Price Trust: Thin volume/OI or stale settle mark detected. Excluded from signals.")
        }

        // Determine Verdict
        val verdict: SignalVerdict
        val verdictRationale: String
        val recommendedAction: String

        when {
            killSwitchEngaged -> {
                verdict = SignalVerdict.BLOCKED
                verdictRationale = "CRITICAL: System-wide Emergency Kill Switch is engaged. All trading is strictly halted."
                recommendedAction = "HALTED: Stay Cash. Clear open orders."
            }
            !isDataHealthClean -> {
                verdict = SignalVerdict.BLOCKED
                val culprit = if (baseSnap.trustStatus != DataTrustStatus.TRUSTED) baseSnap.contractType.symbol else targetSnap.contractType.symbol
                verdictRationale = "DATA INTEGRITY BLOCK: Feed for $culprit is flagged as [${if (baseSnap.trustStatus != DataTrustStatus.TRUSTED) baseSnap.trustStatus else targetSnap.trustStatus}]. Trading blocked to prevent corrupted fills."
                recommendedAction = "BLOCKED: Awaiting feed restoration."
            }
            !isSettleTrusted -> {
                verdict = SignalVerdict.NO_EDGE
                val reason = when {
                    baseSnap.isThinSettle || targetSnap.isThinSettle -> "EXCLUDED FROM SIGNALS: Thin settle mark detected (volume < 5 or OI < 10). Excluded from execution signals."
                    baseSnap.isStaleSettle || targetSnap.isStaleSettle -> "EXCLUDED FROM SIGNALS: Stale settlement mark across sessions. Excluded from quantitative signals."
                    else -> "EXCLUDED FROM SIGNALS: Settle price trust flag unverified."
                }
                verdictRationale = reason
                recommendedAction = "EXCLUDED: Untrusted settle. Signals suppressed for safety."
            }
            hasStatisticalEdge && hasNetEconomicEdge && hasSufficientLiquidity -> {
                verdict = SignalVerdict.TRADABLE
                val direction = if (zScore > 0) "SELL ${targetSnap.contractType.symbol} / BUY ${baseSnap.contractType.symbol}" else "BUY ${targetSnap.contractType.symbol} / SELL ${baseSnap.contractType.symbol}"
                verdictRationale = "HIGH-CONVICTION OPPORTUNITY: Statistical divergence (|Z|=${String.format("%.2f", abs(zScore))}) provides positive net expectancy after deducting ₹${String.format("%.2f", costs)}/g roundtrip frictions."
                recommendedAction = "EXECUTE SPREAD: $direction"
            }
            hasStatisticalEdge && !hasNetEconomicEdge -> {
                verdict = SignalVerdict.NO_EDGE
                verdictRationale = "NO EDGE (DISCIPLINED PASS): Gross spread looks wide, but X-Ray decomposition proves that physical basis, carry, and statutory MCX costs consume the gross margin. Trading would have negative expectancy."
                recommendedAction = "PASS: Do not trade. Capital preservation discipline."
            }
            abs(zScore) in 1.2..1.74 -> {
                verdict = SignalVerdict.WATCH
                verdictRationale = "WATCHLIST CANDIDATE: Spread is expanding (|Z|=${String.format("%.2f", abs(zScore))}) but has not crossed entry trigger threshold. Monitor for widening."
                recommendedAction = "MONITOR: Set alert at Z=1.80."
            }
            else -> {
                verdict = SignalVerdict.NO_EDGE
                verdictRationale = "NO EDGE: Market is fairly priced. Spread is within normal central band (${String.format("%.1f", percentile)}th percentile). No statistical edge exists."
                recommendedAction = "STAND ASIDE: Stay cash."
            }
        }

        val grossEdgePct = if (baseSnap.quotedLtp > 0) (abs(residual) / baseSnap.normalizedLtpPerGram) * 100.0 else 0.0
        val netEdgePct = if (baseSnap.quotedLtp > 0) (abs(netEdge) / baseSnap.normalizedLtpPerGram) * 100.0 else 0.0
        val confidenceScore = when (verdict) {
            SignalVerdict.TRADABLE -> 88
            SignalVerdict.WATCH -> 65
            SignalVerdict.NO_EDGE -> 94
            SignalVerdict.BLOCKED -> 99
        }

        return SpreadSignal(
            pairKey = pairKey,
            baseType = baseSnap.contractType,
            targetType = targetSnap.contractType,
            rawSpreadPerGram = rawSpread,
            netEdgePerGram = netEdge,
            zScore = zScore,
            historicalMean = histMean,
            historicalStdDev = histStdDev,
            percentile = percentile,
            verdict = verdict,
            verdictRationale = verdictRationale,
            passedCriteria = passedCriteria,
            failedCriteria = failedCriteria,
            estimatedGrossEdgePct = grossEdgePct,
            estimatedNetEdgePct = netEdgePct,
            estimatedRoundtripCostsPerGram = costs,
            recommendedAction = recommendedAction,
            dataFreshnessSeconds = max(1, ((System.currentTimeMillis() - baseSnap.lastUpdatedTimestamp) / 1000).toInt()),
            confidenceScorePct = confidenceScore
        )
    }
}
