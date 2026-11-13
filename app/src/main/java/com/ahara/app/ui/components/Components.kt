package com.ahara.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahara.app.i18n.Strings
import com.ahara.app.ui.theme.AharaColors

val LocalLang = staticCompositionLocalOf { "en" }

@Composable
fun t(key: String): String = Strings.t(LocalLang.current, key)

@Composable
fun tf(key: String, vararg args: String): String = Strings.f(LocalLang.current, key, *args)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AharaScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    floating: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("back"))
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = AharaColors.SageDark
                )
            )
        },
        floatingActionButton = floating,
        bottomBar = bottomBar
    ) { pad ->
        val base = Modifier.fillMaxSize().padding(pad)
        Column(
            modifier = if (scroll) base.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)
            else base.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    container: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.clip(RoundedCornerShape(18.dp)).clickable { onClick() } else it },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6DFCC))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, color = AharaColors.SageDark)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
            content()
        }
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, accent: Color = AharaColors.SageDark, icon: ImageVector? = null) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6DFCC))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(label, style = MaterialTheme.typography.labelMedium, color = AharaColors.Muted, maxLines = 2)
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Single-select chip group. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceChips(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AharaColors.SageLight,
                    selectedLabelColor = AharaColors.Ink
                )
            )
        }
    }
}

/** Numeric scale selector with labels shown below. */
@Composable
fun ScaleSelector(title: String, range: IntRange, value: Int, labels: List<String>, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            range.forEachIndexed { i, v ->
                val sel = v == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) AharaColors.SageDark else AharaColors.Beige)
                        .clickable { onChange(v) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(v.toString(), color = if (sel) Color.White else AharaColors.Ink, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        val idx = range.indexOf(value)
        if (idx in labels.indices) Text(labels[idx], style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun NoteBox(text: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFF4EBD3)).padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Filled.Info, null, tint = AharaColors.Gold, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = AharaColors.Ink)
    }
}

