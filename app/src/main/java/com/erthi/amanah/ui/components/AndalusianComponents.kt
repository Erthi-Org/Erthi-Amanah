package com.erthi.amanah.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erthi.amanah.ui.theme.*

/**
 * Ornamental card with Andalusian geometric border and optional gold filigree top stripe.
 * Wraps any content in a premium sapphire-blue raised card with corner arabesques.
 */
@Composable
fun AndalusianCard(
    modifier: Modifier = Modifier,
    accentColor: Color = RoyalGold,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AndalusianCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
        ) {
            // Gold filigree top stripe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, accentColor.copy(alpha = 0.8f), Color.Transparent)
                        )
                    ),
            )
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
        // Ornamental corner diamonds
        AndalusianCornerAccents(accentColor = accentColor)
    }
}

/** Four small diamond corner ornaments drawn in Canvas. */
@Composable
private fun BoxScope.AndalusianCornerAccents(accentColor: Color) {
    Canvas(modifier = Modifier.matchParentSize()) {
        val d = 6.dp.toPx()
        val paint = Paint().apply {
            color = accentColor.copy(alpha = 0.6f)
            style = PaintingStyle.Fill
        }
        // Top-left and top-right
        drawDiamond(Offset(d, d), d * 0.6f, paint)
        drawDiamond(Offset(size.width - d, d), d * 0.6f, paint)
        // Bottom-left and bottom-right
        drawDiamond(Offset(d, size.height - d), d * 0.6f, paint)
        drawDiamond(Offset(size.width - d, size.height - d), d * 0.6f, paint)
    }
}

private fun DrawScope.drawDiamond(center: Offset, r: Float, paint: Paint) {
    val path = Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + r, center.y)
        lineTo(center.x, center.y + r)
        lineTo(center.x - r, center.y)
        close()
    }
    drawPath(path, paint.color, style = Fill)
}

/**
 * Icon wrapped in an Andalusian ornamental hexagonal frame with filigree ring.
 * Use for dashboard section cards, navigation icons, and setting items.
 */
@Composable
fun AndalusianIcon(
    icon: ImageVector,
    iconColor: Color,
    frameColor: Color = iconColor,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
) {
    Box(contentAlignment = Alignment.Center) {
        // Outer filigree ring
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape((size.value * 0.3f).dp))
                .background(frameColor.copy(alpha = 0.08f))
                .border(
                    1.dp,
                    Brush.sweepGradient(
                        listOf(
                            frameColor.copy(alpha = 0.0f),
                            frameColor.copy(alpha = 0.6f),
                            frameColor.copy(alpha = 0.0f),
                            frameColor.copy(alpha = 0.6f),
                        )
                    ),
                    RoundedCornerShape((size.value * 0.3f).dp),
                ),
        )
        // Inner gem background
        Box(
            modifier = Modifier
                .size(size * 0.72f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(frameColor.copy(alpha = 0.22f), frameColor.copy(alpha = 0.06f))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(iconSize))
        }
        // Decorative arc segments at corners
        Canvas(modifier = Modifier.size(size)) {
            val r = size.toPx() / 2f
            val stroke = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            val arcColor = frameColor.copy(alpha = 0.5f)
            // Four small arc segments at diagonal positions
            listOf(45f, 135f, 225f, 315f).forEach { startAngle ->
                drawArc(
                    color = arcColor,
                    startAngle = startAngle - 10f,
                    sweepAngle = 20f,
                    useCenter = false,
                    topLeft = Offset(r * 0.1f, r * 0.1f),
                    size = Size(r * 1.8f, r * 1.8f),
                    style = stroke,
                )
            }
        }
    }
}

/**
 * Dashboard section card with Andalusian ornamental frame and icon.
 */
@Composable
fun AndalusianSectionCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AndalusianCard),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        iconColor.copy(alpha = 0.5f),
                        RoyalGold.copy(alpha = 0.2f),
                        iconColor.copy(alpha = 0.1f),
                    )
                ),
                RoundedCornerShape(16.dp),
            ),
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.Start) {
            AndalusianIcon(icon = icon, iconColor = iconColor, size = 36.dp, iconSize = 18.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 2,
            )
            // Gold filigree bottom accent line
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(iconColor.copy(alpha = 0.6f), Color.Transparent)
                        )
                    ),
            )
        }
    }
}

