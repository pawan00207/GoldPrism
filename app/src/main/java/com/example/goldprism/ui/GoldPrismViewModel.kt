package com.example.goldprism.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldprism.engine.*
import com.example.goldprism.model.*
import com.example.goldprism.repository.GoldPrismRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * The 19 functional sections of GoldPrism as mandated by the brief.
 */
enum class AppSection(val id: Int, val title: String, val category: String) {
    OVERVIEW(0, "Overview Dashboard", "Intelligence"),
    MARKET_HEALTH(1, "Live Market & Data Health", "Intelligence"),
    SPREAD_EXPLORER(2, "Spread Explorer", "Intelligence"),
    XRAY_WATERFALL(3, "X-Ray Waterfall", "Intelligence"),
    SIGNAL_INTELLIGENCE(4, "Signal Intelligence", "Intelligence"),
    VEHICLE_RANKING(5, "Best Vehicle Ranking", "Intelligence"),
    STRATEGY_LAB(6, "Strategy Lab", "Intelligence"),
    BACKTEST_LAB(7, "Backtest Lab", "Intelligence"),
    PAPER_TRADING(8, "Paper Trading", "Execution"),
    TRADE_EXECUTION(9, "Trade Execution", "Execution"),
    POSITIONS_ORDERS(10, "Open Positions & Orders", "Execution"),
    PORTFOLIO_PNL(11, "Portfolio & P&L", "Execution"),
    RISK_MANAGEMENT(12, "Risk Management", "Controls"),
    ALERTS(13, "Alerts & Notifications", "Controls"),
    RESEARCH_REPORTS(14, "Research Reports", "Research"),
    DATA_PIPELINE(15, "Data Sources & Pipeline", "System"),
    SETTINGS_BROKER(16, "Settings & Broker Connections", "System"),
    AUDIT_LOGS(17, "Audit Logs", "System"),
    HELP_METHODOLOGY(18, "Help & Methodology", "Research"),
    MORE(19, "More", "More")
}

data class UiState(
    val currentSection: AppSection = AppSection.OVERVIEW,
    val selectedBase: GoldContractType = GoldContractType.GOLDM,
    val selectedTarget: GoldContractType = GoldContractType.GOLDPETAL,
    val targetExposureGrams: Double = 100.0,
    val executionMode: ExecutionMode = ExecutionMode.PAPER,
    val isDemoWalkthroughActive: Boolean = false,
    val demoWalkthroughStep: Int = 1,
    val orderSymbol: GoldContractType = GoldContractType.GOLDPETAL,
    val orderSide: OrderSide = OrderSide.BUY,
    val orderType: OrderType = OrderType.LIMIT,
    val orderLots: Int = 10,
    val orderLimitPrice: Double = 7910.0,
    val showLiveOrderConfirmationDialog: Boolean = false,
    val lastOrderResult: OrderItem? = null,
    val lastAtomicSpreadResult: AtomicSpreadOrderResult? = null,
    val activeBrokerName: String = "Mock Broker",
    val brokerConnectionStatus: String = "Connected (Simulated Mock Adapter)",
    val activeStrategy: StrategyConfig = StrategyConfig(
        id = "STRAT-01",
        name = "GOLDPETAL/GOLDM Mean Reversion",
        baseContract = GoldContractType.GOLDM,
        targetContract = GoldContractType.GOLDPETAL,
        entryZScore = 1.85,
        exitZScore = 0.40,
        stopLossPerGram = 40.0,
        targetExposureGrams = 100.0
    ),
    val currentBacktestResult: BacktestResult? = null,
    val isBacktesting: Boolean = false
)

