package tv.zelta.app.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.zelta.app.data.PlaylistSource
import tv.zelta.app.data.SourceType
import tv.zelta.app.player.PlayerViewModel
import tv.zelta.app.settings.AppSettings
import tv.zelta.app.settings.I18n
import tv.zelta.app.ui.components.BrandLockup
import tv.zelta.app.ui.components.BrandMark
import tv.zelta.app.ui.components.EmptyPanel
import tv.zelta.app.ui.components.Keycap
import tv.zelta.app.ui.components.PanelSurface
import tv.zelta.app.ui.components.SectionLabel
import tv.zelta.app.ui.components.ZeltaButton
import tv.zelta.app.ui.components.ZeltaChip
import tv.zelta.app.ui.components.ZeltaTextButton
import tv.zelta.app.ui.components.ZeltaTextField
import tv.zelta.app.ui.components.autoFocus
import tv.zelta.app.ui.components.channelCountText
import tv.zelta.app.ui.components.ztSetup
import tv.zelta.app.ui.theme.Ri
import tv.zelta.app.ui.theme.RiIcon

private enum class SetupMode { URL, FILE, LIBRARY }

/**
 * First-run wizard mirroring the desktop flow: welcome, source selection and a
 * ready step. A demo playlist is always available so a TV remote alone is enough.
 */
