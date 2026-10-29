package tv.zelta.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import tv.zelta.app.R
import tv.zelta.app.settings.I18n
import tv.zelta.app.ui.theme.Ri
import tv.zelta.app.ui.theme.RiIcon
import tv.zelta.app.ui.theme.isBrightColor
import tv.zelta.app.ui.theme.zeltaColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Requests focus once the target exists and the window accepts focus. A TV app
 * launched from a cold boot often composes before the window is focusable, so a
 * single request would silently fail; retry until the target reports focus.
 */
@Composable
fun Modifier.autoFocus(focusRequester: FocusRequester, attempts: Int = 12): Modifier {
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focusRequester) {
        repeat(attempts) {
            if (focused) return@LaunchedEffect
            delay(120)
            runCatching { focusRequester.requestFocus() }
        }
    }
    return this.onFocusChanged { if (it.isFocused) focused = true }
}

/** Small bilingual helper. Resources are recreated when the language changes. */
@Composable
fun zt(turkish: String, english: String): String {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val language = context.resources.configuration.locale.language
    val translated = remember(context, language) { I18n.translate(context, language, english) }
    return translated ?: if (language == "tr") turkish else english
}

/**
 * Translate used by the first-run setup wizard. It follows the language chosen
 * on the first step, so the assistant switches language as soon as a card is
 * tapped (the desktop wizard behaves the same way).
 */
@Composable
fun ztSetup(turkish: String, english: String): String {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val language = context.resources.configuration.locale.language
    val translated = remember(context, language) { I18n.translate(context, language, english) }
    return translated ?: if (language == "tr") turkish else english
}

/** "6 channels" with the count first, translated per language. */
@Composable
fun channelCountText(count: Int): String =
    zt("{count} kanal", "{count} channels").replace("{count}", count.toString())

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    markSize: Dp = 28.dp,
    showWordmark: Boolean = true,
    showSubtitle: Boolean = true,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(markSize)
                .clip(RoundedCornerShape(markSize * 0.24f))
                .background(if (MaterialTheme.colorScheme.background.isBrightColor()) Color(0xFF141414) else Color(0xFF0D0D0D)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.zelta_icon),
                contentDescription = null,
                modifier = Modifier.size(markSize),
                contentScale = ContentScale.Fit,
            )
        }
        if (showWordmark) {
            Spacer(Modifier.width(9.dp))
            Column {
                Text(
                    "ZeltaTV",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = if (markSize >= 36.dp) 20.sp else 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                )
                if (showSubtitle) {
                    Text(
                        "IPTV PLAYER",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                    )
                }
            }
        }
    }
}

/** Large brand lockup used by setup and about panels. */
@Composable
fun BrandLockup(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.zelta_icon),
            contentDescription = null,
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(15.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                "ZeltaTV",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
            )
            Text(
                "IPTV PLAYER",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.6.sp,
            )
        }
    }
}

@Composable
fun ZeltaIconButton(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 32.dp,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(if (focused) zeltaColors.focus.copy(alpha = 0.13f) else Color.Transparent)
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) zeltaColors.focus else Color.Transparent,
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun ZeltaIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 32.dp,
) {
    ZeltaIconButton(onClick, description, modifier, enabled, size) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/** Desktop-style icon button using the Remix icon font. */
@Composable
fun ZeltaRiButton(
    glyph: String,
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 32.dp,
) {
    ZeltaIconButton(onClick, description, modifier, enabled, size) {
        RiIcon(glyph, size = size * 0.5f, tint = tint)
    }
}

@Composable
fun ZeltaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    danger: Boolean = false,
    glyph: String? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    val focus = zeltaColors.focus
    val bg = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        danger -> zeltaColors.destructive
        primary -> MaterialTheme.colorScheme.primary
        focused -> focus.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val fg = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        danger -> Color.White
        primary -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val ring = when {
        focused -> focus
        danger -> zeltaColors.destructive
        primary -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(shape)
            .background(bg)
            .border(BorderStroke(if (focused) 2.dp else 1.dp, ring), shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (glyph != null) {
            RiIcon(glyph, size = 15.dp, tint = fg)
            Spacer(Modifier.width(6.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun ZeltaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (focused) zeltaColors.focus.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                if (focused) 2.dp else 0.dp,
                if (focused) zeltaColors.focus else Color.Transparent,
                shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            color = if (focused) zeltaColors.focus else color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun ZeltaChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: String? = null,
    accent: Color? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    val chipColor = accent ?: zeltaColors.focus
    Row(
        modifier = modifier
            .height(28.dp)
            .clip(shape)
            .background(
                if (selected) chipColor.copy(alpha = 0.14f)
                else if (focused) chipColor.copy(alpha = 0.08f)
                else if (accent != null) accent.copy(alpha = 0.06f)
                else Color.Transparent
            )
            .border(
                if (focused || selected) 1.5.dp else 1.dp,
                if (focused || selected) chipColor
                else accent?.copy(alpha = 0.55f) ?: MaterialTheme.colorScheme.outline,
                shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Text(leading, color = if (selected) chipColor else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.width(5.dp))
        }
        Text(
            label,
            color = if (selected || focused) chipColor
            else if (accent != null) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
fun ZeltaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    leadingGlyph: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp) },
        leadingIcon = if (leadingGlyph != null) {
            { RiIcon(leadingGlyph, size = 14.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = RoundedCornerShape(6.dp),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = zeltaColors.focus,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.6.sp,
        modifier = modifier,
    )
}

@Composable
fun Keycap(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun HintBar(modifier: Modifier = Modifier, items: List<Pair<String, String>>) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) Spacer(Modifier.width(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Keycap(item.first)
                Spacer(Modifier.width(5.dp))
                Text(item.second, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun EmptyPanel(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun clockText(): String {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val language = context.resources.configuration.locale.language
    val format = remember(language) { SimpleDateFormat("HH:mm", Locale(language)) }
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            now = Date()
        }
    }
    return format.format(now)
}

@Composable
fun dateText(): String {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val language = context.resources.configuration.locale.language
    val format = remember(language) { SimpleDateFormat("d MMMM EEEE", Locale(language)) }
    return format.format(Date())
}

/** Base panel used by dialogs and full-screen sheets. */
@Composable
fun PanelSurface(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f), RoundedCornerShape(10.dp)),
    ) {
        content()
    }
}

@Suppress("unused")
@Composable
fun AccentDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(40.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary),
    )
}

@Suppress("unused")
val ZeltaFocusColor: Color
    @Composable get() = zeltaColors.focus

@Suppress("unused")
val MutedAmber: Color = tv.zelta.app.ui.theme.ZeltaAmber

@Suppress("unused")
val RiPlay = Ri.Play
