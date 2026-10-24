package com.example.goldprism.engine

import com.example.goldprism.model.GoldContractType
import com.example.goldprism.model.OrderSide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min

/**
 * Individual transaction cost rate specification conforming to Phase 1b.
 * Fields: rate, basis, source, last-verified date, and verification status.
 */
data class CostRateConfigItem(
    val id: String,
    val name: String,
    val rate: Double,
    val basis: String, // "buy turnover" | "sell turnover" | "brokerage+exchange" | "flat"
    val isConfirmed: Boolean,
    val statusLabel: String, // "CONFIRMED" or "UNVERIFIED — check current MCX/SEBI circular"
    val source: String,
    val lastVerifiedDate: String
)

/**
 * Centrally configurable Transaction Cost configuration.
 */
data class TransactionCostConfig(
    val brokerageFlatInr: Double = 20.0,
    val brokeragePctCap: Double = 0.0003, // 0.03%
    val cttRate: CostRateConfigItem = CostRateConfigItem(
        id = "CTT",
        name = "Commodity Transaction Tax (CTT)",
        rate = 0.0001, // 0.01%
        basis = "sell turnover",
        isConfirmed = true,
        statusLabel = "CONFIRMED",
        source = "Finance Act / MCX Statutory Schedule",
        lastVerifiedDate = "01-OCT-2026"
    ),
    val gstRate: CostRateConfigItem = CostRateConfigItem(
        id = "GST",
        name = "Goods & Services Tax (GST 18%)",
        rate = 0.18, // 18%
        basis = "brokerage+exchange",
        isConfirmed = true,
        statusLabel = "CONFIRMED",
        source = "GST Council Financial Services Schedule",
        lastVerifiedDate = "01-OCT-2026"
    ),
    val brokerageRate: CostRateConfigItem = CostRateConfigItem(
        id = "BROKERAGE",
        name = "Brokerage (₹20 flat or 0.03% cap)",
        rate = 0.0003,
        basis = "flat",
        isConfirmed = true,
        statusLabel = "CONFIRMED",
        source = "Discount Broker Standard Tariff",
        lastVerifiedDate = "01-OCT-2026"
    ),
    val exchangeTurnoverRate: CostRateConfigItem = CostRateConfigItem(
        id = "EXCH_TURNOVER",
        name = "Exchange Turnover Charge",
        rate = 0.000021, // 0.0021%
        basis = "total turnover",
        isConfirmed = false,
        statusLabel = "UNVERIFIED — check current MCX/SEBI circular",
        source = "MCX Circular ref MCX/TRD/2024",
        lastVerifiedDate = "UNVERIFIED"
    ),
    val clearingRate: CostRateConfigItem = CostRateConfigItem(
        id = "CLEARING",
        name = "Clearing Member Charge",
        rate = 0.000001, // 0.0001%
        basis = "total turnover",
        isConfirmed = false,
        statusLabel = "UNVERIFIED — check current MCX/SEBI circular",
        source = "MCX CCL Tariff (Unverified)",
        lastVerifiedDate = "UNVERIFIED"
    ),
    val sebiRate: CostRateConfigItem = CostRateConfigItem(
        id = "SEBI",
        name = "SEBI Regulatory Fee",
        rate = 0.000001, // ₹10 per crore (0.0001%)
        basis = "total turnover",
        isConfirmed = false,
        statusLabel = "UNVERIFIED — check current MCX/SEBI circular",
        source = "SEBI (Regulatory Fee Schedule) Circular",
        lastVerifiedDate = "UNVERIFIED"
    ),
    val stampDutyRate: CostRateConfigItem = CostRateConfigItem(
        id = "STAMP_DUTY",
        name = "Stamp Duty (Buy Side)",
        rate = 0.00002, // 0.002% (resolved conflict 0.002% vs 0.003% as one configurable unverified value)
        basis = "buy turnover",
        isConfirmed = false,
        statusLabel = "UNVERIFIED — check current MCX/SEBI circular",
        source = "Indian Stamp Act (MCX Schedule)",
        lastVerifiedDate = "UNVERIFIED"
    ),
    val defaultSlippageBps: Double = 3.0
) {
    fun allRates(): List<CostRateConfigItem> = listOf(
        cttRate, gstRate, brokerageRate, exchangeTurnoverRate, clearingRate, sebiRate, stampDutyRate
    )
}

/**
 * Breakdown of Indian commodity exchange (MCX) statutory and broker fees.
 */
data class CostBreakdown(
    val notionalTurnoverInr: Double,
    val brokerageInr: Double,
    val cttInr: Double,               // Commodity Transaction Tax (0.01% on sell side)
    val exchangeTurnoverFeeInr: Double,// MCX fee (0.0021%)
    val clearingChargesInr: Double,   // 0.0001%
    val gstInr: Double,               // 18% on (Brokerage + Exch + Clearing)
    val sebiChargesInr: Double,       // ₹10 per crore (0.0001%)
    val stampDutyInr: Double,         // Configurable (0.002% buy side)
    val estimatedSlippageInr: Double, // Based on bid-ask spread & depth
    val totalCostInr: Double,
    val costPerGramInr: Double,
    val costBps: Double,
    val unverifiedDisclaimers: List<String> = emptyList()
)

