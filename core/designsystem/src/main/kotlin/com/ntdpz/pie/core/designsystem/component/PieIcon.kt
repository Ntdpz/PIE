package com.ntdpz.pie.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.R

enum class PieGlyph(@param:DrawableRes val resource: Int) {
    Sun(R.drawable.ic_theme_sun), Moon(R.drawable.ic_theme_moon), Device(R.drawable.ic_theme_device),
    Home(R.drawable.ic_house), Calendar(R.drawable.ic_calendar_days),
    Settings(R.drawable.ic_settings_2), Invitation(R.drawable.ic_message_circle_heart),
    Write(R.drawable.ic_pen_line), Book(R.drawable.ic_book_open), Check(R.drawable.ic_check),
    Back(R.drawable.ic_arrow_left), Previous(R.drawable.ic_chevron_left),
    Next(R.drawable.ic_chevron_right), More(R.drawable.ic_ellipsis),
    Media(R.drawable.ic_image_plus), Emoji(R.drawable.ic_smile), Hash(R.drawable.ic_hash),
    Lock(R.drawable.ic_lock_keyhole), Bell(R.drawable.ic_bell), Download(R.drawable.ic_download),
    Storage(R.drawable.ic_hard_drive), Archive(R.drawable.ic_archive), Info(R.drawable.ic_info),
    Eye(R.drawable.ic_eye), EyeOff(R.drawable.ic_eye_off), Delete(R.drawable.ic_delete),
    Plus(R.drawable.ic_plus), Clock(R.drawable.ic_clock_3), Ring(R.drawable.ic_bell_ring),
    Play(R.drawable.ic_play), Close(R.drawable.ic_x), Trash(R.drawable.ic_trash_2),
}

@Composable
fun PieIcon(
    glyph: PieGlyph,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    size: Dp = 22.dp,
) {
    Icon(painterResource(glyph.resource), contentDescription, modifier.size(size), tint = color)
}
