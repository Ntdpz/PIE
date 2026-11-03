package com.ntdpz.pie.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.R

@Composable
fun PieBrand(modifier: Modifier = Modifier) {
    val fontScale = LocalDensity.current.fontScale
    Row(
        modifier.width(120.dp).height(52.dp).semantics(mergeDescendants = true) { contentDescription = "PIE" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(painterResource(R.drawable.pie_symbol), contentDescription = null, modifier = Modifier.size(42.dp, 49.dp))
        Text(
            "PIE", modifier = Modifier.clearAndSetSemantics { },
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = (36 / fontScale).sp, lineHeight = (40 / fontScale).sp),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1, softWrap = false,
        )
    }
}
