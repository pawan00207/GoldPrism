package com.example.goldprism.engine

import com.example.goldprism.model.GoldContractType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Exchange Contract Specification reference record.
 * Conforms to authoritative MCX contract circulars and delivery standards.
 */
data class ContractSpecification(
    val contractType: GoldContractType,
    val tradingSymbol: String,
    val lotSizeGrams: Double,
    val quoteUnitGrams: Double,
    val standardPurityPerMille: Int,
    val expiryWindow: String, // e.g. "3rd–5th" or "27th–31st"
    val tickSizeInr: Double = 1.0,
    val initialMarginPercentage: Double,
    val deliveryType: String, // "Compulsory Physical Delivery" or "Cash/Physical"
    val deliveryCenterBase: String, // e.g. "Ahmedabad (Designated Base Center)"
    val physicalPackagingBasisPerGram: Double, // Packaging, tamper-proof assay card, logistics
    val activeExpiryDate: String,
    val nextExpiryDate: String,
    val daysToExpiry: Int,
    val isEligibleForTrading: Boolean = true,
    val exchangeNoticeUrl: String = "https://www.mcxindia.com"
)

object ContractReferenceService {

    private val _specs = MutableStateFlow<Map<GoldContractType, ContractSpecification>>(emptyMap())
    val specs: StateFlow<Map<GoldContractType, ContractSpecification>> = _specs.asStateFlow()

    init {
        loadDefaultVerifiedExchangeSpecs()
    }

    /**
     * Loads authoritative exchange-verified reference specifications conforming to Phase 1a:
     * - GOLDM: trading unit 100 g, quoted per 10 g, purity 995, expiry window 3rd–5th
     * - GOLDTEN: trading unit 10 g, quoted per 10 g, purity 999, expiry window 27th–31st
     * - GOLDGUINEA: trading unit 8 g, quoted per 8 g, purity 999, expiry window 27th–31st
     * - GOLDPETAL: trading unit 1 g, quoted per 1 g, purity 999, expiry window 27th–31st
     */
    fun loadDefaultVerifiedExchangeSpecs() {
        val defaultMap = mapOf(
            GoldContractType.GOLDM to ContractSpecification(
                contractType = GoldContractType.GOLDM,
                tradingSymbol = "GOLDM-26NOV",
                lotSizeGrams = 100.0,
                quoteUnitGrams = 10.0,
                standardPurityPerMille = 995,
                expiryWindow = "3rd–5th",
                tickSizeInr = 1.0,
                initialMarginPercentage = 10.5,
                deliveryType = "Compulsory Physical Delivery",
                deliveryCenterBase = "Ahmedabad / Mumbai Designated Vaults",
                physicalPackagingBasisPerGram = 0.0, // Wholesale benchmark baseline
                activeExpiryDate = "05-NOV-2026",
                nextExpiryDate = "05-DEC-2026",
                daysToExpiry = 15,
                isEligibleForTrading = true
            ),
            GoldContractType.GOLDTEN to ContractSpecification(
                contractType = GoldContractType.GOLDTEN,
                tradingSymbol = "GOLDTEN-26NOV",
                lotSizeGrams = 10.0,
                quoteUnitGrams = 10.0,
                standardPurityPerMille = 999,
                expiryWindow = "27th–31st",
                tickSizeInr = 1.0,
                initialMarginPercentage = 11.0,
                deliveryType = "Compulsory Physical Delivery",
                deliveryCenterBase = "Ahmedabad Designated Vaults",
                physicalPackagingBasisPerGram = 4.20, // 10g bar blister packing & vault handling
                activeExpiryDate = "28-NOV-2026",
                nextExpiryDate = "31-DEC-2026",
                daysToExpiry = 38,
                isEligibleForTrading = true
            ),
            GoldContractType.GOLDGUINEA to ContractSpecification(
                contractType = GoldContractType.GOLDGUINEA,
                tradingSymbol = "GOLDGUINEA-26NOV",
                lotSizeGrams = 8.0,
                quoteUnitGrams = 8.0,
                standardPurityPerMille = 999,
                expiryWindow = "27th–31st",
                tickSizeInr = 1.0,
                initialMarginPercentage = 11.5,
                deliveryType = "Compulsory Physical Delivery",
                deliveryCenterBase = "Ahmedabad Vaults",
                physicalPackagingBasisPerGram = 14.80, // Minting, tamper-evident cert, assaying
                activeExpiryDate = "28-NOV-2026",
                nextExpiryDate = "31-DEC-2026",
                daysToExpiry = 38,
                isEligibleForTrading = true
            ),
            GoldContractType.GOLDPETAL to ContractSpecification(
                contractType = GoldContractType.GOLDPETAL,
                tradingSymbol = "GOLDPETAL-26NOV",
                lotSizeGrams = 1.0,
                quoteUnitGrams = 1.0,
                standardPurityPerMille = 999,
                expiryWindow = "27th–31st",
                tickSizeInr = 1.0,
                initialMarginPercentage = 12.0,
                deliveryType = "Both Cash / Physical Delivery (1g tamper-proof card)",
                deliveryCenterBase = "All Designated MCX Delivery Centers",
                physicalPackagingBasisPerGram = 22.50, // Micro blister packaging, NABL assay cert
                activeExpiryDate = "28-NOV-2026",
                nextExpiryDate = "31-DEC-2026",
                daysToExpiry = 38,
                isEligibleForTrading = true
            )
        )
        _specs.value = defaultMap
    }

    fun getSpec(type: GoldContractType): ContractSpecification {
        return _specs.value[type] ?: throw IllegalArgumentException("Specification not found for $type")
    }

    fun updateSpecification(spec: ContractSpecification) {
        val updated = _specs.value.toMutableMap()
        updated[spec.contractType] = spec
        _specs.value = updated
    }

    /**
     * Converts quoted market price to Rupees per Gram of fine gold using
     * the authoritative Phase 1a formula:
     * fine-gold ₹/g = (quote / quote_unit_grams) / (purity / 1000)
     */
    fun normalizePrice(type: GoldContractType, quotedPrice: Double): Double {
        val spec = getSpec(type)
        if (spec.quoteUnitGrams <= 0.0 || spec.standardPurityPerMille <= 0) return 0.0
        val rawPerGram = quotedPrice / spec.quoteUnitGrams
        val purityFraction = spec.standardPurityPerMille / 1000.0
        return rawPerGram / purityFraction
    }

    /**
     * Indicates whether the contract is currently within the MCX tender/delivery notice period (<= 5 trading days to expiry).
     */
    fun isTenderPeriod(type: GoldContractType): Boolean {
        val spec = getSpec(type)
        return spec.daysToExpiry <= 5
    }

    /**
     * Computes required margin. During tender notice period (<= 5 days to expiry),
     * MCX mandates escalated physical delivery margin (typically minimum 25% to 50%).
     */
    fun getEffectiveMarginPercentage(type: GoldContractType): Double {
        val spec = getSpec(type)
        return if (isTenderPeriod(type)) {
            maxOf(25.0, spec.initialMarginPercentage * 2.5)
        } else {
            spec.initialMarginPercentage
        }
    }
}

