package com.ntdpz.pie.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.MaterialTheme

@Composable
fun PieJournalPage(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier) {
        val sx = size.width / 264f
        val sy = size.height / 278f
        fun point(x: Float, y: Float) = Offset(x * sx, y * sy)
        drawRect(colors.primaryContainer, point(34f, 19f), Size(183 * sx, 244 * sy))
        val page = Path().apply { moveTo(21*sx,6*sy); lineTo(204*sx,6*sy); lineTo(238*sx,40*sy); lineTo(238*sx,250*sy); lineTo(21*sx,250*sy); close() }
        drawPath(page, colors.surface)
        drawPath(page, colors.outlineVariant, style = Stroke(1.5f * sx))
        drawPath(Path().apply { moveTo(204*sx,6*sy);lineTo(204*sx,40*sy);lineTo(238*sx,40*sy);close() }, colors.primaryContainer)
        listOf(111f to 207f,139f to 191f,167f to 169f).forEach { (y,x) -> drawLine(colors.outlineVariant, point(48f,y),point(x,y),1.2f*sx) }
        drawPath(Path().apply { moveTo(170*sx,211*sy);lineTo(209*sx,145*sy);lineTo(221*sx,152*sy);lineTo(182*sx,218*sy);lineTo(167*sx,226*sy);close() }, colors.primary)
    }
}