class GoldPrismViewModel(
    private val repository: GoldPrismRepository = GoldPrismRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val contracts = repository.contracts
    val integrityChecks = repository.integrityChecks
    val activeFault = repository.activeFault
    val riskSettings = repository.riskSettings
    val accountState = repository.accountState
    val positions = repository.positions
    val orders = repository.orders
    val alerts = repository.alerts
    val auditLogs = repository.auditLogs
    val contractSpecs = repository.contractSpecs
    val automationState = repository.automationState

    // Phase 2c & 4: Provenance and Real-Time Telemetry
    val dataProvenance = repository.dataProvenance
    val provenanceSource = repository.provenanceSource
    val dataDate = repository.dataDate
    val lastUpdatedTimeText = repository.lastUpdatedTimeText
    val isDataFresh = repository.isDataFresh
    val dataSourceUrl = repository.dataSourceUrl
    val pollingIntervalMinutes = repository.pollingIntervalMinutes
    val isSimulatedTickFeedActive = repository.isSimulatedTickFeedActive
    val transactionCostConfig = repository.transactionCostConfig
    val accumulatedHistory = repository.accumulatedHistory

    // Phase 4a: Reactive Core Pipelines
    val xRayDecomposition: StateFlow<XRayDecomposition?> = combine(
        contracts, _uiState, transactionCostConfig
    ) { currentContracts, state, _ ->
        val baseSnap = currentContracts[state.selectedBase] ?: return@combine null
        val targetSnap = currentContracts[state.selectedTarget] ?: return@combine null
        XRayEngine.decompose(baseSnap, targetSnap)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val spreadSignal: StateFlow<SpreadSignal?> = combine(
        contracts, _uiState, riskSettings, transactionCostConfig
    ) { currentContracts, state, risk, _ ->
        val baseSnap = currentContracts[state.selectedBase] ?: return@combine null
        val targetSnap = currentContracts[state.selectedTarget] ?: return@combine null
        SignalEngine.evaluate(baseSnap, targetSnap, risk.killSwitchEngaged)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val vehicleRankings: StateFlow<List<VehicleRanking>> = combine(
        contracts, _uiState, transactionCostConfig
    ) { currentContracts, state, _ ->
        VehicleRankingEngine.rankVehicles(state.targetExposureGrams, currentContracts)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Run initial backtest on default strategy
        runBacktest()
    }

    fun navigateTo(section: AppSection) {
        _uiState.update { it.copy(currentSection = section) }
    }

    fun selectPair(base: GoldContractType, target: GoldContractType) {
        _uiState.update { it.copy(selectedBase = base, selectedTarget = target) }
    }

    fun setTargetExposure(grams: Double) {
        _uiState.update { it.copy(targetExposureGrams = grams) }
    }

    fun setExecutionMode(mode: ExecutionMode) {
        _uiState.update { it.copy(executionMode = mode) }
        repository.updateRiskSettings(riskSettings.value.copy(
            requireManualApprovalForLive = (mode != ExecutionMode.PAPER)
        ))
    }

    fun toggleKillSwitch() {
        repository.toggleKillSwitch()
    }

    fun updateOrderForm(
        symbol: GoldContractType = _uiState.value.orderSymbol,
        side: OrderSide = _uiState.value.orderSide,
        type: OrderType = _uiState.value.orderType,
        lots: Int = _uiState.value.orderLots,
        price: Double = _uiState.value.orderLimitPrice
    ) {
        _uiState.update {
            it.copy(
                orderSymbol = symbol,
                orderSide = side,
                orderType = type,
                orderLots = lots.coerceAtLeast(1),
                orderLimitPrice = price
            )
        }
    }

    fun requestSubmitOrder() {
        val state = _uiState.value
        if (state.executionMode != ExecutionMode.PAPER) {
            // Require explicit confirmation for live execution
            _uiState.update { it.copy(showLiveOrderConfirmationDialog = true) }
        } else {
            executeOrderNow()
        }
    }

    fun confirmLiveOrder() {
        _uiState.update { it.copy(showLiveOrderConfirmationDialog = false) }
        executeOrderNow()
    }

    fun cancelLiveOrderConfirmation() {
        _uiState.update { it.copy(showLiveOrderConfirmationDialog = false) }
    }

    private fun executeOrderNow() {
        val state = _uiState.value
        val result = repository.submitOrder(
            contractType = state.orderSymbol,
            side = state.orderSide,
            orderType = state.orderType,
            lots = state.orderLots,
            price = state.orderLimitPrice,
            mode = state.executionMode
        )
        _uiState.update { it.copy(lastOrderResult = result) }
    }

    fun executeAtomicSpread(targetSide: OrderSide = OrderSide.BUY, targetLots: Int = 1) {
        val state = _uiState.value
        val target = state.selectedTarget
        val base = state.selectedBase

        val targetSnap = contracts.value[target]
        val baseSnap = contracts.value[base]

        val targetPrice = targetSnap?.quotedLtp ?: 7910.0
        val basePrice = baseSnap?.quotedLtp ?: 78500.0

        // Determine base lots to match exposure approximately
        val targetExposureGrams = targetLots * target.lotSizeGrams
        val baseLots = maxOf(1, kotlin.math.round(targetExposureGrams / base.lotSizeGrams).toInt())
        val baseSide = if (targetSide == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY

        val result = repository.submitAtomicSpreadOrder(
            leg1Contract = target,
            leg1Side = targetSide,
            leg1Lots = targetLots,
            leg1Price = targetPrice,
            leg2Contract = base,
            leg2Side = baseSide,
            leg2Lots = baseLots,
            leg2Price = basePrice,
            mode = state.executionMode
        )

        _uiState.update { it.copy(lastAtomicSpreadResult = result) }
    }

    fun connectBroker(brokerName: String, apiKey: String, clientCode: String): Pair<Boolean, String> {
        val adapter: com.example.goldprism.engine.broker.BrokerAdapter = when (brokerName) {
            "Kite Connect" -> com.example.goldprism.engine.broker.KiteConnectBrokerAdapter()
            "Angel SmartAPI" -> com.example.goldprism.engine.broker.AngelSmartApiBrokerAdapter()
            else -> com.example.goldprism.engine.broker.MockBrokerAdapter()
        }

        val success = adapter.connect(apiKey, clientCode)
        if (success) {
            repository.setBrokerAdapter(adapter)
            _uiState.update {
                it.copy(
                    activeBrokerName = brokerName,
                    brokerConnectionStatus = adapter.connectionStatus.label
                )
            }
            return Pair(true, "Successfully connected to $brokerName (${adapter.connectionStatus.label})")
        } else {
            return Pair(false, "Connection failed: Invalid API credentials or Client ID.")
        }
    }

    fun disconnectBroker() {
        val mockAdapter = com.example.goldprism.engine.broker.MockBrokerAdapter()
        repository.setBrokerAdapter(mockAdapter)
        _uiState.update {
            it.copy(
                activeBrokerName = "Mock Broker",
                brokerConnectionStatus = mockAdapter.connectionStatus.label
            )
        }
    }

    fun closePosition(positionId: String, currentLtp: Double) {
        repository.closePosition(positionId, currentLtp)
    }

    fun resetPaperAccount() {
        repository.resetPaperAccount()
    }

    fun evaluatePreTradeRisk(
        contractType: GoldContractType,
        side: OrderSide,
        lots: Int,
        price: Double
    ): PreTradeRiskCheckResult {
        return repository.evaluatePreTradeRisk(contractType, side, lots, price)
    }

    fun runBacktest() {
        val strategy = _uiState.value.activeStrategy
        val res = repository.runBacktest(strategy)
        _uiState.update { it.copy(currentBacktestResult = res) }
    }

    fun updateStrategy(
        entryZ: Double,
        exitZ: Double,
        stopLoss: Double,
        exposureGrams: Double
    ) {
        val current = _uiState.value.activeStrategy
        val updated = current.copy(
            entryZScore = entryZ,
            exitZScore = exitZ,
            stopLossPerGram = stopLoss,
            targetExposureGrams = exposureGrams,
            configHash = "sha256:" + (entryZ.toString() + exitZ + stopLoss).hashCode().toUInt().toString(16)
        )
        _uiState.update { it.copy(activeStrategy = updated) }
        runBacktest()
    }

    // Demo Walkthrough Navigation (Steps 1 to 10 from brief)
    fun setDemoStep(step: Int) {
        val targetSection = when (step) {
            1 -> AppSection.OVERVIEW
            2 -> AppSection.SPREAD_EXPLORER
            3 -> AppSection.XRAY_WATERFALL
            4 -> AppSection.SIGNAL_INTELLIGENCE
            5 -> AppSection.VEHICLE_RANKING
            6 -> AppSection.BACKTEST_LAB
            7 -> AppSection.PAPER_TRADING
            8 -> AppSection.POSITIONS_ORDERS
            9 -> AppSection.TRADE_EXECUTION
            10 -> AppSection.AUDIT_LOGS
            else -> AppSection.OVERVIEW
        }
        _uiState.update { it.copy(demoWalkthroughStep = step, currentSection = targetSection, isDemoWalkthroughActive = true) }
    }

    fun nextDemoStep() {
        val next = minOf(10, _uiState.value.demoWalkthroughStep + 1)
        setDemoStep(next)
    }

    fun prevDemoStep() {
        val prev = maxOf(1, _uiState.value.demoWalkthroughStep - 1)
        setDemoStep(prev)
    }

    fun toggleDemoTour() {
        _uiState.update { it.copy(isDemoWalkthroughActive = !it.isDemoWalkthroughActive) }
    }

    // Fault Injection for Judge Demonstration
    fun injectDataGap() = repository.injectDataGap()
    fun injectStaleData() = repository.injectStaleData()
    fun injectExtremeOutlier() = repository.injectExtremeOutlier()
    fun injectCorruptOhlc() = repository.injectCorruptOhlc()
    fun resetToTrustedData() = repository.resetToTrustedData()

    fun selectStrategyPair(base: GoldContractType, target: GoldContractType) {
        val current = _uiState.value.activeStrategy
        val updated = current.copy(
            baseContract = base,
            targetContract = target,
            name = "${target.symbol}/${base.symbol} Mean Reversion",
            configHash = "sha256:" + (target.symbol + base.symbol + current.entryZScore).hashCode().toUInt().toString(16)
        )
        _uiState.update { it.copy(activeStrategy = updated) }
        runBacktest()
    }

    fun updateRiskLimits(maxLots: Int, maxExposure: Double, dailyLoss: Double) {
        val current = riskSettings.value
        val updated = current.copy(
            maxOrderQtyLots = maxLots,
            maxExposureGrams = maxExposure,
            dailyLossLimitInr = dailyLoss
        )
        repository.updateRiskSettings(updated)
    }

    fun requestAuthorizeAutomation(consent: Boolean) {
        repository.requestAuthorizeAutomation(consent)
    }

    fun emergencyDisarmAutomation() {
        repository.emergencyDisarmAutomation()
    }

    fun parseAndIngestCsvBhavcopy(csvContent: String): com.example.goldprism.engine.BhavcopyIngestionResult {
        return repository.parseAndIngestCsvBhavcopy(csvContent)
    }

    fun updateContractSpec(spec: com.example.goldprism.engine.ContractSpecification) {
        repository.updateContractSpec(spec)
    }

    fun injectThinSettle() = repository.injectThinSettle()

    fun setDataSourceUrl(url: String) = repository.setDataSourceUrl(url)
    fun setPollingInterval(minutes: Int) = repository.setPollingInterval(minutes)
    fun toggleSimulatedTickFeed(enabled: Boolean) = repository.toggleSimulatedTickFeed(enabled)
    fun updateCostConfig(config: com.example.goldprism.engine.TransactionCostConfig) = repository.updateCostConfig(config)

    fun getXRayDecomposition(): XRayDecomposition? {
        val state = _uiState.value
        return repository.getXRayDecomposition(state.selectedBase, state.selectedTarget)
    }

    fun getSignal(): SpreadSignal? {
        val state = _uiState.value
        return repository.getSignal(state.selectedBase, state.selectedTarget)
    }

    fun getVehicleRankings(): List<VehicleRanking> {
        return repository.getVehicleRankings(_uiState.value.targetExposureGrams)
    }
}
