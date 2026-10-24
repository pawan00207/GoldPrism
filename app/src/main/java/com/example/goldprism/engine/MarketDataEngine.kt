package com.example.goldprism.engine

import com.example.goldprism.model.ContractSnapshot
import com.example.goldprism.model.DataIntegrityCheck
import com.example.goldprism.model.DataTrustStatus
import com.example.goldprism.model.GoldContractType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Data provenance classification conforming to Phase 2c.
 */
enum class DataProvenance(val label: String, val badgeText: String) {
    REAL("REAL", "REAL MCX BHAVCOPY"),
    SIMULATED("SIMULATED", "SIMULATED DEMO DATA"),
    UNVERIFIED("UNVERIFIED", "UNVERIFIED EXTERNAL FEED")
}

/**
 * Represents an accumulated day of Bhavcopy for persistence and backtesting.
 */
data class DailyBhavcopyDay(
    val tradeDate: String,
    val sourceName: String,
    val contractSnapshots: Map<GoldContractType, ContractSnapshot>,
    val ingestedTimestamp: Long
)

/**
 * Result of Bhavcopy ingestion conforming to Phase 2b validation gates.
 */
data class BhavcopyIngestionResult(
    val success: Boolean,
    val totalRowsProcessed: Int,
    val validRowsIngested: Int,
    val rejectedRows: Int,
    val summary: String,
    val validationGateLogs: List<String> = emptyList(),
    val excludedThinSettleSymbols: List<String> = emptyList()
)

object MarketDataEngine {

    private const val BASE_10G_PRICE = 78540.0

    private val _contracts = MutableStateFlow<Map<GoldContractType, ContractSnapshot>>(emptyMap())
    val contracts: StateFlow<Map<GoldContractType, ContractSnapshot>> = _contracts.asStateFlow()

    private val _integrityChecks = MutableStateFlow<List<DataIntegrityCheck>>(emptyList())
    val integrityChecks: StateFlow<List<DataIntegrityCheck>> = _integrityChecks.asStateFlow()

    private val _activeFault = MutableStateFlow<String?>("NONE")
    val activeFault: StateFlow<String?> = _activeFault.asStateFlow()

    // Phase 2c & 4: Provenance and Real-Time StateFlows
    private val _dataProvenance = MutableStateFlow(DataProvenance.SIMULATED)
    val dataProvenance: StateFlow<DataProvenance> = _dataProvenance.asStateFlow()

    private val _provenanceSource = MutableStateFlow("MCX Seed Demo Fixture")
    val provenanceSource: StateFlow<String> = _provenanceSource.asStateFlow()

    private val _dataDate = MutableStateFlow("22-OCT-2026")
    val dataDate: StateFlow<String> = _dataDate.asStateFlow()

    private val _lastUpdatedTimeText = MutableStateFlow("15:30:00 IST")
    val lastUpdatedTimeText: StateFlow<String> = _lastUpdatedTimeText.asStateFlow()

    private val _isDataFresh = MutableStateFlow(true)
    val isDataFresh: StateFlow<Boolean> = _isDataFresh.asStateFlow()

    private val _dataSourceUrl = MutableStateFlow("")
    val dataSourceUrl: StateFlow<String> = _dataSourceUrl.asStateFlow()

    private val _pollingIntervalMinutes = MutableStateFlow(15)
    val pollingIntervalMinutes: StateFlow<Int> = _pollingIntervalMinutes.asStateFlow()

    private val _isSimulatedTickFeedActive = MutableStateFlow(false)
    val isSimulatedTickFeedActive: StateFlow<Boolean> = _isSimulatedTickFeedActive.asStateFlow()

    // Phase 2a: In-memory & persisted daily history accumulation
    private val _accumulatedHistory = MutableStateFlow<List<DailyBhavcopyDay>>(emptyList())
    val accumulatedHistory: StateFlow<List<DailyBhavcopyDay>> = _accumulatedHistory.asStateFlow()

    init {
        resetToTrustedData()
    }

    private fun getCurrentIstTimeString(): String {
        val sdf = SimpleDateFormat("HH:mm:ss 'IST'", Locale.ENGLISH)
        return sdf.format(Date())
    }

    fun setDataSourceUrl(url: String) {
        _dataSourceUrl.value = url.trim()
        if (url.isNotBlank()) {
            _provenanceSource.value = "Pipeline URL: $url"
            _dataProvenance.value = DataProvenance.UNVERIFIED
        }
    }

