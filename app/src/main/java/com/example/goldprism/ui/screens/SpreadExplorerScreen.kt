package com.example.goldprism.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goldprism.model.*
import com.example.goldprism.ui.AppSection
import com.example.goldprism.ui.GoldPrismViewModel
import com.example.goldprism.ui.components.*
import com.example.ui.theme.*

@Composable
fun SpreadExplorerScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
    val uiState = viewModel.uiState.collectAsState().value
    val contracts = viewModel.contracts.collectAsState().value
    val signal = viewModel.getSignal()

    val baseSnap = contracts[uiState.selectedBase]
    val targetSnap = contracts[uiState.selectedTarget]

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Pair Selector & Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth().testTag("pair_selector_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MCX GOLD SPREAD EXPLORER",
                        color = GoldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pairwise Contract Dislocation Analysis",
                        color = TextWhitePrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Compare any pair of MCX gold contracts on a normalized basis of Rupees per Gram of 999 Fine Gold.",
                        color = TextMutedSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("SELECT TARGET CONTRACT (LEG 1):", color = TextMutedSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(GoldContractType.GOLDPETAL, GoldContractType.GOLDGUINEA, GoldContractType.GOLDTEN).forEach { type ->
                            val isSelected = uiState.selectedTarget == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectPair(uiState.selectedBase, type) },
                                label = { Text(type.symbol, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = CharcoalSurfaceVariant,
                                    labelColor = TextWhitePrimary
                                ),
                                modifier = Modifier.weight(1f).testTag("select_target_${type.symbol}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("SELECT BASE BENCHMARK CONTRACT (LEG 2):", color = TextMutedSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(GoldContractType.GOLDM, GoldContractType.GOLDTEN).forEach { type ->
                            val isSelected = uiState.selectedBase == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectPair(type, uiState.selectedTarget) },
                                label = { Text(type.symbol, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrismCyan,
                                    selectedLabelColor = Color.Black,
                                    containerColor = CharcoalSurfaceVariant,
                                    labelColor = TextWhitePrimary
                                ),
                                modifier = Modifier.weight(1f).testTag("select_base_${type.symbol}")
                            )
                        }
                    }
                }
            }
        }

        // 2. Normalization Comparison Summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Target: ${uiState.selectedTarget.symbol}",
                    value = "₹${String.format("%.2f", targetSnap?.normalizedLtpPerGram ?: 0.0)}/g",
                    subtitle = "Quoted: ₹${targetSnap?.quotedLtp?.toInt() ?: 0} / ${uiState.selectedTarget.quoteUnitGrams.toInt()}g (${uiState.selectedTarget.standardPurityPerMille}‰)",
                    highlightColor = GoldPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Base: ${uiState.selectedBase.symbol}",
                    value = "₹${String.format("%.2f", baseSnap?.normalizedLtpPerGram ?: 0.0)}/g",
                    subtitle = "Quoted: ₹${baseSnap?.quotedLtp?.toInt() ?: 0} / ${uiState.selectedBase.quoteUnitGrams.toInt()}g (${uiState.selectedBase.standardPurityPerMille}‰)",
                    highlightColor = PrismCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Observed Spread & Verdict Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("OBSERVED RAW SPREAD", color = TextMutedSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "₹${String.format("%.2f", signal?.rawSpreadPerGram ?: 0.0)} /g",
                                color = TextWhitePrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (signal != null) {
                            VerdictBadge(verdict = signal.verdict)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("30-Day Mean: ₹${signal?.historicalMean ?: 28.50} /g", color = TextMutedSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("Std Dev (σ): ₹${signal?.historicalStdDev ?: 8.20} /g", color = TextMutedSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("Percentile: ${String.format("%.1f", signal?.percentile ?: 50.0)}th", color = PrismCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Statistical Bell Curve Chart
                    SpreadDistributionCanvas(
                        currentSpread = signal?.rawSpreadPerGram ?: 28.5,
                        mean = signal?.historicalMean ?: 28.5,
                        stdDev = signal?.historicalStdDev ?: 8.2,
                        zScore = signal?.zScore ?: 0.0
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val targetIsTender = com.example.goldprism.engine.ContractReferenceService.isTenderPeriod(uiState.selectedTarget)
                    val baseIsTender = com.example.goldprism.engine.ContractReferenceService.isTenderPeriod(uiState.selectedBase)
                    if (targetIsTender || baseIsTender) {
                        Surface(
                            color = AmberWarning.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, AmberWarning),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "⚠ TENDER WARNING: ${if (targetIsTender) uiState.selectedTarget.symbol else uiState.selectedBase.symbol} is in physical delivery tender notice (escalated margins active).",
                                color = AmberWarning,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.navigateTo(AppSection.XRAY_WATERFALL) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("open_waterfall_btn")
                        ) {
                            Text("X-Ray Waterfall", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.executeAtomicSpread(targetSide = OrderSide.BUY, targetLots = 10) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("explorer_atomic_exec_btn")
                        ) {
                            Text("Atomic Spread Exec", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.navigateTo(AppSection.SIGNAL_INTELLIGENCE) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("open_signals_btn")
                        ) {
                            Text("Signal", fontSize = 11.sp)
                        }
                    }

                    if (uiState.lastAtomicSpreadResult != null) {
                        val res = uiState.lastAtomicSpreadResult!!
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (res.success) "✓ Spread Executed: ${res.spreadId}" else "⚠ Spread Status: ${res.message}",
                            color = if (res.success) EmeraldProfit else CrimsonLoss,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 4. Contract Normalization Formula Audit Box
        item {
            SectionHeader(
                title = "Mathematical Normalization Audit",
                subtitle = "Formulaic conversion from quoted points to common ₹/g 999 fine gold basis",
                badgeText = "EXPLAINABLE"
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurfaceVariant),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Formula: P_norm = (P_quoted / QuoteUnitGrams) * (999.0 / ContractPurity)",
                        color = GoldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. ${uiState.selectedTarget.symbol}: (₹${targetSnap?.quotedLtp ?: 0.0} / ${uiState.selectedTarget.quoteUnitGrams}g) * (999 / ${uiState.selectedTarget.standardPurityPerMille}) = ₹${String.format("%.2f", targetSnap?.normalizedLtpPerGram ?: 0.0)}/g",
                        color = TextWhitePrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "2. ${uiState.selectedBase.symbol}: (₹${baseSnap?.quotedLtp ?: 0.0} / ${uiState.selectedBase.quoteUnitGrams}g) * (999 / ${uiState.selectedBase.standardPurityPerMille}) = ₹${String.format("%.2f", baseSnap?.normalizedLtpPerGram ?: 0.0)}/g",
                        color = TextWhitePrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
