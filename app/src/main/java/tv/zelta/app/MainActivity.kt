package tv.zelta.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import tv.zelta.app.settings.AppSettings
import tv.zelta.app.ui.ZeltaApp
import tv.zelta.app.ui.theme.ZeltaTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val language = AppSettings.languageOf(newBase)
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocale(Locale(language))
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val settings = AppSettings(this)
        setContent {
            ZeltaTheme(themeId = settings.theme) {
                ZeltaApp(settings = settings)
            }
        }
    }

    /**
     * Xbox/gamepad mapping mirroring the desktop: X behaves like Enter/OK and
     * B like Back. The D-pad already arrives as the four direction keys.
     */
    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val mapped = when (event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_X -> KeyEvent.KEYCODE_DPAD_CENTER
            KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BACK
            else -> null
        }
        if (mapped != null) {
            val synthetic = KeyEvent(
                event.downTime,
                event.eventTime,
                event.action,
                mapped,
                0,
                event.metaState,
            )
            return super.dispatchKeyEvent(synthetic)
        }
        return super.dispatchKeyEvent(event)
    }
}
