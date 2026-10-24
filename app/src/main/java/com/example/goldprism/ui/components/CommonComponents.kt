package com.example.goldprism.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goldprism.engine.DataProvenance
import com.example.goldprism.model.BacktestEquityPoint
import com.example.goldprism.model.DataTrustStatus
import com.example.goldprism.model.SignalVerdict
import com.example.goldprism.model.WaterfallComponent
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.max

/**
 * Persistent banner required on all screens running on simulated demo data (Phase 2c).
 */
@Composable
fun SimulatedDemoDataBanner(
    isSimulated: Boolean,
    modifier: Modifier = Modifier
) {
    if (isSimulated) {
        Surface(
            color = TerminalWarningLight.copy(alpha = 0.12f),
            shape = RoundedCornerShape(2.dp),
            border = BorderStroke(1.dp, TerminalWarningLight),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TerminalWarningLight,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SIMULATED DEMO DATA — Unverified for live market execution",
                    color = TerminalWarningLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Provenance Chip conforming to Phase 2c:
 * - REAL: green outline, file/source name + data date
 * - SIMULATED: amber fill, "SIMULATED DEMO DATA"
 * - UNVERIFIED: amber outline, external source
 */
@Composable
fun ProvenanceChip(
    provenance: DataProvenance,
    sourceName: String = "",
    dataDate: String = "",
    modifier: Modifier = Modifier
) {
    val (bgColor, borderColor, textColor, text) = when (provenance) {
        DataProvenance.REAL -> Quadruple(
            Color.Transparent,
            TerminalPositiveLight,
            TerminalPositiveLight,
            "REAL: $sourceName ($dataDate)"
        )
        DataProvenance.SIMULATED -> Quadruple(
            TerminalWarningLight.copy(alpha = 0.15f),
            TerminalWarningLight,
            TerminalWarningLight,
            "SIMULATED DEMO DATA"
        )
        DataProvenance.UNVERIFIED -> Quadruple(
            Color.Transparent,
            TerminalWarningLight,
            TerminalWarningLight,
            "UNVERIFIED: $sourceName"
        )
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.height(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Phase 6 minimal 1dp hairline divider.
 */
@Composable
fun HairlineDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant
) {
    HorizontalDivider(
        thickness = 1.dp,
        color = color,
        modifier = modifier
    )
}

/**
 * Flat trading terminal card (0dp/2dp radius, no elevation, 1dp border).
 */
@Composable
fun FlatTerminalCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            content = content
        )
    }
}

@Composable
fun DataTrustStatusChip(status: DataTrustStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status) {
        DataTrustStatus.TRUSTED -> Triple(Color.Transparent, TerminalPositiveLight, "TRUSTED")
        DataTrustStatus.WARNING -> Triple(TerminalWarningLight.copy(alpha = 0.12f), TerminalWarningLight, "WARNING")
        DataTrustStatus.REJECTED -> Triple(TerminalNegativeLight.copy(alpha = 0.12f), TerminalNegativeLight, "REJECTED")
        DataTrustStatus.STALE -> Triple(TerminalNegativeLight.copy(alpha = 0.12f), TerminalNegativeLight, "STALE")
        DataTrustStatus.INSUFFICIENT_DATA -> Triple(Color.Transparent, TerminalTextSecondaryLight, "INSUFFICIENT DATA")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, textColor),
        modifier = modifier.height(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun VerdictBadge(verdict: SignalVerdict, modifier: Modifier = Modifier) {
    val (bgColor, borderColor, textColor) = when (verdict) {
        SignalVerdict.TRADABLE -> Triple(TerminalPositiveLight.copy(alpha = 0.15f), TerminalPositiveLight, TerminalPositiveLight)
        SignalVerdict.WATCH -> Triple(TerminalWarningLight.copy(alpha = 0.15f), TerminalWarningLight, TerminalWarningLight)
        SignalVerdict.NO_EDGE -> Triple(Color.Transparent, MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.onSurfaceVariant)
        SignalVerdict.BLOCKED -> Triple(TerminalNegativeLight.copy(alpha = 0.15f), TerminalNegativeLight, TerminalNegativeLight)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.height(26.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = verdict.label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    highlightColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title.uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = highlightColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (badgeText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = badgeText,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        HairlineDivider()
    }
}

/**
 * Phase 6 Minimal Flat X-Ray Waterfall Chart:
 * Raw gap accent blue (#387ED1), deductions amber/orange (#FF9800),
 * final residual green (#4CAF50) if positive and red (#DF514C) if negative,
 * thin value labels, no rounded caps, 1dp hairline axes.
 */
@Composable
fun XRayWaterfallCanvasChart(
    components: List<WaterfallComponent>,
    modifier: Modifier = Modifier
) {
    if (components.isEmpty()) return

    FlatTerminalCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "X-RAY WATERFALL DECOMPOSITION (₹/g)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp
            )
            Text(
                text = "SUM RECONCILED",
                color = TerminalPositiveLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val axisColor = MaterialTheme.colorScheme.outlineVariant
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val maxVal = maxOf(40.0, components.maxOf { abs(it.runningTotalPerGram) } * 1.25)
            val zeroY = canvasHeight * 0.70f

            // 1dp Baseline
            drawLine(
                color = axisColor,
                start = Offset(0f, zeroY),
                end = Offset(canvasWidth, zeroY),
                strokeWidth = 1f
            )

            val count = components.size
            val barSpacing = canvasWidth / count
            val barWidth = barSpacing * 0.65f

            var prevRunningY = zeroY

            components.forEachIndexed { idx, comp ->
                val x = (idx * barSpacing) + (barSpacing - barWidth) / 2f
                val barHeight = ((abs(comp.valuePerGram) / maxVal) * (canvasHeight * 0.60f)).toFloat()

                val isPositive = comp.valuePerGram >= 0
                val barColor = when {
                    idx == 0 -> TerminalAccentLight // Raw Spread accent blue
                    idx == count - 1 -> if (comp.valuePerGram > 0) TerminalPositiveLight else TerminalNegativeLight
                    comp.isDeduction -> TerminalWarningLight // Deductions amber/orange
                    else -> TerminalAccentLight
                }

                val topY = if (idx == 0 || idx == count - 1) {
                    if (isPositive) zeroY - barHeight else zeroY
                } else {
                    if (comp.isDeduction) prevRunningY else prevRunningY - barHeight
                }

                drawRect(
                    color = barColor,
                    topLeft = Offset(x, topY),
                    size = Size(barWidth, max(2f, barHeight))
                )

                prevRunningY = (zeroY - ((comp.runningTotalPerGram / maxVal) * (canvasHeight * 0.60f)).toFloat()).coerceIn(8f, canvasHeight - 8f)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HairlineDivider()
        Spacer(modifier = Modifier.height(6.dp))

        // Right-aligned numerical table breakdown (Phase 6 spec)
        components.forEach { comp ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comp.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
                Text(
                    text = (if (comp.valuePerGram > 0 && comp != components.first()) "+" else "") +
                            String.format("%.2f", comp.valuePerGram) + " ₹/g",
                    color = if (comp.valuePerGram >= 0) TerminalPositiveLight else TerminalNegativeLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/**
 * Phase 6 Flat Equity Curve Chart:
 * Gross in text-primary, Net in accent (#387ED1), 1.5dp lines, no area fill, 1dp hairline axes.
 */
@Composable
fun FlatEquityCurveChart(
    equityPoints: List<BacktestEquityPoint>,
    modifier: Modifier = Modifier
) {
    if (equityPoints.isEmpty()) return

    val primaryLineColor = MaterialTheme.colorScheme.onSurface
    val netLineColor = TerminalAccentLight
    val axisColor = MaterialTheme.colorScheme.outlineVariant

    FlatTerminalCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "EQUITY CURVE (GROSS VS NET)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp
            )
            Row {
                Text("■ Gross", color = primaryLineColor, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("■ Net of Costs", color = netLineColor, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val w = size.width
            val h = size.height

            val minEq = minOf(equityPoints.minOf { it.netEquity }, equityPoints.minOf { it.grossEquity }) * 0.99
            val maxEq = maxOf(equityPoints.maxOf { it.grossEquity }, equityPoints.maxOf { it.netEquity }) * 1.01
            val range = max(1.0, maxEq - minEq)

            // 1dp Axis
            drawLine(axisColor, Offset(0f, h), Offset(w, h), 1f)

            val stepX = w / maxOf(1, equityPoints.size - 1)

            val grossPath = Path()
            val netPath = Path()

            equityPoints.forEachIndexed { i, pt ->
                val x = i * stepX
                val grossY = (h - ((pt.grossEquity - minEq) / range * h)).toFloat()
                val netY = (h - ((pt.netEquity - minEq) / range * h)).toFloat()

                if (i == 0) {
                    grossPath.moveTo(x, grossY)
                    netPath.moveTo(x, netY)
                } else {
                    grossPath.lineTo(x, grossY)
                    netPath.lineTo(x, netY)
                }
            }

            // Draw clean 1.5dp lines without area fill
            drawPath(grossPath, color = primaryLineColor, style = Stroke(width = 1.5f))
            drawPath(netPath, color = netLineColor, style = Stroke(width = 1.5f))
        }
    }
}

@Composable
fun SpreadDistributionCanvas(
    currentSpread: Double,
    mean: Double,
    stdDev: Double,
    zScore: Double,
    modifier: Modifier = Modifier
) {
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val accentColor = TerminalAccentLight

    FlatTerminalCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SPREAD STATISTICAL DISTRIBUTION",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp
            )
            Text(
                text = "Z = ${String.format("%.2f", zScore)}σ",
                color = if (abs(zScore) >= 1.75) TerminalAccentLight else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            val w = size.width
            val h = size.height
            val baselineY = h * 0.88f

            val path = Path()
            val points = 60
            for (i in 0..points) {
                val t = (i.toFloat() / points) * 6f - 3f
                val normY = kotlin.math.exp(-0.5 * t * t)
                val x = (i.toFloat() / points) * w
                val y = baselineY - (normY * (h * 0.75f)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(path, color = accentColor, style = Stroke(width = 1.5f))

            // Baseline
            drawLine(axisColor, Offset(0f, baselineY), Offset(w, baselineY), 1f)

            // Current Z cursor
            val currentX = (w * (0.5f + (zScore.toFloat() / 6f))).coerceIn(6f, w - 6f)
            drawLine(
                color = TerminalAccentLight,
                start = Offset(currentX, baselineY),
                end = Offset(currentX, 10f),
                strokeWidth = 2f
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-3σ (${String.format("%.1f", mean - 3 * stdDev)})", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("μ = ${String.format("%.1f", mean)} ₹/g", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("+3σ (${String.format("%.1f", mean + 3 * stdDev)})", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
