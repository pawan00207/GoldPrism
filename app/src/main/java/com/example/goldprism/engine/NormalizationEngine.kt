package com.example.goldprism.engine

import com.example.goldprism.model.GoldContractType

data class NormalizationStep(
    val title: String,
    val formula: String,
    val calculation: String,
    val resultValue: Double,
    val unit: String
)

data class NormalizationAudit(
    val contractType: GoldContractType,
    val quotedPrice: Double,
    val steps: List<NormalizationStep>,
    val finalPricePerGram999Fine: Double,
    val formulaDocumentation: String
)

object NormalizationEngine {

    /**
     * Decomposes the exact normalization steps for transparent inspection.
     * Formula per Phase 1a:
     * fine-gold ₹/g = (quote / quote_unit_grams) / (purity / 1000)
     */
    fun auditNormalization(contractType: GoldContractType, quotedPrice: Double): NormalizationAudit {
        val spec = ContractReferenceService.getSpec(contractType)
        val steps = mutableListOf<NormalizationStep>()

        // Step 1: Quoted Price to Raw Per Gram
        val rawPerGram = quotedPrice / spec.quoteUnitGrams
        steps.add(
            NormalizationStep(
                title = "1. Quotation Unit Normalization",
                formula = "Raw Price Per Gram = Quoted Price / Quote Unit (grams)",
                calculation = "₹$quotedPrice / ${spec.quoteUnitGrams}g",
                resultValue = rawPerGram,
                unit = "₹ / gram (${spec.standardPurityPerMille}‰ fineness)"
            )
        )

        // Step 2: Purity Normalization to 100% Fine Gold Equivalent
        val purityFraction = spec.standardPurityPerMille / 1000.0
        val normalizedPerGram = rawPerGram / purityFraction
        steps.add(
            NormalizationStep(
                title = "2. Purity Normalization to Fine Gold",
                formula = "Fine Gold Price (₹/g) = Raw Price / (Purity / 1000)",
                calculation = "₹${String.format("%.2f", rawPerGram)} / (${spec.standardPurityPerMille} / 1000) [factor: ${String.format("%.4f", 1.0 / purityFraction)}]",
                resultValue = normalizedPerGram,
                unit = "₹ / gram (Fine Gold Standard)"
            )
        )

        val doc = """
            Formula: fine-gold ₹/g = (quote / quote_unit_grams) / (purity / 1000)
            Where:
            - quote: Exchange quoted market price (LTP)
            - quote_unit_grams: Quotation unit in grams (${spec.quoteUnitGrams}g)
            - purity: Fineness per 1000 parts (${spec.standardPurityPerMille}‰)
            - purity / 1000: Fine gold fraction (${purityFraction})
        """.trimIndent()

        return NormalizationAudit(
            contractType = contractType,
            quotedPrice = quotedPrice,
            steps = steps,
            finalPricePerGram999Fine = normalizedPerGram,
            formulaDocumentation = doc
        )
    }
}
