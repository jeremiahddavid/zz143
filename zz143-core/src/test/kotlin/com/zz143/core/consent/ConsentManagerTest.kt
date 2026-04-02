package com.zz143.core.consent

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ConsentManagerTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var context: Context
    private var revokedCallCount = 0

    // In-memory store for SharedPreferences
    private val prefsStore = mutableMapOf<String, String?>()

    @Before
    fun setUp() {
        revokedCallCount = 0
        prefsStore.clear()

        editor = mockk(relaxed = true)
        val keySlot = slot<String>()
        val valueSlot = slot<String>()
        every { editor.putString(capture(keySlot), capture(valueSlot)) } answers {
            prefsStore[keySlot.captured] = valueSlot.captured
            editor
        }
        every { editor.apply() } returns Unit

        prefs = mockk()
        every { prefs.getString(any(), any()) } answers {
            prefsStore[firstArg()] ?: secondArg()
        }
        every { prefs.edit() } returns editor

        context = mockk()
        every { context.getSharedPreferences(ConsentManager.PREFS_NAME, Context.MODE_PRIVATE) } returns prefs
    }

    private fun createManager(): ConsentManager =
        ConsentManager(context, onRevoked = { revokedCallCount++ })

    // --- Default state ---

    @Test
    fun defaultConsentLevelIsFull() {
        val manager = createManager()
        assertThat(manager.level.value).isEqualTo(ConsentLevel.FULL)
    }

    @Test
    fun canCaptureIsTrueByDefault() {
        val manager = createManager()
        assertThat(manager.canCapture()).isTrue()
    }

    @Test
    fun canFireAnalyticsIsTrueByDefault() {
        val manager = createManager()
        assertThat(manager.canFireAnalytics()).isTrue()
    }

    // --- Grant FULL ---

    @Test
    fun grantFullAllowsCapture() {
        val manager = createManager()
        manager.grant(ConsentLevel.FULL)
        assertThat(manager.canCapture()).isTrue()
    }

    @Test
    fun grantFullAllowsAnalytics() {
        val manager = createManager()
        manager.grant(ConsentLevel.FULL)
        assertThat(manager.canFireAnalytics()).isTrue()
    }

    // --- Grant ANALYTICS_ONLY ---

    @Test
    fun analyticsOnlyBlocksCapture() {
        val manager = createManager()
        manager.grant(ConsentLevel.ANALYTICS_ONLY)
        assertThat(manager.canCapture()).isFalse()
    }

    @Test
    fun analyticsOnlyAllowsAnalytics() {
        val manager = createManager()
        manager.grant(ConsentLevel.ANALYTICS_ONLY)
        assertThat(manager.canFireAnalytics()).isTrue()
    }

    // --- Grant NONE ---

    @Test
    fun noneBlocksCapture() {
        val manager = createManager()
        manager.grant(ConsentLevel.NONE)
        assertThat(manager.canCapture()).isFalse()
    }

    @Test
    fun noneBlocksAnalytics() {
        val manager = createManager()
        manager.grant(ConsentLevel.NONE)
        assertThat(manager.canFireAnalytics()).isFalse()
    }

    // --- Revoke ---

    @Test
    fun revokeSetLevelToNone() {
        val manager = createManager()
        manager.revoke()
        assertThat(manager.level.value).isEqualTo(ConsentLevel.NONE)
    }

    @Test
    fun revokeTriggersOnRevokedCallback() {
        val manager = createManager()
        manager.revoke()
        assertThat(revokedCallCount).isEqualTo(1)
    }

    @Test
    fun grantNoneFromFullTriggersOnRevokedCallback() {
        val manager = createManager()
        manager.grant(ConsentLevel.NONE)
        assertThat(revokedCallCount).isEqualTo(1)
    }

    @Test
    fun grantNoneFromNoneDoesNotTriggerOnRevokedAgain() {
        val manager = createManager()
        manager.grant(ConsentLevel.NONE)
        revokedCallCount = 0
        manager.grant(ConsentLevel.NONE)
        assertThat(revokedCallCount).isEqualTo(0)
    }

    @Test
    fun grantFullDoesNotTriggerOnRevoked() {
        val manager = createManager()
        manager.grant(ConsentLevel.FULL)
        assertThat(revokedCallCount).isEqualTo(0)
    }

    // --- Persistence ---

    @Test
    fun grantPersistsLevelToSharedPreferences() {
        val manager = createManager()
        manager.grant(ConsentLevel.ANALYTICS_ONLY)
        assertThat(prefsStore[ConsentManager.KEY_CONSENT_LEVEL]).isEqualTo("ANALYTICS_ONLY")
    }

    @Test
    fun managerRestoresPersistedLevel() {
        prefsStore[ConsentManager.KEY_CONSENT_LEVEL] = "NONE"
        val manager = createManager()
        assertThat(manager.level.value).isEqualTo(ConsentLevel.NONE)
    }

    @Test
    fun managerDefaultsToFullForUnknownPersistedValue() {
        prefsStore[ConsentManager.KEY_CONSENT_LEVEL] = "INVALID_VALUE"
        val manager = createManager()
        assertThat(manager.level.value).isEqualTo(ConsentLevel.FULL)
    }

    // --- StateFlow updates ---

    @Test
    fun levelFlowUpdatesOnGrant() {
        val manager = createManager()
        assertThat(manager.level.value).isEqualTo(ConsentLevel.FULL)
        manager.grant(ConsentLevel.ANALYTICS_ONLY)
        assertThat(manager.level.value).isEqualTo(ConsentLevel.ANALYTICS_ONLY)
        manager.grant(ConsentLevel.NONE)
        assertThat(manager.level.value).isEqualTo(ConsentLevel.NONE)
    }
}