@Composable
fun SetupScreen(
    settings: AppSettings,
    viewModel: PlayerViewModel,
    onDone: () -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(0) }
    var mode by rememberSaveable { mutableStateOf(SetupMode.URL) }
    var url by rememberSaveable { mutableStateOf("") }
    var alias by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedLibraryId by rememberSaveable { mutableStateOf(viewModel.activeSource?.id) }
    var importing by remember { mutableStateOf(false) }
    var importedName by remember { mutableStateOf<String?>(null) }
    val urlRequired = ztSetup("M3U adresi gerekli.", "An M3U URL is required.")
    val addFailed = ztSetup("Adres eklenemedi.", "Could not add the URL.")
    val readingFile = ztSetup("Dosya okunuyor…", "Reading the file…")
    val chooseFile = ztSetup("Önce bir M3U dosyası seçin.", "Choose an M3U file first.")
    val selectSource = ztSetup("Bir kaynak seçin.", "Select a source.")

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importing = true
            importedName = uri.lastPathSegment
            viewModel.importFile(uri)
        }
    }

    LaunchedEffect(viewModel.loading, viewModel.loadError, importing) {
        if (importing && !viewModel.loading) {
            importing = false
            val failure = viewModel.loadError
            if (failure != null) {
                error = failure
            } else if (viewModel.channels.isNotEmpty()) {
                step = 3
            }
        }
    }

    val scroll = rememberScrollState()
    val activity = LocalContext.current as? Activity
    BackHandler(enabled = step > 0) {
        if (step > 0) step -= 1
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .widthIn(max = 1040.dp),
        ) {
            val narrow = maxWidth < 760.dp
            val cardHeight = if (maxHeight > 680.dp) 620.dp else maxHeight - 32.dp
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
            ) {
                if (!narrow) {
                    SetupBrandPanel(active = step, modifier = Modifier.fillMaxHeight().width(320.dp))
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scroll)
                            .padding(
                                horizontal = if (narrow) 16.dp else 26.dp,
                                vertical = if (narrow) 16.dp else 22.dp,
                            ),
                    ) {
                        when (step) {
                            0 -> LanguageStep(
                                settings = settings,
                                onLanguage = { language ->
                                    if (settings.language != language) {
                                        settings.updateLanguage(language)
                                        activity?.recreate()
                                    }
                                },
                                onContinue = { step = 1 },
                            )
                            1 -> WelcomeStep(
                                settings = settings,
                                onLanguage = { language ->
                                    if (settings.language != language) {
                                        settings.updateLanguage(language)
                                        activity?.recreate()
                                    }
                                },
                                onStart = { step = 2 },
                            )
                            2 -> SourceStep(
                                viewModel = viewModel,
                                mode = mode,
                                onModeChange = {
                                    error = null
                                    mode = it
                                },
                                url = url,
                                onUrlChange = { url = it },
                                alias = alias,
                                onAliasChange = { alias = it },
                                error = error,
                                selectedLibraryId = selectedLibraryId,
                                onSelectLibrary = {
                                    error = null
                                    selectedLibraryId = it
                                },
                                importedName = importedName,
                                importing = importing,
                                onPickFile = { filePicker.launch(arrayOf("*/*")) },
                                onBack = { step = 1; error = null },
                                onContinue = {
                                    error = null
                                    when (mode) {
                                        SetupMode.URL -> {
                                            if (url.isBlank()) {
                                                error = urlRequired
                                            } else if (viewModel.addUrlSource(url, alias)) {
                                                step = 3
                                            } else {
                                                error = viewModel.loadError ?: addFailed
                                            }
                                        }
                                        SetupMode.FILE -> {
                                            if (importing) {
                                                error = readingFile
                                            } else if (viewModel.channels.isEmpty()) {
                                                error = chooseFile
                                                filePicker.launch(arrayOf("*/*"))
                                            } else {
                                                step = 3
                                            }
                                        }
                                        SetupMode.LIBRARY -> {
                                            val chosenId = selectedLibraryId ?: viewModel.activeSource?.id
                                            val source = viewModel.sources.firstOrNull { it.id == chosenId }
                                            if (source == null) {
                                                error = selectSource
                                            } else {
                                                viewModel.loadSource(source)
                                                step = 3
                                            }
                                        }
                                    }
                                },
                            )
                            3 -> ReadyStep(
                                viewModel = viewModel,
                                settings = settings,
                                onBack = { step = 2 },
                                onDone = onDone,
                            )
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (step <= 1) {
                            ZeltaTextButton(ztSetup("Daha sonra ayarla", "Set up later"), {
                                settings.completeSetup()
                                onDone()
                            })
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${ztSetup("Adım", "Step")} ${step + 1}/4",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupBrandPanel(active: Int, modifier: Modifier = Modifier) {
    val focus = tv.zelta.app.ui.theme.zeltaColors.focus
    val labels = listOf(
        ztSetup("Dil", "Language") to ztSetup("Arayüz", "Interface"),
        ztSetup("Hoş geldiniz", "Welcome") to ztSetup("Başlangıç", "Start"),
        ztSetup("Kaynak", "Source") to ztSetup("Liste seçimi", "Playlist"),
        ztSetup("Hazır", "Ready") to ztSetup("Kurulum", "Install"),
    )
    Column(
        modifier
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(focus.copy(alpha = 0.10f), MaterialTheme.colorScheme.surface.copy(alpha = 0.15f)),
                ),
            )
            .padding(horizontal = 26.dp, vertical = 28.dp),
    ) {
        BrandLockup()
        Spacer(Modifier.height(10.dp))
        Text(
            ztSetup(
                "Uydu alıcısı deneyimi — oynatma listenizi ekleyin",
                "Satellite receiver experience — add your playlist",
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(34.dp))
        labels.forEachIndexed { index, item ->
            val done = index < active
            val current = index == active
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                current -> focus.copy(alpha = 0.18f)
                                done -> MaterialTheme.colorScheme.surface
                                else -> Color.Transparent
                            },
                        )
                        .border(
                            1.dp,
                            if (current) focus else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(50),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        RiIcon(Ri.Check, size = 12.dp, tint = focus)
                    } else {
                        Text(
                            (index + 1).toString(),
                            color = if (current) focus else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        item.first,
                        color = if (current || done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = if (current) FontWeight.SemiBold else FontWeight.Medium,
                    )
                    Text(item.second, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
            if (index < labels.lastIndex) {
                Box(
                    Modifier
                        .padding(start = 11.dp)
                        .width(2.dp)
                        .height(26.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            "ZeltaTV 1.0.0 · GPL-3.0",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 9.sp,
        )
    }
}

@Composable
private fun StepIndicator(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        val labels = listOf(
            ztSetup("Hoş geldiniz", "Welcome"),
            ztSetup("Kaynak", "Source"),
            ztSetup("Hazır", "Ready"),
        )
        labels.forEachIndexed { index, label ->
            val active = index == step
            val done = index < step
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                        else if (done) MaterialTheme.colorScheme.surfaceVariant
                        else Color.Transparent,
                    )
                    .border(
                        1.dp,
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                        RoundedCornerShape(50),
                    )
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (done) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    "${index + 1}. $label",
                    color = if (active || done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun LanguageStep(
    settings: AppSettings,
    onLanguage: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val continueFocus = remember { FocusRequester() }

    Column {
        SectionLabel(ztSetup("KURULUM", "SETUP"))
        Spacer(Modifier.height(7.dp))
        Text(
            ztSetup("Dil seç", "Choose your language"),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            ztSetup(
                "Kurulumda kullanmak istediğiniz dili seçin. Daha sonra Ayarlar'dan değiştirebilirsiniz.",
                "Choose the language you want to use in ZeltaTV. You can change it later in Settings.",
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            I18n.LANGUAGES.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (code, label) ->
                        ZeltaChip(label, settings.language == code, { onLanguage(code) })
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        ZeltaButton(
            ztSetup("Devam et", "Continue"),
            onContinue,
            primary = true,
            modifier = Modifier
                .focusRequester(continueFocus)
                .autoFocus(continueFocus),
        )
    }
}

@Composable
private fun WelcomeStep(
    settings: AppSettings,
    onLanguage: (String) -> Unit,
    onStart: () -> Unit,
) {
    val startFocus = remember { FocusRequester() }

    Column {
        SectionLabel(ztSetup("KURULUM", "SETUP"))
        Spacer(Modifier.height(7.dp))
        Text(
            ztSetup("ZeltaTV'ye hoş geldiniz", "Welcome to ZeltaTV"),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            ztSetup(
                "Kendi IPTV listenizi ekleyin; kanallar, favoriler ve izleme geçmişi bu cihazda kalır.",
                "Add your own IPTV playlist; channels, favourites and watch history stay on this device.",
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureCard(Icons.Filled.LiveTv, ztSetup("Uzaktan kumanda", "Remote first"), ztSetup("D-pad ile tam gezinme", "Full D-pad navigation"), Modifier.weight(1f))
            FeatureCard(Icons.Filled.Cloud, ztSetup("M3U / URL", "M3U / URL"), ztSetup("HTTP, dosya ve demo", "HTTP, file and demo"), Modifier.weight(1f))
            FeatureCard(Icons.Filled.Storage, ztSetup("Yerel kütüphane", "Local library"), ztSetup("Favoriler ve geçmiş", "Favourites and history"), Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            ZeltaButton(
                ztSetup("Kaynak ekleyerek başla", "Start with a source"),
                onStart,
                primary = true,
                modifier = Modifier
                    .focusRequester(startFocus)
                    .autoFocus(startFocus),
            )
        }
    }
}

@Composable
private fun FeatureCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(7.dp))
            .padding(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SourceStep(
    viewModel: PlayerViewModel,
    mode: SetupMode,
    onModeChange: (SetupMode) -> Unit,
    url: String,
    onUrlChange: (String) -> Unit,
    alias: String,
    onAliasChange: (String) -> Unit,
    error: String?,
    selectedLibraryId: String?,
    onSelectLibrary: (String) -> Unit,
    importedName: String?,
    importing: Boolean,
    onPickFile: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val continueFocus = remember { FocusRequester() }
    val urlRequired = ztSetup("M3U adresi gerekli.", "An M3U URL is required.")
    val addFailed = ztSetup("Adres eklenemedi.", "Could not add the URL.")
    val readingFile = ztSetup("Dosya okunuyor…", "Reading the file…")
    val chooseFile = ztSetup("Önce bir M3U dosyası seçin.", "Choose an M3U file first.")
    val selectSource = ztSetup("Bir kaynak seçin.", "Select a source.")

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ztSetup("Kaynak seçin", "Choose a source"),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            SectionLabel("${ztSetup("ADIM", "STEP")} 3/4")
        }
        Spacer(Modifier.height(4.dp))
        Text(
            ztSetup("Bir M3U bağlantısı, cihazdaki dosya veya kayıtlı bir liste kullanın.", "Use an M3U link, a local file or a saved playlist."),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ZeltaChip(ztSetup("URL", "URL"), mode == SetupMode.URL, { onModeChange(SetupMode.URL) })
            ZeltaChip(ztSetup("Dosya", "File"), mode == SetupMode.FILE, { onModeChange(SetupMode.FILE) })
            ZeltaChip(ztSetup("Kütüphane", "Library"), mode == SetupMode.LIBRARY, { onModeChange(SetupMode.LIBRARY) })
        }
        Spacer(Modifier.height(14.dp))
        when (mode) {
            SetupMode.URL -> {
                Text(ztSetup("M3U / M3U8 adresi", "M3U / M3U8 address"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                Spacer(Modifier.height(5.dp))
                ZeltaTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    placeholder = "https://example.com/list.m3u",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text(ztSetup("Görünen ad (isteğe bağlı)", "Display name (optional)"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                Spacer(Modifier.height(5.dp))
                ZeltaTextField(
                    value = alias,
                    onValueChange = onAliasChange,
                    placeholder = ztSetup("Örn. Ev listesi", "e.g. Home playlist"),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SetupMode.FILE -> {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(9.dp))
                    Text(
                        importedName ?: ztSetup("M3U dosyası seçilmedi", "No M3U file selected"),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        ztSetup("Telefon, tablet veya USB depolamadan seçin.", "Pick from phone, tablet or USB storage."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    )
                    Spacer(Modifier.height(11.dp))
                    ZeltaButton(ztSetup("Dosya seç", "Choose file"), onPickFile, icon = Icons.Filled.Description)
                }
            }
            SetupMode.LIBRARY -> {
                if (viewModel.sources.isEmpty()) {
                    EmptyPanel(ztSetup("Kayıtlı kaynak yok", "No saved sources"), ztSetup("URL veya dosya ile yeni bir kaynak ekleyin.", "Add a new source with a URL or file."))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        viewModel.sources.forEach { source ->
                            LibraryRow(
                                source = source,
                                selected = selectedLibraryId == source.id || (selectedLibraryId == null && source.id == viewModel.activeSource?.id),
                                onClick = { onSelectLibrary(source.id) },
                            )
                        }
                    }
                }
            }
        }
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error, color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ZeltaTextButton(ztSetup("← Geri", "← Back"), onBack)
            Spacer(Modifier.weight(1f))
            if (importing) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            ZeltaButton(
                if (mode == SetupMode.LIBRARY) ztSetup("Seç ve devam et", "Use and continue") else ztSetup("Devam et", "Continue"),
                onContinue,
                primary = true,
                modifier = Modifier
                    .focusRequester(continueFocus)
                    .autoFocus(continueFocus),
            )
        }
    }
}

@Composable
private fun LibraryRow(source: PlaylistSource, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                if (focused || selected) 1.5.dp else 1.dp,
                if (focused || selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                RoundedCornerShape(6.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            when (source.type) {
                SourceType.FILE -> Icons.Filled.Description
                SourceType.DEMO -> Icons.Filled.LiveTv
                else -> Icons.Filled.Cloud
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(source.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when (source.type) {
                    SourceType.DEMO -> ztSetup("Yerleşik demo liste", "Built-in demo playlist")
                    SourceType.FILE -> source.fileName ?: ztSetup("Yerel dosya", "Local file")
                    else -> source.value
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun ReadyStep(
    viewModel: PlayerViewModel,
    settings: AppSettings,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val doneFocus = remember { FocusRequester() }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        BrandMark(markSize = 52.dp, showWordmark = false)
        Spacer(Modifier.height(14.dp))
        Text(
            ztSetup("Her şey hazır", "You're all set"),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        if (viewModel.loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(ztSetup("Kanal listesi yükleniyor…", "Loading channels…"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        } else if (viewModel.loadError != null) {
            Text(
                viewModel.loadError ?: "",
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                "${channelCountText(viewModel.channels.size)} · ${viewModel.activeSource?.name ?: ""}",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                ztSetup("Kanallar, favoriler ve geçmiş bu cihazda saklanır.", "Channels, favourites and history are stored on this device."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ZeltaButton(ztSetup("← Geri", "← Back"), onBack)
            ZeltaButton(
                ztSetup("İzlemeye başla", "Start watching"),
                {
                    settings.completeSetup()
                    onDone()
                },
                primary = true,
                enabled = !viewModel.loading,
                icon = Icons.Filled.PlayArrow,
                modifier = Modifier
                    .focusRequester(doneFocus)
                    .autoFocus(doneFocus),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Keycap("OK")
            Text(ztSetup("seç", "select"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            Keycap("BACK")
            Text(ztSetup("geri", "back"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
    }
}
