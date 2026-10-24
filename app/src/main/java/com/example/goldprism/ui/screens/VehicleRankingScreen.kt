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
import com.example.goldprism.ui.AppSection
import com.example.goldprism.ui.GoldPrismViewModel
import com.example.goldprism.ui.components.FlatTerminalCard
import com.example.goldprism.ui.components.HairlineDivider
import com.example.goldprism.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun VehicleRankingScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
    val uiState = viewModel.uiState.collectAsState().value
    val rankings = viewModel.vehicleRankings.collectAsState().value

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Exposure Selector Header
        item {
            FlatTerminalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vehicle_ranking_header_card")
            ) {
                Text(
                    text = "BEST VEHICLE RANKING ENGINE",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.4.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Optimal Contract Selection",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Ranks MCX contracts for chosen target exposure based on lot granularity, liquidity score, cost basis, and margin efficiency.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))
                HairlineDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text("TARGET GOLD EXPOSURE:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(8.0, 10.0, 50.0, 100.0, 500.0).forEach { grams ->
                        val isSelected = uiState.targetExposureGrams == grams
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setTargetExposure(grams) },
                            shape = RoundedCornerShape(2.dp),
                            label = { Text("${grams.toInt()}g", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TerminalAccentLight,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) TerminalAccentLight else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("exposure_chip_${grams.toInt()}")
                        )
                    }
                }
            }
        }

        // 2. Ranking Cards (Ordered 1 to 4)
        item {
            SectionHeader(
                title = "Ranked Contracts (${uiState.targetExposureGrams.toInt()} Grams)",
                subtitle = "Composite score incorporating liquidity, slippage & statutory fees",
                badgeText = "BEST MATCH"
            )
        }

        items(rankings) { rankItem ->
            val isTopRank = rankItem.rank == 1
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(
                    1.dp,
                    if (isTopRank) TerminalAccentLight else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ranking_card_${rankItem.contractType.symbol}")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (isTopRank) TerminalAccentLight else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(2.dp),
                                border = BorderStroke(1.dp, if (isTopRank) TerminalAccentLight else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = "#${rankItem.rank}",
                                    color = if (isTopRank) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = rankItem.contractType.symbol,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = rankItem.contractType.displayName,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Score: ${String.format("%.1f", rankItem.overallScore)}/100",
                                color = if (isTopRank) TerminalAccentLight else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Liquidity: ${rankItem.liquidityScore}/100",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HairlineDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ORDER SIZING", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text("${rankItem.requiredLots} lots (${rankItem.actualExposureGrams.toInt()}g)", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MARGIN", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text("₹${String.format("%.0f", rankItem.marginRequirementInr)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("COST BASIS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text("${String.format("%.1f", rankItem.costBasisBps)} bps", color = TerminalWarningLight, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = rankItem.justification,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.updateOrderForm(
                                symbol = rankItem.contractType,
                                lots = rankItem.requiredLots
                            )
                            viewModel.navigateTo(AppSection.TRADE_EXECUTION)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTopRank) TerminalAccentLight else MaterialTheme.colorScheme.surface,
                            contentColor = if (isTopRank) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(4.dp),
                        border = if (!isTopRank) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("trade_vehicle_${rankItem.contractType.symbol}")
                    ) {
                        Text("Select for Order Ticket (${rankItem.requiredLots} lots)", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
