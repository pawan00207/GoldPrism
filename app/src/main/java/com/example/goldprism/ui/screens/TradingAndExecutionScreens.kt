package com.example.goldprism.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.goldprism.engine.TransactionCostEngine
import com.example.goldprism.model.*
import com.example.goldprism.ui.AppSection
import com.example.goldprism.ui.GoldPrismViewModel
import com.example.goldprism.ui.components.MetricStatCard
import com.example.goldprism.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun PaperTradingScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
    val account = viewModel.accountState.collectAsState().value
    val positions = viewModel.positions.collectAsState().value
    val orders = viewModel.orders.collectAsState().value

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Account Balance Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth().testTag("paper_account_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PAPER TRADING SIMULATOR",
                            color = GoldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            color = AmberContainer,
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, AmberWarning)
                        ) {
                            Text(
                                text = "SIMULATION (MOCK)",
                                color = AmberWarning,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("TOTAL NET EQUITY (CASH + UNREALIZED P&L)", color = TextMutedSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "₹${String.format("%,.2f", account.totalEquity)}",
                        color = TextWhitePrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Cash: ₹${String.format("%,.2f", account.cashBalance)} | Unrealized: ₹${String.format("%,.2f", account.unrealizedPnl)} (No double counting)",
                        color = TextMutedSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("MARGIN BLOCKED", color = TextMutedSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("₹${String.format("%,.0f", account.marginBlocked)}", color = AmberWarning, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("REALIZED P&L", color = TextMutedSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = (if (account.realizedPnl >= 0) "+₹" else "-₹") + String.format("%,.2f", kotlin.math.abs(account.realizedPnl)),
                                color = if (account.realizedPnl >= 0) EmeraldProfit else CrimsonLoss,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text("FEES PAID", color = TextMutedSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("₹${String.format("%,.0f", account.totalRoundtripFeesPaid)}", color = TextMutedSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.navigateTo(AppSection.TRADE_EXECUTION) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("paper_new_order_btn")
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Order Ticket", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetPaperAccount() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("paper_reset_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Balance", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2. Open Positions Quick View
        item {
            SectionHeader(
                title = "Current Open Positions",
                subtitle = "Virtual contract exposures with live mark-to-market pricing",
                badgeText = "${positions.size} ACTIVE"
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (positions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TerminalBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No active positions. Execute a spread or single leg order.", color = TextMutedSecondary, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(positions) { pos ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(pos.symbol, color = GoldPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = if (pos.side == OrderSide.BUY) EmeraldContainer else CrimsonContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = pos.side.name,
                                        color = if (pos.side == OrderSide.BUY) EmeraldProfit else CrimsonLoss,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text("${pos.quantityLots} lots • ${pos.exposureGrams.toInt()}g @ Avg ₹${String.format("%.2f", pos.averageEntryPrice)}", color = TextMutedSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        Button(
                            onClick = { viewModel.closePosition(pos.positionId, pos.currentLtp) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonContainer),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("squareoff_${pos.positionId}")
                        ) {
                            Text("Square Off", color = CrimsonLoss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TradeExecutionScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
    val uiState = viewModel.uiState.collectAsState().value
    val contracts = viewModel.contracts.collectAsState().value
    val risk = viewModel.riskSettings.collectAsState().value

    val snap = contracts[uiState.orderSymbol]
    val quotedPrice = snap?.quotedLtp ?: 78500.0

    var selectedSymbol by remember { mutableStateOf(uiState.orderSymbol) }
    var side by remember { mutableStateOf(uiState.orderSide) }
    var orderType by remember { mutableStateOf(uiState.orderType) }
    var lots by remember { mutableStateOf(uiState.orderLots) }
    var limitPrice by remember { mutableStateOf(quotedPrice) }

    val cost = TransactionCostEngine.calculateLegCost(selectedSymbol, lots, limitPrice, side)
    val preTradeCheck = viewModel.evaluatePreTradeRisk(selectedSymbol, side, lots, limitPrice)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Order Execution Banner & Mode Switcher
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth().testTag("trade_ticket_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ORDER EXECUTION TICKET", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Surface(
                            color = if (uiState.executionMode == ExecutionMode.PAPER) EmeraldContainer else GoldContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = uiState.executionMode.label,
                                color = if (uiState.executionMode == ExecutionMode.PAPER) EmeraldProfit else GoldPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode Toggle: Paper vs Live Mock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.executionMode == ExecutionMode.PAPER,
                            onClick = { viewModel.setExecutionMode(ExecutionMode.PAPER) },
                            label = { Text("Paper Trading Mode") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldProfit,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f).testTag("chip_mode_paper")
                        )
                        FilterChip(
                            selected = uiState.executionMode != ExecutionMode.PAPER,
                            onClick = { viewModel.setExecutionMode(ExecutionMode.LIVE_MOCK) },
                            label = { Text("Live Broker (Mock)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f).testTag("chip_mode_live")
                        )
                    }
                }
            }
        }

        // 2. Order Parameters Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. CONTRACT SELECTION:", color = TextMutedSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GoldContractType.values().forEach { type ->
                            val isSel = selectedSymbol == type
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedSymbol = type
                                    limitPrice = contracts[type]?.quotedLtp ?: 78500.0
                                    viewModel.updateOrderForm(symbol = type, price = limitPrice)
                                },
                                label = { Text(type.symbol, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = CharcoalSurfaceVariant,
                                    labelColor = TextWhitePrimary
                                ),
                                modifier = Modifier.weight(1f).testTag("ticket_contract_${type.symbol}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("2. ORDER SIDE:", color = TextMutedSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                side = OrderSide.BUY
                                viewModel.updateOrderForm(side = OrderSide.BUY)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (side == OrderSide.BUY) EmeraldProfit else CharcoalSurfaceVariant,
                                contentColor = if (side == OrderSide.BUY) Color.Black else TextWhitePrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("order_side_buy")
                        ) {
                            Text("BUY / LONG", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                side = OrderSide.SELL
                                viewModel.updateOrderForm(side = OrderSide.SELL)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (side == OrderSide.SELL) CrimsonLoss else CharcoalSurfaceVariant,
                                contentColor = if (side == OrderSide.SELL) Color.White else TextWhitePrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("order_side_sell")
                        ) {
                            Text("SELL / SHORT", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Lots & Price Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("QUANTITY (LOTS):", color = TextMutedSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (lots > 1) {
                                            lots--
                                            viewModel.updateOrderForm(lots = lots)
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp).testTag("btn_decrease_lot")
                                ) {
                                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("$lots", color = TextWhitePrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                OutlinedButton(
                                    onClick = {
                                        lots++
                                        viewModel.updateOrderForm(lots = lots)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp).testTag("btn_increase_lot")
                                ) {
                                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("(${(lots * selectedSymbol.lotSizeGrams).toInt()}g)", color = PrismCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("LIMIT PRICE:", color = TextMutedSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹${String.format("%.2f", limitPrice)}",
                                color = GoldPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text("per ${selectedSymbol.quoteUnitGrams.toInt()}g", color = TextMutedSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // 3. Pre-Trade Risk Checks & Cost Breakdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("STATUTORY CHARGES & MARGIN BREAKDOWN", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (preTradeCheck.isTenderPeriod) {
                        Surface(
                            color = AmberWarning.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AmberWarning),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("MCX TENDER PERIOD ACTIVE (≤5 DTE)", color = AmberWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text(
                                        text = "Physical delivery margins escalated to ${String.format("%.1f", preTradeCheck.effectiveMarginPercentage)}% (minimum 25.0%). Delivery obligations apply at expiry.",
                                        color = TextWhitePrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Notional Turnover", color = TextMutedSecondary, fontSize = 11.sp)
                        Text("₹${String.format("%,.2f", cost.notionalTurnoverInr)}", color = TextWhitePrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Margin Required (${String.format("%.1f", preTradeCheck.effectiveMarginPercentage)}%${if (preTradeCheck.isTenderPeriod) " [Tender]" else ""})", color = TextMutedSecondary, fontSize = 11.sp)
                        Text("₹${String.format("%,.0f", preTradeCheck.estimatedMarginInr)}", color = TextWhitePrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Brokerage + MCX Turnover + GST", color = TextMutedSecondary, fontSize = 11.sp)
                        Text("₹${String.format("%.2f", cost.brokerageInr + cost.exchangeTurnoverFeeInr + cost.gstInr)}", color = TextWhitePrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Statutory CTT & Stamp Duty", color = TextMutedSecondary, fontSize = 11.sp)
                        Text("₹${String.format("%.2f", cost.cttInr + cost.stampDutyInr)}", color = TextWhitePrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Execution Friction", color = AmberWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("₹${String.format("%.2f", cost.totalCostInr)} (${String.format("%.1f", cost.costBps)} bps)", color = AmberWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pre-Trade Risk Status
                    Surface(
                        color = if (preTradeCheck.passed) EmeraldContainer else CrimsonContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (preTradeCheck.passed) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (preTradeCheck.passed) EmeraldProfit else CrimsonLoss,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (preTradeCheck.passed) "Pre-trade risk check PASSED (${preTradeCheck.checksPassed}/${preTradeCheck.checksRun} gates)"
                                else preTradeCheck.violations.firstOrNull() ?: "Risk check failed",
                                color = if (preTradeCheck.passed) EmeraldProfit else CrimsonLoss,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.requestSubmitOrder() },
                        enabled = preTradeCheck.passed,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (side == OrderSide.BUY) EmeraldProfit else CrimsonLoss,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("submit_order_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.executionMode == ExecutionMode.PAPER) "Submit Paper Order" else "Preview & Submit Live Order",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Atomic Multi-Leg Spread Execution Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth().testTag("atomic_spread_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ATOMIC MULTI-LEG SPREAD TICKET", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Surface(
                            color = PrismCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "FOK BRACKET (ZERO LEGGING RISK)",
                                color = PrismCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Simultaneously executes Leg 1 (${uiState.selectedTarget.symbol}) and Leg 2 (${uiState.selectedBase.symbol}). If either leg fails, an automated compensating square-off rolls back the fill to eliminate unhedged overnight exposure.",
                        color = TextMutedSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.executeAtomicSpread(targetSide = OrderSide.BUY, targetLots = 10) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldProfit, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("atomic_spread_long_btn")
                        ) {
                            Text("BUY Spread (Long ${uiState.selectedTarget.symbol})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.executeAtomicSpread(targetSide = OrderSide.SELL, targetLots = 10) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonLoss, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("atomic_spread_short_btn")
                        ) {
                            Text("SELL Spread (Short ${uiState.selectedTarget.symbol})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (uiState.lastAtomicSpreadResult != null) {
                        val res = uiState.lastAtomicSpreadResult!!
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = if (res.success) EmeraldContainer else if (res.rolledBack) CrimsonContainer else CharcoalSurfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = if (res.success) "✓ ATOMIC SPREAD EXECUTED (${res.spreadId})" else if (res.rolledBack) "⚠ ATOMIC ROLLBACK COMPLETED (${res.spreadId})" else "✗ ATOMIC SPREAD REJECTED",
                                    color = if (res.success) EmeraldProfit else if (res.rolledBack) AmberWarning else CrimsonLoss,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(res.message, color = TextWhitePrimary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Live Orders
    if (uiState.showLiveOrderConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelLiveOrderConfirmation() },
            title = {
                Text("Confirm Live Execution", color = GoldPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "You are placing a LIVE order to $side $lots lots (${lots * selectedSymbol.lotSizeGrams}g) of ${selectedSymbol.symbol} at ₹${String.format("%.2f", limitPrice)} via authorized broker adapter.\n\nEstimated Margin: ₹${String.format("%.0f", preTradeCheck.estimatedMarginInr)}\nFees: ₹${String.format("%.2f", cost.totalCostInr)}\n\nDo you authorize order submission?",
                    color = TextWhitePrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmLiveOrder() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_live_order_btn")
                ) {
                    Text("Authorize & Send", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.cancelLiveOrderConfirmation() }) {
                    Text("Cancel")
                }
            },
            containerColor = CharcoalSurface
        )
    }
}

@Composable
fun PositionsOrdersScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
    val positions = viewModel.positions.collectAsState().value
    val orders = viewModel.orders.collectAsState().value

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Live & Paper Positions",
                subtitle = "Active contracts with real-time mark-to-market valuations",
                badgeText = "${positions.size} OPEN"
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (positions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TerminalBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No active positions held.", color = TextMutedSecondary, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(positions) { pos ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TerminalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(pos.symbol, color = GoldPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(pos.side.name, color = if (pos.side == OrderSide.BUY) EmeraldProfit else CrimsonLoss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(pos.mode.label, color = PrismCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Exposure: ${pos.exposureGrams.toInt()}g (${pos.quantityLots} lots)", color = TextWhitePrimary, fontSize = 12.sp)
                            Text("Avg Price: ₹${String.format("%.2f", pos.averageEntryPrice)}", color = TextMutedSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { viewModel.closePosition(pos.positionId, pos.currentLtp) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonContainer),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().testTag("close_pos_${pos.positionId}")
                        ) {
                            Text("Square Off Position", color = CrimsonLoss, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Order History & Executions",
                subtitle = "Audit log of submitted, filled, and rejected tickets",
                badgeText = "${orders.size} RECORDS"
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(orders) { ord ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, TerminalBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${ord.side} ${ord.quantityLots}x ${ord.symbol}", color = TextWhitePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            color = if (ord.status == OrderStatus.FILLED) EmeraldContainer else CrimsonContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = ord.status.name,
                                color = if (ord.status == OrderStatus.FILLED) EmeraldProfit else CrimsonLoss,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Price: ₹${String.format("%.2f", if (ord.status == OrderStatus.FILLED) ord.executedPrice else ord.limitPrice)} • ID: ${ord.orderId} [${ord.mode.label}]", color = TextMutedSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    if (ord.rejectionReason != null) {
                        Text("Rejection: ${ord.rejectionReason}", color = CrimsonLoss, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PortfolioScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
    val account = viewModel.accountState.collectAsState().value
    val positions = viewModel.positions.collectAsState().value

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth().testTag("portfolio_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PORTFOLIO PERFORMANCE & ATTRIBUTION", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("TOTAL NET EQUITY", color = TextMutedSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "₹${String.format("%,.2f", account.totalEquity)}",
                        color = TextWhitePrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetricStatCard("Realized P&L", "₹${String.format("%,.0f", account.realizedPnl)}", highlightColor = if (account.realizedPnl >= 0) EmeraldProfit else CrimsonLoss, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(10.dp))
                        MetricStatCard("Fees Paid", "₹${String.format("%,.0f", account.totalRoundtripFeesPaid)}", highlightColor = AmberWarning, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
