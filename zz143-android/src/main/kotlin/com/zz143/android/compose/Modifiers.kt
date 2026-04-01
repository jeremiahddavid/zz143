package com.zz143.android.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import com.zz143.core.ZZ143
import com.zz143.core.model.ActionSource
import com.zz143.core.model.SemanticAction

val LocalZZ143 = staticCompositionLocalOf { ZZ143 }

fun Modifier.watchAction(
    actionType: String,
    parameters: Map<String, String> = emptyMap()
): Modifier = this.semantics {
    testTag = "zz143:$actionType"
}.then(Modifier)

fun Modifier.watchSensitive(): Modifier = this.semantics {
    testTag = "zz143:sensitive"
}

@Composable
fun TrackAction(
    actionType: String,
    parameters: Map<String, String> = emptyMap()
) {
    DisposableEffect(actionType) {
        ZZ143.trackAction(actionType, parameters)
        onDispose { }
    }
}

@Composable
fun ZZ143Provider(content: @Composable () -> Unit) {
    content()
}