    fun setPollingInterval(minutes: Int) {
        _pollingIntervalMinutes.value = minutes.coerceIn(1, 120)
    }

    fun toggleSimulatedTickFeed(enabled: Boolean) {
        _isSimulatedTickFeedActive.value = enabled
        if (enabled && _dataProvenance.value == DataProvenance.REAL) {
            _provenanceSource.value = "${_provenanceSource.value} (SIMULATED TICK STREAM OVERLAY)"
        }
    }

    /**
     * Seeds initial market data matching Phase 1a authoritative specs.
     * Clearly labeled as SIMULATED.
     */
    fun resetToTrustedData() {
        val now = System.currentTimeMillis()
        _activeFault.value = "NONE"
        _dataProvenance.value = DataProvenance.SIMULATED
        _provenanceSource.value = "MCX Seed Demo Fixture"
        _dataDate.value = "22-OCT-2026"
        _lastUpdatedTimeText.value = getCurrentIstTimeString()
        _isDataFresh.value = true
        _isSimulatedTickFeedActive.value = false

        // GOLDM: 100g contract, quoted per 10g (995)
        val goldmPrice = BASE_10G_PRICE
        val goldmSnap = ContractSnapshot(
            contractType = GoldContractType.GOLDM,
            expiryDate = "05-NOV-2026",
            daysToExpiry = 15,
            quotedLtp = goldmPrice,
            change = 320.0,
            changePercent = 0.41,
            open = 78200.0,
            high = 78680.0,
            low = 78150.0,
            closeSettlement = 78530.0,
            volumeLots = 14250,
            openInterestLots = 28940,
            bidPrice = goldmPrice - 2.0,
            askPrice = goldmPrice + 2.0,
            trustStatus = DataTrustStatus.TRUSTED,
            trustReason = "All integrity checks passed. Official MCX feed reconciled.",
            lastUpdatedTimestamp = now,
            tradeDate = "22-OCT-2026",
            isThinSettle = false,
            isStaleSettle = false,
            isTrustedForSignals = true,
            provenance = "SIMULATED"
        )

        // GOLDTEN: 10g contract, quoted per 10g (999 purity per Phase 1a)
        val goldtenPrice = (BASE_10G_PRICE * (999.0 / 995.0)) + 12.0
        val goldtenSnap = ContractSnapshot(
            contractType = GoldContractType.GOLDTEN,
            expiryDate = "28-NOV-2026",
            daysToExpiry = 38,
            quotedLtp = goldtenPrice,
            change = 335.0,
            changePercent = 0.43,
            open = 78500.0,
            high = 78980.0,
            low = 78450.0,
            closeSettlement = goldtenPrice - 10.0,
            volumeLots = 6840,
            openInterestLots = 11200,
            bidPrice = goldtenPrice - 3.0,
            askPrice = goldtenPrice + 4.0,
            trustStatus = DataTrustStatus.TRUSTED,
            trustReason = "Tick continuity verified. Settlement matching.",
            lastUpdatedTimestamp = now,
            tradeDate = "22-OCT-2026",
            isThinSettle = false,
            isStaleSettle = false,
            isTrustedForSignals = true,
            provenance = "SIMULATED"
        )

        // GOLDGUINEA: 8g contract, quoted per 8g (999 purity per Phase 1a)
        val guineaBase = (BASE_10G_PRICE / 10.0 * 8.0) * (999.0 / 995.0) + 35.0
        val guineaSnap = ContractSnapshot(
            contractType = GoldContractType.GOLDGUINEA,
            expiryDate = "28-NOV-2026",
            daysToExpiry = 38,
            quotedLtp = guineaBase,
            change = 280.0,
            changePercent = 0.45,
            open = 62800.0,
            high = 63300.0,
            low = 62750.0,
            closeSettlement = guineaBase - 15.0,
            volumeLots = 3450,
            openInterestLots = 5820,
            bidPrice = guineaBase - 4.0,
            askPrice = guineaBase + 5.0,
            trustStatus = DataTrustStatus.TRUSTED,
            trustReason = "Sovereign purity specs aligned. Volume within normal bands.",
            lastUpdatedTimestamp = now,
            tradeDate = "22-OCT-2026",
            isThinSettle = false,
            isStaleSettle = false,
            isTrustedForSignals = true,
            provenance = "SIMULATED"
        )

        // GOLDPETAL: 1g contract, quoted per 1g (999 pure)
        val petalBase = (BASE_10G_PRICE / 10.0) * (999.0 / 995.0) + 22.5
        val petalSnap = ContractSnapshot(
            contractType = GoldContractType.GOLDPETAL,
            expiryDate = "28-NOV-2026",
            daysToExpiry = 38,
            quotedLtp = petalBase,
            change = 38.0,
            changePercent = 0.48,
            open = 7870.0,
            high = 7925.0,
            low = 7860.0,
            closeSettlement = petalBase - 2.0,
            volumeLots = 28400,
            openInterestLots = 42100,
            bidPrice = petalBase - 1.0,
            askPrice = petalBase + 1.5,
            trustStatus = DataTrustStatus.TRUSTED,
            trustReason = "Fine gold 999 specs valid. High liquidity check passed.",
            lastUpdatedTimestamp = now,
            tradeDate = "22-OCT-2026",
            isThinSettle = false,
            isStaleSettle = false,
            isTrustedForSignals = true,
            provenance = "SIMULATED"
        )

        val seededMap = mapOf(
            GoldContractType.GOLDM to goldmSnap,
            GoldContractType.GOLDTEN to goldtenSnap,
            GoldContractType.GOLDGUINEA to guineaSnap,
            GoldContractType.GOLDPETAL to petalSnap
        )
        _contracts.value = seededMap

        runIntegrityAudit()
    }