@Composable
fun ModuleBadge(module: Int) {
    val color = when (module) {
        1 -> AharaColors.Sage
        2 -> AharaColors.Olive
        3 -> AharaColors.Clay
        4 -> AharaColors.Gold
        else -> AharaColors.Muted
    }
    Box(Modifier.size(28.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(if (module == 0) "•" else module.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/** Simple vertical bar chart built from Compose layouts. */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    color: Color = AharaColors.Sage,
    height: Dp = 140.dp,
    valueFormatter: (Float) -> String = { if (it == it.toInt().toFloat()) it.toInt().toString() else String.format("%.1f", it) },
    highlight: (Int) -> Color? = { null }
) {
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(height), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            values.forEachIndexed { i, v ->
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (v > 0f) Text(valueFormatter(v), fontSize = 9.sp, color = AharaColors.Muted, maxLines = 1)
                    Box(
                        Modifier
                            .fillMaxWidth(0.8f)
                            .fillMaxHeight(((v / max) * 0.85f).coerceIn(0.01f, 0.85f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (v > 0f) (highlight(i) ?: color) else Color(0xFFE9E3D3))
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEach {
                Text(it, modifier = Modifier.weight(1f), fontSize = 9.sp, textAlign = TextAlign.Center, color = AharaColors.Muted, maxLines = 1)
            }
        }
    }
}

/** Line chart; null values create gaps. */
@Composable
fun LineChart(
    values: List<Float?>,
    labels: List<String>,
    minY: Float,
    maxY: Float,
    modifier: Modifier = Modifier,
    color: Color = AharaColors.Olive,
    height: Dp = 130.dp,
    refLines: List<Pair<Float, Color>> = emptyList()
) {
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(height).padding(vertical = 6.dp)) {
            val n = values.size
            if (n == 0) return@Canvas
            val stepX = if (n == 1) 0f else size.width / (n - 1)
            fun yOf(v: Float) = size.height - ((v - minY) / (maxY - minY)).coerceIn(0f, 1f) * size.height
            // grid
            for (k in 0..4) {
                val gy = size.height * k / 4f
                drawLine(Color(0xFFECE6D6), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
            }
            refLines.forEach { (v, c) ->
                val ry = yOf(v)
                drawLine(c, Offset(0f, ry), Offset(size.width, ry), strokeWidth = 2f)
            }
            var path: Path? = null
            values.forEachIndexed { i, v ->
                val x = if (n == 1) size.width / 2 else i * stepX
                if (v == null) {
                    path?.let { drawPath(it, color, style = Stroke(width = 4f, cap = StrokeCap.Round)) }
                    path = null
                } else {
                    val y = yOf(v)
                    if (path == null) path = Path().apply { moveTo(x, y) } else path?.lineTo(x, y)
                    drawCircle(color, radius = 6f, center = Offset(x, y))
                }
            }
            path?.let { drawPath(it, color, style = Stroke(width = 4f, cap = StrokeCap.Round)) }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach {
                Text(it, modifier = Modifier.weight(1f), fontSize = 9.sp, textAlign = TextAlign.Center, color = AharaColors.Muted, maxLines = 1)
            }
        }
    }
}

/** 24-hour timeline with dots at minute-of-day positions. */
@Composable
fun TimelineChart(points: List<Pair<Int, Color>>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(36.dp)) {
            val cy = size.height / 2
            drawLine(Color(0xFFDCD3BD), Offset(0f, cy), Offset(size.width, cy), strokeWidth = 6f, cap = StrokeCap.Round)
            for (h in 0..24 step 3) {
                val x = size.width * h / 24f
                drawLine(Color(0xFFBDB49C), Offset(x, cy - 8f), Offset(x, cy + 8f), strokeWidth = 2f)
            }
            points.forEach { (m, c) ->
                drawCircle(c, radius = 11f, center = Offset(size.width * m / 1440f, cy))
                drawCircle(Color.White, radius = 4f, center = Offset(size.width * m / 1440f, cy))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "6", "12", "18", "24").forEach { Text(it, fontSize = 9.sp, color = AharaColors.Muted) }
        }
    }
}

/** Horizontal proportion bar. */
@Composable
fun ProportionBar(parts: List<Pair<Int, Color>>, modifier: Modifier = Modifier) {
    val total = parts.sumOf { it.first }
    Row(modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFFE9E3D3))) {
        if (total > 0) parts.forEach { (v, c) ->
            if (v > 0) Box(Modifier.weight(v.toFloat()).fillMaxHeight().background(c))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Legend(items: List<Pair<String, Color>>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { (label, c) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(c))
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
            }
        }
    }
}

/** Decorative botanical sprig drawn with vector paths (no external assets). */
@Composable
fun Botanical(modifier: Modifier = Modifier, color: Color = AharaColors.Sage.copy(alpha = 0.35f)) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stem = Path().apply {
            moveTo(w * 0.5f, h)
            cubicTo(w * 0.45f, h * 0.7f, w * 0.55f, h * 0.4f, w * 0.5f, h * 0.05f)
        }
        drawPath(stem, color, style = Stroke(width = w * 0.02f, cap = StrokeCap.Round))
        val leaves = listOf(0.25f to -1f, 0.4f to 1f, 0.55f to -1f, 0.7f to 1f, 0.85f to -1f)
        leaves.forEach { (t, side) ->
            val cx = w * 0.5f
            val cy = h * (1f - t)
            val leaf = Path().apply {
                moveTo(cx, cy)
                cubicTo(cx + side * w * 0.22f, cy - h * 0.12f, cx + side * w * 0.22f, cy - h * 0.12f, cx + side * w * 0.38f, cy - h * 0.04f)
                cubicTo(cx + side * w * 0.2f, cy + h * 0.03f, cx + side * w * 0.2f, cy + h * 0.03f, cx, cy)
                close()
            }
            drawPath(leaf, color)
        }
    }
}

@Composable
fun Pill(text: String, color: Color = AharaColors.SageLight, textColor: Color = AharaColors.Ink) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = textColor)
    }
}

@Composable
fun MenuRow(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
            .border(1.dp, Color(0xFFE6DFCC), RoundedCornerShape(14.dp))
            .clickable { onClick() }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(AharaColors.SageLight), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = AharaColors.SageDark, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AharaColors.Muted)
        }
    }
}
