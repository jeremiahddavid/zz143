package com.zz143.suggest.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zz143.core.model.Suggestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZZ143SuggestionSheet(
    suggestion: Suggestion?,
    onAccept: (Suggestion) -> Unit,
    onDismiss: (Suggestion) -> Unit,
    onReject: (Suggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestion == null) return

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = { onDismiss(suggestion) },
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = suggestion.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = suggestion.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (suggestion.estimatedTimeSavedMs > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Save ~${suggestion.estimatedTimeSavedMs / 1000}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onAccept(suggestion) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Yes, do it")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { onDismiss(suggestion) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Not now")
            }

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
                onClick = { onReject(suggestion) }
            ) {
                Text(
                    text = "Don't suggest this again",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
