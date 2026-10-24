package com.example.goldprism.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goldprism.model.GoldContractType
import com.example.goldprism.model.SignalVerdict
import com.example.goldprism.ui.AppSection
import com.example.goldprism.ui.GoldPrismViewModel
import com.example.goldprism.ui.components.*
import com.example.ui.theme.*

@Composable
fun OverviewScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
    val contracts = viewModel.contracts.value
    val signal = viewModel.getSignal()
    val activeFault = viewModel.activeFault.value
    val riskSettings = viewModel.riskSettings.value

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Executive Banner: Normalization Basis & Status
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth().testTag("overview_header_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldProfit)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MCX DERIVATIVES TERMINAL",
                                color = GoldPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                        Surface(
                            color = CharcoalSurfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, TerminalBorder)
                        ) {
                            Text(
                                text = "BENCHMARK: ₹/g 999 FINE",
                                color = PrismCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "GoldPrism Intelligence Core",
                        color = TextWhitePrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time contract normalization, spread decomposition, cost-aware signal verification, and risk-gated execution.",
                        color = TextMutedSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 2. Demonstration Flow Interactive Banner (Hackathon Walkthrough)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GoldContainer),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = "Demo Tour", tint = GoldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("JUDGE DEMO TOUR (10-STEP CUJ)", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Text(
                            text = "STEP ${viewModel.uiState.value.demoWalkthroughStep}/10",
                            color = TextWhitePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val stepDescs = listOf(
                        "1. Overview & Data Health Audit",
                        "2. Spread Explorer & Normalization",
                        "3. Signature X-Ray Waterfall Decomposition",
                        "4. Signal Intelligence & Disciplined No-Edge",
                        "5. Best Vehicle Ranking Engine",
                        "6. Walk-Forward Chronological Backtest Lab",
                        "7. Paper Trading Simulator with Real MCX Costs",
                        "8. Portfolio & Open Positions Reconciliation",
                        "9. Manual Live Execution Ticket & Risk Gates",
                        "10. Emergency Kill Switch & Audit Logs"
                    )
                    val currentStepText = stepDescs.getOrElse(viewModel.uiState.value.demoWalkthroughStep - 1) { "" }

                    Text(
                        text = currentStepText,
                        color = TextWhitePrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.prevDemoStep() },
                            enabled = viewModel.uiState.value.demoWalkthroughStep > 1,
                            modifier = Modifier.weight(1f).testTag("demo_prev_button")
                        ) {
                            Text("Previous", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.nextDemoStep() },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            modifier = Modifier.weight(1f).testTag("demo_next_button")
                        ) {
                            Text("Next Step", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Quick Stats Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val goldm = contracts[GoldContractType.GOLDM]
                val petal = contracts[GoldContractType.GOLDPETAL]

                MetricStatCard(
                    title = "GOLDM (Norm)",
                    value = "₹${String.format("%.2f", goldm?.normalizedLtpPerGram ?: 0.0)}/g",
                    subtitle = "Quoted: ₹${goldm?.quotedLtp?.toInt() ?: 0} /10g",
                    highlightColor = GoldPrimary,
                    modifier = Modifier.weight(1f).testTag("stat_goldm")
                )
                MetricStatCard(
                    title = "GOLDPETAL (Norm)",
                    value = "₹${String.format("%.2f", petal?.normalizedLtpPerGram ?: 0.0)}/g",
                    subtitle = "Quoted: ₹${petal?.quotedLtp?.toInt() ?: 0} /1g",
                    highlightColor = PrismCyan,
                    modifier = Modifier.weight(1f).testTag("stat_petal")
                )
            }
        }

        // 4. Active Spread & Signal Focus
        item {
            SectionHeader(
                title = "Active Focus Spread",
                subtitle = "Pair: GOLDPETAL vs GOLDM (Rupees per gram 999 fine gold)",
                badgeText = "REAL-TIME"
            )
            Spacer(modifier = Modifier.height(8.dp))

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
                            Text("OBSERVED SPREAD", color = TextMutedSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = if (signal != null) "₹${String.format("%.2f", signal.rawSpreadPerGram)} /g" else "--",
                                color = TextWhitePrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (signal != null) {
                            VerdictBadge(verdict = signal.verdict, modifier = Modifier.testTag("overview_verdict_badge"))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(color = TerminalBorderSubtle)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("NET EDGE (AFTER COSTS)", color = TextMutedSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = if (signal != null) "₹${String.format("%.2f", signal.netEdgePerGram)} /g" else "--",
                                color = if ((signal?.netEdgePerGram ?: 0.0) > 0) EmeraldProfit else CrimsonLoss,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text("STATISTICAL Z-SCORE", color = TextMutedSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = if (signal != null) "${String.format("%.2f", signal.zScore)}σ" else "--",
                                color = PrismCyan,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text("ROUND-TRIP FRICTION", color = TextMutedSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = if (signal != null) "₹${String.format("%.2f", signal.estimatedRoundtripCostsPerGram)} /g" else "--",
                                color = AmberWarning,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = signal?.verdictRationale ?: "Evaluating contract dislocation...",
                        color = TextMutedSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.navigateTo(AppSection.XRAY_WATERFALL) },
                            colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("inspect_xray_btn")
                        ) {
                            Icon(Icons.Default.Insights, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("X-Ray Waterfall", color = TextWhitePrimary, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.navigateTo(AppSection.SIGNAL_INTELLIGENCE) },
                            colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("signal_details_btn")
                        ) {
                            Icon(Icons.Default.Rule, contentDescription = null, tint = PrismCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Signal Check", color = TextWhitePrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 5. MCX Contract Family Cards
        item {
            SectionHeader(
                title = "MCX Gold Contract Family",
                subtitle = "Normalized to Rupees per Gram 999 Fine Gold",
                badgeText = "4 ACTIVE"
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(contracts.values.toList()) { snap ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = snap.contractType.symbol,
                                color = GoldPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            DataTrustStatusChip(status = snap.trustStatus)
                            if (com.example.goldprism.engine.ContractReferenceService.isTenderPeriod(snap.contractType)) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = AmberWarning.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, AmberWarning)
                                ) {
                                    Text(
                                        text = "TENDER (≤5d)",
                                        color = AmberWarning,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${snap.contractType.displayName} • ${snap.contractType.standardPurityPerMille}‰ • Lot: ${snap.contractType.lotSizeGrams.toInt()}g",
                            color = TextMutedSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Quoted: ₹${String.format("%.2f", snap.quotedLtp)} / ${snap.contractType.quoteUnitGrams.toInt()}g (Exp: ${snap.expiryDate})",
                            color = TextMutedSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹${String.format("%.2f", snap.normalizedLtpPerGram)}",
                            color = TextWhitePrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "per gram 999",
                            color = TextMutedSecondary,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = (if (snap.change >= 0) "+" else "") + "${String.format("%.2f", snap.changePercent)}%",
                            color = if (snap.change >= 0) EmeraldProfit else CrimsonLoss,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