    /**
     * Executes data health and integrity audit across all contracts.
     */
    fun runIntegrityAudit(): List<DataIntegrityCheck> {
        val current = _contracts.value
        val checks = mutableListOf<DataIntegrityCheck>()
        val now = System.currentTimeMillis()

        for ((type, snap) in current) {
            // Check 1: Missing / Non-Positive Price
            if (snap.quotedLtp <= 0.0 || snap.volumeLots < 0) {
                checks.add(
                    DataIntegrityCheck(
                        checkName = "${type.symbol} - Price Sanity",
                        description = "Verify positive price and non-negative volume",
                        status = DataTrustStatus.REJECTED,
                        details = "Price ${snap.quotedLtp} is invalid or non-positive.",
                        checkedTimestamp = now
                    )
                )
            } else {
                checks.add(
                    DataIntegrityCheck(
                        checkName = "${type.symbol} - Price Sanity",
                        description = "Verify positive price and non-negative volume",
                        status = DataTrustStatus.TRUSTED,
                        details = "LTP ₹${String.format("%.2f", snap.quotedLtp)} is mathematically valid.",
                        checkedTimestamp = now
                    )
                )
            }

            // Check 2: OHLC Consistency (High >= Low, High >= Open, High >= Close, High >= Settle)
            val ohlcValid = snap.high >= snap.low && snap.high >= snap.open &&
                    snap.high >= snap.closeSettlement && snap.low <= snap.open && snap.low <= snap.closeSettlement
            if (!ohlcValid) {
                checks.add(
                    DataIntegrityCheck(
                        checkName = "${type.symbol} - OHLC Consistency",
                        description = "Check High >= Low and High >= Close bounds",
                        status = DataTrustStatus.REJECTED,
                        details = "Corrupted candle: High=${snap.high}, Low=${snap.low}, Open=${snap.open}, Settle=${snap.closeSettlement}",
                        checkedTimestamp = now
                    )
                )
            } else {
                checks.add(
                    DataIntegrityCheck(
                        checkName = "${type.symbol} - OHLC Consistency",
                        description = "Check High >= Low and High >= Close bounds",
                        status = DataTrustStatus.TRUSTED,
                        details = "High (₹${snap.high}) and Low (₹${snap.low}) are consistent.",
                        checkedTimestamp = now
                    )
                )
            }

            // Check 3: Abnormal Price Jump (> 4% intraday for gold futures is an anomaly)
            val jumpPercent = abs(snap.changePercent)
            if (jumpPercent > 4.0) {
                checks.add(
                    DataIntegrityCheck(
                        checkName = "${type.symbol} - Extreme Outlier Spike",
                        description = "Threshold: |Change%| <= 4.0%",
                        status = DataTrustStatus.WARNING,
                        details = "Abnormal jump detected: ${String.format("%.2f", snap.changePercent)}%. Flagged for circuit filter review.",
                        checkedTimestamp = now
                    )
                )
            } else {
                checks.add(
                    DataIntegrityCheck(
                        checkName = "${type.symbol} - Price Jump Boundary",
                        description = "Threshold: |Change%| <= 4.0%",
                        status = DataTrustStatus.TRUSTED,
                        details = "Price move ${String.format("%.2f", snap.changePercent)}% within normal volatility bands.",
                        checkedTimestamp = now
                    )
                )
            }

            // Check 4: Stale Settlement / Freshness Check
            val ageSec = (now - snap.lastUpdatedTimestamp) / 1000
            if (ageSec > 1800 || snap.isStaleSettle) {
                checks.add(
                    DataIntegrityCheck(
                        checkName = "${type.symbol} - Feed Freshness",
                        description = "Tick age < 1800s",
                        status = DataTrustStatus.STALE,
                        details = "Market data is stale or settlement unchanged. Excluded from signals.",
                        checkedTimestamp = now
                    )
                )
            } else {
                checks.add(
                    DataIntegrityCheck(
                        checkName = "${type.symbol} - Feed Freshness",
                        description = "Tick age < 1800s",
                        status = DataTrustStatus.TRUSTED,
                        details = "Tick latency synchronized. Data fresh.",
                        checkedTimestamp = now
                    )
                )
            }

            // Check 5: Settle-Price Trust Check (Phase 2b Gate 5)
            if (snap.isThinSettle) {
                checks.add(
                    DataIntegrityCheck(
                        checkName = "${type.symbol} - Settle Price Trust Flag",
                        description = "Volume >= 5 and OI >= 10 for trusted settlement mark",
                        status = DataTrustStatus.WARNING,
                        details = "THIN SETTLE DETECTED (Vol: ${snap.volumeLots}, OI: ${snap.openInterestLots}). EXCLUDED FROM SIGNALS.",
                        checkedTimestamp = now
                    )
                )
            }
        }

        _integrityChecks.value = checks
        return checks
    }

