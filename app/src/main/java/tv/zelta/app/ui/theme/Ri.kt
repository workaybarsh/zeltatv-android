package tv.zelta.app.ui.theme

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Remix Icon glyph codepoints used by the desktop UI. Keeping the same icon
 * font makes the Android shell visually identical to the Electron application.
 */
object Ri {
    const val Home = "\uee1f"
    const val Menu = "\uef32"
    const val Add = "\uea13"
    const val Close = "\ueb99"
    const val SidebarFold = "\uf45b"
    const val SidebarUnfold = "\uf45d"
    const val Info = "\uee59"
    const val Settings = "\uf0e6"
    const val Github = "\uedca"
    const val Remote = "\uf070"
    const val Folder = "\ued54"
    const val Flashlight = "\ued3d"
    const val Link = "\ueeb2"
    const val FileList = "\uecef"
    const val PlayList = "\uf00d"
    const val UploadCloud = "\uf24c"
    const val ArrowDown = "\uea4e"
    const val ArrowLeft = "\uea60"
    const val ArrowRight = "\uea6c"
    const val Check = "\ueb7b"
    const val Warning = "\ueca1"
    const val Loader = "\ueec6"
    const val Search = "\uf0d1"
    const val Heart = "\uee0f"
    const val HeartFill = "\uee0e"
    const val History = "\uee17"
    const val Folders = "\ued8a"
    const val ListUnordered = "\ueebe"
    const val SortAlpha = "\uf45f"
    const val Fullscreen = "\ued9c"
    const val FileText = "\ued0f"
    const val Live = "\ueec0"
    const val Play = "\uf00b"
    const val Pause = "\uefd8"
    const val Stop = "\uf1a1"
    const val VolumeUp = "\uf2a2"
    const val VolumeMute = "\uf29e"
    const val PictureInPicture = "\ueff4"
    const val Sun = "\uf1bf"
    const val ListCheck = "\ueeb9"
    const val Keyboard = "\uee75"
    const val Palette = "\uefc5"
    const val Download = "\uec54"
    const val CloseCircle = "\ueb97"
    const val ShutDown = "\uf126"
    const val Delete = "\uec2a"
    const val Edit = "\uec86"
    const val FolderAdd = "\ued5a"
    const val FolderLine = "\ued6a"
    const val FolderOpen = "\ued70"
    const val Question = "\uf045"
    const val CheckDouble = "\ueb79"
    const val Restart = "\uf080"
    const val InfoCircle = "\uf448"
    const val PlayCircle = "\uf009"
    const val Translate = "\uf226"
    const val Database = "\uec16"
    const val Refresh = "\uf064"
    const val Tv = "\uf237"
    const val Contrast = "\uebda"
    const val Movie = "\uef81"
    const val Image = "\uee4b"
    const val PaintBrush = "\uefc1"
    const val Smartphone = "\uf15a"
}

@Composable
fun RiIcon(
    glyph: String,
    size: Dp = 16.dp,
    tint: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Text(
        text = glyph,
        modifier = modifier.size(size),
        color = if (tint == Color.Unspecified) Color.Unspecified else tint,
        fontFamily = RemixFont,
        fontSize = with(density) { size.toSp() },
        lineHeight = with(density) { size.toSp() },
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}
