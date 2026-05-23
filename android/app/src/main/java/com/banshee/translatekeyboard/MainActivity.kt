package com.banshee.translatekeyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalConfiguration
import com.banshee.translatekeyboard.settings.HistoryEntry
import com.banshee.translatekeyboard.settings.Prefs
import com.banshee.translatekeyboard.settings.TargetLang
import com.banshee.translatekeyboard.translate.DeepLClient
import com.banshee.translatekeyboard.translate.Translator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private fun buildColorScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = onColor(accent),
    background = Color.Black,
    surface = Color(0xFF1A1D24),
    surfaceVariant = Color(0xFF23272F),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFA5ADBA),
    error = Color(0xFFFF6B6B),
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val initialText = extractSharedText(intent)
        setContent {
            val prefs = remember { Prefs(this@MainActivity) }
            var accent by remember { mutableStateOf(Color(prefs.accentColor)) }
            MaterialTheme(colorScheme = buildColorScheme(accent)) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot(
                        initialText = initialText,
                        onAccentChanged = { newAccent ->
                            accent = newAccent
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun extractSharedText(intent: Intent?): String {
        intent ?: return ""
        return when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty()
            else -> ""
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(
    initialText: String,
    onAccentChanged: (Color) -> Unit = {},
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val translator = remember { Translator(DeepLClient { prefs.deeplApiKey }) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var source by rememberSaveable {
        mutableStateOf(initialText.ifBlank { prefs.savedSource })
    }
    var translation by rememberSaveable {
        mutableStateOf(if (initialText.isBlank()) prefs.savedTranslation else "")
    }
    var targetLang by rememberSaveable { mutableStateOf(prefs.targetLang) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var inFlight: Job? = remember { null }

    var backgroundUri by remember { mutableStateOf(prefs.backgroundImageUri) }
    var copyButtonColor by remember { mutableStateOf(Color(prefs.copyButtonColor)) }
    var sendButtonColor by remember { mutableStateOf(Color(prefs.sendButtonColor)) }
    var inputBoxColor by remember { mutableStateOf(Color(prefs.inputBoxColor)) }
    var autoTranslate by remember { mutableStateOf(prefs.autoTranslate) }
    var uiOpacity by remember { mutableStateOf(prefs.uiOpacity) }
    var favoriteLanguages by remember { mutableStateOf(prefs.favoriteLanguages) }
    var showLanguagePicker by rememberSaveable { mutableStateOf(false) }
    var historyEntries by remember { mutableStateOf(prefs.loadHistory()) }
    val hasBackground = backgroundUri.isNotBlank()
    val tintedInputColor = inputBoxColor.copy(
        alpha = (inputBoxColor.alpha * uiOpacity).coerceAtMost(0.65f),
    )

    fun refreshHistory() { historyEntries = prefs.loadHistory() }

    LaunchedEffect(source, targetLang) {
        if (source.isBlank()) {
            isLoading = false
        } else if (source != translation) {
            isLoading = true
        }
    }

    fun runTranslate(text: String, lang: TargetLang) {
        inFlight?.cancel()
        inFlight = scope.launch {
            if (text.isBlank()) {
                translation = ""; errorMessage = null; isLoading = false
                prefs.savedTranslation = ""
                return@launch
            }
            isLoading = true; errorMessage = null
            val result = translator.translate(text, lang)
            isLoading = false
            result.onSuccess {
                translation = it
                prefs.savedTranslation = it
            }.onFailure { errorMessage = it.message ?: "Translation failed" }
        }
    }

    LaunchedEffect(autoTranslate) {
        snapshotFlow { source to targetLang }
            .drop(if (initialText.isBlank() && prefs.savedSource.isBlank()) 1 else 0)
            .debounce(250)
            .distinctUntilChanged()
            .collect { (text, lang) ->
                if (!autoTranslate) return@collect
                runTranslate(text, lang)
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasBackground) {
            AsyncImage(
                model = backgroundUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.55f),
                            ),
                        ),
                    ),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.ime),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 4.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = { showHistory = true }) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = stringRes(R.string.history),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            val listState = rememberLazyListState()
            LaunchedEffect(historyEntries.size) {
                if (historyEntries.isNotEmpty()) listState.animateScrollToItem(0)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (historyEntries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringRes(R.string.no_translations_yet),
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 13.sp,
                            )
                        }
                    }
                } else {
                    items(historyEntries, key = { it.id }) { entry ->
                        HistoryBubble(
                            entry = entry,
                            onCopy = {
                                copyToClipboard(context, entry.translation)
                                scope.launch { snackbar.showSnackbar(context.getString(R.string.copied)) }
                            },
                            onFavorite = {
                                prefs.toggleFavorite(entry.id)
                                refreshHistory()
                            },
                            onUse = {
                                source = entry.source
                                translation = entry.translation
                                prefs.savedSource = entry.source
                                prefs.savedTranslation = entry.translation
                            },
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                    TargetLangPicker(
                        selected = targetLang,
                        favorites = favoriteLanguages,
                        hasBackground = hasBackground,
                        onSelect = { picked ->
                            targetLang = picked
                            prefs.targetLang = picked
                        },
                        onOpenPicker = { showLanguagePicker = true },
                    )
                }
                Spacer(Modifier.height(8.dp))
                CombinedTranslateContainer(
                    source = source,
                    translation = translation,
                    targetLangName = targetLang.fullName,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    inputBoxColor = tintedInputColor,
                    sendButtonColor = sendButtonColor,
                    autoTranslate = autoTranslate,
                    onSourceChange = {
                        source = it
                        prefs.savedSource = it
                    },
                    onClearText = {
                        source = ""
                        translation = ""
                        prefs.savedSource = ""
                        prefs.savedTranslation = ""
                    },
                    onCopyTranslation = {
                        if (translation.isBlank()) return@CombinedTranslateContainer
                        copyToClipboard(context, translation)
                        prefs.addHistoryEntry(source, translation, targetLang)
                        refreshHistory()
                        scope.launch { snackbar.showSnackbar(context.getString(R.string.copied)) }
                    },
                    onSendToWhatsApp = {
                        if (translation.isBlank() || source.isBlank()) return@CombinedTranslateContainer
                        prefs.addHistoryEntry(source, translation, targetLang)
                        refreshHistory()
                        sendToWhatsApp(context, translation, prefs.defaultContactNumber)
                    },
                    onTranslateNow = { runTranslate(source, targetLang) },
                )
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(bottom = 120.dp),
        )
    }

    if (showSettings) {
        SettingsDialog(
            prefs = prefs,
            onSaved = {
                backgroundUri = prefs.backgroundImageUri
                copyButtonColor = Color(prefs.copyButtonColor)
                sendButtonColor = Color(prefs.sendButtonColor)
                inputBoxColor = Color(prefs.inputBoxColor)
                autoTranslate = prefs.autoTranslate
                uiOpacity = prefs.uiOpacity
                favoriteLanguages = prefs.favoriteLanguages
                onAccentChanged(Color(prefs.accentColor))
            },
            onDismiss = { showSettings = false },
        )
    }

    if (showLanguagePicker) {
        LanguagePickerSheet(
            selected = targetLang,
            favorites = favoriteLanguages,
            onSelect = { picked ->
                targetLang = picked
                prefs.targetLang = picked
            },
            onToggleFavorite = { lang ->
                prefs.toggleFavoriteLanguage(lang)
                favoriteLanguages = prefs.favoriteLanguages
            },
            onDismiss = { showLanguagePicker = false },
        )
    }

    if (showHistory) {
        HistorySheet(
            prefs = prefs,
            hasBackground = hasBackground,
            onDismiss = {
                showHistory = false
                refreshHistory()
            },
            onUseEntry = { entry ->
                source = entry.source
                translation = entry.translation
                prefs.savedSource = entry.source
                prefs.savedTranslation = entry.translation
                showHistory = false
            },
            onCopyEntry = { entry ->
                copyToClipboard(context, entry.translation)
                scope.launch { snackbar.showSnackbar(context.getString(R.string.copied)) }
            },
        )
    }
}

@Composable
private fun TargetLangPicker(
    selected: TargetLang,
    favorites: List<TargetLang>,
    hasBackground: Boolean,
    onSelect: (TargetLang) -> Unit,
    onOpenPicker: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val list = if (favorites.isEmpty()) listOf(TargetLang.EN) else favorites
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (lang in list) {
            val isSelected = lang == selected
            val baseSurface = if (hasBackground) Color.Black.copy(alpha = 0.30f)
            else Color.White.copy(alpha = 0.06f)
            val activeSurface = if (hasBackground) Color.White.copy(alpha = 0.12f)
            else Color.White.copy(alpha = 0.10f)
            val shape = RoundedCornerShape(14.dp)
            val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .widthIn(min = 64.dp)
                    .clip(shape)
                    .background(if (isSelected) activeSurface else baseSurface)
                    .border(
                        width = if (isSelected) 1.5.dp else 0.8.dp,
                        color = if (isSelected) accent else Color.White.copy(alpha = 0.15f),
                        shape = shape,
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onSelect(lang) },
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .padding(vertical = 11.dp, horizontal = 10.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        lang.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
        // "More" chip — opens the full picker sheet
        val moreShape = RoundedCornerShape(14.dp)
        val moreInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        Box(
            modifier = Modifier
                .widthIn(min = 44.dp)
                .clip(moreShape)
                .background(
                    if (hasBackground) Color.Black.copy(alpha = 0.30f)
                    else Color.White.copy(alpha = 0.06f),
                )
                .border(
                    width = 0.8.dp,
                    color = accent.copy(alpha = 0.5f),
                    shape = moreShape,
                )
                .clickable(
                    interactionSource = moreInteraction,
                    indication = null,
                    onClick = onOpenPicker,
                ),
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = 11.dp, horizontal = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("+", color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerSheet(
    selected: TargetLang,
    favorites: List<TargetLang>,
    onSelect: (TargetLang) -> Unit,
    onToggleFavorite: (TargetLang) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = MaterialTheme.colorScheme.primary
    val others = TargetLang.entries.filter { it !in favorites }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF101216),
        scrimColor = Color.Black.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Select a language",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                    Text(
                        "Translate to",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
            ) {
                if (favorites.isNotEmpty()) {
                    Text(
                        "FAVORITES",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp),
                    )
                    IosGroup {
                        favorites.forEachIndexed { idx, lang ->
                            LanguageRow(
                                lang = lang,
                                isSelected = lang == selected,
                                isFavorite = true,
                                accent = accent,
                                onTap = {
                                    onSelect(lang)
                                    onDismiss()
                                },
                                onToggleFavorite = { onToggleFavorite(lang) },
                            )
                            if (idx < favorites.lastIndex) {
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.05f),
                                    modifier = Modifier.padding(start = 14.dp),
                                )
                            }
                        }
                    }
                }

                if (others.isNotEmpty()) {
                    Text(
                        "ALL LANGUAGES",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 6.dp),
                    )
                    IosGroup {
                        others.forEachIndexed { idx, lang ->
                            LanguageRow(
                                lang = lang,
                                isSelected = lang == selected,
                                isFavorite = false,
                                accent = accent,
                                onTap = {
                                    onSelect(lang)
                                    onDismiss()
                                },
                                onToggleFavorite = { onToggleFavorite(lang) },
                            )
                            if (idx < others.lastIndex) {
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.05f),
                                    modifier = Modifier.padding(start = 14.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun LanguageRow(
    lang: TargetLang,
    isSelected: Boolean,
    isFavorite: Boolean,
    accent: Color,
    onTap: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable { onTap() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                lang.fullName,
                fontSize = 16.sp,
                color = if (isSelected) accent else Color.White,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(
                if (lang.deeplCode != null) "DeepL · ${lang.label}" else "MyMemory · ${lang.label}",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.45f),
            )
        }
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Currently selected",
                tint = accent,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 4.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
            Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                tint = if (isFavorite) accent else Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CardLabel(text: String, trailingLoading: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        if (trailingLoading) {
            Spacer(Modifier.width(8.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CombinedTranslateContainer(
    source: String,
    translation: String,
    targetLangName: String,
    isLoading: Boolean,
    errorMessage: String?,
    inputBoxColor: Color,
    sendButtonColor: Color,
    autoTranslate: Boolean,
    onSourceChange: (String) -> Unit,
    onClearText: () -> Unit,
    onCopyTranslation: () -> Unit,
    onSendToWhatsApp: () -> Unit,
    onTranslateNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val sendEnabled = translation.isNotBlank() && source.isNotBlank()
    val imeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(inputBoxColor)
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 0.6.dp.toPx(),
                )
            }
            .then(if (imeVisible) Modifier else Modifier.navigationBarsPadding()),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "→ $targetLangName",
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = accent,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                if (translation.isNotBlank()) {
                    IconButton(onClick = onCopyTranslation, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            when {
                errorMessage != null -> Text(
                    "⚠ $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                )
                translation.isBlank() && !autoTranslate && source.isNotBlank() -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        OutlinedButton(
                            onClick = onTranslateNow,
                            modifier = Modifier.heightIn(min = 32.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary,
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 14.dp, vertical = 2.dp,
                            ),
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringRes(R.string.translate_action), fontSize = 13.sp)
                        }
                    }
                }
                translation.isBlank() -> Text(
                    if (source.isBlank()) stringRes(R.string.result_placeholder) else "…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                else -> Text(
                    translation,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        HorizontalDivider(
            color = accent.copy(alpha = 0.25f),
            thickness = 0.5.dp,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 6.dp, bottom = 4.dp, top = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = source,
                onValueChange = onSourceChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp, max = 100.dp),
                placeholder = {
                    Text(
                        stringRes(R.string.source_hint),
                        fontSize = 14.sp,
                    )
                },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                trailingIcon = {
                    if (source.isNotBlank()) {
                        IconButton(onClick = onClearText, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringRes(R.string.clear_field),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )

            FilledIconButton(
                onClick = onSendToWhatsApp,
                enabled = sendEnabled,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = sendButtonColor,
                    contentColor = onColor(sendButtonColor),
                    disabledContainerColor = sendButtonColor.copy(alpha = 0.30f),
                    disabledContentColor = onColor(sendButtonColor).copy(alpha = 0.5f),
                ),
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = stringRes(R.string.send_to_whatsapp),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private fun onColor(bg: Color): Color {
    val luminance = 0.2126 * bg.red + 0.7152 * bg.green + 0.0722 * bg.blue
    return if (luminance > 0.55) Color(0xFF111111) else Color.White
}

private fun relativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000L}m"
        diff < 86_400_000L -> "${diff / 3_600_000L}h"
        diff < 604_800_000L -> "${diff / 86_400_000L}d"
        else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
}

@Composable
private fun HistoryBubble(
    entry: HistoryEntry,
    onCopy: () -> Unit,
    onFavorite: () -> Unit,
    onUse: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val langName = TargetLang.entries.firstOrNull { it.name == entry.targetLang }?.fullName
        ?: entry.targetLang
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onUse,
            )
            .padding(horizontal = 12.dp, vertical = 3.dp),
    ) {
        Text(
            entry.translation,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 3,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            lineHeight = 18.sp,
        )
        Text(
            entry.source,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            lineHeight = 14.sp,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$langName · ${relativeTime(entry.timestamp)}",
                color = accent.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onFavorite, modifier = Modifier.size(28.dp)) {
                Icon(
                    if (entry.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (entry.favorite) accent else Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(14.dp),
                )
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySheet(
    prefs: Prefs,
    hasBackground: Boolean,
    onDismiss: () -> Unit,
    onUseEntry: (HistoryEntry) -> Unit,
    onCopyEntry: (HistoryEntry) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var entries by remember { mutableStateOf(prefs.loadHistory()) }
    var showFavoritesOnly by rememberSaveable { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    val visible = remember(entries, showFavoritesOnly) {
        if (showFavoritesOnly) entries.filter { it.favorite } else entries
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF101216),
        scrimColor = Color.Black.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(max = 640.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringRes(R.string.history),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f),
                )
                if (entries.isNotEmpty()) {
                    IconButton(onClick = { confirmClear = true }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = stringRes(R.string.clear_history),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showFavoritesOnly,
                    onClick = { showFavoritesOnly = false },
                    label = { Text(stringRes(R.string.all), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                    },
                )
                FilterChip(
                    selected = showFavoritesOnly,
                    onClick = { showFavoritesOnly = true },
                    label = { Text(stringRes(R.string.favorites_only), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                    },
                )
            }
            Spacer(Modifier.height(8.dp))

            if (visible.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (showFavoritesOnly) Icons.Default.FavoriteBorder else Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringRes(
                                if (showFavoritesOnly) R.string.empty_favorites
                                else R.string.empty_history,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    visible.forEach { entry ->
                        HistoryItem(
                            entry = entry,
                            onUse = { onUseEntry(entry) },
                            onCopy = { onCopyEntry(entry) },
                            onFavoriteToggle = {
                                prefs.toggleFavorite(entry.id)
                                entries = prefs.loadHistory()
                            },
                            onDelete = {
                                prefs.deleteHistoryEntry(entry.id)
                                entries = prefs.loadHistory()
                            },
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringRes(R.string.clear_history)) },
            text = { Text(stringRes(R.string.confirm_clear)) },
            confirmButton = {
                TextButton(onClick = {
                    prefs.clearAllHistory()
                    entries = emptyList()
                    confirmClear = false
                }) { Text(stringRes(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun HistoryItem(
    entry: HistoryEntry,
    onUse: () -> Unit,
    onCopy: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onUse() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            accent.copy(alpha = if (entry.favorite) 0.5f else 0.12f),
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        "→ ${entry.targetLang}",
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    relativeTime(entry.timestamp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onFavoriteToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (entry.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (entry.favorite) "Unfavorite" else "Favorite",
                        tint = if (entry.favorite) accent
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                entry.source,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                entry.translation,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(
    prefs: Prefs,
    onSaved: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var key by rememberSaveable { mutableStateOf(prefs.deeplApiKey) }
    var contact by rememberSaveable { mutableStateOf(prefs.defaultContactNumber) }
    var bgUri by rememberSaveable { mutableStateOf(prefs.backgroundImageUri) }
    var copyColorInt by rememberSaveable { mutableStateOf(prefs.copyButtonColor) }
    var sendColorInt by rememberSaveable { mutableStateOf(prefs.sendButtonColor) }
    var inputColorInt by rememberSaveable { mutableStateOf(prefs.inputBoxColor) }
    var accentColorInt by rememberSaveable { mutableStateOf(prefs.accentColor) }
    var customColors by remember { mutableStateOf(prefs.customColors) }
    var autoTranslateLocal by rememberSaveable { mutableStateOf(prefs.autoTranslate) }
    var uiOpacityLocal by rememberSaveable { mutableStateOf(prefs.uiOpacity) }
    var showThemePage by rememberSaveable { mutableStateOf(false) }
    var usage by remember { mutableStateOf<com.banshee.translatekeyboard.translate.DeepLUsage?>(null) }
    var usageError by remember { mutableStateOf<String?>(null) }
    var usageLoading by remember { mutableStateOf(false) }

    suspend fun fetchUsage(keyToUse: String) {
        if (keyToUse.isBlank()) { usage = null; usageError = null; return }
        usageLoading = true
        usageError = null
        val result = DeepLClient { keyToUse }.getUsage()
        usageLoading = false
        result.onSuccess { usage = it }.onFailure {
            usage = null
            usageError = it.message ?: "Could not fetch usage"
        }
    }

    LaunchedEffect(Unit) { fetchUsage(prefs.deeplApiKey) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val favorites = remember { prefs.loadHistory().filter { it.favorite }.take(3) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val destination = java.io.File(context.filesDir, "background.jpg")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                }
                bgUri = android.net.Uri.fromFile(destination).toString()
            } catch (_: Exception) {
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(top = 32.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Color(0xFF101216))
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                ),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                ) {
                    if (showThemePage) {
                        IconButton(onClick = { showThemePage = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        if (showThemePage) "Theme" else "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        modifier = Modifier.weight(1f).padding(start = if (showThemePage) 0.dp else 12.dp),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                  if (showThemePage) {
                    val saveCustom: (Int) -> Unit = { c ->
                        prefs.addCustomColor(c)
                        customColors = prefs.customColors
                    }
                    val deleteCustom: (Int) -> Unit = { c ->
                        prefs.removeCustomColor(c)
                        customColors = prefs.customColors
                    }
                    IosSectionLabel("Background")
                    IosGroup {
                        if (bgUri.isNotBlank()) {
                            IosRow {
                                AsyncImage(
                                    model = bgUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                        IosTapRow(
                            title = if (bgUri.isBlank()) "Choose image…" else "Change image…",
                            accent = true,
                            onClick = {
                                pickImageLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        )
                        if (bgUri.isNotBlank()) {
                            IosTapRow(
                                title = "Remove image",
                                danger = true,
                                onClick = { bgUri = "" },
                            )
                        }
                    }

                    IosSectionLabel("Colors")
                    IosGroup {
                        IosColorPickerRow(
                            label = "Accent",
                            selected = accentColorInt,
                            onSelect = { accentColorInt = it },
                            customColors = customColors,
                            onSaveCustom = saveCustom,
                            onDeleteCustom = deleteCustom,
                        )
                        IosColorPickerRow(
                            label = "Send button",
                            selected = sendColorInt,
                            onSelect = { sendColorInt = it },
                            customColors = customColors,
                            onSaveCustom = saveCustom,
                            onDeleteCustom = deleteCustom,
                        )
                        IosColorPickerRow(
                            label = "Copy button",
                            selected = copyColorInt,
                            onSelect = { copyColorInt = it },
                            customColors = customColors,
                            onSaveCustom = saveCustom,
                            onDeleteCustom = deleteCustom,
                        )
                        IosColorPickerRow(
                            label = "Message box",
                            selected = inputColorInt,
                            onSelect = { inputColorInt = it },
                            customColors = customColors,
                            onSaveCustom = saveCustom,
                            onDeleteCustom = deleteCustom,
                            allowAlpha = true,
                        )
                    }

                    IosSectionLabel(stringRes(R.string.ui_opacity))
                    IosGroup {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Opacity",
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${(uiOpacityLocal * 100).toInt()}%",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                )
                            }
                            Slider(
                                value = uiOpacityLocal,
                                onValueChange = { uiOpacityLocal = it },
                                valueRange = 0f..1f,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                stringRes(R.string.ui_opacity_help),
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.55f),
                            )
                        }
                    }

                    Spacer(Modifier.height(48.dp))
                  } else {
                    IosSectionLabel("Behavior")
                    IosGroup {
                        IosToggleRow(
                            title = stringRes(R.string.auto_translate),
                            hint = stringRes(R.string.auto_translate_help),
                            checked = autoTranslateLocal,
                            onCheckedChange = { autoTranslateLocal = it },
                        )
                    }

                    IosSectionLabel("WhatsApp")
                    IosGroup {
                        IosRow {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Default contact", fontSize = 15.sp, color = Color.White)
                                Spacer(Modifier.height(6.dp))
                                InlineTextField(
                                    value = contact,
                                    onValueChange = { contact = it },
                                    placeholder = "+62 812 3456 7890",
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringRes(R.string.contact_help),
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }

                    IosSectionLabel("Appearance")
                    IosGroup {
                        IosTapRow(
                            title = "Theme",
                            accent = true,
                            onClick = { showThemePage = true },
                        )
                    }

                    if (favorites.isNotEmpty()) {
                        IosSectionLabel("Favorites")
                        IosGroup {
                            favorites.forEach { entry ->
                                IosRow {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            entry.translation,
                                            fontSize = 15.sp,
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            "${entry.source} · ${TargetLang.entries.firstOrNull { it.name == entry.targetLang }?.fullName ?: entry.targetLang}",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.55f),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    IosSectionLabel("API settings")
                    IosGroup {
                        IosRow {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("DeepL API key", fontSize = 15.sp, color = Color.White)
                                Spacer(Modifier.height(6.dp))
                                InlineTextField(
                                    value = key,
                                    onValueChange = { key = it },
                                    placeholder = "Paste your DeepL key",
                                )
                            }
                        }
                        IosTapRow(
                            title = if (testing) "Testing…" else "Test key",
                            accent = true,
                            enabled = key.isNotBlank() && !testing,
                            onClick = {
                                scope.launch {
                                    testing = true; testResult = null
                                    val r = DeepLClient { key }.translate("hello", TargetLang.ID.deeplCode!!)
                                    testing = false
                                    testResult = r.fold(
                                        onSuccess = { "OK → $it" },
                                        onFailure = { "Error: ${it.message}" },
                                    )
                                    fetchUsage(key)
                                }
                            },
                        )
                    }
                    testResult?.let {
                        Text(
                            it,
                            color = if (it.startsWith("OK")) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 14.dp, top = 4.dp),
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    UsageCard(
                        usage = usage,
                        loading = usageLoading,
                        error = usageError,
                        onRefresh = { scope.launch { fetchUsage(key) } },
                    )

                    Spacer(Modifier.height(48.dp))
                  }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                Button(
                    onClick = {
                        prefs.deeplApiKey = key
                        prefs.defaultContactNumber = contact
                        prefs.backgroundImageUri = bgUri
                        prefs.copyButtonColor = copyColorInt
                        prefs.sendButtonColor = sendColorInt
                        prefs.inputBoxColor = inputColorInt
                        prefs.accentColor = accentColorInt
                        prefs.autoTranslate = autoTranslateLocal
                        prefs.uiOpacity = uiOpacityLocal
                        onSaved()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun UsageCard(
    usage: com.banshee.translatekeyboard.translate.DeepLUsage?,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Usage",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp,
                    color = accent,
                )
            } else {
                IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "Refresh",
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        when {
            error != null -> {
                Spacer(Modifier.height(6.dp))
                Text(
                    "⚠ $error",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            usage == null && !loading -> {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Enter a DeepL key and tap Test or refresh to see usage.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
            usage != null -> {
                val u = usage
                val pct = (u.percentUsed * 100f)
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { u.percentUsed },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (u.percentUsed > 0.85f) MaterialTheme.colorScheme.error else accent,
                    trackColor = Color.White.copy(alpha = 0.08f),
                )
                Spacer(Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Used", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                        Text(
                            formatChars(u.charactersUsed),
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Left", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                        Text(
                            formatChars(u.charactersRemaining),
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Used %", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                        Text(
                            "${"%.1f".format(pct)}%",
                            fontSize = 15.sp,
                            color = if (u.percentUsed > 0.85f) MaterialTheme.colorScheme.error else accent,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Plan limit: ${formatChars(u.charactersLimit)} chars",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.45f),
                )
            }
        }
    }
}

private fun formatChars(n: Long): String = when {
    n >= 1_000_000 -> "%.2fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%,d".format(n)
    else -> n.toString()
}

@Composable
private fun InlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = 15.sp,
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = Color.White.copy(alpha = 0.40f),
                        fontSize = 15.sp,
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun IosSectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Color.White.copy(alpha = 0.50f),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 14.dp, top = 18.dp, end = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun IosGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f)),
        content = content,
    )
}

@Composable
private fun IosRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun IosTapRow(
    title: String,
    accent: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontSize = 15.sp,
            color = when {
                !enabled -> Color.White.copy(alpha = 0.30f)
                accent -> MaterialTheme.colorScheme.primary
                danger -> MaterialTheme.colorScheme.error
                else -> Color.White
            },
            fontWeight = if (accent) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun IosToggleRow(
    title: String,
    hint: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = Color.White)
            if (!hint.isNullOrBlank()) {
                Text(hint, fontSize = 11.sp, color = Color.White.copy(alpha = 0.55f))
            }
        }
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun IosColorPickerRow(
    label: String,
    selected: Int,
    onSelect: (Int) -> Unit,
    customColors: List<Int>,
    onSaveCustom: (Int) -> Unit,
    onDeleteCustom: (Int) -> Unit,
    allowAlpha: Boolean = false,
) {
    val accent = MaterialTheme.colorScheme.primary
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, color = Color.White)
        }
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(selected))
                .border(0.8.dp, Color.White.copy(alpha = 0.35f), CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        OutlinedButton(
            onClick = { showPicker = true },
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                accent.copy(alpha = 0.6f),
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 12.dp, vertical = 2.dp,
            ),
            modifier = Modifier.heightIn(min = 32.dp),
        ) {
            Text(stringRes(R.string.pick_color), fontSize = 13.sp, color = accent)
        }
    }

    if (showPicker) {
        PickColorDialog(
            initialColor = selected,
            allowAlpha = allowAlpha,
            presets = Prefs.COLOR_PRESETS,
            customColors = customColors,
            onSave = { picked, addToCustoms ->
                onSelect(picked)
                if (addToCustoms) onSaveCustom(picked)
                showPicker = false
            },
            onDeleteCustom = onDeleteCustom,
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PickColorDialog(
    initialColor: Int,
    allowAlpha: Boolean,
    presets: List<Int>,
    customColors: List<Int>,
    onSave: (Int, addToCustoms: Boolean) -> Unit,
    onDeleteCustom: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var picked by remember { mutableStateOf(initialColor) }
    var hexInput by remember { mutableStateOf(String.format(if (allowAlpha) "%08X" else "%06X", if (allowAlpha) initialColor else initialColor and 0xFFFFFF)) }
    var showWheel by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Int?>(null) }
    val accent = MaterialTheme.colorScheme.primary

    val allColors = remember(customColors, presets) {
        // customs first, then standard presets
        (customColors + presets).distinct()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101216)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringRes(R.string.pick_color),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(picked))
                            .border(0.8.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    )
                }
                Spacer(Modifier.height(16.dp))

                HoneycombGrid(
                    colors = allColors,
                    selected = picked,
                    customSet = customColors.toSet(),
                    onSelect = {
                        picked = it
                        hexInput = String.format(
                            if (allowAlpha) "%08X" else "%06X",
                            if (allowAlpha) it else it and 0xFFFFFF,
                        )
                    },
                    onLongPress = { c -> if (c in customColors) pendingDelete = c },
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        val cleaned = input.uppercase().filter { it.isLetterOrDigit() }.take(if (allowAlpha) 8 else 6)
                        hexInput = cleaned
                        val target = if (allowAlpha) 8 else 6
                        if (cleaned.length == target) {
                            runCatching {
                                val v = cleaned.toLong(16)
                                picked = if (allowAlpha) v.toInt() else (0xFF000000.toInt() or v.toInt())
                            }
                        }
                    },
                    leadingIcon = { Text("#", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp) },
                    placeholder = { Text(if (allowAlpha) "AARRGGBB" else "RRGGBB", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        letterSpacing = 0.05.em,
                    ),
                )

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showWheel = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(stringRes(R.string.open_color_wheel))
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            val isCustomAlready = picked in customColors
                            val isPreset = picked in presets
                            onSave(picked, !isCustomAlready && !isPreset)
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Save") }
                }
            }
        }
    }

    if (showWheel) {
        ColorWheelDialog(
            initialColor = picked,
            allowAlpha = allowAlpha,
            onConfirm = { c ->
                picked = c
                hexInput = String.format(
                    if (allowAlpha) "%08X" else "%06X",
                    if (allowAlpha) c else c and 0xFFFFFF,
                )
                showWheel = false
            },
            onDismiss = { showWheel = false },
        )
    }

    pendingDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this saved color?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCustom(c)
                    pendingDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun HoneycombGrid(
    colors: List<Int>,
    selected: Int,
    customSet: Set<Int>,
    onSelect: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val swatchSize = 44.dp
    val perRow = 5
    val rowOffset = (swatchSize / 2)
    val rows = colors.chunked(perRow)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((-8).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEachIndexed { rowIdx, row ->
            Row(
                modifier = Modifier.padding(start = if (rowIdx % 2 == 1) rowOffset else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { c ->
                    val isSelected = c == selected
                    val scale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = 0.5f,
                            stiffness = 600f,
                        ),
                        label = "swatch-scale",
                    )
                    val isCustom = c in customSet
                    Box(
                        modifier = Modifier
                            .size(swatchSize)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(
                                width = if (isSelected) 2.5.dp else 0.8.dp,
                                color = if (isSelected) accent
                                else Color.White.copy(alpha = 0.18f),
                                shape = CircleShape,
                            )
                            .combinedClickable(
                                onClick = { onSelect(c) },
                                onLongClick = if (isCustom) { { onLongPress(c) } } else null,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorWheelDialog(
    initialColor: Int,
    allowAlpha: Boolean,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val initHsv = remember(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor, hsv)
        hsv
    }
    var hue by remember { mutableStateOf(initHsv[0]) }
    var saturation by remember { mutableStateOf(initHsv[1]) }
    var value by remember { mutableStateOf(initHsv[2].coerceAtLeast(0.15f)) }
    var alpha by remember {
        mutableStateOf(((initialColor ushr 24) and 0xFF).toFloat() / 255f)
    }
    val color = Color.hsv(hue, saturation, value).copy(alpha = if (allowAlpha) alpha else 1f)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101216)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Pick a color",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White,
                )
                Spacer(Modifier.height(14.dp))

                Canvas(
                    modifier = Modifier
                        .size(240.dp)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val radius = (if (w < h) w else h) / 2f
                                val cx = w / 2f
                                val cy = h / 2f
                                fun apply(pos: Offset) {
                                    val dx = pos.x - cx
                                    val dy = pos.y - cy
                                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                    val ang = (kotlin.math.atan2(dy, dx) * 180.0 / Math.PI).toFloat()
                                    hue = ((ang + 360f) % 360f)
                                    saturation = (dist / radius).coerceIn(0f, 1f)
                                }
                                val down = awaitFirstDown(requireUnconsumed = false)
                                apply(down.position)
                                drag(down.id) { change ->
                                    apply(change.position)
                                    change.consume()
                                }
                            }
                        },
                ) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Red, Color.Yellow, Color.Green,
                                Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
                            ),
                            center = center,
                        ),
                        radius = radius,
                        center = center,
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                    drawCircle(
                        color = Color.Black.copy(alpha = 1f - value),
                        radius = radius,
                        center = center,
                    )
                    val angleRad = (hue * Math.PI / 180.0).toFloat()
                    val px = center.x + kotlin.math.cos(angleRad) * saturation * radius
                    val py = center.y + kotlin.math.sin(angleRad) * saturation * radius
                    drawCircle(
                        color = Color.White,
                        radius = 9.dp.toPx(),
                        center = Offset(px, py),
                        style = Stroke(width = 2.5.dp.toPx()),
                    )
                    drawCircle(
                        color = Color.hsv(hue, saturation, value),
                        radius = 7.dp.toPx(),
                        center = Offset(px, py),
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    "Brightness",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Start),
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0.05f..1f,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (allowAlpha) {
                    Text(
                        "Opacity",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.Start),
                    )
                    Slider(
                        value = alpha,
                        onValueChange = { alpha = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    )
                    Spacer(Modifier.width(12.dp))
                    val argb = color.toArgb()
                    Text(
                        "#" + String.format(
                            if (allowAlpha) "%08X" else "%06X",
                            if (allowAlpha) argb else (argb and 0xFFFFFF),
                        ),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color.White,
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(color.toArgb()) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun stringRes(id: Int): String = LocalContext.current.getString(id)

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("translation", text))
}

private fun sendToWhatsApp(context: Context, text: String, contactDigits: String) {
    val payload = text.ifBlank { "" }
    val encoded = Uri.encode(payload)
    val digits = contactDigits.filter { it.isDigit() }

    if (digits.isNotEmpty()) {
        val directUrl = "https://wa.me/$digits?text=$encoded"
        val direct = Intent(Intent.ACTION_VIEW, Uri.parse(directUrl)).apply {
            setPackage("com.whatsapp")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(direct)
            return
        } catch (_: Exception) {
        }
        val noPkg = Intent(Intent.ACTION_VIEW, Uri.parse(directUrl))
            .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        try {
            context.startActivity(noPkg)
            return
        } catch (_: Exception) {
        }
    }

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        setPackage("com.whatsapp")
        putExtra(Intent.EXTRA_TEXT, payload)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(sendIntent)
        return
    } catch (_: Exception) {
    }
    val fallback = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://wa.me/?text=$encoded"),
    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
    context.startActivity(fallback)
}
