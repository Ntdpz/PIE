package com.ntdpz.pie.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.R

@Composable
fun PieBrand(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.pie_logo),
        contentDescription = "PIE",
        modifier = modifier.width(120.dp).height(52.dp),
    )
}
