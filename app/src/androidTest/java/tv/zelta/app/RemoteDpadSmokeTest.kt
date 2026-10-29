package tv.zelta.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Skeleton remote-control smoke test.
 *
 * Runs on any API 34/35 emulator (TV, phone, tablet). It does not assert
 * playback success — public test streams are network dependent — it asserts
 * that the app boots, survives D-pad input and stays in the foreground.
 *
 * Run:
 *   scripts/android/run-smoke.ps1 -Profile tv
 * or:
 *   ./gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class RemoteDpadSmokeTest {

    @Test
    fun appBootsAndSurvivesRemoteInput() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val packageName = context.packageName

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        requireNotNull(launchIntent) { "Launch intent not found: $packageName" }
        launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)

        assertTrue(
            "The app window did not appear within 15 s",
            device.wait(Until.hasObject(By.pkg(packageName).depth(0)), 15_000),
        )

        repeat(5) {
            device.pressDPadDown()
            SystemClock.sleep(250)
        }
        device.pressDPadCenter()
        SystemClock.sleep(3_000)
        device.pressDPadRight()
        device.pressDPadCenter()
        SystemClock.sleep(2_000)

        assertEquals("The app left the foreground", packageName, device.currentPackageName)

        val artifacts = File(context.getExternalFilesDir(null), "artifacts").apply { mkdirs() }
        device.takeScreenshot(File(artifacts, "remote-smoke.png"))
    }

    private object SystemClock {
        fun sleep(millis: Long) = android.os.SystemClock.sleep(millis)
    }
}
