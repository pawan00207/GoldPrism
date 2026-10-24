package com.example.goldprism.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goldprism.engine.DataProvenance
import com.example.goldprism.model.ExecutionMode
import com.example.goldprism.ui.components.HairlineDivider
import com.example.goldprism.ui.components.SimulatedDemoDataBanner
import com.example.goldprism.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: GoldPrismViewModel) {
    val uiState = viewModel.uiState.collectAsState().value
    val riskSettings = viewModel.riskSettings.collectAsState().value
    val alerts = viewModel.alerts.collectAsState().value
    val provenance = viewModel.dataProvenance.collectAsState().value
    val dataDate = viewModel.dataDate.collectAsState().value

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Handle back button: if not on hero screen 1 (Today / MARKET_HEALTH), return to MARKET_HEALTH
    BackHandler(enabled = uiState.currentSection != AppSection.MARKET_HEALTH) {
        viewModel.navigateTo(AppSection.MARKET_HEALTH)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GOLDPRISM",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(2.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = if (provenance == DataProvenance.REAL) "REAL" else "SIMULATED",
                                    color = if (provenance == DataProvenance.REAL) TerminalPositiveLight else TerminalWarningLight,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HairlineDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        Text(
                            text = "HERO TERMINAL SCREENS",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    listOf(
                        AppSection.MARKET_HEALTH to ("1. Today & Data Health" to Icons.Default.HealthAndSafety),
                        AppSection.XRAY_WATERFALL to ("2. X-Ray Waterfall" to Icons.Default.Insights),
                        AppSection.SIGNAL_INTELLIGENCE to ("3. Verdict (Tradable/No Edge)" to Icons.Default.Rule),
                        AppSection.VEHICLE_RANKING to ("4. Best Vehicle Ranking" to Icons.Default.Leaderboard)
                    ).forEach { (section, pair) ->
                        val (label, icon) = pair
                        item {
                            DrawerItemRow(
                                title = label,
                                icon = icon,
                                isSelected = uiState.currentSection == section,
                                onClick = {
                                    viewModel.navigateTo(section)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "MORE TRADING MODULES",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    listOf(
                        AppSection.OVERVIEW to ("Overview Dashboard" to Icons.Default.Dashboard),
                        AppSection.SPREAD_EXPLORER to ("Spread Explorer" to Icons.Default.Timeline),
                        AppSection.STRATEGY_LAB to ("Strategy Lab" to Icons.Default.Science),
                        AppSection.BACKTEST_LAB to ("Backtest Lab" to Icons.Default.Assessment),
                        AppSection.PAPER_TRADING to ("Paper Trading [SIMULATION]" to Icons.Default.AccountBalanceWallet),
                        AppSection.TRADE_EXECUTION to ("Trade Execution [SIMULATION]" to Icons.Default.ShoppingCart),
                        AppSection.POSITIONS_ORDERS to ("Open Positions & Orders" to Icons.Default.ListAlt),
                        AppSection.PORTFOLIO_PNL to ("Portfolio & P&L" to Icons.Default.PieChart),
                        AppSection.RISK_MANAGEMENT to ("Risk Management" to Icons.Default.Security),
                        AppSection.ALERTS to ("Alerts & Notifications" to Icons.Default.Notifications),
                        AppSection.RESEARCH_REPORTS to ("Research Reports" to Icons.Default.Article),
                        AppSection.DATA_PIPELINE to ("Data Pipeline & Bhavcopy" to Icons.Default.CloudSync),
                        AppSection.SETTINGS_BROKER to ("Settings & Broker [SIMULATION]" to Icons.Default.Settings),
                        AppSection.AUDIT_LOGS to ("Audit Logs" to Icons.Default.Lock),
                        AppSection.HELP_METHODOLOGY to ("Help & Methodology" to Icons.Default.MenuBook)
                    ).forEach { (section, pair) ->
                        val (label, icon) = pair
                        item {
                            DrawerItemRow(
                                title = label,
                                icon = icon,
                                isSelected = uiState.currentSection == section,
                                onClick = {
                                    viewModel.navigateTo(section)
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "GoldPrism",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = if (uiState.executionMode == ExecutionMode.PAPER) MaterialTheme.colorScheme.surfaceVariant else TerminalWarningLight.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(2.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = if (uiState.executionMode == ExecutionMode.PAPER) "PAPER" else "ROADMAP — NOT ACTIVE",
                                        color = if (uiState.executionMode == ExecutionMode.PAPER) TerminalPositiveLight else TerminalWarningLight,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("open_drawer_btn")
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        },
                        actions = {
                            // Provenance tag in top bar: Data: <date> · REAL/SIMULATED (11sp)
                            Text(
                                text = "Data: $dataDate · ${provenance.label}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            // Kill Switch Action
                            IconButton(
                                onClick = { viewModel.toggleKillSwitch() },
                                modifier = Modifier.testTag("topbar_killswitch_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = "Kill Switch",
                                    tint = if (riskSettings.killSwitchEngaged) TerminalNegativeLight else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    HairlineDivider()
                }
            },
            bottomBar = {
                Column {
                    HairlineDivider()
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .height(56.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // 5 Hero Items: Today, X-Ray, Verdict, Vehicle, More (Phase 5 & 6)
                        val bottomNavItems = listOf(
                            Triple(AppSection.MARKET_HEALTH, Icons.Default.HealthAndSafety, "Today"),
                            Triple(AppSection.XRAY_WATERFALL, Icons.Default.Insights, "X-Ray"),
                            Triple(AppSection.SIGNAL_INTELLIGENCE, Icons.Default.Rule, "Verdict"),
                            Triple(AppSection.VEHICLE_RANKING, Icons.Default.Leaderboard, "Vehicle"),
                            Triple(AppSection.MORE, Icons.Default.MoreHoriz, "More")
                        )

                        bottomNavItems.forEach { (section, icon, label) ->
                            val isSelected = uiState.currentSection == section ||
                                    (section == AppSection.MORE && isMoreSection(uiState.currentSection))

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.navigateTo(section) },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) TerminalAccentLight else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                        color = if (isSelected) TerminalAccentLight else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TerminalAccentLight,
                                    selectedTextColor = TerminalAccentLight,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_item_${label.lowercase()}")
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Persistent SIMULATED DEMO DATA Banner (Phase 2c)
                SimulatedDemoDataBanner(isSimulated = (provenance == DataProvenance.SIMULATED))

                Crossfade(
                    targetState = uiState.currentSection,
                    modifier = Modifier.fillMaxSize()
                ) { section ->
                    when (section) {
                        // Hero Screen 1
                        AppSection.MARKET_HEALTH -> MarketHealthScreen(viewModel)
                        // Hero Screen 2
                        AppSection.XRAY_WATERFALL -> XRayWaterfallScreen(viewModel)
                        // Hero Screen 3
                        AppSection.SIGNAL_INTELLIGENCE -> SignalIntelligenceScreen(viewModel)
                        // Hero Screen 4
                        AppSection.VEHICLE_RANKING -> VehicleRankingScreen(viewModel)
                        // More Hub
                        AppSection.MORE -> MoreMenuScreen(viewModel)

                        // Secondary screens (all accessible and completely preserved)
                        AppSection.OVERVIEW -> OverviewScreen(viewModel)
                        AppSection.SPREAD_EXPLORER -> SpreadExplorerScreen(viewModel)
                        AppSection.STRATEGY_LAB -> StrategyLabScreen(viewModel)
                        AppSection.BACKTEST_LAB -> BacktestLabScreen(viewModel)
                        AppSection.PAPER_TRADING -> PaperTradingScreen(viewModel)
                        AppSection.TRADE_EXECUTION -> TradeExecutionScreen(viewModel)
                        AppSection.POSITIONS_ORDERS -> PositionsOrdersScreen(viewModel)
                        AppSection.PORTFOLIO_PNL -> PortfolioScreen(viewModel)
                        AppSection.RISK_MANAGEMENT -> RiskManagementScreen(viewModel)
                        AppSection.ALERTS -> AlertsScreen(viewModel)
                        AppSection.RESEARCH_REPORTS -> ResearchReportsScreen(viewModel)
                        AppSection.DATA_PIPELINE -> DataPipelineScreen(viewModel)
                        AppSection.SETTINGS_BROKER -> SettingsBrokerScreen(viewModel)
                        AppSection.AUDIT_LOGS -> AuditLogsScreen(viewModel)
                        AppSection.HELP_METHODOLOGY -> HelpMethodologyScreen(viewModel)
                    }
                }
            }
        }
    }
}

private fun isMoreSection(section: AppSection): Boolean {
    return section !in listOf(
        AppSection.MARKET_HEALTH,
        AppSection.XRAY_WATERFALL,
        AppSection.SIGNAL_INTELLIGENCE,
        AppSection.VEHICLE_RANKING
    )
}

@Composable
private fun MoreMenuScreen(viewModel: GoldPrismViewModel) {
    val modules = listOf(
        Triple(AppSection.OVERVIEW, "Overview Dashboard", Icons.Default.Dashboard),
        Triple(AppSection.SPREAD_EXPLORER, "Spread Explorer", Icons.Default.Timeline),
        Triple(AppSection.STRATEGY_LAB, "Strategy Lab", Icons.Default.Science),
        Triple(AppSection.BACKTEST_LAB, "Backtest Lab", Icons.Default.Assessment),
        Triple(AppSection.PAPER_TRADING, "Paper Trading [SIMULATION]", Icons.Default.AccountBalanceWallet),
        Triple(AppSection.TRADE_EXECUTION, "Trade Execution [SIMULATION]", Icons.Default.ShoppingCart),
        Triple(AppSection.POSITIONS_ORDERS, "Positions & Orders Ledger", Icons.Default.ListAlt),
        Triple(AppSection.PORTFOLIO_PNL, "Portfolio Mark-to-Market", Icons.Default.PieChart),
        Triple(AppSection.RISK_MANAGEMENT, "Risk & Tender Notice Controls", Icons.Default.Security),
        Triple(AppSection.ALERTS, "Alerts & Risk Notifications", Icons.Default.Notifications),
        Triple(AppSection.RESEARCH_REPORTS, "Quantitative Research Reports", Icons.Default.Article),
        Triple(AppSection.DATA_PIPELINE, "Data Pipeline & Bhavcopy Import", Icons.Default.CloudSync),
        Triple(AppSection.SETTINGS_BROKER, "Settings & Broker Connections [ROADMAP]", Icons.Default.Settings),
        Triple(AppSection.AUDIT_LOGS, "Immutable Audit Trail", Icons.Default.Lock),
        Triple(AppSection.HELP_METHODOLOGY, "Formulas & Methodology Guide", Icons.Default.MenuBook)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                text = "TRADING TERMINAL MODULES",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            HairlineDivider()
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(modules) { (section, title, icon) ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(section) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = TerminalAccentLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerItemRow(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        shape = RoundedCornerShape(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) TerminalAccentLight else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}
