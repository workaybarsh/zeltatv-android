package tv.zelta.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import tv.zelta.app.data.PlaylistSource
import tv.zelta.app.data.SourceType
import tv.zelta.app.data.DemoSourceId
import tv.zelta.app.player.PlayerViewModel
import tv.zelta.app.ui.components.EmptyPanel
import tv.zelta.app.ui.components.PanelSurface
import tv.zelta.app.ui.components.SectionLabel
import tv.zelta.app.ui.components.ZeltaButton
import tv.zelta.app.ui.components.ZeltaChip
import tv.zelta.app.ui.components.ZeltaIconButton
import tv.zelta.app.ui.components.ZeltaRiButton
import tv.zelta.app.ui.components.ZeltaTextButton
import tv.zelta.app.ui.components.ZeltaTextField
import tv.zelta.app.ui.components.autoFocus
import tv.zelta.app.ui.components.zt
import tv.zelta.app.ui.theme.Ri
import tv.zelta.app.ui.theme.RiIcon

/** The demo playlist has no user-given name, so it follows the UI language. */
@Composable
private fun sourceLabel(source: PlaylistSource): String =
    if (source.id == DemoSourceId) zt("Demo liste", "Demo list") else source.name

/** Playlist library: URL/file import, categories and source switching. */
@Composable
fun SourcesScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<PlaylistSource?>(null) }
    var categoryTarget by remember { mutableStateOf<PlaylistSource?>(null) }
    var newCategory by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val addFocus = remember { FocusRequester() }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            error = null
            viewModel.importFile(uri)
        }
    }

    LaunchedEffect(viewModel.loadError) {
        if (viewModel.loadError != null) error = viewModel.loadError
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ZeltaIconButton(Icons.Filled.ArrowBack, onBack, zt("Geri", "Back"))
            Spacer(Modifier.width(6.dp))
            Text(
                zt("Kaynaklar", "Sources"),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "${viewModel.sources.size} ${zt("liste", "playlists")}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
            )
            Spacer(Modifier.weight(1f))
            ZeltaButton(zt("Dosya içe aktar", "Import file"), { filePicker.launch(arrayOf("*/*")) }, icon = Icons.Filled.FolderOpen)
            Spacer(Modifier.width(7.dp))
            ZeltaButton(
                zt("Kaynak ekle", "Add source"),
                {
                    error = null
                    showAdd = true
                },
                modifier = Modifier
                    .focusRequester(addFocus)
                    .autoFocus(addFocus),
                primary = true,
                icon = Icons.Filled.Add,
            )
        }

        if (viewModel.loading) {
            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .widthIn(max = 1000.dp)
                .padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (error != null) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(7.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f), RoundedCornerShape(7.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 10.sp, modifier = Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis)
                        ZeltaTextButton(zt("Kapat", "Dismiss"), { error = null })
                    }
                }
            }

            if (viewModel.sources.isEmpty()) {
                item {
                    EmptyPanel(
                        zt("Henüz kaynak yok", "No sources yet"),
                        zt("M3U bağlantısı ekleyin veya cihazdan bir dosya içe aktarın.", "Add an M3U link or import a file from this device."),
                        Modifier.fillMaxWidth(),
                    )
                }
            } else {
                items(viewModel.sources, key = { it.id }) { source ->
                    SourceCard(
                        source = source,
                        active = source.id == viewModel.activeSource?.id,
                        categoryNames = viewModel.categoryNames(source),
                        loading = viewModel.loading && source.id == viewModel.activeSource?.id,
                        onOpen = {
                            error = null
                            viewModel.loadSource(source)
                        },
                        onCategories = { categoryTarget = source },
                        onDelete = { deleteTarget = source },
                    )
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                SectionLabel(zt("KATEGORİLER", "CATEGORIES"))
                Spacer(Modifier.height(7.dp))
                if (viewModel.categories.isEmpty()) {
                    Text(
                        zt("Henüz kategori yok. Kaynakları gruplamak için ekleyin.", "No categories yet. Add one to group sources."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        viewModel.categories.forEach { category ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 11.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(category.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Text(
                                    "${viewModel.sources.count { category.id in it.categoryIds }} ${zt("kaynak", "sources")}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp,
                                )
                                Spacer(Modifier.width(7.dp))
                                ZeltaIconButton(
                                    Icons.Filled.Delete,
                                    { viewModel.deleteCategory(category.id) },
                                    zt("Kategoriyi sil", "Delete category"),
                                    size = 30.dp,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ZeltaTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        placeholder = zt("Yeni kategori adı", "New category name"),
                        modifier = Modifier.weight(1f),
                    )
                    ZeltaButton(zt("Ekle", "Add"), {
                        if (viewModel.addCategory(newCategory) != null) newCategory = ""
                    }, icon = Icons.Filled.Add)
                }
            }
        }
    }

    if (showAdd) {
        AddSourceDialog(
            viewModel = viewModel,
            onDismiss = { showAdd = false },
            onPickFile = {
                showAdd = false
                filePicker.launch(arrayOf("*/*"))
            },
        )
    }

    deleteTarget?.let { source ->
        ConfirmDialog(
            title = zt("Kaynağı sil", "Delete source"),
            body = zt(
                "\"{name}\" ve buna bağlı kanal listesi kaldırılacak. Favoriler ve geçmiş korunur.",
                "\"{name}\" and its channel list will be removed. Favourites and history are kept.",
            ).replace("{name}", sourceLabel(source)),
            confirmLabel = zt("Sil", "Delete"),
            danger = true,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                error = null
                viewModel.removeSource(source)
                deleteTarget = null
            },
        )
    }

    categoryTarget?.let { source ->
        CategoryAssignDialog(
            viewModel = viewModel,
            source = source,
            onDismiss = { categoryTarget = null },
        )
    }
}

@Composable
private fun SourceCard(
    source: PlaylistSource,
    active: Boolean,
    categoryNames: String,
    loading: Boolean,
    onOpen: () -> Unit,
    onCategories: () -> Unit,
    onDelete: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface)
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) MaterialTheme.colorScheme.primary else if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                RoundedCornerShape(8.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                when (source.type) {
                    SourceType.FILE -> Icons.Filled.Description
                    SourceType.DEMO -> Icons.Filled.LiveTv
                    else -> Icons.Filled.Cloud
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    sourceLabel(source),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (active) {
                    Spacer(Modifier.width(7.dp))
                    Text(
                        zt("ETKİN", "ACTIVE"),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                when (source.type) {
                    SourceType.DEMO -> zt("Yerleşik demo liste", "Built-in demo playlist")
                    SourceType.FILE -> source.fileName ?: zt("Yerel M3U dosyası", "Local M3U file")
                    else -> source.value
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (categoryNames.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(categoryNames, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(9.dp))
        if (loading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        ZeltaButton(
            if (active) zt("Yeniden yükle", "Reload") else zt("Aç", "Open"),
            onOpen,
            primary = !active,
            icon = if (active) Icons.Filled.Storage else Icons.Filled.PlayArrow,
        )
        Spacer(Modifier.width(6.dp))
        ZeltaButton(zt("Kategoriler", "Categories"), onCategories)
        Spacer(Modifier.width(6.dp))
        ZeltaIconButton(Icons.Filled.Delete, onDelete, zt("Sil", "Delete"), tint = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun AddSourceDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var error by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    val urlRequired = zt("M3U adresi gerekli.", "An M3U URL is required.")
    val addFailed = zt("Kaynak eklenemedi.", "Could not add the source.")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .focusProperties { canFocus = false }
                .clickable(onClick = onDismiss)
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            PanelSurface(
                modifier = Modifier
                    .widthIn(max = 620.dp)
                    .fillMaxWidth(0.95f)
                    .focusProperties { canFocus = false }
                    .clickable(onClick = {}),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(zt("Kaynak ekle", "Add source"), color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        ZeltaTextButton(zt("Kapat", "Close"), onDismiss)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(zt("M3U / M3U8 bağlantısı", "M3U / M3U8 link"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Spacer(Modifier.height(4.dp))
                    ZeltaTextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = "https://example.com/playlist.m3u",
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .autoFocus(focusRequester),
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(zt("Görünen ad (isteğe bağlı)", "Display name (optional)"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Spacer(Modifier.height(4.dp))
                    ZeltaTextField(value = alias, onValueChange = { alias = it }, placeholder = zt("Örn. Ev listesi", "e.g. Home playlist"), modifier = Modifier.fillMaxWidth())
                    if (viewModel.categories.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        SectionLabel(zt("KATEGORİLER", "CATEGORIES"))
                        Spacer(Modifier.height(5.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            viewModel.categories.take(5).forEach { category ->
                                ZeltaChip(
                                    category.name,
                                    category.id in selected,
                                    {
                                        selected = if (category.id in selected) selected - category.id else selected + category.id
                                    },
                                )
                            }
                        }
                    }
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ZeltaButton(zt("Dosyadan içe aktar", "Import from file"), onPickFile, icon = Icons.Filled.FolderOpen)
                        Spacer(Modifier.weight(1f))
                        ZeltaButton(zt("İptal", "Cancel"), onDismiss)
                        Spacer(Modifier.width(7.dp))
                        ZeltaButton(zt("Ekle", "Add"), {
                            if (url.isBlank()) {
                                error = urlRequired
                            } else if (viewModel.addUrlSource(url, alias, selected.firstOrNull())) {
                                onDismiss()
                            } else {
                                error = viewModel.loadError ?: addFailed
                            }
                        }, primary = true, icon = Icons.Filled.Add)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryAssignDialog(
    viewModel: PlayerViewModel,
    source: PlaylistSource,
    onDismiss: () -> Unit,
) {
    var selected by remember(source.id) { mutableStateOf(source.categoryIds.toSet()) }
    var newCategory by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .focusProperties { canFocus = false }
                .clickable(onClick = onDismiss)
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            PanelSurface(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(0.95f)
                    .focusProperties { canFocus = false }
                    .clickable(onClick = {}),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(zt("Kategoriler", "Categories"), color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text(sourceLabel(source), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                    if (viewModel.categories.isEmpty()) {
                        Text(zt("Önce bir kategori oluşturun.", "Create a category first."), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            viewModel.categories.forEach { category ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (category.id in selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .border(1.dp, if (category.id in selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                                        .clickable {
                                            selected = if (category.id in selected) selected - category.id else selected + category.id
                                        }
                                        .padding(horizontal = 11.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(category.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    if (category.id in selected) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ZeltaTextField(
                            value = newCategory,
                            onValueChange = { newCategory = it },
                            placeholder = zt("Yeni kategori", "New category"),
                            modifier = Modifier.weight(1f),
                        )
                        ZeltaButton(zt("Oluştur", "Create"), {
                            val created = viewModel.addCategory(newCategory)
                            if (created != null) {
                                selected = selected + created.id
                                newCategory = ""
                            }
                        })
                    }
                    Spacer(Modifier.height(15.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.weight(1f))
                        ZeltaButton(zt("İptal", "Cancel"), onDismiss)
                        Spacer(Modifier.width(7.dp))
                        ZeltaButton(zt("Kaydet", "Save"), {
                            viewModel.setSourceCategories(source.id, selected.toList())
                            onDismiss()
                        }, primary = true)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    danger: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val cancelFocus = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .focusProperties { canFocus = false }
                .clickable(onClick = onDismiss)
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            PanelSurface(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(0.9f)
                    .focusProperties { canFocus = false }
                    .clickable(onClick = {}),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Spacer(Modifier.height(17.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.weight(1f))
                        ZeltaButton(
                            zt("İptal", "Cancel"),
                            onDismiss,
                            modifier = Modifier
                                .focusRequester(cancelFocus)
                                .autoFocus(cancelFocus),
                        )
                        Spacer(Modifier.width(7.dp))
                        ZeltaButton(confirmLabel, onConfirm, primary = !danger, danger = danger)
                    }
                }
            }
        }
    }
}

private enum class SourcesTab { LINK, FILE, LIBRARY }

/**
 * Desktop sources dialog: Link / File / Library tabs with add, rename and delete.
 * Opened from the + button in the top bar.
 */
@Composable
fun SourcesDialog(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
) {
    var tab by remember { mutableStateOf(SourcesTab.LINK) }
    var url by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var editName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<PlaylistSource?>(null) }
    var categoryTarget by remember { mutableStateOf<PlaylistSource?>(null) }
    var newCategory by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val urlMissing = zt("Bir oynatma listesi adresi girin.", "Enter a playlist address.")
    val addFailed = zt("Kaynak eklenemedi.", "Could not add the source.")
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            error = null
            viewModel.importFile(uri, categoryId)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .focusProperties { canFocus = false }
                .clickable(onClick = onDismiss)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            PanelSurface(
                modifier = Modifier
                    .widthIn(max = 672.dp)
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.9f)
                    .focusProperties { canFocus = false }
                    .clickable(onClick = {}),
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                zt("Kaynaklar", "Sources"),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.2).sp,
                            )
                            Text(
                                zt("Bağlantı ya da dosya ekleyin veya kayıtlı kitaplığınızı yönetin.", "Add a link or a file, or manage your saved library."),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                            )
                        }
                        ZeltaRiButton(Ri.Close, onDismiss, zt("Kapat", "Close"), size = 28.dp)
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SourcesTabButton(Ri.Link, zt("Bağlantı", "Link"), tab == SourcesTab.LINK, focus) {
                            tab = SourcesTab.LINK
                            error = null
                        }
                        SourcesTabButton(Ri.FileList, zt("Dosya", "File"), tab == SourcesTab.FILE) {
                            tab = SourcesTab.FILE
                            error = null
                        }
                        SourcesTabButton(Ri.PlayList, zt("Kitaplık", "Library"), tab == SourcesTab.LIBRARY) {
                            tab = SourcesTab.LIBRARY
                            error = null
                        }
                    }

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        when (tab) {
                            SourcesTab.LINK -> {
                                FieldLabel(zt("OYNATMA LİSTESİ ADRESİ", "PLAYLIST ADDRESS"))
                                ZeltaTextField(
                                    value = url,
                                    onValueChange = { url = it; error = null },
                                    placeholder = "https://example.com/playlist.m3u",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                FieldLabel(zt("GÖRÜNEN AD", "DISPLAY NAME"))
                                ZeltaTextField(
                                    value = alias,
                                    onValueChange = { alias = it },
                                    placeholder = zt("Ad (isteğe bağlı)", "Name (optional)"),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                CategoryChips(viewModel, categoryId) { categoryId = it }
                                ZeltaButton(
                                    zt("Bağlantı ekle", "Add link"),
                                    {
                                        if (url.isBlank()) {
                                            error = urlMissing
                                        } else if (viewModel.addUrlSource(url, alias, categoryId)) {
                                            onDismiss()
                                        } else {
                                            error = viewModel.loadError ?: addFailed
                                        }
                                    },
                                    primary = true,
                                    glyph = Ri.Add,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            SourcesTab.FILE -> {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                                        .clickable { filePicker.launch(arrayOf("*/*")) }
                                        .padding(vertical = 26.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    RiIcon(Ri.UploadCloud, size = 30.dp, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        zt("Dosya seç", "Choose file"),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        zt("M3U, M3U8 veya TXT", "M3U, M3U8 or TXT"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                    )
                                }
                                CategoryChips(viewModel, categoryId) { categoryId = it }
                                Text(
                                    zt("Telefon, tablet veya USB depolamadan seçin.", "Pick from phone, tablet or USB storage."),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                )
                            }

                            SourcesTab.LIBRARY -> {
                                if (viewModel.sources.isEmpty()) {
                                    EmptyPanel(
                                        zt("Kitaplık boş", "Your library is empty"),
                                        zt("Başlamak için bir bağlantı veya dosya ekleyin.", "Add a link or a file to get started."),
                                    )
                                } else {
                                    viewModel.sources.forEach { source ->
                                        SourceManagerRow(
                                            source = source,
                                            active = source.id == viewModel.activeSource?.id,
                                            categoryNames = viewModel.categoryNames(source),
                                            editing = editingId == source.id,
                                            editName = editName,
                                            onEditName = { editName = it },
                                            onStartEdit = {
                                                editingId = source.id
                                                editName = source.name
                                            },
                                            onSaveEdit = {
                                                viewModel.renameSource(source, editName)
                                                editingId = null
                                            },
                                            onCancelEdit = { editingId = null },
                                            onOpen = { viewModel.loadSource(source) },
                                            onCategories = { categoryTarget = source },
                                            onDelete = { deleteTarget = source },
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                FieldLabel(zt("KATEGORİLER", "CATEGORIES"))
                                if (viewModel.categories.isEmpty()) {
                                    Text(
                                        zt("Henüz kategori yok. Kaynakları gruplamak için ekleyin.", "No categories yet. Add one to group sources."),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        viewModel.categories.forEach { category ->
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
                                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                    .padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                RiIcon(Ri.FolderLine, size = 13.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    category.name,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                ZeltaRiButton(
                                                    Ri.Delete,
                                                    { viewModel.deleteCategory(category.id) },
                                                    zt("Kategoriyi sil", "Delete category"),
                                                    size = 26.dp,
                                                    tint = MaterialTheme.colorScheme.error,
                                                )
                                            }
                                        }
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    ZeltaTextField(
                                        value = newCategory,
                                        onValueChange = { newCategory = it },
                                        placeholder = zt("Yeni kategori adı", "New category name"),
                                        modifier = Modifier.weight(1f),
                                    )
                                    ZeltaButton(
                                        zt("Ekle", "Add"),
                                        {
                                            if (viewModel.addCategory(newCategory) != null) newCategory = ""
                                        },
                                        glyph = Ri.FolderAdd,
                                    )
                                }
                            }
                        }

                        if (error != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RiIcon(Ri.Warning, size = 14.dp, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(6.dp))
                                Text(error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { source ->
        ConfirmDialog(
            title = zt("Kaynağı sil", "Delete source"),
            body = zt(
                "\"{name}\" ve buna bağlı kanal listesi kaldırılacak. Favoriler ve geçmiş korunur.",
                "\"{name}\" and its channel list will be removed. Favourites and history are kept.",
            ).replace("{name}", sourceLabel(source)),
            confirmLabel = zt("Sil", "Delete"),
            danger = true,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.removeSource(source)
                deleteTarget = null
            },
        )
    }

    categoryTarget?.let { source ->
        CategoryAssignDialog(
            viewModel = viewModel,
            source = source,
            onDismiss = { categoryTarget = null },
        )
    }
}

@Composable
private fun SourcesTabButton(
    glyph: String,
    label: String,
    active: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                if (focused) 2.dp else 0.dp,
                if (focused) tv.zelta.app.ui.theme.zeltaColors.focus else Color.Transparent,
                RoundedCornerShape(6.dp),
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RiIcon(glyph, size = 13.dp, tint = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.4.sp,
    )
}

@Composable
private fun CategoryChips(viewModel: PlayerViewModel, selected: String?, onSelect: (String?) -> Unit) {
    if (viewModel.categories.isEmpty()) return
    Column {
        FieldLabel(zt("KATEGORİ", "CATEGORY"))
        Spacer(Modifier.height(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ZeltaChip(zt("Kategori yok", "No category"), selected == null, { onSelect(null) })
            viewModel.categories.take(4).forEach { category ->
                ZeltaChip(category.name, selected == category.id, { onSelect(category.id) })
            }
        }
    }
}

@Composable
private fun SourceManagerRow(
    source: PlaylistSource,
    active: Boolean,
    categoryNames: String,
    editing: Boolean,
    editName: String,
    onEditName: (String) -> Unit,
    onStartEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onOpen: () -> Unit,
    onCategories: () -> Unit,
    onDelete: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) tv.zelta.app.ui.theme.zeltaColors.focus
                else if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                RoundedCornerShape(8.dp),
            )
            .onFocusChanged { focused = it.isFocused },
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RiIcon(
                when (source.type) {
                    SourceType.FILE -> Ri.FileText
                    SourceType.DEMO -> Ri.Live
                    else -> Ri.Link
                },
                size = 15.dp,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    sourceLabel(source),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    categoryNames.ifBlank {
                        when (source.type) {
                            SourceType.DEMO -> zt("Yerleşik demo liste", "Built-in demo playlist")
                            SourceType.FILE -> source.fileName ?: zt("Yerel M3U dosyası", "Local M3U file")
                            else -> source.value
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (active) {
                Text(
                    zt("ETKİN", "ACTIVE"),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.width(6.dp))
            }
            ZeltaRiButton(Ri.Play, onOpen, zt("Oynat", "Play"), size = 28.dp)
            ZeltaRiButton(Ri.Edit, onStartEdit, zt("Düzenle", "Edit"), size = 28.dp)
            ZeltaRiButton(Ri.FolderLine, onCategories, zt("Kategoriler", "Categories"), size = 28.dp)
            ZeltaRiButton(Ri.Delete, onDelete, zt("Sil", "Delete"), size = 28.dp, tint = MaterialTheme.colorScheme.error)
        }
        if (editing) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ZeltaTextField(
                    value = editName,
                    onValueChange = onEditName,
                    placeholder = zt("Ad", "Name"),
                    modifier = Modifier.weight(1f),
                )
                ZeltaButton(zt("İptal", "Cancel"), onCancelEdit)
                ZeltaButton(zt("Kaydet", "Save"), onSaveEdit, primary = true)
            }
        }
    }
}
