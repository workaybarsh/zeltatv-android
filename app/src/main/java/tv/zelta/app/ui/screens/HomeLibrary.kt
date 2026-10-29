package tv.zelta.app.ui.screens

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.zelta.app.data.PlaylistSource
import tv.zelta.app.data.SourceType
import tv.zelta.app.data.DemoSourceId
import tv.zelta.app.player.PlayerViewModel
import tv.zelta.app.ui.components.BrandMark
import tv.zelta.app.ui.components.EmptyPanel
import tv.zelta.app.ui.components.SectionLabel
import tv.zelta.app.ui.components.ZeltaButton
import tv.zelta.app.ui.components.ZeltaChip
import tv.zelta.app.ui.components.ZeltaIconButton
import tv.zelta.app.ui.components.ZeltaRiButton
import tv.zelta.app.ui.components.ZeltaTextButton
import tv.zelta.app.ui.components.ZeltaTextField
import tv.zelta.app.ui.components.zt
import tv.zelta.app.ui.theme.Ri
import tv.zelta.app.ui.theme.RiIcon
import tv.zelta.app.ui.theme.zeltaColors

/**
 * Saved playlists home screen, ported from the desktop `PlaylistHome`: the saved
 * playlists are large tiles a remote can walk through and the URL / file form
 * sits underneath them. Shown whenever no channel list is loaded yet.
 */
