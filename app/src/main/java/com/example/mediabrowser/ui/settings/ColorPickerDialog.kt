package com.example.mediabrowser.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mediabrowser.ui.theme.parseHexColor

private val SWATCHES = listOf(
    "#2DD4BF", "#5EEAD4", "#14B8A6", "#FF8A65", "#81C784",
    "#BA68C8", "#90A4AE", "#64B5F6", "#EF5DA8", "#EFA85D",
    "#B05DEF", "#EF5D5D", "#5DEFEF", "#D4EF5D", "#FFFFFF", "#050607"
)

/**
 * A simple color picker: swatch grid plus a free-text hex input, used
 * throughout Settings for accent/background/tag colors. Calls [onColorSelected]
 * with a "#RRGGBB" string immediately on confirm.
 */
@Composable
fun ColorPickerDialog(
    title: String,
    currentHex: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var hexInput by remember { mutableStateOf(currentHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SWATCHES) { hex ->
                        val color = parseHexColor(hex, Color.Gray)
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(color, CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                    .clickable { hexInput = hex }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { hexInput = it },
                    label = { Text("Hex color (#RRGGBB)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onColorSelected(hexInput)
                onDismiss()
            }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