object TransactionCostEngine {

    private val _config = MutableStateFlow(TransactionCostConfig())
    val config: StateFlow<TransactionCostConfig> = _config.asStateFlow()

    fun updateConfig(newConfig: TransactionCostConfig) {
        _config.value = newConfig
    }

    /**
     * Calculates transaction costs for a single leg with configurable broker and statutory rates.
     */
    fun calculateLegCost(
        contractType: GoldContractType,
        lots: Int,
        pricePerUnit: Double,
        side: OrderSide,
        slippageBps: Double = _config.value.defaultSlippageBps
    ): CostBreakdown {
        val cfg = _config.value
        val totalGrams = lots * contractType.lotSizeGrams
        val units = totalGrams / contractType.quoteUnitGrams
        val turnover = units * pricePerUnit

        // Brokerage rule: ₹20 flat or 0.03% of turnover, whichever is lower
        val brokerage = min(cfg.brokerageFlatInr, max(2.0, turnover * cfg.brokeragePctCap))
        val ctt = if (side == OrderSide.SELL) turnover * cfg.cttRate.rate else 0.0
        val exchangeFee = turnover * cfg.exchangeTurnoverRate.rate
        val clearing = turnover * cfg.clearingRate.rate
        val gst = (brokerage + exchangeFee + clearing) * cfg.gstRate.rate
        val sebi = turnover * cfg.sebiRate.rate
        val stampDuty = if (side == OrderSide.BUY) turnover * cfg.stampDutyRate.rate else 0.0
        val slippage = turnover * (slippageBps / 10000.0)

        val total = brokerage + ctt + exchangeFee + clearing + gst + sebi + stampDuty + slippage
        val costPerGram = if (totalGrams > 0) total / totalGrams else 0.0
        val bps = if (turnover > 0) (total / turnover) * 10000.0 else 0.0

        val unverifiedList = mutableListOf<String>()
        if (!cfg.exchangeTurnoverRate.isConfirmed) unverifiedList.add("Exchange Turnover: ${cfg.exchangeTurnoverRate.statusLabel}")
        if (!cfg.clearingRate.isConfirmed) unverifiedList.add("Clearing Fee: ${cfg.clearingRate.statusLabel}")
        if (!cfg.sebiRate.isConfirmed) unverifiedList.add("SEBI Charges: ${cfg.sebiRate.statusLabel}")
        if (!cfg.stampDutyRate.isConfirmed) unverifiedList.add("Stamp Duty: ${cfg.stampDutyRate.statusLabel}")

        return CostBreakdown(
            notionalTurnoverInr = turnover,
            brokerageInr = brokerage,
            cttInr = ctt,
            exchangeTurnoverFeeInr = exchangeFee,
            clearingChargesInr = clearing,
            gstInr = gst,
            sebiChargesInr = sebi,
            stampDutyInr = stampDuty,
            estimatedSlippageInr = slippage,
            totalCostInr = total,
            costPerGramInr = costPerGram,
            costBps = bps,
            unverifiedDisclaimers = unverifiedList
        )
    }

    /**
     * Calculates round-trip costs (Buy entry leg + Sell exit leg) in Rupees per Gram
     * for a given lot size.
     */
    fun calculateRoundtripCostPerGram(
        contractType: GoldContractType,
        lots: Int,
        currentPrice: Double,
        slippageBps: Double = _config.value.defaultSlippageBps
    ): Double {
        val buyLeg = calculateLegCost(contractType, lots, currentPrice, OrderSide.BUY, slippageBps)
        val sellLeg = calculateLegCost(contractType, lots, currentPrice, OrderSide.SELL, slippageBps)
        val totalGrams = lots * contractType.lotSizeGrams
        return if (totalGrams > 0) (buyLeg.totalCostInr + sellLeg.totalCostInr) / totalGrams else 0.0
    }

    /**
     * For a 2-legged spread trade:
     * Calculates true roundtrip execution cost per gram on an exposure-balanced basis (100g standard exposure).
     * This avoids single-lot distortions where a 1g Petal ticket pays a full flat fee on 1g.
     */
    fun calculateSpreadRoundtripCostPerGram(
        baseContract: GoldContractType,
        targetContract: GoldContractType,
        basePrice: Double,
        targetPrice: Double,
        baseSlippageBps: Double = 3.0,
        targetSlippageBps: Double = 5.0,
        balancedExposureGrams: Double = 100.0
    ): Double {
        val baseLots = max(1, (balancedExposureGrams / baseContract.lotSizeGrams).toInt())
        val targetLots = max(1, (balancedExposureGrams / targetContract.lotSizeGrams).toInt())

        val baseCostPerGram = calculateRoundtripCostPerGram(baseContract, baseLots, basePrice, baseSlippageBps)
        val targetCostPerGram = calculateRoundtripCostPerGram(targetContract, targetLots, targetPrice, targetSlippageBps)
        return baseCostPerGram + targetCostPerGram
    }
}