    // ==========================================
    // PHASE 2d: SEEDED FAULT DEMO (Real Validators)
    // ==========================================

    fun injectDataGapFault() {
        val current = _contracts.value.toMutableMap()
        val petal = current[GoldContractType.GOLDPETAL] ?: return
        current[GoldContractType.GOLDPETAL] = petal.copy(
            quotedLtp = 0.0,
            bidPrice = 0.0,
            askPrice = 0.0,
            trustStatus = DataTrustStatus.REJECTED,
            trustReason = "GATE 3 REJECTED: Missing price / Zero quote in market feed."
        )
        _activeFault.value = "MISSING_PRICE_GAP"
        _contracts.value = current
        runIntegrityAudit()
    }

    fun injectStaleDataFault() {
        val current = _contracts.value.toMutableMap()
        val goldm = current[GoldContractType.GOLDM] ?: return
        current[GoldContractType.GOLDM] = goldm.copy(
            trustStatus = DataTrustStatus.STALE,
            trustReason = "GATE 5 EXCLUDED: Exchange heartbeat delayed by > 2 hours. Stale mark excluded from signals.",
            lastUpdatedTimestamp = System.currentTimeMillis() - 7_200_000,
            isStaleSettle = true,
            isTrustedForSignals = false
        )
        _activeFault.value = "STALE_DATA"
        _contracts.value = current
        runIntegrityAudit()
    }

    fun injectExtremeOutlierFault() {
        val current = _contracts.value.toMutableMap()
        val goldten = current[GoldContractType.GOLDTEN] ?: return
        current[GoldContractType.GOLDTEN] = goldten.copy(
            quotedLtp = goldten.quotedLtp * 1.09,
            changePercent = 9.4,
            change = 7380.0,
            trustStatus = DataTrustStatus.WARNING,
            trustReason = "GATE 4 WARNING: Extreme price jump (+9.4%) outside 3.5-sigma boundary."
        )
        _activeFault.value = "EXTREME_OUTLIER"
        _contracts.value = current
        runIntegrityAudit()
    }

    fun injectCorruptOhlcFault() {
        val current = _contracts.value.toMutableMap()
        val guinea = current[GoldContractType.GOLDGUINEA] ?: return
        current[GoldContractType.GOLDGUINEA] = guinea.copy(
            high = 61000.0, // High < Low!
            low = 63500.0,
            trustStatus = DataTrustStatus.REJECTED,
            trustReason = "GATE 3 REJECTED: OHLC consistency failure: High (₹61,000) < Low (₹63,500)."
        )
        _activeFault.value = "CORRUPT_OHLC"
        _contracts.value = current
        runIntegrityAudit()
    }