/**
 * Premium Quranic verse display with an ornamental frame.
 * Always shows attribution and a clear excerpt marker.
 */
@Composable
fun QuranVerseFrame(
    verse: String,
    attribution: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // Outer glow border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(RoyalGold.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
                .border(1.dp, RoyalGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Decorative top ornament
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = RoyalGold.copy(alpha = 0.4f),
                        thickness = 1.dp,
                    )
                    Text(
                        "✦",
                        fontSize = 10.sp,
                        color = RoyalGold.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = RoyalGold.copy(alpha = 0.4f),
                        thickness = 1.dp,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    verse,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = RoyalGoldLight,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = RoyalGold.copy(alpha = 0.4f),
                        thickness = 1.dp,
                    )
                    Text(
                        "✦",
                        fontSize = 10.sp,
                        color = RoyalGold.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = RoyalGold.copy(alpha = 0.4f),
                        thickness = 1.dp,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    attribution,
                    fontSize = 11.sp,
                    color = RoyalGold.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Ornamental gold lock emblem for use on the auth/PIN screens.
 */
@Composable
fun AndalusianLockEmblem(
    size: Dp = 80.dp,
    locked: Boolean = true,
) {
    Box(contentAlignment = Alignment.Center) {
        // Outer ring with Andalusian pattern
        Canvas(modifier = Modifier.size(size)) {
            val cx = size.toPx() / 2f
            val cy = size.toPx() / 2f
            val outerR = size.toPx() / 2f - 2.dp.toPx()
            val innerR = outerR * 0.72f
            // Outer gold ring
            drawCircle(
                color = RoyalGold.copy(alpha = 0.5f),
                radius = outerR,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx()),
            )
            // Inner filled gem circle
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(AndalusianElevated, AndalusianCard),
                    center = Offset(cx, cy),
                    radius = innerR,
                ),
                radius = innerR,
                center = Offset(cx, cy),
            )
            // Inner ring
            drawCircle(
                color = RoyalGold.copy(alpha = 0.3f),
                radius = innerR,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx()),
            )
            // 8-pointed star ornament segments
            val segCount = 8
            for (i in 0 until segCount) {
                val angle = (Math.PI * 2 * i / segCount).toFloat()
                val segStart = Offset(
                    cx + (innerR + 3.dp.toPx()) * kotlin.math.cos(angle),
                    cy + (innerR + 3.dp.toPx()) * kotlin.math.sin(angle),
                )
                val segEnd = Offset(
                    cx + outerR * kotlin.math.cos(angle),
                    cy + outerR * kotlin.math.sin(angle),
                )
                drawLine(
                    color = RoyalGold.copy(alpha = 0.4f),
                    start = segStart,
                    end = segEnd,
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
        // Lock icon in center
        Icon(
            imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
            contentDescription = null,
            tint = Brush.radialGradient(
                listOf(RoyalGoldLight, RoyalGold)
            ).let { RoyalGoldLight },
            modifier = Modifier.size(size * 0.38f),
        )
    }
}

/**
 * Settings item with Andalusian ornamental treatment.
 */
@Composable
fun AndalusianSettingItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    badge: String? = null,         // null → chevron; non-null → badge text
    badgeColor: Color = RoyalGold,
    onClick: () -> Unit = {},
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (badge != null) AndalusianCard.copy(alpha = 0.6f) else AndalusianCard,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, iconColor.copy(alpha = if (badge != null) 0.15f else 0.3f), RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AndalusianIcon(
                icon = icon,
                iconColor = if (badge != null) iconColor.copy(alpha = 0.4f) else iconColor,
                size = 40.dp,
                iconSize = 20.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (badge != null) TextPrimary.copy(alpha = 0.5f) else TextPrimary,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (badge != null) TextSecondary.copy(alpha = 0.5f) else TextSecondary,
                )
            }
            if (badge != null) {
                Surface(
                    color = badgeColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            } else {
                OrnamentalIcon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    null,
                    tint = RoyalGold.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
