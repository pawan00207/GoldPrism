package com.example.goldprism.model

/**
 * Trust status of an MCX market data feed or observation.
 */
enum class DataTrustStatus(val label: String) {
    TRUSTED("TRUSTED"),
    WARNING("WARNING"),
    REJECTED("REJECTED"),
    STALE("STALE"),
    INSUFFICIENT_DATA("INSUFFICIENT DATA")
}

/**
 * Supported MCX Gold Contract identifiers.
 */
enum class GoldContractType(
    val symbol: String,
    val displayName: String,
    val lotSizeGrams: Double,
    val quoteUnitGrams: Double,
    val standardPurityPerMille: Int, // e.g. 995/1000 or 999/1000
    val tickSize: Double,
    val marginPercentage: Double, // Initial margin %
    val description: String
) {
    GOLDM(
        symbol = "GOLDM",
        displayName = "Gold Mini",
        lotSizeGrams = 100.0,
        quoteUnitGrams = 10.0,
        standardPurityPerMille = 995,
        tickSize = 1.0,
        marginPercentage = 10.5,
        description = "MCX 100g Mini contract, quoted per 10g (995 purity, expiry 3rd–5th)"
    ),
    GOLDTEN(
        symbol = "GOLDTEN",
        displayName = "Gold 10g",
        lotSizeGrams = 10.0,
        quoteUnitGrams = 10.0,
        standardPurityPerMille = 999,
        tickSize = 1.0,
        marginPercentage = 11.0,
        description = "MCX 10g contract, quoted per 10g (999 purity, expiry 27th–31st)"
    ),
    GOLDGUINEA(
        symbol = "GOLDGUINEA",
        displayName = "Gold Guinea",
        lotSizeGrams = 8.0,
        quoteUnitGrams = 8.0,
        standardPurityPerMille = 999,
        tickSize = 1.0,
        marginPercentage = 11.5,
        description = "MCX 8g Guinea, quoted per 8g (999 purity, expiry 27th–31st)"
    ),
    GOLDPETAL(
        symbol = "GOLDPETAL",
        displayName = "Gold Petal",
        lotSizeGrams = 1.0,
        quoteUnitGrams = 1.0,
        standardPurityPerMille = 999,
        tickSize = 1.0,
        marginPercentage = 12.0,
        description = "MCX 1g Petal contract, quoted per 1g (999 purity, expiry 27th–31st)"
    );

    /**
     * Normalizes a quoted market price into Rupees per Gram of fine gold.
     * Formula per Phase 1a:
     * fine-gold ₹/g = (quote / quote_unit_grams) / (purity / 1000)
     */
    fun normalizeToRupeesPerGram(quotedPrice: Double): Double {
        if (quoteUnitGrams <= 0.0 || standardPurityPerMille <= 0) return 0.0
        val rawPerGram = quotedPrice / quoteUnitGrams
        val purityFraction = standardPurityPerMille / 1000.0
        return rawPerGram / purityFraction
    }
}

/**
 * Current market snapshot of an MCX Gold Contract.
 */
data class ContractSnapshot(
    val contractType: GoldContractType,
    val expiryDate: String, // e.g. "05-NOV-2026"
    val daysToExpiry: Int,
    val quotedLtp: Double, // e.g. 78,450 per 10g for GOLDM
    val change: Double,
    val changePercent: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val closeSettlement: Double,
    val volumeLots: Long,
    val openInterestLots: Long,
    val bidPrice: Double,
    val askPrice: Double,
    val trustStatus: DataTrustStatus,
    val trustReason: String,
    val lastUpdatedTimestamp: Long,
    val tradeDate: String = "22-OCT-2026",
    val isThinSettle: Boolean = false,
    val isStaleSettle: Boolean = false,
    val isTrustedForSignals: Boolean = true,
    val provenance: String = "SIMULATED" // "REAL", "SIMULATED", "UNVERIFIED"
) {
    val normalizedLtpPerGram: Double
        get() = contractType.normalizeToRupeesPerGram(quotedLtp)

    val normalizedBidPerGram: Double
        get() = contractType.normalizeToRupeesPerGram(bidPrice)

    val normalizedAskPerGram: Double
        get() = contractType.normalizeToRupeesPerGram(askPrice)

    val spreadBps: Double
        get() = if (quotedLtp > 0) ((askPrice - bidPrice) / quotedLtp) * 10000.0 else 0.0
}
