// TimePickerWheel.kt
package com.example.myapplication.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttoolfactory.numberpicker.NumberPicker

@Composable
fun TimePickerWheel(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        NumberPicker(
            value = hour,
            range = 0..23,
            onValueChange = onHourChange,
            textStyle = LocalTextStyle.current.copy(fontSize = 24.sp),
            dividersColor = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        androidx.compose.material3.Text(
            ":",
            fontSize = 24.sp,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.width(16.dp))
        NumberPicker(
            value = minute,
            range = 0..59,
            onValueChange = onMinuteChange,
            textStyle = LocalTextStyle.current.copy(fontSize = 24.sp),
            dividersColor = MaterialTheme.colorScheme.primary
        )
    }
}