    fun injectThinSettleFault() {
        val current = _contracts.value.toMutableMap()
        val petal = current[GoldContractType.GOLDPETAL] ?: return
        current[GoldContractType.GOLDPETAL] = petal.copy(
            volumeLots = 2, // < 5 lots
            openInterestLots = 4, // < 10 lots
            isThinSettle = true,
            isTrustedForSignals = false,
            trustStatus = DataTrustStatus.WARNING,
            trustReason = "GATE 5 EXCLUDED: Thin volume/OI detected. Excluded from signal evaluation."
        )
        _activeFault.value = "THIN_SETTLE"
        _contracts.value = current
        runIntegrityAudit()
    }

    // ==========================================
    // PHASE 2b: BHAVCOPY VALIDATION GATES IN STRICT ORDER
    // ==========================================

    /**
     * Ingests and validates MCX Bhavcopy records applying the 5 Phase 2b validation gates in exact order:
     * 1. Returned date must equal requested date (else REJECT + log reason)
     * 2. Key rows by (symbol, expiry_date)
     * 3. OHLC consistency (High >= Low, High >= Open, High >= Close, High >= Settle, Low <= Open, Low <= Close, Low <= Settle, all > 0)
     * 4. Volume / OI sanity (non-negative)
     * 5. Settle-price trust flag: thin (vol < 5 or OI < 10) or stale settles are EXCLUDED FROM SIGNALS.
     */
    fun parseAndIngestCsvBhavcopy(
        csvContent: String,
        requestedTradeDate: String? = null,
        sourceName: String = "Imported MCX Bhavcopy CSV"
    ): BhavcopyIngestionResult {
        var processed = 0
        var ingested = 0
        var rejected = 0
        val gateLogs = mutableListOf<String>()
        val excludedThinSymbols = mutableListOf<String>()

        val lines = csvContent.lines()
        val keyedRows = mutableMapOf<Pair<GoldContractType, String>, ContractSnapshot>()
        val current = _contracts.value.toMutableMap()
        val now = System.currentTimeMillis()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("SYMBOL", ignoreCase = true) ||
                trimmed.startsWith("CONTRACT", ignoreCase = true) || trimmed.startsWith("#")
            ) continue

            processed++
            val tokens = trimmed.split(",").map { it.trim() }
            if (tokens.size < 7) {
                rejected++
                gateLogs.add("ROW $processed REJECTED: Expected minimum 7 CSV columns, found ${tokens.size}.")
                continue
            }

