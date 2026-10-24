package com.example.goldprism.engine

import com.example.goldprism.model.ContractSnapshot
import com.example.goldprism.model.GoldContractType
import com.example.goldprism.model.VehicleRanking
import kotlin.math.abs
import kotlin.math.roundToInt

object VehicleRankingEngine {

    /**
     * Ranks all 4 MCX gold contracts for a requested target exposure (in grams).
     * e.g., 50g, 100g, 500g, 1000g.
     */
    fun rankVehicles(
        targetExposureGrams: Double,
        contracts: Map<GoldContractType, ContractSnapshot>
    ): List<VehicleRanking> {
        val rankings = mutableListOf<VehicleRanking>()

        for ((type, snap) in contracts) {
            // Calculate integer lots required to match exposure
            val lots = maxOf(1, (targetExposureGrams / type.lotSizeGrams).roundToInt())
            val actualExposure = lots * type.lotSizeGrams
            val trackingError = abs(actualExposure - targetExposureGrams)

            // Calculate turnover value
            val units = actualExposure / type.quoteUnitGrams
            val notionalValue = units * snap.quotedLtp
            val marginReq = notionalValue * (type.marginPercentage / 100.0)

            // Trading friction & roundtrip costs
            val roundtripCost = TransactionCostEngine.calculateRoundtripCostPerGram(
                type, lots, snap.quotedLtp, snap.spreadBps / 2.0
            ) * actualExposure
            val costBps = if (notionalValue > 0) (roundtripCost / notionalValue) * 10000.0 else 0.0

            // Liquidity Score (0-100) based on volume and OI
            val liquidityScore = when {
                snap.volumeLots > 20000 -> 95
                snap.volumeLots > 10000 -> 88
                snap.volumeLots > 5000 -> 76
                snap.volumeLots > 2000 -> 62
                else -> 45
            }

            val slippageBps = (snap.spreadBps * 0.4).coerceAtLeast(1.5)

            // Tracking error penalty (heavy penalty if you wanted 50g but minimum lot is 100g)
            val trackingErrorPenalty = if (targetExposureGrams > 0) (trackingError / targetExposureGrams) * 45.0 else 0.0

            // Composite score (Higher is better)
            // Weight: 35% liquidity, 30% cost efficiency, 35% lot granularity & tracking fit
            val costEfficiencyScore = (100.0 - (costBps * 2.5)).coerceIn(10.0, 99.0)
            val granularityFitScore = (100.0 - trackingErrorPenalty).coerceIn(5.0, 100.0)

            // Wholesale efficiency bonus: 100g wholesale bar is the exchange standard for wholesale exposures
            val wholesaleBonus = if (type == GoldContractType.GOLDM && targetExposureGrams >= 100.0 && (targetExposureGrams % 100.0 == 0.0)) 15.0 else 0.0

            val compositeScore = (liquidityScore * 0.35) + (costEfficiencyScore * 0.30) + (granularityFitScore * 0.35) + wholesaleBonus

            val justification = when (type) {
                GoldContractType.GOLDM -> {
                    if (targetExposureGrams >= 100.0 && (targetExposureGrams % 100.0 == 0.0)) {
                        "TOP PICK FOR WHOLESALE: Perfect 100g lot fit, lowest exchange cost basis (${String.format("%.1f", costBps)} bps), and deep institutional order book."
                    } else if (targetExposureGrams < 100.0) {
                        "OVERSIZED LOT: Minimum contract size is 100g (₹${String.format("%.0f", notionalValue)} notional). Exceeds requested ${targetExposureGrams.toInt()}g target."
                    } else {
                        "HIGH EFFICIENCY: Low friction wholesale instrument, with minor cash rounding error (${trackingError.toInt()}g)."
                    }
                }
                GoldContractType.GOLDTEN -> {
                    if (targetExposureGrams in 10.0..90.0) {
                        "OPTIMAL MID-TIER FIT: Granular 10g increments match ${targetExposureGrams.toInt()}g target with zero exposure mismatch and reasonable liquidity."
                    } else {
                        "BALANCED ALTERNATIVE: 10g bar size offers good granularity and moderate slippage (${String.format("%.1f", slippageBps)} bps)."
                    }
                }
                GoldContractType.GOLDGUINEA -> {
                    "SOVEREIGN COIN PROXY: 8g lot size (22k/996 purity). Excellent for jeweler hedging, though slightly wider bid-ask spread (${String.format("%.1f", snap.spreadBps)} bps)."
                }
                GoldContractType.GOLDPETAL -> {
                    if (targetExposureGrams < 10.0) {
                        "PERFECT MICRO-HEDGE: Exactly 1g precision for small exposures with active retail liquidity (${snap.volumeLots} lots)."
                    } else {
                        "HIGH TURNOVER FRICTION: 1g lots offer zero tracking error, but higher cumulative brokerage and retail assay blister premium (${String.format("%.1f", costBps)} bps)."
                    }
                }
            }

            rankings.add(
                VehicleRanking(
                    contractType = type,
                    targetExposureGrams = targetExposureGrams,
                    requiredLots = lots,
                    actualExposureGrams = actualExposure,
                    trackingErrorGrams = trackingError,
                    notionalValueInr = notionalValue,
                    marginRequirementInr = marginReq,
                    estimatedRoundtripCostsInr = roundtripCost,
                    costBasisBps = costBps,
                    liquidityScore = liquidityScore,
                    slippageEstimateBps = slippageBps,
                    overallScore = compositeScore,
                    rank = 0,
                    justification = justification
                )
            )
        }

        // Sort descending by overallScore and assign ranks 1..4
        return rankings.sortedByDescending { it.overallScore }.mapIndexed { index, item ->
            item.copy(rank = index + 1)
        }
    }
}
