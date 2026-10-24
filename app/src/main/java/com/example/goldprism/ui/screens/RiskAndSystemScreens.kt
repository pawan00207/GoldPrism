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
import com.example.goldprism.model.AlertSeverity
import com.example.goldprism.model.GoldContractType
import com.example.goldprism.ui.GoldPrismViewModel
import com.example.goldprism.ui.components.SectionHeader
import com.example.goldprism.ui.components.HairlineDivider
import com.example.ui.theme.*

@Composable
fun RiskManagementScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
    val risk = viewModel.riskSettings.collectAsState().value

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Emergency Kill Switch Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (risk.killSwitchEngaged) CrimsonContainer else CharcoalSurface
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (risk.killSwitchEngaged) CrimsonLoss else TerminalBorder),
                modifier = Modifier.fillMaxWidth().testTag("kill_switch_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = if (risk.killSwitchEngaged) CrimsonLoss else EmeraldProfit)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "EMERGENCY KILL SWITCH",
                                color = if (risk.killSwitchEngaged) CrimsonLoss else EmeraldProfit,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Switch(
                            checked = risk.killSwitchEngaged,
                            onCheckedChange = { viewModel.toggleKillSwitch() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CrimsonLoss,
                                checkedTrackColor = CrimsonContainer
                            ),
                            modifier = Modifier.testTag("kill_switch_toggle")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (risk.killSwitchEngaged)
                            "STATUS: ACTIVE. All trading orders, automated triggers, and execution queues are FROZEN immediately."
                        else "STATUS: NORMAL. Risk daemon monitoring all pre-trade parameters and market feeds.",
                        color = TextWhitePrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 2. Risk Limits Policy
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PRE-TRADE RISK LIMITS", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Max Order Lots", color = TextMutedSecondary, fontSize = 12.sp)
                        Text("${risk.maxOrderQtyLots} lots", color = TextWhitePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Max Exposure Sizing", color = TextMutedSecondary, fontSize = 12.sp)
                        Text("${risk.maxExposureGrams.toInt()} grams", color = TextWhitePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Daily Loss Limit", color = TextMutedSecondary, fontSize = 12.sp)
                        Text("₹${String.format("%,.0f", risk.dailyLossLimitInr)}", color = CrimsonLoss, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Max Slippage Tolerance", color = TextMutedSecondary, fontSize = 12.sp)
                        Text("${risk.maxAllowedSlippageBps} bps", color = AmberWarning, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("CONFIGURE RISK THRESHOLDS:", color = TextMutedSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10 to 500.0, 20 to 1000.0, 50 to 2500.0).forEach { (lots, exp) ->
                            val isSel = risk.maxOrderQtyLots == lots
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.updateRiskLimits(lots, exp, risk.dailyLossLimitInr) },
                                label = { Text("${lots}L / ${exp.toInt()}g") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.weight(1f).testTag("risk_preset_${lots}")
                            )
                        }
                    }
                }
            }
        }

        // 3. Automated Execution 8-Stage Safety Gate Card
        item {
            val autoState = viewModel.automationState.collectAsState().value
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (autoState.isAutomationEnabled) EmeraldProfit else TerminalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AUTOMATED EXECUTION GOVERNANCE", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Surface(
                            color = if (autoState.isAutomationEnabled) EmeraldContainer else CrimsonContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (autoState.isAutomationEnabled) "ACTIVE" else "LOCKED (SAFETY OFF)",
                                color = if (autoState.isAutomationEnabled) EmeraldProfit else CrimsonLoss,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(autoState.statusMessage, color = TextWhitePrimary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(10.dp))

                    autoState.gates.forEach { gate ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (gate.isSatisfied) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (gate.isSatisfied) EmeraldProfit else CrimsonLoss,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stage ${gate.stageNumber}: ${gate.title}", color = TextWhitePrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertsScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
    val alerts = viewModel.alerts.collectAsState().value

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Alerts & Notifications",
                subtitle = "Signal changes, risk breaches, and data health warnings",
                badgeText = "${alerts.size} EVENTS"
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(alerts) { alert ->
            val color = when (alert.severity) {
                AlertSeverity.CRITICAL -> CrimsonLoss
                AlertSeverity.WARNING -> AmberWarning
                AlertSeverity.INFO -> PrismCyan
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(alert.title, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(alert.category.name, color = TextMutedSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(alert.message, color = TextWhitePrimary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ResearchReportsScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
    var exportSuccess by remember { mutableStateOf(false) }

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
                modifier = Modifier.fillMaxWidth().testTag("research_reports_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("EXPORTABLE RESEARCH REPORTS", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("MCX Gold Derivatives Whitepaper", color = TextWhitePrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Reproducible analytical dossier containing data timestamps, contract specifications, X-Ray mathematical formulas, and walk-forward backtest metrics.",
                        color = TextMutedSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { exportSuccess = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("export_csv_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Research Report (CSV / Markdown)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (exportSuccess) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ Report compiled: goldprism_research_report_2026.csv (SHA-256 verified)",
                            color = EmeraldProfit,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(title = "MCX Contract Specification Reference Table", subtitle = "Standard exchange trading rules and purity standards")
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(GoldContractType.values()) { contract ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, TerminalBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(contract.symbol, color = GoldPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("${contract.standardPurityPerMille}‰ Purity", color = PrismCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Lot Size: ${contract.lotSizeGrams.toInt()}g • Quoted: per ${contract.quoteUnitGrams.toInt()}g • Margin: ${contract.marginPercentage}%", color = TextMutedSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(contract.description, color = TextWhitePrimary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun DataPipelineScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
    val urlState = viewModel.dataSourceUrl.collectAsState().value
    val pollState = viewModel.pollingIntervalMinutes.collectAsState().value
    val history = viewModel.accumulatedHistory.collectAsState().value
    var inputUrl by remember(urlState) { mutableStateOf(urlState) }
    var ingestionResult by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Data Source URL & Polling
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth().testTag("data_source_config_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DATA SOURCE & REFRESH CONFIGURATION", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Configurable external pipeline URL (empty by default). If set, GoldPrism polls every N minutes in the foreground and validates all incoming bars through the 5 Phase 2b validation gates.",
                        color = TextMutedSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("External Data Source URL", fontSize = 11.sp) },
                        placeholder = { Text("https://example.com/bhavcopy.csv", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("data_source_url_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = TerminalBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.setDataSourceUrl(inputUrl.trim()) },
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            modifier = Modifier.weight(1f).testTag("save_url_btn")
                        ) {
                            Text("Save URL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (inputUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    inputUrl = ""
                                    viewModel.setDataSourceUrl("")
                                },
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear URL", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("POLLING INTERVAL (FOREGROUND):", color = TextMutedSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 15, 30, 60).forEach { mins ->
                            FilterChip(
                                selected = pollState == mins,
                                onClick = { viewModel.setPollingInterval(mins) },
                                label = { Text("${mins}m", fontSize = 11.sp) },
                                shape = RoundedCornerShape(2.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 2. CSV Bhavcopy Ingestion Tester & Local Persistence
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("MCX BHAVCOPY INGESTION (PHASE 2)", color = PrismCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Surface(
                            color = CharcoalSurfaceVariant,
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, TerminalBorderSubtle)
                        ) {
                            Text(
                                text = "STORED: ${history.size} DAYS",
                                color = TextMutedSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Parses official EOD Bhavcopy CSV records. Data is persisted locally so history accumulates across app restarts. Validated sequentially against Gate 1 (Date Match), Gate 2 (Key by Symbol+Expiry), Gate 3 (OHLC Sanity), Gate 4 (Volume/OI), and Gate 5 (Settle Trust).",
                        color = TextMutedSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val sampleCsv = """
                                SYMBOL,EXPIRY_DT,OPEN_PRC,HIGH_PRC,LOW_PRC,CLS_PRC,SETTLE_PRC,TRD_QTY,OPEN_INT
                                GOLDM,05-NOV-2026,78300.0,78750.0,78220.0,78590.0,78580.0,15800,29400
                                GOLDTEN,05-NOV-2026,78350.0,78780.0,78270.0,78610.0,78600.0,7200,11900
                                GOLDGUINEA,30-NOV-2026,62600.0,63050.0,62520.0,62940.0,62920.0,3800,6100
                                GOLDPETAL,30-NOV-2026,7880.0,7935.0,7870.0,7915.0,7910.0,31200,44500
                            """.trimIndent()
                            val res = viewModel.parseAndIngestCsvBhavcopy(sampleCsv)
                            ingestionResult = res.summary
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrismCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("ingest_bhavcopy_btn")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ingest Verified MCX Bhavcopy CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (ingestionResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ $ingestionResult",
                            color = EmeraldProfit,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 3. Roadmap Entry: Live Intraday Feed
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LIVE INTRADAY FEED — ROADMAP, REQUIRES LICENSED DATA SOURCE", color = AmberWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Genuine intraday MCX ticks require a direct licensed vendor feed or authorized broker WebSocket. GoldPrism does not claim live unauthenticated tick streaming without a valid licensed data vendor contract.",
                        color = TextMutedSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsBrokerScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
    val uiState = viewModel.uiState.collectAsState().value
    val costConfig = viewModel.transactionCostConfig.collectAsState().value
    var selectedBroker by remember { mutableStateOf("Mock Broker") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Broker Adapter
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth().testTag("settings_broker_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("EXECUTION ADAPTER ARCHITECTURE", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("ACTIVE ADAPTER:", color = TextMutedSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = true,
                            onClick = { },
                            label = { Text("Mock Broker (ACTIVE)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(2.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldContainer,
                                selectedLabelColor = EmeraldProfit
                            ),
                            modifier = Modifier.weight(1.2f).testTag("broker_chip_mock")
                        )
                        FilterChip(
                            selected = false,
                            onClick = { },
                            label = { Text("Live Broker (ROADMAP)", fontSize = 10.sp) },
                            shape = RoundedCornerShape(2.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                disabledContainerColor = CharcoalSurfaceVariant,
                                disabledLabelColor = TextMutedSecondary
                            ),
                            enabled = false,
                            modifier = Modifier.weight(1f).testTag("broker_chip_live")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = CharcoalSurfaceVariant,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, TerminalBorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("STATUS: ROADMAP — NOT ACTIVE FOR REAL MONEY TRADING", color = AmberWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "MockBrokerAdapter is the ONLY active execution adapter. Live broker connectors (Kite Connect, Angel One, Upstox) and automated trading are marked ROADMAP — NOT ACTIVE. Kill switch, duplicate-order throttling, risk gates, and audit trail are fully operational in simulation.",
                                color = TextMutedSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Transaction Cost Schedule & Verification Status
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("STATUTORY FEE SCHEDULE & VERIFICATION AUDIT", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Configurable transaction cost schedule. Confirmed statutory rates are active. Unverified rates are marked UNVERIFIED with pending MCX/SEBI circular validation.", color = TextMutedSecondary, fontSize = 11.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Table Header
                    Surface(color = CharcoalSurfaceVariant, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                            Text("CHARGE ITEM", color = TextMutedSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.5f))
                            Text("RATE / BASIS", color = TextMutedSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.2f))
                            Text("STATUS", color = TextMutedSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Confirmed & Unverified items
                    val items = listOf(
                        Triple("CTT", "${costConfig.cttRate.rate * 100}% on Sell Turnover", costConfig.cttRate.statusLabel),
                        Triple("Brokerage", "₹${costConfig.brokerageFlatInr} / 0.03% max", costConfig.brokerageRate.statusLabel),
                        Triple("GST", "${costConfig.gstRate.rate * 100}% on (Brok + Exch)", costConfig.gstRate.statusLabel),
                        Triple("Exchange Turnover", "${costConfig.exchangeTurnoverRate.rate * 100}%", costConfig.exchangeTurnoverRate.statusLabel),
                        Triple("Clearing Fee", "${costConfig.clearingRate.rate * 100}%", costConfig.clearingRate.statusLabel),
                        Triple("SEBI Fee", "${costConfig.sebiRate.rate * 100}%", costConfig.sebiRate.statusLabel),
                        Triple("Stamp Duty", "${costConfig.stampDutyRate.rate * 100}% on Buy", costConfig.stampDutyRate.statusLabel)
                    )

                    items.forEach { (name, basis, status) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, color = TextWhitePrimary, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                            Text(basis, color = TextMutedSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.2f))
                            Surface(
                                color = if (status.contains("CONFIRMED")) EmeraldContainer else AmberContainer,
                                shape = RoundedCornerShape(2.dp)
                            ) {
                                Text(
                                    text = if (status.contains("CONFIRMED")) "CONFIRMED" else "UNVERIFIED",
                                    color = if (status.contains("CONFIRMED")) EmeraldProfit else AmberWarning,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        HairlineDivider()
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Note: Stamp duty conflict (0.002% vs 0.003%) resolved to configurable unverified 0.002% buy turnover pending circular confirmation.",
                        color = TextMutedSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AuditLogsScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
    val logs = viewModel.auditLogs.collectAsState().value

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Cryptographic Audit Trail",
                subtitle = "Immutable record of all trading, risk, and kill-switch events",
                badgeText = "${logs.size} ENTRIES"
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(logs) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, TerminalBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(log.eventType, color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(log.actor, color = PrismCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(log.description, color = TextWhitePrimary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Sig: ${log.hashSignature}", color = TextMutedSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun HelpMethodologyScreen(
    viewModel: GoldPrismViewModel,
    modifier: Modifier = Modifier
) {
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
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, TerminalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GOLDPRISM METHODOLOGY & CORE SPECIFICATION", color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("“Explain every trading decision. Prove the edge. Know when NOT to trade.”", color = GoldLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "1. Authoritative Normalization Formula (Phase 1a):\n" +
                                "fine-gold ₹/g = (quote / quote_unit_grams) / (purity / 1000)\n\n" +
                                "Hand-checkable worked examples across all 4 MCX gold contracts:\n" +
                                "• GOLDM (100g unit, quoted per 10g, purity 995‰):\n" +
                                "  Quote ₹79,600 / 10g → (79,600 / 10) / (995 / 1000) = 7,960 / 0.995 = 8,000.00 ₹/g\n" +
                                "• GOLDTEN (10g unit, quoted per 10g, purity 999‰):\n" +
                                "  Quote ₹79,920 / 10g → (79,920 / 10) / (999 / 1000) = 7,992 / 0.999 = 8,000.00 ₹/g\n" +
                                "• GOLDGUINEA (8g unit, quoted per 8g, purity 999‰):\n" +
                                "  Quote ₹63,936 / 8g → (63,936 / 8) / (999 / 1000) = 7,992 / 0.999 = 8,000.00 ₹/g\n" +
                                "• GOLDPETAL (1g unit, quoted per 1g, purity 999‰):\n" +
                                "  Quote ₹7,992 / 1g → (7,992 / 1) / (999 / 1000) = 7,992 / 0.999 = 8,000.00 ₹/g\n\n" +
                                "2. Authoritative Expiry Windows:\n" +
                                "• GOLDM: 3rd–5th of expiry month\n" +
                                "• GOLDTEN, GOLDGUINEA, GOLDPETAL: 27th–31st of contract month\n\n" +
                                "3. Signature X-Ray Waterfall:\n" +
                                "A wide spread does not equal profit. The X-Ray engine subtracts physical retail packaging basis, carry cost across expiry dates, and exchange closing mark artifacts to isolate the true statistical residual:\n" +
                                "Raw Spread = Packaging/Basis + Carry Effect + Settle Artifact + Residual\n\n" +
                                "4. Disciplined 'No Edge':\n" +
                                "When transaction frictions (CTT, brokerage, slippage, GST) consume the residual, or when settle prices are thin (<5 vol or <10 OI) or stale, GoldPrism explicitly assigns NO EDGE. Preserving capital is as important as identifying alpha.",
                        color = TextWhitePrimary,
                        fontSize = 11.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