@Composable
fun HomeLibraryScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
) {
    var url by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var filterCategory by remember { mutableStateOf<String?>(null) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<PlaylistSource?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importFile(uri)
    }

    val query = search.trim().lowercase()
    val filtered = viewModel.sources
        .filter { filterCategory == null || filterCategory in it.categoryIds }
        .filter {
            query.isEmpty() ||
                it.name.lowercase().contains(query) ||
                it.value.lowercase().contains(query)
        }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val columns = when {
            maxWidth < 520.dp -> 1
            maxWidth < 900.dp -> 2
            else -> 3
        }
        val compactForm = maxWidth < 700.dp
        // The tagline only fits next to the wordmark on wider screens; on a
        // phone it would be cut mid-sentence.
        val wideHeader = maxWidth >= 640.dp

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 1120.dp)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark(markSize = 34.dp, showSubtitle = false)
                if (wideHeader) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        zt("Uydu alıcısı deneyimi · listenizi ekleyin", "Satellite receiver experience · add your playlist"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (viewModel.loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        zt("Yükleniyor…", "Loading…"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(zt("IPTV Kütüphanesi", "IPTV Library"))
                Spacer(Modifier.width(7.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                ) {
                    Text(
                        viewModel.sources.size.toString(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (viewModel.sources.size > 5) {
                    ZeltaTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = zt("Listelerde ara…", "Search playlists..."),
                        modifier = Modifier.width(190.dp),
                        leadingGlyph = Ri.Search,
                    )
                }
            }

            if (viewModel.categories.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ZeltaChip(
                        label = zt("Tümü", "All"),
                        selected = filterCategory == null,
                        onClick = { filterCategory = null },
                        leading = Ri.FolderOpen,
                    )
                    viewModel.categories.take(4).forEach { category ->
                        ZeltaChip(
                            label = category.name,
                            selected = filterCategory == category.id,
                            onClick = { filterCategory = category.id },
                            leading = Ri.FolderLine,
                        )
                    }
                }
            }

            Spacer(Modifier.height(11.dp))

            if (viewModel.sources.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            RoundedCornerShape(9.dp),
                        ),
                ) {
                    EmptyPanel(
                        zt("Kütüphaneniz boş", "Your library is empty"),
                        zt("Başlamak için bir bağlantı veya dosya ekleyin.", "Add a URL or file to get started."),
                        Modifier.fillMaxWidth(),
                    )
                }
            } else {
                filtered.chunked(columns).forEach { rowSources ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        rowSources.forEach { source ->
                            SourceTile(
                                source = source,
                                categoryNames = viewModel.categoryNames(source),
                                active = source.id == viewModel.activeSource?.id,
                                editing = editingId == source.id,
                                onOpen = { viewModel.loadSource(source) },
                                onEdit = {
                                    editingId = if (editingId == source.id) null else source.id
                                },
                                onDelete = { deleteTarget = source },
                                onSaveEdit = { name, value ->
                                    viewModel.updateSource(source, name, value)
                                    editingId = null
                                },
                                onCancelEdit = { editingId = null },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columns - rowSources.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        RoundedCornerShape(9.dp),
                    )
                    .padding(14.dp),
            ) {
                SectionLabel(zt("Kaynak ekle", "Add source"))
                Spacer(Modifier.height(9.dp))
                if (compactForm) {
                    Column(Modifier.fillMaxWidth()) {
                        ZeltaTextField(
                            value = alias,
                            onValueChange = { alias = it },
                            placeholder = zt("Takma ad (isteğe bağlı)", "Alias (optional)"),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                        ZeltaTextField(
                            value = url,
                            onValueChange = { url = it },
                            placeholder = zt("https://ornek.com/oynatma-listesi.m3u", "https://example.com/playlist.m3u"),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(7.dp))
                        ZeltaButton(
                            text = zt("Listeyi yükle", "Load Playlist"),
                            onClick = {
                                if (viewModel.addUrlSource(url, alias)) {
                                    url = ""
                                    alias = ""
                                }
                            },
                            primary = true,
                            enabled = url.isNotBlank() && !viewModel.loading,
                            glyph = Ri.Download,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ZeltaTextField(
                            value = alias,
                            onValueChange = { alias = it },
                            placeholder = zt("Takma ad (isteğe bağlı)", "Alias (optional)"),
                            modifier = Modifier.widthIn(min = 110.dp).weight(1f),
                        )
                        ZeltaTextField(
                            value = url,
                            onValueChange = { url = it },
                            placeholder = zt("https://ornek.com/oynatma-listesi.m3u", "https://example.com/playlist.m3u"),
                            modifier = Modifier.weight(2f),
                        )
                        ZeltaButton(
                            text = zt("Listeyi yükle", "Load Playlist"),
                            onClick = {
                                if (viewModel.addUrlSource(url, alias)) {
                                    url = ""
                                    alias = ""
                                }
                            },
                            primary = true,
                            enabled = url.isNotBlank() && !viewModel.loading,
                            glyph = Ri.Download,
                        )
                    }
                }

                Spacer(Modifier.height(7.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ZeltaButton(
                        text = zt("Dosya seç", "Choose Files"),
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        enabled = !viewModel.loading,
                        glyph = Ri.FolderOpen,
                    )
                    Text(
                        zt(
                            "Desteklenen biçimler: .m3u, .m3u8, .txt",
                            "Supported file formats: .m3u, .m3u8, .txt",
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                    )
                }

                val error = viewModel.loadError
                if (error != null) {
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RiIcon(Ri.Warning, size = 12.dp, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 10.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        ZeltaTextButton(zt("Kapat", "Dismiss"), { viewModel.clearLoadError() })
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }

    deleteTarget?.let { source ->
        val label = if (source.id == DemoSourceId) zt("Demo liste", "Demo list") else source.name
        ConfirmDialog(
            title = zt("Kaynağı sil", "Delete source"),
            body = zt(
                "\"{name}\" ve buna bağlı kanal listesi kaldırılacak. Favoriler ve geçmiş korunur.",
                "\"{name}\" and its channel list will be removed. Favourites and history are kept.",
            ).replace("{name}", label),
            confirmLabel = zt("Sil", "Delete"),
            danger = true,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.removeSource(source)
                deleteTarget = null
            },
        )
    }
}

@Composable
private fun SourceTile(
    source: PlaylistSource,
    categoryNames: String,
    active: Boolean,
    editing: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSaveEdit: (String, String) -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    var editName by remember(source.id, editing) { mutableStateOf(source.name) }
    var editValue by remember(source.id, editing) { mutableStateOf(source.value) }
    val shape = RoundedCornerShape(9.dp)

    Column(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
            .border(
                1.dp,
                when {
                    focused -> zeltaColors.focus
                    active -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                },
                shape,
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .clickable(onClick = onOpen)
                .padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                RiIcon(
                    if (source.type == SourceType.URL) Ri.Link else Ri.FileList,
                    size = 18.dp,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (source.id == DemoSourceId) zt("Demo liste", "Demo list") else source.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = when (source.type) {
                    SourceType.URL -> source.value
                    SourceType.FILE -> source.fileName ?: ""
                    SourceType.DEMO -> ""
                }
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (categoryNames.isNotEmpty()) {
                    Text(
                        categoryNames,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (active) {
                RiIcon(Ri.Check, size = 14.dp, tint = MaterialTheme.colorScheme.primary)
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.25f))
                .padding(start = 9.dp, end = 3.dp, top = 1.dp, bottom = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when (source.type) {
                    SourceType.URL -> zt("BAĞLANTI", "URL")
                    SourceType.FILE -> zt("DOSYA", "FILE")
                    SourceType.DEMO -> zt("DEMO", "DEMO")
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.weight(1f))
            ZeltaRiButton(
                glyph = Ri.Edit,
                onClick = onEdit,
                description = zt("Düzenle", "Edit"),
                size = 28.dp,
            )
            ZeltaRiButton(
                glyph = Ri.Delete,
                onClick = onDelete,
                description = zt("Kaynağı sil", "Delete source"),
                size = 28.dp,
                tint = MaterialTheme.colorScheme.error,
            )
        }

        if (editing) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.35f))
                    .padding(9.dp),
            ) {
                ZeltaTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    placeholder = zt("Takma ad (isteğe bağlı)", "Alias (optional)"),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (source.type == SourceType.URL) {
                    Spacer(Modifier.height(5.dp))
                    ZeltaTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        placeholder = zt("M3U bağlantı adresi", "M3U playlist url"),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ZeltaTextButton(zt("Vazgeç", "Cancel"), onCancelEdit)
                    Spacer(Modifier.width(6.dp))
                    ZeltaButton(zt("Kaydet", "Save"), { onSaveEdit(editName, editValue) })
                }
            }
        }
    }
}
