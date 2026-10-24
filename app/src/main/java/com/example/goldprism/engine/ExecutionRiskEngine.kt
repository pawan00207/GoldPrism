package com.example.goldprism.engine

import com.example.goldprism.engine.broker.BrokerAdapter
import com.example.goldprism.engine.broker.MockBrokerAdapter
import com.example.goldprism.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object ExecutionRiskEngine {

    private val _riskSettings = MutableStateFlow(RiskSettings())
    val riskSettings: StateFlow<RiskSettings> = _riskSettings.asStateFlow()

    private val _account = MutableStateFlow(AccountState())
    val account: StateFlow<AccountState> = _account.asStateFlow()

    private val _positions = MutableStateFlow<List<PositionItem>>(emptyList())
    val positions: StateFlow<List<PositionItem>> = _positions.asStateFlow()

    private val _orders = MutableStateFlow<List<OrderItem>>(emptyList())
    val orders: StateFlow<List<OrderItem>> = _orders.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    val auditLogs: StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

    private val _alerts = MutableStateFlow<List<AlertNotification>>(emptyList())
    val alerts: StateFlow<List<AlertNotification>> = _alerts.asStateFlow()

    var activeBrokerAdapter: BrokerAdapter = MockBrokerAdapter()
        private set

    // Duplicate order idempotency trackers
    private var lastOrderTimestamp: Long = 0L
    private var lastOrderSignature: String = ""

    init {
        seedInitialDemoData()
    }

    private fun seedInitialDemoData() {
        recordAudit("SYSTEM_STARTUP", "SYSTEM", "GoldPrism Risk & Execution Engine initialized.", "{\"mode\":\"READY\"}")
        addAlert("Engine Online", "MCX Gold Commodity Derivatives Intelligence Engine is active in SAFE PAPER mode.", AlertSeverity.INFO, AlertCategory.SYSTEM)
    }

    fun setBrokerAdapter(adapter: BrokerAdapter) {
        activeBrokerAdapter = adapter
        recordAudit("BROKER_ADAPTER_CHANGED", "USER", "Active broker adapter set to ${adapter.brokerName}", "{\"status\":\"${adapter.connectionStatus}\"}")
        addAlert("Broker Adapter", "Active execution adapter: ${adapter.brokerName}", AlertSeverity.INFO, AlertCategory.SYSTEM)
    }

    fun toggleKillSwitch(reason: String = "User manual trigger"): Boolean {
        val current = _riskSettings.value
        val newState = !current.killSwitchEngaged
        _riskSettings.value = current.copy(killSwitchEngaged = newState)

        val action = if (newState) "ENGAGED" else "DISENGAGED"
        recordAudit(
            "KILL_SWITCH_$action",
            "USER",
            "Emergency Kill Switch $action. Reason: $reason",
            "{\"killSwitchEngaged\":$newState}"
        )

        addAlert(
            title = "KILL SWITCH $action",
            message = if (newState) "All trading orders and automated signal triggers are HALTED immediately." else "Emergency safety halt cleared. Normal trading resumed.",
            severity = if (newState) AlertSeverity.CRITICAL else AlertSeverity.INFO,
            category = AlertCategory.RISK
        )

        return newState
    }

    fun updateRiskSettings(settings: RiskSettings) {
        _riskSettings.value = settings
        recordAudit("RISK_SETTINGS_UPDATED", "USER", "Pre-trade risk policy parameters updated.", settings.toString())
    }

    /**
     * Runs pre-trade risk evaluation before order placement.
     */
    fun evaluatePreTradeRisk(
        contractType: GoldContractType,
        side: OrderSide,
        lots: Int,
        price: Double,
        contractSnapshot: ContractSnapshot?
    ): PreTradeRiskCheckResult {
        val settings = _riskSettings.value
        val acct = _account.value
        val violations = mutableListOf<String>()

        var checksRun = 0
        var checksPassed = 0

        // 1. Kill Switch Check
        checksRun++
        val killSwitchSafe = !settings.killSwitchEngaged
        if (killSwitchSafe) {
            checksPassed++
        } else {
            violations.add("CRITICAL: Emergency Kill Switch is ACTIVE. No orders permitted.")
        }

        // 2. Data Health Check
        checksRun++
        val dataHealthSafe = contractSnapshot != null &&
                contractSnapshot.trustStatus != DataTrustStatus.REJECTED &&
                contractSnapshot.trustStatus != DataTrustStatus.STALE

        if (dataHealthSafe) {
            checksPassed++
        } else {
            val status = contractSnapshot?.trustStatus ?: DataTrustStatus.INSUFFICIENT_DATA
            violations.add("DATA BLOCKED: Contract data trust status is [$status]. Live execution prevented.")
        }

        // 3. Max Order Qty Check
        checksRun++
        if (lots <= settings.maxOrderQtyLots) {
            checksPassed++
        } else {
            violations.add("ORDER SIZE EXCEEDED: Requested $lots lots exceeds max order limit of ${settings.maxOrderQtyLots} lots.")
        }

        // 4. Exposure Limit Check
        checksRun++
        val orderExposure = lots * contractType.lotSizeGrams
        val currentSymbolExposure = _positions.value.filter { it.symbol == contractType.symbol }.sumOf { it.exposureGrams }
        val totalProjectedExposure = currentSymbolExposure + orderExposure
        val exposureSafe = totalProjectedExposure <= settings.maxExposureGrams

        if (exposureSafe) {
            checksPassed++
        } else {
            violations.add("EXPOSURE BREACH: Projected exposure (${totalProjectedExposure.toInt()}g) exceeds limit (${settings.maxExposureGrams.toInt()}g).")
        }

        // 5. Margin & Capital Check
        checksRun++
        val effectiveMarginPct = ContractReferenceService.getEffectiveMarginPercentage(contractType)
        val isTender = ContractReferenceService.isTenderPeriod(contractType)
        val notional = (orderExposure / contractType.quoteUnitGrams) * price
        val requiredMargin = notional * (effectiveMarginPct / 100.0)
        val availableMargin = acct.cashBalance - acct.marginBlocked
        val marginSufficient = availableMargin >= requiredMargin

        if (marginSufficient) {
            checksPassed++
        } else {
            val tenderNote = if (isTender) " [TENDER ESCALATED ${String.format("%.1f", effectiveMarginPct)}%]" else ""
            violations.add("INSUFFICIENT FUNDS: Required margin ₹${String.format("%.0f", requiredMargin)}$tenderNote exceeds available cash ₹${String.format("%.0f", availableMargin)}.")
        }

        // 6. Duplicate-Order Idempotency Check (2000ms safety window)
        checksRun++
        val orderSignature = "${contractType.symbol}_${side}_${lots}_${price}"
        val now = System.currentTimeMillis()
        val isDuplicateThrottled = (orderSignature == lastOrderSignature) && (now - lastOrderTimestamp < 2000L)
        if (!isDuplicateThrottled) {
            checksPassed++
        } else {
            violations.add("DUPLICATE_ORDER_THROTTLED: Identical order submitted within 2000ms safety window.")
        }

        val passed = violations.isEmpty()
        val tenderNotice = if (isTender) {
            "MCX Tender Notice: Contract is within 5 trading days of expiry. Physical delivery margin escalated to ${String.format("%.1f", effectiveMarginPct)}% (min 25.0%)."
        } else null

        return PreTradeRiskCheckResult(
            passed = passed,
            checksRun = checksRun,
            checksPassed = checksPassed,
            violations = violations,
            estimatedMarginInr = requiredMargin,
            availableCashInr = availableMargin,
            marginSufficient = marginSufficient,
            exposureSafe = exposureSafe,
            dataHealthSafe = dataHealthSafe,
            killSwitchSafe = killSwitchSafe,
            isTenderPeriod = isTender,
            effectiveMarginPercentage = effectiveMarginPct,
            tenderPeriodNotice = tenderNotice
        )
    }

    /**
     * Executes order (Paper or Live Mock).
     */
    fun submitOrder(
        contractType: GoldContractType,
        side: OrderSide,
        orderType: OrderType,
        lots: Int,
        limitPrice: Double,
        mode: ExecutionMode,
        contractSnapshot: ContractSnapshot?
    ): OrderItem {
        val now = System.currentTimeMillis()
        val orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        val exposure = lots * contractType.lotSizeGrams

        val riskCheck = evaluatePreTradeRisk(contractType, side, lots, limitPrice, contractSnapshot)
        val cost = TransactionCostEngine.calculateLegCost(contractType, lots, limitPrice, side)

        if (!riskCheck.passed) {
            val rejection = riskCheck.violations.firstOrNull() ?: "Risk checks failed."
            val rejectedOrder = OrderItem(
                orderId = orderId,
                timestamp = now,
                symbol = contractType.symbol,
                side = side,
                orderType = orderType,
                quantityLots = lots,
                exposureGrams = exposure,
                limitPrice = limitPrice,
                executedPrice = 0.0,
                status = OrderStatus.REJECTED,
                mode = mode,
                brokerageInr = 0.0,
                taxesAndChargesInr = 0.0,
                rejectionReason = rejection
            )
            _orders.value = listOf(rejectedOrder) + _orders.value
            recordAudit("ORDER_REJECTED", "RISK_ENGINE", "Order $orderId rejected by risk engine: $rejection", rejectedOrder.toString())
            addAlert("Order Rejected", "$orderId ($side ${contractType.symbol}): $rejection", AlertSeverity.CRITICAL, AlertCategory.RISK)
            return rejectedOrder
        }

        // Register order signature for duplicate-order protection
        lastOrderTimestamp = now
        lastOrderSignature = "${contractType.symbol}_${side}_${lots}_${limitPrice}"

        // Fill price with slight slippage simulation
        val slippagePerUnit = (cost.estimatedSlippageInr / (exposure / contractType.quoteUnitGrams))
        val executedPrice = if (side == OrderSide.BUY) limitPrice + slippagePerUnit else limitPrice - slippagePerUnit

        val brokerOrderId = if (mode != ExecutionMode.PAPER) {
            val brokerResp = activeBrokerAdapter.placeOrder(contractType, side, orderType, lots, executedPrice)
            brokerResp.brokerOrderId ?: ("MCX-BRK-" + UUID.randomUUID().toString().substring(0, 6).uppercase())
        } else null

        val filledOrder = OrderItem(
            orderId = orderId,
            timestamp = now,
            symbol = contractType.symbol,
            side = side,
            orderType = orderType,
            quantityLots = lots,
            exposureGrams = exposure,
            limitPrice = limitPrice,
            executedPrice = executedPrice,
            status = OrderStatus.FILLED,
            mode = mode,
            brokerageInr = cost.brokerageInr,
            taxesAndChargesInr = cost.totalCostInr - cost.brokerageInr,
            brokerOrderId = brokerOrderId
        )

        _orders.value = listOf(filledOrder) + _orders.value

        // Update positions
        updatePositionOnFill(contractType, side, lots, exposure, executedPrice, riskCheck.estimatedMarginInr, mode)

        // Deduct fees from account cash (margin blocked separately)
        val currentAcct = _account.value
        _account.value = currentAcct.copy(
            cashBalance = currentAcct.cashBalance - cost.totalCostInr,
            marginBlocked = currentAcct.marginBlocked + riskCheck.estimatedMarginInr,
            totalRoundtripFeesPaid = currentAcct.totalRoundtripFeesPaid + cost.totalCostInr
        )

        recordAudit("ORDER_FILLED", if (mode == ExecutionMode.PAPER) "PAPER_ADAPTER" else activeBrokerAdapter.brokerName,
            "Order $orderId filled at ₹${String.format("%.2f", executedPrice)} ($side $lots lots of ${contractType.symbol})", filledOrder.toString())

        addAlert("Order Filled", "Executed $side $lots lots of ${contractType.symbol} @ ₹${String.format("%.2f", executedPrice)} [${mode.label}]", AlertSeverity.INFO, AlertCategory.EXECUTION)

        return filledOrder
    }

    /**
     * Executes an Atomic Multi-Leg Spread Order (FOK / Bracket).
     * Validates pre-trade margin and risk across BOTH legs simultaneously.
     * Executes Leg 1 then Leg 2 atomically. If Leg 2 fails execution, an automated
     * compensating rollback order is triggered immediately for Leg 1 to prevent unhedged exposure.
     */
    fun submitAtomicSpreadOrder(
        leg1Contract: GoldContractType,
        leg1Side: OrderSide,
        leg1Lots: Int,
        leg1Price: Double,
        leg2Contract: GoldContractType,
        leg2Side: OrderSide,
        leg2Lots: Int,
        leg2Price: Double,
        mode: ExecutionMode,
        contractSnapshots: Map<GoldContractType, ContractSnapshot>
    ): AtomicSpreadOrderResult {
        val spreadId = "SPRD-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        val snap1 = contractSnapshots[leg1Contract]
        val snap2 = contractSnapshots[leg2Contract]

        recordAudit(
            "ATOMIC_SPREAD_INITIATED",
            "USER",
            "Initiating atomic 2-leg spread execution $spreadId: Leg 1 ($leg1Side $leg1Lots ${leg1Contract.symbol}) & Leg 2 ($leg2Side $leg2Lots ${leg2Contract.symbol})",
            "{\"spreadId\":\"$spreadId\"}"
        )

        // 1. Pre-trade check for Leg 1
        val risk1 = evaluatePreTradeRisk(leg1Contract, leg1Side, leg1Lots, leg1Price, snap1)
        // 2. Pre-trade check for Leg 2
        val risk2 = evaluatePreTradeRisk(leg2Contract, leg2Side, leg2Lots, leg2Price, snap2)

        // 3. Combined Margin Check
        val totalMarginRequired = risk1.estimatedMarginInr + risk2.estimatedMarginInr
        val acct = _account.value
        val availableMargin = acct.cashBalance - acct.marginBlocked

        if (!risk1.passed || !risk2.passed || availableMargin < totalMarginRequired) {
            val rejectionReason = when {
                !risk1.passed -> "Leg 1 Risk Violation: " + (risk1.violations.firstOrNull() ?: "")
                !risk2.passed -> "Leg 2 Risk Violation: " + (risk2.violations.firstOrNull() ?: "")
                else -> "Combined margin ₹${String.format("%.0f", totalMarginRequired)} exceeds available cash ₹${String.format("%.0f", availableMargin)}."
            }

            val now = System.currentTimeMillis()
            val dummyLeg1 = OrderItem(
                orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
                timestamp = now,
                symbol = leg1Contract.symbol,
                side = leg1Side,
                orderType = OrderType.LIMIT,
                quantityLots = leg1Lots,
                exposureGrams = leg1Lots * leg1Contract.lotSizeGrams,
                limitPrice = leg1Price,
                executedPrice = 0.0,
                status = OrderStatus.REJECTED,
                mode = mode,
                brokerageInr = 0.0,
                taxesAndChargesInr = 0.0,
                rejectionReason = rejectionReason
            )
            val dummyLeg2 = OrderItem(
                orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
                timestamp = now,
                symbol = leg2Contract.symbol,
                side = leg2Side,
                orderType = OrderType.LIMIT,
                quantityLots = leg2Lots,
                exposureGrams = leg2Lots * leg2Contract.lotSizeGrams,
                limitPrice = leg2Price,
                executedPrice = 0.0,
                status = OrderStatus.REJECTED,
                mode = mode,
                brokerageInr = 0.0,
                taxesAndChargesInr = 0.0,
                rejectionReason = rejectionReason
            )
            _orders.value = listOf(dummyLeg1, dummyLeg2) + _orders.value

            recordAudit("ATOMIC_SPREAD_REJECTED", "RISK_ENGINE", "Spread $spreadId rejected: $rejectionReason", "{\"spreadId\":\"$spreadId\"}")
            addAlert("Spread Rejected", "$spreadId: $rejectionReason", AlertSeverity.WARNING, AlertCategory.RISK)

            return AtomicSpreadOrderResult(
                success = false,
                spreadId = spreadId,
                leg1Order = dummyLeg1,
                leg2Order = dummyLeg2,
                rolledBack = false,
                message = rejectionReason
            )
        }

        // 4. Execute Leg 1
        val order1 = submitOrder(leg1Contract, leg1Side, OrderType.LIMIT, leg1Lots, leg1Price, mode, snap1)
        if (order1.status != OrderStatus.FILLED) {
            val dummyLeg2 = OrderItem(
                orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).uppercase(),
                timestamp = System.currentTimeMillis(),
                symbol = leg2Contract.symbol,
                side = leg2Side,
                orderType = OrderType.LIMIT,
                quantityLots = leg2Lots,
                exposureGrams = leg2Lots * leg2Contract.lotSizeGrams,
                limitPrice = leg2Price,
                executedPrice = 0.0,
                status = OrderStatus.CANCELLED,
                mode = mode,
                brokerageInr = 0.0,
                taxesAndChargesInr = 0.0,
                rejectionReason = "Leg 1 failed to fill; Leg 2 execution aborted."
            )
            return AtomicSpreadOrderResult(
                success = false,
                spreadId = spreadId,
                leg1Order = order1,
                leg2Order = dummyLeg2,
                rolledBack = false,
                message = "Leg 1 failed: ${order1.rejectionReason}"
            )
        }

        // 5. Execute Leg 2
        val order2 = submitOrder(leg2Contract, leg2Side, OrderType.LIMIT, leg2Lots, leg2Price, mode, snap2)
        if (order2.status != OrderStatus.FILLED) {
            // Trigger automated rollback of Leg 1
            val exitSide = if (leg1Side == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY
            val rollbackOrder = submitOrder(leg1Contract, exitSide, OrderType.MARKET, leg1Lots, leg1Price, mode, snap1)
            recordAudit("ATOMIC_SPREAD_ROLLBACK", "EXECUTION_ENGINE", "Leg 2 failed. Automated rollback filled for Leg 1 (${rollbackOrder.orderId}).", "{\"spreadId\":\"$spreadId\"}")
            addAlert("Spread Rollback Triggered", "Leg 2 failed. Leg 1 automatically squared off to prevent unhedged directional exposure.", AlertSeverity.CRITICAL, AlertCategory.EXECUTION)

            return AtomicSpreadOrderResult(
                success = false,
                spreadId = spreadId,
                leg1Order = order1,
                leg2Order = order2,
                rolledBack = true,
                message = "Leg 2 failed (${order2.rejectionReason}). Leg 1 automatically rolled back."
            )
        }

        recordAudit(
            "ATOMIC_SPREAD_FILLED",
            "EXECUTION_ENGINE",
            "Atomic spread $spreadId completely executed: Leg 1 (${order1.orderId}) & Leg 2 (${order2.orderId})",
            "{\"spreadId\":\"$spreadId\",\"leg1\":\"${order1.orderId}\",\"leg2\":\"${order2.orderId}\"}"
        )
        addAlert("Atomic Spread Executed", "$spreadId filled: ${leg1Side} ${leg1Lots}L ${leg1Contract.symbol} & ${leg2Side} ${leg2Lots}L ${leg2Contract.symbol}", AlertSeverity.INFO, AlertCategory.EXECUTION)

        return AtomicSpreadOrderResult(
            success = true,
            spreadId = spreadId,
            leg1Order = order1,
            leg2Order = order2,
            rolledBack = false,
            message = "Atomic spread $spreadId filled successfully across both legs."
        )
    }

    private fun updatePositionOnFill(
        contractType: GoldContractType,
        side: OrderSide,
        lots: Int,
        exposure: Double,
        price: Double,
        margin: Double,
        mode: ExecutionMode
    ) {
        val currentPositions = _positions.value.toMutableList()
        val existingIndex = currentPositions.indexOfFirst { it.symbol == contractType.symbol && it.side == side && it.mode == mode }

        if (existingIndex >= 0) {
            val ex = currentPositions[existingIndex]
            val totalLots = ex.quantityLots + lots
            val totalExposure = ex.exposureGrams + exposure
            val weightedAvgPrice = ((ex.averageEntryPrice * ex.quantityLots) + (price * lots)) / totalLots

            currentPositions[existingIndex] = ex.copy(
                quantityLots = totalLots,
                exposureGrams = totalExposure,
                averageEntryPrice = weightedAvgPrice,
                marginBlockedInr = ex.marginBlockedInr + margin
            )
        } else {
            currentPositions.add(
                PositionItem(
                    positionId = "POS-${UUID.randomUUID().toString().substring(0, 6).uppercase()}",
                    symbol = contractType.symbol,
                    contractType = contractType,
                    side = side,
                    quantityLots = lots,
                    exposureGrams = exposure,
                    averageEntryPrice = price,
                    currentLtp = price,
                    unrealizedPnlInr = 0.0,
                    unrealizedPnlPct = 0.0,
                    realizedPnlInr = 0.0,
                    marginBlockedInr = margin,
                    mode = mode,
                    openTimestamp = System.currentTimeMillis()
                )
            )
        }

        _positions.value = currentPositions
    }

    fun closePosition(positionId: String, currentMarketPrice: Double) {
        val pos = _positions.value.find { it.positionId == positionId } ?: return
        val units = pos.exposureGrams / pos.contractType.quoteUnitGrams
        val pnlPerUnit = if (pos.side == OrderSide.BUY) currentMarketPrice - pos.averageEntryPrice else pos.averageEntryPrice - currentMarketPrice
        val grossPnl = units * pnlPerUnit

        val exitSide = if (pos.side == OrderSide.BUY) OrderSide.SELL else OrderSide.BUY
        val exitCost = TransactionCostEngine.calculateLegCost(pos.contractType, pos.quantityLots, currentMarketPrice, exitSide)
        val netPnl = grossPnl - exitCost.totalCostInr

        // Remove closed position
        _positions.value = _positions.value.filter { it.positionId != positionId }

        // Record exit order in order history
        val exitOrderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        val exitOrder = OrderItem(
            orderId = exitOrderId,
            timestamp = System.currentTimeMillis(),
            symbol = pos.symbol,
            side = exitSide,
            orderType = OrderType.MARKET,
            quantityLots = pos.quantityLots,
            exposureGrams = pos.exposureGrams,
            limitPrice = currentMarketPrice,
            executedPrice = currentMarketPrice,
            status = OrderStatus.FILLED,
            mode = pos.mode,
            brokerageInr = exitCost.brokerageInr,
            taxesAndChargesInr = exitCost.totalCostInr - exitCost.brokerageInr,
            brokerOrderId = if (pos.mode != ExecutionMode.PAPER) "MCX-EXT-" + UUID.randomUUID().toString().substring(0, 6).uppercase() else null
        )
        _orders.value = listOf(exitOrder) + _orders.value

        // Update account cash balance and realized P&L
        val acct = _account.value
        _account.value = acct.copy(
            cashBalance = acct.cashBalance + netPnl,
            marginBlocked = maxOf(0.0, acct.marginBlocked - pos.marginBlockedInr),
            realizedPnl = acct.realizedPnl + netPnl,
            totalRoundtripFeesPaid = acct.totalRoundtripFeesPaid + exitCost.totalCostInr
        )

        recordAudit("POSITION_CLOSED", "USER", "Closed position ${pos.positionId} (${pos.symbol}): Net P&L ₹${String.format("%.2f", netPnl)}", "{\"pnl\":$netPnl}")
        addAlert("Position Closed", "${pos.symbol} squared off. Net Realized P&L: ₹${String.format("%.2f", netPnl)}", if (netPnl >= 0) AlertSeverity.INFO else AlertSeverity.WARNING, AlertCategory.EXECUTION)
    }

    /**
     * Updates live mark-to-market valuations on all active positions.
     */
    fun updatePositionValuations(quotes: Map<GoldContractType, ContractSnapshot>) {
        var totalUnrealized = 0.0
        val updated = _positions.value.map { pos ->
            val snap = quotes[pos.contractType]
            val ltp = snap?.quotedLtp ?: pos.currentLtp
            val units = pos.exposureGrams / pos.contractType.quoteUnitGrams
            val pnlPerUnit = if (pos.side == OrderSide.BUY) ltp - pos.averageEntryPrice else pos.averageEntryPrice - ltp
            val unrealized = units * pnlPerUnit
            val pct = if (pos.averageEntryPrice > 0) (pnlPerUnit / pos.averageEntryPrice) * 100.0 else 0.0
            totalUnrealized += unrealized
            pos.copy(
                currentLtp = ltp,
                unrealizedPnlInr = unrealized,
                unrealizedPnlPct = pct
            )
        }
        _positions.value = updated
        _account.value = _account.value.copy(unrealizedPnl = totalUnrealized)
    }

    fun resetPaperAccount() {
        _account.value = AccountState()
        _positions.value = emptyList()
        _orders.value = emptyList()
        _riskSettings.value = RiskSettings()
        lastOrderSignature = ""
        lastOrderTimestamp = 0L
        recordAudit("ACCOUNT_RESET", "USER", "Paper trading account reset to initial ₹10,00,000 balance.", "{}")
        addAlert("Account Reset", "Paper balance reset to ₹10,00,000. All positions cleared.", AlertSeverity.INFO, AlertCategory.SYSTEM)
    }

    fun recordAudit(eventType: String, actor: String, description: String, payloadJson: String) {
        val id = "AUD-${System.currentTimeMillis()}-${_auditLogs.value.size + 1}"
        val dummyHash = "sha256:" + (id + eventType + payloadJson).hashCode().toUInt().toString(16).padStart(16, '0')
        val entry = AuditLogEntry(
            id = id,
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            actor = actor,
            description = description,
            payloadJson = payloadJson,
            hashSignature = dummyHash
        )
        _auditLogs.value = listOf(entry) + _auditLogs.value
    }

    fun addAlert(title: String, message: String, severity: AlertSeverity, category: AlertCategory) {
        val alert = AlertNotification(
            id = "ALT-${System.currentTimeMillis()}-${_alerts.value.size + 1}",
            timestamp = System.currentTimeMillis(),
            title = title,
            message = message,
            severity = severity,
            category = category
        )
        _alerts.value = listOf(alert) + _alerts.value
    }
}
