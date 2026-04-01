package com.zz143.demo

import android.app.Application
import com.zz143.core.ZZ143
import com.zz143.core.model.SuggestionDisplayType

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        ZZ143.init(this) {
            suggestionsEnabled(true)
            suggestionDisplayType(SuggestionDisplayType.BOTTOM_SHEET)
            minPatternOccurrences(3)
            captureTextValues(false)
            debugLogging(true)
        }

        ZZ143.startCapturing()
    }
}
