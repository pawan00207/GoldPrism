package com.example.goldprism.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goldprism.model.SignalVerdict
import com.example.goldprism.ui.AppSection
import com.example.goldprism.ui.GoldPrismViewModel
import com.example.goldprism.ui.components.FlatTerminalCard
import com.example.goldprism.ui.components.HairlineDivider
import com.example.goldprism.ui.components.MetricStatCard
import com.example.goldprism.ui.components.SectionHeader
import com.example.goldprism.ui.components.VerdictBadge
import com.example.ui.theme.*

@Composable
fun SignalIntelligenceScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
    val signal = viewModel.spreadSignal.collectAsState().value
    val uiState = viewModel.uiState.collectAsState().value
    val provenance = viewModel.dataProvenance.collectAsState().value

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Primary Verdict Card with the 24sp headline number
        item {
            FlatTerminalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signal_verdict_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QUANT SIGNAL VERDICT",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp
                    )
                    if (signal != null) {
                        VerdictBadge(verdict = signal.verdict, modifier = Modifier.testTag("signal_verdict_badge"))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // The single 24sp headline number mandated by Phase 6
                val netEdge = signal?.netEdgePerGram ?: 0.0
                val netColor = when {
                    signal?.verdict == SignalVerdict.TRADABLE -> TerminalPositiveLight
                    signal?.verdict == SignalVerdict.NO_EDGE -> MaterialTheme.colorScheme.onSurfaceVariant
                    signal?.verdict == SignalVerdict.BLOCKED -> TerminalNegativeLight
                    else -> TerminalWarningLight
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "₹${String.format("%.2f", netEdge)}",
                        color = netColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("signal_headline_number")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "₹/g net edge",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = signal?.recommendedAction ?: "ANALYZING PAIR...",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = signal?.verdictRationale ?: "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))
                HairlineDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pair: ${signal?.pairKey}", color = TerminalAccentLight, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("Confidence: ${signal?.confidenceScorePct}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("Z-Score: ${String.format("%.2f", signal?.zScore ?: 0.0)}σ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // 2. Cost-Benefit & Expectancy Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricStatCard(
                    title = "Raw Spread",
                    value = "₹${String.format("%.2f", signal?.rawSpreadPerGram ?: 0.0)}/g",
                    subtitle = "Z: ${String.format("%.2f", signal?.zScore ?: 0.0)}σ",
                    highlightColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Net Edge Expectancy",
                    value = "₹${String.format("%.2f", signal?.netEdgePerGram ?: 0.0)}/g",
                    subtitle = "Fee: ₹${String.format("%.2f", signal?.estimatedRoundtripCostsPerGram ?: 0.0)}/g",
                    highlightColor = if ((signal?.netEdgePerGram ?: 0.0) > 0) TerminalPositiveLight else TerminalNegativeLight,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Quantitative Criteria Audit Checklist
        item {
            SectionHeader(
                title = "Execution Gate Criteria Checklist",
                subtitle = "Transparent audit of conditions required before capital allocation",
                badgeText = "DISCIPLINE"
            )
        }

        // Passed Gates
        signal?.passedCriteria?.let { passed ->
            items(passed) { item ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, TerminalPositiveLight.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Passed", tint = TerminalPositiveLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Failed Gates
        signal?.failedCriteria?.let { failed ->
            items(failed) { item ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, TerminalNegativeLight.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Failed", tint = TerminalNegativeLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item,
                            color = TerminalNegativeLight,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 4. "What Would Change This Verdict" Explainer
        item {
            FlatTerminalCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "WHAT WOULD CHANGE THIS VERDICT?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.4.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (signal?.verdict) {
                        SignalVerdict.TRADABLE -> "Spread narrowing below Z=0.5σ triggers take-profit square-off. Spread expansion beyond ₹45/g stop-loss triggers immediate exit."
                        SignalVerdict.NO_EDGE -> "To become TRADABLE, normalized spread must expand beyond 1.75σ (|Z| >= 1.75) such that net edge exceeds the ₹${String.format("%.2f", signal.estimatedRoundtripCostsPerGram)}/g statutory round-trip cost hurdle."
                        SignalVerdict.WATCH -> "Spread expansion by ~₹3.5/g or decrease in slippage will promote candidate to TRADABLE status."
                        SignalVerdict.BLOCKED -> "Clearing data anomalies in Bhavcopy feed or disengaging Emergency Kill Switch will restore algorithmic evaluation."
                        null -> "--"
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // 5. Action Buttons (Solid accent 4dp radius, 40dp height per Phase 6)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.navigateTo(AppSection.TRADE_EXECUTION) },
                    enabled = signal?.verdict == SignalVerdict.TRADABLE,
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalAccentLight, contentColor = Color.White),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("signal_execute_btn")
                ) {
                    Text("Execute Order Ticket", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                OutlinedButton(
                    onClick = { viewModel.navigateTo(AppSection.BACKTEST_LAB) },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("signal_backtest_btn")
                ) {
                    Text("Backtest Strategy", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                }
            }
        }
    }
}
