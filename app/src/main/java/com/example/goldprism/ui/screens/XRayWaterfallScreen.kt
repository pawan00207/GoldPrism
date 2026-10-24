package com.example.goldprism.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.goldprism.ui.AppSection
import com.example.goldprism.ui.GoldPrismViewModel
import com.example.goldprism.ui.components.*
import com.example.ui.theme.*

@Composable
fun XRayWaterfallScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
    val decomp = viewModel.xRayDecomposition.collectAsState().value
    val signal = viewModel.spreadSignal.collectAsState().value

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header Card
        item {
            FlatTerminalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("xray_header_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QUANT X-RAY WATERFALL",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp
                    )
                    if (signal != null) {
                        VerdictBadge(verdict = signal.verdict)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Spread Component Decomposition",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Decomposes observed gross spread into physical delivery basis, expiry carry, settlement friction, gross residual, and transaction fees.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))
                HairlineDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("PAIR", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "${decomp?.targetSymbol ?: ""} / ${decomp?.baseSymbol ?: ""}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("RECONCILIATION ERROR", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "Δ = ${String.format("%.4f", decomp?.reconciliationError ?: 0.0)} ₹/g",
                                color = TerminalPositiveLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // 2. High-Precision Waterfall Canvas Chart
        item {
            if (decomp != null) {
                XRayWaterfallCanvasChart(
                    components = decomp.components,
                    modifier = Modifier.testTag("waterfall_chart_canvas")
                )
            }
        }

        // 3. Quantitative Residual vs Net Edge Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricStatCard(
                    title = "Model Residual (Gross)",
                    value = "₹${String.format("%.2f", decomp?.residualPerGram ?: 0.0)}/g",
                    subtitle = "Unexplained raw spread dislocation",
                    highlightColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Net Extractable Edge",
                    value = "₹${String.format("%.2f", decomp?.netEstimatedEdgePerGram ?: 0.0)}/g",
                    subtitle = "95% CI: [${String.format("%.1f", decomp?.confidenceIntervalLow ?: 0.0)}, ${String.format("%.1f", decomp?.confidenceIntervalHigh ?: 0.0)}]",
                    highlightColor = if ((decomp?.netEstimatedEdgePerGram ?: 0.0) > 0) TerminalPositiveLight else TerminalNegativeLight,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Component-by-Component In-Depth Dissection
        item {
            SectionHeader(
                title = "Decomposition Methodology Breakdown",
                subtitle = "Documented formulas and economic assumptions for each tier",
                badgeText = "EXACT SUM"
            )
        }

        decomp?.components?.let { components ->
            items(components.size) { idx ->
                val comp = components[idx]
                FlatTerminalCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}. ${comp.name.uppercase()}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = (if (comp.valuePerGram > 0 && idx != 0) "+" else "") +
                                    "${String.format("%.2f", comp.valuePerGram)} ₹/g",
                            color = if (comp.valuePerGram >= 0) TerminalPositiveLight else TerminalNegativeLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = comp.explanation,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Running: ₹${String.format("%.2f", comp.runningTotalPerGram)}/g", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("Confidence: ${comp.confidencePct}%", color = TerminalAccentLight, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // 5. Action Buttons (40dp height, 4dp radius)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.navigateTo(AppSection.SIGNAL_INTELLIGENCE) },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalAccentLight, contentColor = Color.White),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("xray_goto_signal_btn")
                ) {
                    Text("Validate Signal", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                OutlinedButton(
                    onClick = { viewModel.navigateTo(AppSection.PAPER_TRADING) },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("xray_goto_paper_btn")
                ) {
                    Text("Paper Trading [SIM]", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                }
            }
        }
    }
}
