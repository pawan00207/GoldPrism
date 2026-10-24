package com.example.goldprism.repository

import com.example.goldprism.engine.*
import com.example.goldprism.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Authoritative single repository for the entire GoldPrism terminal conforming to Phase 4a.
 * Exposes reactive StateFlows across market data, risk, execution, cost configs, and specs.
 */
class GoldPrismRepository {

    // Market Data & Health
    val contracts: StateFlow<Map<GoldContractType, ContractSnapshot>> = MarketDataEngine.contracts
    val integrityChecks: StateFlow<List<DataIntegrityCheck>> = MarketDataEngine.integrityChecks
    val activeFault: StateFlow<String?> = MarketDataEngine.activeFault

    // Provenance and Real-Time Telemetry
    val dataProvenance: StateFlow<DataProvenance> = MarketDataEngine.dataProvenance
    val provenanceSource: StateFlow<String> = MarketDataEngine.provenanceSource
    val dataDate: StateFlow<String> = MarketDataEngine.dataDate
    val lastUpdatedTimeText: StateFlow<String> = MarketDataEngine.lastUpdatedTimeText
    val isDataFresh: StateFlow<Boolean> = MarketDataEngine.isDataFresh
    val dataSourceUrl: StateFlow<String> = MarketDataEngine.dataSourceUrl
    val pollingIntervalMinutes: StateFlow<Int> = MarketDataEngine.pollingIntervalMinutes
    val isSimulatedTickFeedActive: StateFlow<Boolean> = MarketDataEngine.isSimulatedTickFeedActive
    val accumulatedHistory: StateFlow<List<DailyBhavcopyDay>> = MarketDataEngine.accumulatedHistory

    // Risk and Execution
    val riskSettings: StateFlow<RiskSettings> = ExecutionRiskEngine.riskSettings
    val accountState: StateFlow<AccountState> = ExecutionRiskEngine.account
    val positions: StateFlow<List<PositionItem>> = ExecutionRiskEngine.positions
    val orders: StateFlow<List<OrderItem>> = ExecutionRiskEngine.orders
    val alerts: StateFlow<List<AlertNotification>> = ExecutionRiskEngine.alerts
    val auditLogs: StateFlow<List<AuditLogEntry>> = ExecutionRiskEngine.auditLogs

    // Contract Specs & Transaction Costs
    val contractSpecs: StateFlow<Map<GoldContractType, ContractSpecification>> = ContractReferenceService.specs
    val transactionCostConfig: StateFlow<TransactionCostConfig> = TransactionCostEngine.config
    val automationState: StateFlow<AutomationState> = AutomatedExecutionManager.state

    fun getXRayDecomposition(base: GoldContractType, target: GoldContractType): XRayDecomposition? {
        val baseSnap = contracts.value[base] ?: return null
        val targetSnap = contracts.value[target] ?: return null
        return XRayEngine.decompose(baseSnap, targetSnap)
    }

    fun getSignal(base: GoldContractType, target: GoldContractType): SpreadSignal? {
        val baseSnap = contracts.value[base] ?: return null
        val targetSnap = contracts.value[target] ?: return null
        val killSwitch = riskSettings.value.killSwitchEngaged
        return SignalEngine.evaluate(baseSnap, targetSnap, killSwitch)
    }

    fun getVehicleRankings(exposureGrams: Double): List<VehicleRanking> {
        return VehicleRankingEngine.rankVehicles(exposureGrams, contracts.value)
    }

    fun runBacktest(strategy: StrategyConfig, customRealBars: List<BacktestBar>? = null): BacktestResult {
        return BacktestEngine.runBacktest(strategy, customRealBars)
    }

    fun submitOrder(
        contractType: GoldContractType,
        side: OrderSide,
        orderType: OrderType,
        lots: Int,
        price: Double,
        mode: ExecutionMode
    ): OrderItem {
        val snap = contracts.value[contractType]
        return ExecutionRiskEngine.submitOrder(contractType, side, orderType, lots, price, mode, snap)
    }

    fun submitAtomicSpreadOrder(
        leg1Contract: GoldContractType,
        leg1Side: OrderSide,
        leg1Lots: Int,
        leg1Price: Double,
        leg2Contract: GoldContractType,
        leg2Side: OrderSide,
        leg2Lots: Int,
        leg2Price: Double,
        mode: ExecutionMode
    ): AtomicSpreadOrderResult {
        return ExecutionRiskEngine.submitAtomicSpreadOrder(
            leg1Contract, leg1Side, leg1Lots, leg1Price,
            leg2Contract, leg2Side, leg2Lots, leg2Price,
            mode, contracts.value
        )
    }

    fun evaluatePreTradeRisk(
        contractType: GoldContractType,
        side: OrderSide,
        lots: Int,
        price: Double
    ): PreTradeRiskCheckResult {
        val snap = contracts.value[contractType]
        return ExecutionRiskEngine.evaluatePreTradeRisk(contractType, side, lots, price, snap)
    }

    fun closePosition(positionId: String, currentLtp: Double) {
        ExecutionRiskEngine.closePosition(positionId, currentLtp)
    }

    fun resetPaperAccount() {
        ExecutionRiskEngine.resetPaperAccount()
    }

    fun toggleKillSwitch(): Boolean {
        return ExecutionRiskEngine.toggleKillSwitch("User UI button toggle")
    }

    fun updateRiskSettings(settings: RiskSettings) {
        ExecutionRiskEngine.updateRiskSettings(settings)
    }

    fun updateCostConfig(config: TransactionCostConfig) {
        TransactionCostEngine.updateConfig(config)
    }

    fun updateContractSpec(spec: ContractSpecification) {
        ContractReferenceService.updateSpecification(spec)
    }

    fun setBrokerAdapter(adapter: com.example.goldprism.engine.broker.BrokerAdapter) {
        ExecutionRiskEngine.setBrokerAdapter(adapter)
    }

    fun requestAuthorizeAutomation(consent: Boolean) {
        AutomatedExecutionManager.requestAuthorizeUserConsent(consent)
    }

    fun emergencyDisarmAutomation() {
        AutomatedExecutionManager.emergencyDisarm()
    }

    fun setDataSourceUrl(url: String) {
        MarketDataEngine.setDataSourceUrl(url)
    }

    fun setPollingInterval(minutes: Int) {
        MarketDataEngine.setPollingInterval(minutes)
    }

    fun toggleSimulatedTickFeed(enabled: Boolean) {
        MarketDataEngine.toggleSimulatedTickFeed(enabled)
    }

    fun parseAndIngestCsvBhavcopy(
        csvContent: String,
        requestedTradeDate: String? = null,
        sourceName: String = "Imported MCX Bhavcopy CSV"
    ): BhavcopyIngestionResult {
        return MarketDataEngine.parseAndIngestCsvBhavcopy(csvContent, requestedTradeDate, sourceName)
    }

    // Seeded Fault Demo (Phase 2d)
    fun injectDataGap() = MarketDataEngine.injectDataGapFault()
    fun injectStaleData() = MarketDataEngine.injectStaleDataFault()
    fun injectExtremeOutlier() = MarketDataEngine.injectExtremeOutlierFault()
    fun injectCorruptOhlc() = MarketDataEngine.injectCorruptOhlcFault()
    fun injectThinSettle() = MarketDataEngine.injectThinSettleFault()
    fun resetToTrustedData() = MarketDataEngine.resetToTrustedData()
}
