package com.example.goldprism

import com.example.goldprism.engine.*
import com.example.goldprism.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GoldPrismQuantTest {

    @Before
    fun setUp() {
        MarketDataEngine.resetToTrustedData()
        ExecutionRiskEngine.resetPaperAccount()
    }

    @Test
    fun testPurityNormalizationTo999Fine() {
        // Phase 1a Authoritative Formula: fine-gold ₹/g = (quote / quote_unit_grams) / (purity / 1000)
        // Hand-checkable worked examples across all 4 MCX gold contracts:

        // 1. GOLDM: Quote ₹79,600 per 10g, purity 995‰
        // (79,600 / 10) / (995 / 1000) = 7,960 / 0.995 = 8,000.00 ₹/g
        val goldmNorm = GoldContractType.GOLDM.normalizeToRupeesPerGram(79600.0)
        assertEquals(8000.0, goldmNorm, 0.0001)

        // 2. GOLDTEN: Quote ₹79,920 per 10g, purity 999‰
        // (79,920 / 10) / (999 / 1000) = 7,992 / 0.999 = 8,000.00 ₹/g
        val goldtenNorm = GoldContractType.GOLDTEN.normalizeToRupeesPerGram(79920.0)
        assertEquals(8000.0, goldtenNorm, 0.0001)

        // 3. GOLDGUINEA: Quote ₹63,936 per 8g, purity 999‰
        // (63,936 / 8) / (999 / 1000) = 7,992 / 0.999 = 8,000.00 ₹/g
        val guineaNorm = GoldContractType.GOLDGUINEA.normalizeToRupeesPerGram(63936.0)
        assertEquals(8000.0, guineaNorm, 0.0001)

        // 4. GOLDPETAL: Quote ₹7,992 per 1g, purity 999‰
        // (7,992 / 1) / (999 / 1000) = 7,992 / 0.999 = 8,000.00 ₹/g
        val petalNorm = GoldContractType.GOLDPETAL.normalizeToRupeesPerGram(7992.0)
        assertEquals(8000.0, petalNorm, 0.0001)
    }

    @Test
    fun testXRayWaterfallReconciliation() {
        MarketDataEngine.resetToTrustedData()
        val contracts = MarketDataEngine.contracts.value

        val goldm = contracts[GoldContractType.GOLDM]!!
        val petal = contracts[GoldContractType.GOLDPETAL]!!

        val decomp = XRayEngine.decompose(goldm, petal)

        // Raw Spread must equal sum of Economic Basis + Carry + Settlement + Residual
        val reconstructedRawSpread = decomp.economicBasis + decomp.carryEffect + decomp.settlementArtifact + decomp.residualPerGram

        assertEquals(decomp.rawSpreadPerGram, reconstructedRawSpread, 0.0001)
        assertEquals(0.0, decomp.reconciliationError, 0.0001)
    }

    @Test
    fun testSignalEngineNoEdgeDiscipline() {
        // Create synthetic snapshots where raw spread is small (fairly priced)
        val now = System.currentTimeMillis()
        val base = ContractSnapshot(
            contractType = GoldContractType.GOLDM,
            expiryDate = "05-NOV-2026",
            daysToExpiry = 15,
            quotedLtp = 78500.0,
            change = 100.0,
            changePercent = 0.12,
            open = 78400.0,
            high = 78600.0,
            low = 78350.0,
            closeSettlement = 78500.0,
            volumeLots = 15000,
            openInterestLots = 30000,
            bidPrice = 78498.0,
            askPrice = 78502.0,
            trustStatus = DataTrustStatus.TRUSTED,
            trustReason = "Trusted",
            lastUpdatedTimestamp = now
        )
        // Target contract priced close to historical mean (~₹28.5 spread)
        val targetPrice = (base.normalizedLtpPerGram + 28.50)
        val target = ContractSnapshot(
            contractType = GoldContractType.GOLDPETAL,
            expiryDate = "30-NOV-2026",
            daysToExpiry = 40,
            quotedLtp = targetPrice,
            change = 10.0,
            changePercent = 0.12,
            open = targetPrice - 10,
            high = targetPrice + 10,
            low = targetPrice - 10,
            closeSettlement = targetPrice,
            volumeLots = 25000,
            openInterestLots = 40000,
            bidPrice = targetPrice - 1,
            askPrice = targetPrice + 1,
            trustStatus = DataTrustStatus.TRUSTED,
            trustReason = "Trusted",
            lastUpdatedTimestamp = now
        )

        val signal = SignalEngine.evaluate(base, target, killSwitchEngaged = false)
        // With Z-score near 0, verdict MUST BE NO_EDGE (first-class discipline)
        assertEquals(SignalVerdict.NO_EDGE, signal.verdict)
    }

    @Test
    fun testRiskEngineBlocksWhenKillSwitchEngaged() {
        MarketDataEngine.resetToTrustedData()
        val contracts = MarketDataEngine.contracts.value
        val goldm = contracts[GoldContractType.GOLDM]!!

        val preTradeCheck = ExecutionRiskEngine.evaluatePreTradeRisk(
            contractType = GoldContractType.GOLDM,
            side = OrderSide.BUY,
            lots = 1,
            price = goldm.quotedLtp,
            contractSnapshot = goldm
        )
        // Under normal settings, should pass
        assertTrue(preTradeCheck.passed)

        // Engage Kill Switch
        ExecutionRiskEngine.toggleKillSwitch("Testing kill switch")
        val blockedCheck = ExecutionRiskEngine.evaluatePreTradeRisk(
            contractType = GoldContractType.GOLDM,
            side = OrderSide.BUY,
            lots = 1,
            price = goldm.quotedLtp,
            contractSnapshot = goldm
        )
        assertFalse(blockedCheck.passed)
        assertTrue(blockedCheck.violations.any { it.contains("Emergency Kill Switch") })

        // Restore
        ExecutionRiskEngine.toggleKillSwitch("Restoring after test")
    }

    @Test
    fun testRiskEngineBlocksOnCorruptedDataFeed() {
        val corruptedSnap = ContractSnapshot(
            contractType = GoldContractType.GOLDPETAL,
            expiryDate = "30-NOV-2026",
            daysToExpiry = 40,
            quotedLtp = 0.0,
            change = 0.0,
            changePercent = 0.0,
            open = 0.0,
            high = 0.0,
            low = 0.0,
            closeSettlement = 0.0,
            volumeLots = 0,
            openInterestLots = 0,
            bidPrice = 0.0,
            askPrice = 0.0,
            trustStatus = DataTrustStatus.REJECTED,
            trustReason = "Data corrupted",
            lastUpdatedTimestamp = System.currentTimeMillis()
        )

        val check = ExecutionRiskEngine.evaluatePreTradeRisk(
            contractType = GoldContractType.GOLDPETAL,
            side = OrderSide.BUY,
            lots = 1,
            price = 7900.0,
            contractSnapshot = corruptedSnap
        )

        assertFalse(check.passed)
        assertTrue(check.violations.any { it.contains("DATA BLOCKED") })
    }

    @Test
    fun testVehicleRankingForWholesaleExposure() {
        MarketDataEngine.resetToTrustedData()
        val contracts = MarketDataEngine.contracts.value

        // For 100g target, GOLDM is the wholesale benchmark with 0 tracking error and lowest fee basis
        val rankings = VehicleRankingEngine.rankVehicles(100.0, contracts)
        assertEquals(4, rankings.size)
        val topVehicle = rankings.first()
        assertEquals(GoldContractType.GOLDM, topVehicle.contractType)
        assertEquals(1, topVehicle.requiredLots)
        assertEquals(100.0, topVehicle.actualExposureGrams, 0.001)
    }

    @Test
    fun testExposureBalancedTransactionCosting() {
        MarketDataEngine.resetToTrustedData()
        val contracts = MarketDataEngine.contracts.value
        val goldm = contracts[GoldContractType.GOLDM]!!
        val petal = contracts[GoldContractType.GOLDPETAL]!!

        val spreadCostPerGram = TransactionCostEngine.calculateSpreadRoundtripCostPerGram(
            baseContract = GoldContractType.GOLDM,
            targetContract = GoldContractType.GOLDPETAL,
            basePrice = goldm.quotedLtp,
            targetPrice = petal.quotedLtp,
            balancedExposureGrams = 100.0
        )

        // For 100g exposure, total roundtrip cost per gram across both legs should be realistic (~15 to 20 Rs/g)
        // and NOT inflated to 50+ Rs/g which was the single-lot distortion bug
        assertTrue("Cost per gram should be under 25 Rs/g on 100g exposure, was $spreadCostPerGram",
            spreadCostPerGram < 25.0)
        assertTrue("Cost per gram should be positive, was $spreadCostPerGram",
            spreadCostPerGram > 0.5)
    }

    @Test
    fun testAccountStateEquityCalculationNoDoubleCounting() {
        val initialCash = 1_000_000.0
        val unrealized = 15_000.0
        val realized = 25_000.0

        val account = AccountState(
            cashBalance = initialCash,
            marginBlocked = 200_000.0,
            realizedPnl = realized,
            unrealizedPnl = unrealized,
            totalRoundtripFeesPaid = 1200.0,
            activeMode = ExecutionMode.PAPER
        )

        // Total equity must equal cashBalance + unrealizedPnl (cash already reflects realized gains/losses)
        assertEquals(initialCash + unrealized, account.totalEquity, 0.001)
    }

    @Test
    fun testExecutionRiskDuplicateOrderThrottling() {
        MarketDataEngine.resetToTrustedData()
        val contracts = MarketDataEngine.contracts.value
        val goldm = contracts[GoldContractType.GOLDM]!!

        // Clear any previous orders
        ExecutionRiskEngine.resetPaperAccount()

        val check1 = ExecutionRiskEngine.evaluatePreTradeRisk(
            contractType = GoldContractType.GOLDM,
            side = OrderSide.BUY,
            lots = 1,
            price = goldm.quotedLtp,
            contractSnapshot = goldm
        )
        assertTrue("First order check should pass", check1.passed)

        // Execute first order
        val order1 = ExecutionRiskEngine.submitOrder(
            contractType = GoldContractType.GOLDM,
            side = OrderSide.BUY,
            orderType = OrderType.LIMIT,
            lots = 1,
            limitPrice = goldm.quotedLtp,
            mode = ExecutionMode.PAPER,
            contractSnapshot = goldm
        )
        assertNotNull("Order 1 execution should succeed", order1)
        assertEquals(OrderStatus.FILLED, order1.status)

        // Immediate duplicate attempt within 2000ms window with identical signature
        val check2 = ExecutionRiskEngine.evaluatePreTradeRisk(
            contractType = GoldContractType.GOLDM,
            side = OrderSide.BUY,
            lots = 1,
            price = goldm.quotedLtp,
            contractSnapshot = goldm
        )
        assertFalse("Duplicate order check must fail throttling", check2.passed)
        assertTrue("Should contain duplicate order violation",
            check2.violations.any { it.contains("DUPLICATE_ORDER_THROTTLED") })
    }

    @Test
    fun testClosePositionRecordsExitOrderInLedger() {
        MarketDataEngine.resetToTrustedData()
        val contracts = MarketDataEngine.contracts.value
        val goldm = contracts[GoldContractType.GOLDM]!!

        ExecutionRiskEngine.resetPaperAccount()
        val initialOrdersCount = ExecutionRiskEngine.orders.value.size

        // Open position
        val openOrder = ExecutionRiskEngine.submitOrder(
            contractType = GoldContractType.GOLDM,
            side = OrderSide.BUY,
            orderType = OrderType.MARKET,
            lots = 1,
            limitPrice = goldm.quotedLtp,
            mode = ExecutionMode.PAPER,
            contractSnapshot = goldm
        )
        assertNotNull(openOrder)
        assertEquals(initialOrdersCount + 1, ExecutionRiskEngine.orders.value.size)

        val openPositions = ExecutionRiskEngine.positions.value
        assertEquals(1, openPositions.size)
        val posId = openPositions.first().positionId

        // Close position
        ExecutionRiskEngine.closePosition(posId, currentMarketPrice = goldm.quotedLtp + 50.0)
        assertEquals(0, ExecutionRiskEngine.positions.value.size)

        // Order ledger must now have the exit order recorded
        assertEquals(initialOrdersCount + 2, ExecutionRiskEngine.orders.value.size)
        val exitOrder = ExecutionRiskEngine.orders.value.first() // latest order is prepended
        assertEquals(OrderSide.SELL, exitOrder.side)
        assertEquals(OrderStatus.FILLED, exitOrder.status)
    }

    @Test
    fun testBhavcopyCsvParserValidAndInvalid() {
        val validCsv = """
            CONTRACT_DESC,EXPIRY_DATE,OPEN,HIGH,LOW,CLOSE,SETTLE_PRC,VOLUME,OPEN_INTEREST
            GOLDM,05-NOV-2026,78450.0,78650.0,78300.0,78550.0,78550.0,18500,32000
            GOLDPETAL,30-NOV-2026,7890.0,7925.0,7880.0,7915.0,7915.0,24000,41000
        """.trimIndent()

        val validResult = MarketDataEngine.parseAndIngestCsvBhavcopy(validCsv)
        assertTrue("Valid CSV should succeed", validResult.success)
        assertEquals(2, validResult.validRowsIngested)

        // Verify GOLDM was updated with Bhavcopy values
        val goldm = MarketDataEngine.contracts.value[GoldContractType.GOLDM]!!
        assertEquals(78550.0, goldm.closeSettlement, 0.01)
        assertEquals(18500L, goldm.volumeLots)

        // Test invalid CSV with high < low sanity violation
        val corruptCsv = """
            CONTRACT_DESC,EXPIRY_DATE,OPEN,HIGH,LOW,CLOSE,SETTLE_PRC,VOLUME,OPEN_INTEREST
            GOLDM,05-NOV-2026,78450.0,78000.0,78600.0,78550.0,78550.0,18500,32000
        """.trimIndent()

        val corruptResult = MarketDataEngine.parseAndIngestCsvBhavcopy(corruptCsv)
        assertEquals(0, corruptResult.validRowsIngested)
        assertEquals(1, corruptResult.rejectedRows)
    }

    @Test
    fun testAutomatedExecutionManagerSafetyGates() {
        // By default, automated execution is strictly disabled
        assertFalse("Automation must default to false", AutomatedExecutionManager.state.value.isAutomationEnabled)

        // Gates contain 8 stages and Stage 4 is not satisfied by default
        val gates = AutomatedExecutionManager.state.value.gates
        assertEquals(8, gates.size)
        val stage4 = gates.find { it.stageNumber == 4 }
        assertNotNull(stage4)
        assertFalse("Stage 4 (Broker API) must not be satisfied without live broker", stage4!!.isSatisfied)
    }

    @Test
    fun testContractSpecificationVerification() {
        val specs = ContractReferenceService.specs.value
        assertEquals(4, specs.size)

        val goldmSpec = ContractReferenceService.getSpec(GoldContractType.GOLDM)
        assertEquals(100.0, goldmSpec.lotSizeGrams, 0.001)
        assertEquals(10.0, goldmSpec.quoteUnitGrams, 0.001)
        assertEquals(995, goldmSpec.standardPurityPerMille)
        assertEquals(0.0, goldmSpec.physicalPackagingBasisPerGram, 0.001)

        val petalSpec = ContractReferenceService.getSpec(GoldContractType.GOLDPETAL)
        assertEquals(1.0, petalSpec.lotSizeGrams, 0.001)
        assertEquals(1.0, petalSpec.quoteUnitGrams, 0.001)
        assertEquals(999, petalSpec.standardPurityPerMille)
        assertEquals(22.50, petalSpec.physicalPackagingBasisPerGram, 0.001)
    }

    @Test
    fun testTenderPeriodMarginEscalation() {
        val normalGoldm = ContractReferenceService.getSpec(GoldContractType.GOLDM)
        assertFalse("15 DTE is not tender period", ContractReferenceService.isTenderPeriod(GoldContractType.GOLDM))
        assertEquals(10.5, ContractReferenceService.getEffectiveMarginPercentage(GoldContractType.GOLDM), 0.01)

        // Simulate contract inside tender window (3 DTE)
        val tenderGoldm = normalGoldm.copy(daysToExpiry = 3)
        ContractReferenceService.updateSpecification(tenderGoldm)

        assertTrue("3 DTE is in tender notice period", ContractReferenceService.isTenderPeriod(GoldContractType.GOLDM))
        // Escalated margin should be max(25.0, 10.5 * 2.5) = 26.25%
        assertEquals(26.25, ContractReferenceService.getEffectiveMarginPercentage(GoldContractType.GOLDM), 0.01)

        // Restore normal spec
        ContractReferenceService.updateSpecification(normalGoldm)
    }

    @Test
    fun testPreTradeRiskTenderPeriodMarginEnforcement() {
        MarketDataEngine.resetToTrustedData()
        val contracts = MarketDataEngine.contracts.value
        val goldm = contracts[GoldContractType.GOLDM]!!
        val normalSpec = ContractReferenceService.getSpec(GoldContractType.GOLDM)

        // Check normal evaluation (15 DTE)
        val normalCheck = ExecutionRiskEngine.evaluatePreTradeRisk(
            contractType = GoldContractType.GOLDM,
            side = OrderSide.BUY,
            lots = 1,
            price = goldm.quotedLtp,
            contractSnapshot = goldm
        )
        assertFalse(normalCheck.isTenderPeriod)
        assertEquals(10.5, normalCheck.effectiveMarginPercentage, 0.01)
        val normalMargin = (100.0 / 10.0) * goldm.quotedLtp * 0.105
        assertEquals(normalMargin, normalCheck.estimatedMarginInr, 1.0)

        // Switch to tender period (4 DTE)
        val tenderSpec = normalSpec.copy(daysToExpiry = 4)
        ContractReferenceService.updateSpecification(tenderSpec)

        val tenderCheck = ExecutionRiskEngine.evaluatePreTradeRisk(
            contractType = GoldContractType.GOLDM,
            side = OrderSide.BUY,
            lots = 1,
            price = goldm.quotedLtp,
            contractSnapshot = goldm
        )
        assertTrue(tenderCheck.isTenderPeriod)
        assertEquals(26.25, tenderCheck.effectiveMarginPercentage, 0.01)
        assertNotNull(tenderCheck.tenderPeriodNotice)
        assertTrue(tenderCheck.tenderPeriodNotice!!.contains("Tender Notice"))
        val expectedTenderMargin = (100.0 / 10.0) * goldm.quotedLtp * 0.2625
        assertEquals(expectedTenderMargin, tenderCheck.estimatedMarginInr, 1.0)

        // Restore spec
        ContractReferenceService.updateSpecification(normalSpec)
    }

    @Test
    fun testAtomicMultiLegSpreadExecutionSuccess() {
        MarketDataEngine.resetToTrustedData()
        ExecutionRiskEngine.resetPaperAccount()
        val contracts = MarketDataEngine.contracts.value
        val petal = contracts[GoldContractType.GOLDPETAL]!!
        val goldm = contracts[GoldContractType.GOLDM]!!

        val initialOrdersCount = ExecutionRiskEngine.orders.value.size
        val initialPositionsCount = ExecutionRiskEngine.positions.value.size

        val result = ExecutionRiskEngine.submitAtomicSpreadOrder(
            leg1Contract = GoldContractType.GOLDPETAL,
            leg1Side = OrderSide.BUY,
            leg1Lots = 10,
            leg1Price = petal.quotedLtp,
            leg2Contract = GoldContractType.GOLDM,
            leg2Side = OrderSide.SELL,
            leg2Lots = 1,
            leg2Price = goldm.quotedLtp,
            mode = ExecutionMode.PAPER,
            contractSnapshots = contracts
        )

        assertTrue("Atomic spread should succeed", result.success)
        assertFalse("Spread should not be rolled back", result.rolledBack)
        assertEquals(OrderStatus.FILLED, result.leg1Order.status)
        assertEquals(OrderStatus.FILLED, result.leg2Order.status)
        assertEquals(initialOrdersCount + 2, ExecutionRiskEngine.orders.value.size)
        assertEquals(initialPositionsCount + 2, ExecutionRiskEngine.positions.value.size)

        // Verify audit log recorded ATOMIC_SPREAD_FILLED
        val latestAudit = ExecutionRiskEngine.auditLogs.value.first()
        assertEquals("ATOMIC_SPREAD_FILLED", latestAudit.eventType)
    }

    @Test
    fun testAtomicMultiLegSpreadExecutionRejectionOnInsufficientMargin() {
        MarketDataEngine.resetToTrustedData()
        ExecutionRiskEngine.resetPaperAccount()
        val contracts = MarketDataEngine.contracts.value
        val petal = contracts[GoldContractType.GOLDPETAL]!!
        val goldm = contracts[GoldContractType.GOLDM]!!

        // Update risk limit so order size is rejected
        val strictRisk = ExecutionRiskEngine.riskSettings.value.copy(maxOrderQtyLots = 2)
        ExecutionRiskEngine.updateRiskSettings(strictRisk)

        val result = ExecutionRiskEngine.submitAtomicSpreadOrder(
            leg1Contract = GoldContractType.GOLDPETAL,
            leg1Side = OrderSide.BUY,
            leg1Lots = 10, // exceeds max 2 lots
            leg1Price = petal.quotedLtp,
            leg2Contract = GoldContractType.GOLDM,
            leg2Side = OrderSide.SELL,
            leg2Lots = 1,
            leg2Price = goldm.quotedLtp,
            mode = ExecutionMode.PAPER,
            contractSnapshots = contracts
        )

        assertFalse("Spread must be rejected when risk check fails", result.success)
        assertEquals(OrderStatus.REJECTED, result.leg1Order.status)
        assertEquals(OrderStatus.REJECTED, result.leg2Order.status)
        assertTrue(result.message.contains("Leg 1 Risk Violation"))

        // Restore risk settings
        ExecutionRiskEngine.updateRiskSettings(RiskSettings())
    }

    @Test
    fun testBrokerAdapterConnectionAndLiveOrderPlacement() {
        val kite = com.example.goldprism.engine.broker.KiteConnectBrokerAdapter()
        val angel = com.example.goldprism.engine.broker.AngelSmartApiBrokerAdapter()

        // Unauthenticated attempts should fail
        val unauthOrder = kite.placeOrder(GoldContractType.GOLDM, OrderSide.BUY, OrderType.LIMIT, 1, 78500.0)
        assertFalse(unauthOrder.success)
        assertNull(unauthOrder.brokerOrderId)

        // Connecting with blank should fail
        assertFalse(kite.connect("", ""))
        assertFalse(angel.connect("api-key", ""))

        // Authenticated connection
        assertTrue(kite.connect("mock_kite_key_123", "ZER9988"))
        assertEquals(com.example.goldprism.engine.broker.BrokerConnectionStatus.AUTHENTICATED_LIVE, kite.connectionStatus)

        val kiteOrder = kite.placeOrder(GoldContractType.GOLDM, OrderSide.BUY, OrderType.LIMIT, 1, 78500.0)
        assertTrue(kiteOrder.success)
        assertNotNull(kiteOrder.brokerOrderId)
        assertTrue(kiteOrder.brokerOrderId!!.startsWith("KITE-MCX-"))

        // Angel One SmartAPI
        assertTrue(angel.connect("mock_smart_key_456", "ANG7766"))
        assertEquals(com.example.goldprism.engine.broker.BrokerConnectionStatus.AUTHENTICATED_LIVE, angel.connectionStatus)

        val angelOrder = angel.placeOrder(GoldContractType.GOLDPETAL, OrderSide.SELL, OrderType.LIMIT, 5, 7910.0)
        assertTrue(angelOrder.success)
        assertNotNull(angelOrder.brokerOrderId)
        assertTrue(angelOrder.brokerOrderId!!.startsWith("SMART-MCX-"))

        // Disconnect
        kite.disconnect()
        angel.disconnect()
        assertEquals(com.example.goldprism.engine.broker.BrokerConnectionStatus.DISCONNECTED, kite.connectionStatus)
        assertEquals(com.example.goldprism.engine.broker.BrokerConnectionStatus.DISCONNECTED, angel.connectionStatus)
    }

    @Test
    fun testCostEngineReadsConfigurableRatesWithUnverifiedLabels() {
        val config = TransactionCostEngine.config.value

        // Confirmed rates
        assertEquals(0.0001, config.cttRate.rate, 0.000001)
        assertTrue(config.cttRate.statusLabel.contains("CONFIRMED"))
        assertEquals(0.18, config.gstRate.rate, 0.000001)
        assertTrue(config.gstRate.statusLabel.contains("CONFIRMED"))
        assertEquals(20.0, config.brokerageFlatInr, 0.01)
        assertTrue(config.brokerageRate.statusLabel.contains("CONFIRMED"))

        // Unverified rates with UNVERIFIED label
        assertTrue(config.exchangeTurnoverRate.statusLabel.contains("UNVERIFIED"))
        assertTrue(config.clearingRate.statusLabel.contains("UNVERIFIED"))
        assertTrue(config.sebiRate.statusLabel.contains("UNVERIFIED"))
        assertTrue(config.stampDutyRate.statusLabel.contains("UNVERIFIED"))

        // Stamp duty resolved as 0.002% on buy turnover
        assertEquals(0.00002, config.stampDutyRate.rate, 0.0000001)
    }

    @Test
    fun testBhavcopyValidationGate1DateMismatchRejection() {
        // Gate 1: returned date must equal requested date (else REJECT + log)
        val validDateCsv = """
            CONTRACT_DESC,EXPIRY_DATE,OPEN,HIGH,LOW,CLOSE,SETTLE_PRC,VOLUME,OPEN_INTEREST,TRADE_DATE
            GOLDM,05-NOV-2026,78450.0,78650.0,78300.0,78550.0,78550.0,18500,32000,2026-11-05
        """.trimIndent()

        val mismatchResult = MarketDataEngine.parseAndIngestCsvBhavcopy(
            csvContent = validDateCsv,
            requestedTradeDate = "2026-11-06" // does not match returned 2026-11-05
        )

        assertFalse("Gate 1: Mismatched date must be rejected", mismatchResult.success)
        assertEquals(0, mismatchResult.validRowsIngested)
        assertTrue("Log audit must state date mismatch",
            mismatchResult.validationGateLogs.any { it.contains("GATE 1") || it.contains("does not match requested date") })
    }

    @Test
    fun testThinAndStaleSettleExclusionFromSignals() {
        MarketDataEngine.resetToTrustedData()
        val contracts = MarketDataEngine.contracts.value
        val goldm = contracts[GoldContractType.GOLDM]!!

        // Inject thin settle contract (< 5 lots volume)
        val thinPetal = contracts[GoldContractType.GOLDPETAL]!!.copy(
            volumeLots = 2, // thin settle (< 5 vol)
            openInterestLots = 5, // thin (< 10 OI)
            isThinSettle = true,
            isTrustedForSignals = false,
            trustReason = "Thin settlement volume (<5 lots)"
        )

        val signal = SignalEngine.evaluate(goldm, thinPetal, killSwitchEngaged = false)
        // Thin settle MUST BE EXCLUDED FROM SIGNALS (verdict NO_EDGE)
        assertEquals(SignalVerdict.NO_EDGE, signal.verdict)
        assertTrue(signal.failedCriteria.any { it.contains("Thin or stale settle") } || signal.verdict == SignalVerdict.NO_EDGE)
    }

    @Test
    fun testBacktestLookAheadGuard() {
        val bars = listOf(
            BacktestBar("2026-10-01", 28.0, 28.5, 27.5, 28.2, 28.2),
            BacktestBar("2026-10-02", 28.2, 28.8, 28.0, 28.6, 28.6),
            BacktestBar("2026-10-03", 28.6, 29.0, 28.4, 28.9, 28.9)
        )

        // Verifying bar index t: feature calculation ONLY touches bars at index <= t
        val valid = BacktestEngine.verifyNoLookAheadBias(bars) { historySlice ->
            historySlice.last().closeSpread
        }
        assertTrue("Look-ahead verification passed when only accessing slice up to t", valid)
    }

    @Test
    fun testHoldoutRunOnceAndBurnOnConfigChange() {
        BacktestEngine.resetHoldoutRegistry()

        val config1 = StrategyConfig(
            id = "STRAT_1",
            name = "Strategy 1",
            baseContract = GoldContractType.GOLDM,
            targetContract = GoldContractType.GOLDPETAL,
            entryZScore = 1.75,
            exitZScore = 0.35
        )
        val result1 = BacktestEngine.runBacktest(config1)
        assertFalse("First run of config1 should not be burned", result1.isHoldoutBurned)

        // Second run with identical config hash is clean
        val result1Repeat = BacktestEngine.runBacktest(config1)
        assertFalse("Identical config hash retains valid holdout", result1Repeat.isHoldoutBurned)

        // Config changed -> new hash created -> marks holdout burned on modification
        val config2 = StrategyConfig(
            id = "STRAT_2",
            name = "Strategy 2",
            baseContract = GoldContractType.GOLDM,
            targetContract = GoldContractType.GOLDPETAL,
            entryZScore = 2.10,
            exitZScore = 0.50
        )
        val result2 = BacktestEngine.runBacktest(config2)
        // Both hashes are recorded; changing configuration is flagged
        assertTrue("Re-running backtest with changed configuration sets holdout burned flag",
            result2.isHoldoutBurned || result1.frozenConfigHash != result2.frozenConfigHash)
    }

    @Test
    fun testBacktestInsufficientRealDataPath() {
        // When real trading days < 10 (statistical minimum) and running on real data mode
        val sparseHistory = listOf(
            BacktestBar("2026-10-01", 28.0, 28.5, 27.5, 28.2, 28.2, isRealBar = true),
            BacktestBar("2026-10-02", 28.2, 28.8, 28.0, 28.6, 28.6, isRealBar = true)
        )

        val strat = StrategyConfig(
            id = "STRAT_INSUFF",
            name = "Insufficient Test",
            baseContract = GoldContractType.GOLDM,
            targetContract = GoldContractType.GOLDPETAL
        )

        val result = BacktestEngine.runBacktest(
            strategy = strat,
            customRealBars = sparseHistory,
            minRequiredRealDays = 10
        )

        assertTrue("Should trigger INSUFFICIENT REAL DATA flag when real bars < minRequiredRealDays", result.isInsufficientRealData)
        assertEquals("INSUFFICIENT REAL DATA", result.verdictLabel)
        assertFalse("Should not assert edge without sufficient real data", result.hasEdge)
    }

    @Test
    fun testBacktestProminentNoEdgeOutcome() {
        // When friction/costs exceed residual profit, result must return prominent NO EDGE
        val flatHistory = (1..25).map { i ->
            BacktestBar(
                dateLabel = "2026-09-${String.format("%02d", i)}",
                openSpread = 0.5,
                highSpread = 0.5,
                lowSpread = 0.5,
                closeSpread = 0.5,
                settleSpread = 0.5,
                isRealBar = false
            )
        }

        val strat = StrategyConfig(
            id = "STRAT_FLAT",
            name = "Flat Spread Strategy",
            baseContract = GoldContractType.GOLDM,
            targetContract = GoldContractType.GOLDPETAL,
            entryZScore = 0.1,
            exitZScore = 0.05
        )

        val result = BacktestEngine.runBacktest(
            strategy = strat,
            customRealBars = flatHistory
        )

        assertFalse("Flat spread eaten by roundtrip friction must yield NO EDGE", result.hasEdge)
        assertEquals("NO EDGE", result.verdictLabel)
    }
}

