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
import com.example.goldprism.model.DataTrustStatus
import com.example.goldprism.ui.GoldPrismViewModel
import com.example.goldprism.ui.components.*
import com.example.ui.theme.*

@Composable
fun MarketHealthScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
    val contracts = viewModel.contracts.collectAsState().value
    val integrityChecks = viewModel.integrityChecks.collectAsState().value
    val activeFault = viewModel.activeFault.collectAsState().value
    val provenance = viewModel.dataProvenance.collectAsState().value
    val provenanceSource = viewModel.provenanceSource.collectAsState().value
    val dataDate = viewModel.dataDate.collectAsState().value
    val lastUpdatedText = viewModel.lastUpdatedTimeText.collectAsState().value
    val isFresh = viewModel.isDataFresh.collectAsState().value

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header & Live Trust Status Summary
        item {
            FlatTerminalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("market_health_summary_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DATA HEALTH & VALIDATION DAEMON",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp
                    )
                    val totalChecks = integrityChecks.size
                    val passedChecks = integrityChecks.count { it.status == DataTrustStatus.TRUSTED }
                    Text(
                        text = "$passedChecks/$totalChecks PASSED",
                        color = if (passedChecks == totalChecks) TerminalPositiveLight else TerminalNegativeLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProvenanceChip(
                        provenance = provenance,
                        sourceName = provenanceSource,
                        dataDate = dataDate
                    )

                    Surface(
                        color = if (isFresh) TerminalPositiveLight.copy(alpha = 0.15f) else TerminalNegativeLight.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, if (isFresh) TerminalPositiveLight else TerminalNegativeLight),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            text = if (isFresh) "FRESH" else "STALE",
                            color = if (isFresh) TerminalPositiveLight else TerminalNegativeLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Data Date: $dataDate", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("Last updated: $lastUpdatedText", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // 2. Seeded Fault Demo (Phase 2d requirement)
        item {
            FlatTerminalCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = TerminalAccentLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SEEDED FAULT DEMO (REAL VALIDATORS)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Inject intentional anomalies to verify that Phase 2b validation gates catch faults and suppress corrupted signals in real-time.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.injectDataGap() },
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, TerminalNegativeLight),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("inject_gap_btn")
                    ) {
                        Text("Missing Price", color = TerminalNegativeLight, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.injectStaleData() },
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, TerminalNegativeLight),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("inject_stale_btn")
                    ) {
                        Text("Stale Feed", color = TerminalNegativeLight, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.injectCorruptOhlc() },
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, TerminalNegativeLight),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("inject_corrupt_ohlc_btn")
                    ) {
                        Text("Corrupt OHLC", color = TerminalNegativeLight, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.injectThinSettle() },
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, TerminalWarningLight),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("inject_thin_settle_btn")
                    ) {
                        Text("Thin Settle", color = TerminalWarningLight, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.resetToTrustedData() },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalPositiveLight, contentColor = Color.White),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("reset_trusted_data_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset All Feeds to Healthy (TRUSTED)", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // 3. Contract Streams Table (Dense table with 0dp radius, right-aligned numbers per Phase 6)
        item {
            SectionHeader(
                title = "Contract Quotes & Settle Marks",
                subtitle = "Authoritative exchange prices and liquidity",
                badgeText = "MCX"
            )
        }

        // Table Header
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SYMBOL", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.2f))
                    Text("LTP", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f))
                    Text("SETTLE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f))
                    Text("VOL / OI", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f))
                    Text("STATUS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f))
                }
            }
            HairlineDivider()
        }

        // Table Rows (40dp height, right-aligned numbers)
        items(contracts.values.toList()) { snap ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = snap.contractType.symbol,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = "₹${String.format("%.1f", snap.quotedLtp)}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.4f)
                    )
                    Text(
                        text = "₹${String.format("%.1f", snap.closeSettlement)}",
                        color = if (snap.isThinSettle || snap.isStaleSettle) TerminalWarningLight else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.4f)
                    )
                    Text(
                        text = "${snap.volumeLots}/${snap.openInterestLots}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.4f)
                    )
                    Box(modifier = Modifier.weight(1.4f), contentAlignment = Alignment.CenterEnd) {
                        DataTrustStatusChip(status = snap.trustStatus)
                    }
                }
            }
            HairlineDivider()
        }

        // 4. Detailed Automated Integrity Checks Checklist
        item {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader(
                title = "Automated Integrity Log",
                subtitle = "Rule-based pre-ingestion audit results across 5 Phase 2b gates",
                badgeText = "5 GATES"
            )
        }

        items(integrityChecks) { check ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = check.checkName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = check.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = check.details,
                            color = if (check.status == DataTrustStatus.TRUSTED) TerminalPositiveLight else TerminalNegativeLight,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    DataTrustStatusChip(status = check.status)
                }
            }
        }
    }
}
