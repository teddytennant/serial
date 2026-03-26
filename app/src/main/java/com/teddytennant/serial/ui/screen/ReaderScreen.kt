package com.teddytennant.serial.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teddytennant.serial.rsvp.RsvpWord
import com.teddytennant.serial.ui.theme.OrpRed

@Composable
fun ReaderScreen(
    bookId: Long,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val book by viewModel.book.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentWord by viewModel.engine.currentWord.collectAsState()
    val isPlaying by viewModel.engine.isPlaying.collectAsState()
    val wordIndex by viewModel.engine.wordIndex.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val chapterIndex by viewModel.currentChapterIndex.collectAsState()
    val wpm by viewModel.wpm.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var showChapterPicker by remember { mutableStateOf(false) }

    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
    }

    // Auto-hide controls after delay when playing
    LaunchedEffect(isPlaying, showControls) {
        if (isPlaying && showControls) {
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }

    val orpColor = settings?.let { Color(it.orpColor.toULong()) } ?: OrpRed
    val textSize = settings?.textSize ?: 48

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (isPlaying) {
                    viewModel.pause()
                    showControls = true
                } else {
                    showControls = !showControls
                }
            }
    ) {
        when {
            loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onBack) { Text("Go back") }
                }
            }
            else -> {
                // RSVP word display
                RsvpWordDisplay(
                    word = currentWord,
                    orpColor = orpColor,
                    textSize = textSize,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Controls overlay
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                showControls = false
                                viewModel.play()
                            }
                    ) {
                        // Top bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                viewModel.pause()
                                onBack()
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book?.title ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1
                                )
                                // Chapter picker
                                Box {
                                    Text(
                                        text = chapters.getOrNull(chapterIndex)?.title ?: "Chapter ${chapterIndex + 1}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.clickable { showChapterPicker = true }
                                    )
                                    DropdownMenu(
                                        expanded = showChapterPicker,
                                        onDismissRequest = { showChapterPicker = false }
                                    ) {
                                        chapters.forEachIndexed { index, chapter ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "${index + 1}. ${chapter.title}",
                                                        fontWeight = if (index == chapterIndex) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.goToChapter(index)
                                                    showChapterPicker = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Center play/pause and chapter nav
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.previousChapter() }) {
                                Icon(
                                    Icons.Default.SkipPrevious,
                                    contentDescription = "Previous chapter",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            IconButton(onClick = { viewModel.nextChapter() }) {
                                Icon(
                                    Icons.Default.SkipNext,
                                    contentDescription = "Next chapter",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Bottom controls
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // WPM controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.setWpm(wpm - 25) }) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Decrease speed",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                Text(
                                    text = "$wpm WPM",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.width(100.dp),
                                    textAlign = TextAlign.Center
                                )

                                IconButton(onClick = { viewModel.setWpm(wpm + 25) }) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Increase speed",
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }

                            // WPM presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf(300, 500, 700, 1000).forEach { preset ->
                                    TextButton(
                                        onClick = { viewModel.setWpm(preset) }
                                    ) {
                                        Text(
                                            "$preset",
                                            color = if (wpm == preset)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (wpm == preset) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            // Progress bar
                            val totalWords = viewModel.engine.totalWords
                            val progress = if (totalWords > 0) wordIndex.toFloat() / totalWords else 0f

                            Slider(
                                value = progress,
                                onValueChange = { newProgress ->
                                    viewModel.seekToWord((newProgress * totalWords).toInt())
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            // Progress text
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Ch ${chapterIndex + 1}/${chapters.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RsvpWordDisplay(
    word: RsvpWord?,
    orpColor: Color,
    textSize: Int,
    modifier: Modifier = Modifier
) {
    if (word == null) {
        Text(
            text = "Tap to start",
            modifier = modifier,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    // Fixed-width alignment: the ORP character is always at the center
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Guide marker
        Text(
            text = "|",
            fontSize = (textSize * 0.4f).sp,
            fontFamily = FontFamily.Monospace,
            color = orpColor.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )

        // The word with ORP highlighted
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                    append(word.beforeOrp)
                }
                withStyle(SpanStyle(color = orpColor, fontWeight = FontWeight.Bold)) {
                    append(word.orpChar.toString())
                }
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                    append(word.afterOrp)
                }
            },
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = textSize.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            letterSpacing = 2.sp
        )

        // Bottom guide
        Text(
            text = "|",
            fontSize = (textSize * 0.4f).sp,
            fontFamily = FontFamily.Monospace,
            color = orpColor.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}
