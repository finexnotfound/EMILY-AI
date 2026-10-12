package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.gemini.GeminiBrain
import com.example.data.gemini.GeminiResult
import com.example.data.model.AssistantSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Emily AI", appName)
  }

  @Test
  fun `gemini brain recognizes offline tool commands`() = runBlocking {
    val brain = GeminiBrain()
    val settings = AssistantSettings(customApiKey = "") // offline mode

    // Test flashlight heuristic
    val resultFlashlight = brain.processUserPrompt("turn on flashlight", settings)
    assertTrue("Should be tool call", resultFlashlight is GeminiResult.ToolCall)
    assertEquals("toggle_flashlight", (resultFlashlight as GeminiResult.ToolCall).functionName)

    // Test volume heuristic
    val resultVolume = brain.processUserPrompt("turn the volume down", settings)
    assertTrue("Should be tool call", resultVolume is GeminiResult.ToolCall)
    assertEquals("adjust_volume", (resultVolume as GeminiResult.ToolCall).functionName)

    // Test alarm heuristic
    val resultAlarm = brain.processUserPrompt("set alarm for 7:30 am", settings)
    assertTrue("Should be tool call", resultAlarm is GeminiResult.ToolCall)
    assertEquals("set_alarm", (resultAlarm as GeminiResult.ToolCall).functionName)

    // Test app open heuristic
    val resultApp = brain.processUserPrompt("open YouTube app", settings)
    assertTrue("Should be tool call", resultApp is GeminiResult.ToolCall)
    assertEquals("open_app", (resultApp as GeminiResult.ToolCall).functionName)
  }
}