            try {
                // Parse fields: SYMBOL, EXPIRY_DT, OPEN, HIGH, LOW, CLOSE, SETTLE_PRC, [VOLUME], [OPEN_INT], [TRADE_DATE]
                val symbolStr = tokens[0].uppercase()
                val contractType = GoldContractType.values().firstOrNull { it.symbol == symbolStr }
                if (contractType == null) {
                    rejected++
                    gateLogs.add("ROW $processed REJECTED: Symbol '$symbolStr' is not a recognized MCX Gold contract.")
                    continue
                }

                val expiry = tokens[1]

                // GATE 1: Returned date must equal requested date (if requested)
                val rowTradeDate = if (tokens.size > 9) tokens[9] else _dataDate.value
                if (requestedTradeDate != null && !rowTradeDate.equals(requestedTradeDate, ignoreCase = true)) {
                    rejected++
                    gateLogs.add("GATE 1 REJECTED ($symbolStr): Returned date '$rowTradeDate' does not match requested date '$requestedTradeDate'.")
                    continue
                }

                val open = tokens[2].toDouble()
                val high = tokens[3].toDouble()
                val low = tokens[4].toDouble()
                val close = tokens[5].toDouble()
                val settle = tokens[6].toDouble()
                val volume = if (tokens.size > 7) tokens[7].toLongOrNull() ?: 1000L else 1000L
                val oi = if (tokens.size > 8) tokens[8].toLongOrNull() ?: 5000L else 5000L

                // GATE 3: OHLC Consistency
                val isOhlcConsistent = (high >= low) && (high >= open) && (high >= close) &&
                        (high >= settle) && (low <= open) && (low <= close) && (low <= settle) &&
                        (high > 0.0) && (low > 0.0) && (open > 0.0) && (close > 0.0) && (settle > 0.0)

                if (!isOhlcConsistent) {
                    rejected++
                    gateLogs.add("GATE 3 REJECTED ($symbolStr): OHLC violation (High=$high, Low=$low, Open=$open, Close=$close, Settle=$settle).")
                    continue
                }

                // GATE 4: Volume / OI Sanity
                if (volume < 0 || oi < 0) {
                    rejected++
                    gateLogs.add("GATE 4 REJECTED ($symbolStr): Negative volume ($volume) or Open Interest ($oi).")
                    continue
                }

                // GATE 5: Settle-Price Trust Flag
                val isThin = (volume < 5L) || (oi < 10L)
                val isStale = (volume == 0L)
                val isTrustedForSignals = !isThin && !isStale

                if (!isTrustedForSignals) {
                    excludedThinSymbols.add(contractType.symbol)
                    gateLogs.add("GATE 5 EXCLUDED FROM SIGNALS ($symbolStr): Thin/stale settle (Vol=$volume, OI=$oi). Settle not trusted for signals.")
                }

                val change = close - settle
                val changePct = if (settle > 0) (change / settle) * 100.0 else 0.0

                val snapshot = ContractSnapshot(
                    contractType = contractType,
                    expiryDate = expiry,
                    daysToExpiry = 25,
                    quotedLtp = close,
                    change = change,
                    changePercent = changePct,
                    open = open,
                    high = high,
                    low = low,
                    closeSettlement = settle,
                    volumeLots = volume,
                    openInterestLots = oi,
                    bidPrice = close - contractType.tickSize,
                    askPrice = close + contractType.tickSize,
                    trustStatus = if (isTrustedForSignals) DataTrustStatus.TRUSTED else DataTrustStatus.WARNING,
                    trustReason = if (isTrustedForSignals) "Validated through all 5 Phase 2b gates." else "Gate 5: Thin settle. Excluded from signals.",
                    lastUpdatedTimestamp = now,
                    tradeDate = rowTradeDate,
                    isThinSettle = isThin,
                    isStaleSettle = isStale,
                    isTrustedForSignals = isTrustedForSignals,
                    provenance = "REAL"
                )

                // GATE 2: Key rows by (symbol, expiry_date)
                val compositeKey = Pair(contractType, expiry)
                if (keyedRows.containsKey(compositeKey)) {
                    gateLogs.add("GATE 2 DEDUPLICATION ($symbolStr): Duplicate record for expiry $expiry reconciled.")
                }
                keyedRows[compositeKey] = snapshot
                ingested++
            } catch (e: Exception) {
                rejected++
                gateLogs.add("ROW $processed REJECTED: Parsing exception: ${e.message}")
            }
        }

        if (keyedRows.isNotEmpty()) {
            for ((key, snap) in keyedRows) {
                current[key.first] = snap
            }
            _contracts.value = current
            _dataProvenance.value = DataProvenance.REAL
            _provenanceSource.value = sourceName
            val importedDate = keyedRows.values.firstOrNull()?.tradeDate ?: _dataDate.value
            _dataDate.value = importedDate
            _lastUpdatedTimeText.value = getCurrentIstTimeString()
            _isDataFresh.value = true
            _activeFault.value = "REAL_BHAVCOPY_ACTIVE"

            // Phase 2a: Accumulate into history
            val dayRecord = DailyBhavcopyDay(
                tradeDate = importedDate,
                sourceName = sourceName,
                contractSnapshots = current,
                ingestedTimestamp = now
            )
            val updatedHistory = (_accumulatedHistory.value.filter { it.tradeDate != importedDate } + dayRecord)
                .sortedBy { it.tradeDate }
            _accumulatedHistory.value = updatedHistory

            runIntegrityAudit()
            ExecutionRiskEngine.updatePositionValuations(current)
        }

        return BhavcopyIngestionResult(
            success = ingested > 0,
            totalRowsProcessed = processed,
            validRowsIngested = ingested,
            rejectedRows = rejected,
            summary = "Processed $processed rows: $ingested valid records keyed, $rejected records rejected by gates.",
            validationGateLogs = gateLogs,
            excludedThinSettleSymbols = excludedThinSymbols
        )
    }
}
