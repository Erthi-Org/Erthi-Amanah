package com.erthi.amanah.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.erthi.amanah.ui.theme.RoyalGold
import com.erthi.amanah.ui.theme.RoyalGoldLight

@Composable
fun OrnamentalIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Box(modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val x = size.width
            val y = size.height
            // Eight cusps around the pictogram follow an Andalusian tile rosette.
            val frame = Path().apply {
                moveTo(x * .5f, y * .02f)
                quadraticTo(x * .61f, y * .18f, x * .82f, y * .18f)
                quadraticTo(x * .82f, y * .39f, x * .98f, y * .5f)
                quadraticTo(x * .82f, y * .61f, x * .82f, y * .82f)
                quadraticTo(x * .61f, y * .82f, x * .5f, y * .98f)
                quadraticTo(x * .39f, y * .82f, x * .18f, y * .82f)
                quadraticTo(x * .18f, y * .61f, x * .02f, y * .5f)
                quadraticTo(x * .18f, y * .39f, x * .18f, y * .18f)
                quadraticTo(x * .39f, y * .18f, x * .5f, y * .02f)
                close()
            }
            drawPath(
                frame,
                Brush.linearGradient(listOf(RoyalGoldLight, RoyalGold.copy(alpha = .45f), RoyalGold)),
                style = Stroke(width = .8.dp.toPx()),
            )
        }
        Icon(imageVector, contentDescription, Modifier.fillMaxSize(.66f), tint)
    }
}
