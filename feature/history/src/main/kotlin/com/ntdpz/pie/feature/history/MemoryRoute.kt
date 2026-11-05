package com.ntdpz.pie.feature.history

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.designsystem.format.PieDates
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

@Composable
fun MemoryRoute(viewModel: MemoryViewModel, onBack: () -> Unit, onEdit: (LocalDate) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val loader = rememberMemoryImageLoader()
    var readingDate by remember { mutableStateOf<LocalDate?>(null) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PieLoader("กำลังเปิดความทรงจำ…") }
            state.failed -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { PiePrimaryButton("เปิดไม่สำเร็จ · ลองอีกครั้ง", viewModel::retry) }
            state.days.isEmpty() -> Text("ยังไม่มีความทรงจำ", Modifier.padding(start = 24.dp, top = 84.dp))
            else -> {
                val day = state.days.firstOrNull { it.summary.date == viewModel.initialDate }
                if (day == null) {
                    Text("ไม่พบบันทึกของวันนี้", Modifier.padding(start = 24.dp, top = 84.dp))
                } else key(day.summary.date) {
                    val date = day.summary.date
                    var viewport by remember { mutableStateOf(Rect.Zero) }
                    var zooming by remember { mutableStateOf(false) }
                    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { zooming = false }
                    val scroll = rememberScrollState()
                    val scope = rememberCoroutineScope()
                    BoxWithConstraints(
                        Modifier.fillMaxSize().testTag("memory-feed")
                            .onGloballyPositioned { viewport = it.boundsInRoot() },
                    ) {
                        val heroHeight = maxHeight
                        val fadeDistance = with(LocalDensity.current) { heroHeight.toPx().coerceAtLeast(1f) }
                        val headerHeight = with(LocalDensity.current) { 68.dp.roundToPx() }
                        val readingStart = (fadeDistance.toInt() - headerHeight).coerceAtLeast(0)
                        val reading by remember(scroll, readingStart) {
                            derivedStateOf { scroll.value >= readingStart }
                        }
                        LaunchedEffect(date, reading) { readingDate = date.takeIf { reading } }
                        DisposableEffect(date) { onDispose { readingDate = null } }
                        Column(
                            Modifier.fillMaxSize().testTag("memory-entry-$date")
                                .verticalScroll(scroll, enabled = !zooming),
                        ) {
                            if (day.media.isNotEmpty()) {
                                val mediaPager = rememberPagerState { day.media.size }
                                Box(Modifier.fillMaxWidth().height(heroHeight).graphicsLayer {
                                    alpha = 1f - (scroll.value / fadeDistance).coerceIn(0f, 1f)
                                }) {
                                    MemoryMediaCarousel(
                                        day, loader, viewport,
                                        zooming = zooming,
                                        onZoom = { zooming = it },
                                        heroHeight = heroHeight,
                                        pager = mediaPager,
                                    )
                                    Column(
                                        Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(PieDates.month(YearMonth.from(date)), color = Color(0xFFE7AD93), style = MaterialTheme.typography.labelMedium)
                                        Text(PieDates.entry(date).substringBeforeLast(' '), color = Color.White, style = MaterialTheme.typography.headlineMedium)
                                        state.entries[date]?.text?.takeIf { it.isNotBlank() }?.let {
                                            Text(it, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                        MemoryPhotoIndicators(mediaPager)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Button(
                                                onClick = { onEdit(date) },
                                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("memory-edit-$date"),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39332D), contentColor = Color.White),
                                            ) { Text("แก้ไข") }
                                            Button(
                                                onClick = { scope.launch { scroll.animateScrollTo(readingStart.coerceAtMost(scroll.maxValue)) } },
                                                modifier = Modifier.weight(2f).heightIn(min = 48.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE7AD93), contentColor = Color(0xFF302B28)),
                                            ) {
                                                Text("↓ เลื่อนลงอ่านทั้งหมด", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        if (day.media.size > 1) {
                                            Text(
                                                "ปัดซ้าย–ขวาบนภาพเพื่อดูรูปอื่น",
                                                Modifier.align(Alignment.CenterHorizontally),
                                                color = Color(0xFFD4C8B9),
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 76.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(PieDates.entry(date), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                                    TextButton(
                                        onClick = { onEdit(date) },
                                        modifier = Modifier.testTag("memory-edit-$date"),
                                    ) { Text("แก้ไข") }
                                }
                            }
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                val entry = state.entries[date]
                                when {
                                    entry == null -> PieLoader("กำลังเปิดเรื่องเล่า…", compact = true)
                                    entry.failed -> TextButton(onClick = viewModel::retry) { Text("เปิดเรื่องเล่าไม่สำเร็จ · ลองอีกครั้ง") }
                                    entry.text.isNotBlank() -> Text(
                                        styledMemoryText(entry.text, entry.formatting), Modifier.fillMaxWidth().testTag("memory-story-$date"),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 29.sp),
                                    )
                                }
                                if (entry?.text?.isNotBlank() == true) {
                                    HorizontalDivider(Modifier.padding(top = 16.dp))
                                    Text(
                                        "จบบันทึกของวันนี้",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        if (readingDate != null) listOf(Color(0xFF292622), Color(0xFF292622))
                        else listOf(Color(0xCC1D1B19), Color.Transparent),
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledIconButton(
                onClick = onBack,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFF302B28),
                    contentColor = Color.White,
                ),
                modifier = Modifier.size(48.dp),
            ) { PieIcon(PieGlyph.Back, "กลับสู่ความทรงจำ", color = Color.White, size = 20.dp) }
            Crossfade(targetState = readingDate, label = "Feed reading header") { date ->
                Text(
                    date?.let(PieDates::entry) ?: "ความทรงจำ",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
