package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * §690 — real date + time pickers for the booking flow. The booking screens used
 * free-text fields ("e.g. 25 June 2026") so "the calendar never opened". These
 * composables render a read-only field with a tap overlay that opens a Material3
 * DatePicker / TimePicker. Date is clamped to today..+14 days. Output strings are
 * the same human format the screens already submit, so nothing downstream changes.
 */

private fun startOfTodayUtcMillis(): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun formatPickedDate(utcMillis: Long): String {
    // DatePicker returns a UTC-midnight millis; format in UTC so the day can't shift.
    val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    fmt.timeZone = TimeZone.getTimeZone("UTC")
    return fmt.format(Date(utcMillis))
}

private fun formatPickedTime(hour: Int, minute: Int): String {
    val ampm = if (hour < 12) "AM" else "PM"
    val hr = ((hour + 11) % 12) + 1   // 0->12, 13->1, etc.
    return String.format(Locale.getDefault(), "%02d:%02d %s", hr, minute, ampm)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NikhatDateField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Appointed Date",
    iconTint: Color = MaterialTheme.colorScheme.primary,
) {
    var show by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Tap to pick a date") },
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = iconTint) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
        )
        // Transparent overlay on TOP captures the tap so the field never focuses /
        // pops the keyboard — it just opens the calendar.
        Box(Modifier.matchParentSize().clickable { show = true })
    }
    if (show) {
        val today = remember { startOfTodayUtcMillis() }
        val maxDay = remember { today + 14L * 24 * 60 * 60 * 1000 }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = today,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in today..maxDay
            },
        )
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(formatPickedDate(it)) }
                    show = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NikhatTimeField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Preferred Time",
    iconTint: Color = MaterialTheme.colorScheme.primary,
) {
    var show by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Tap to pick a time") },
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = iconTint) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
        )
        Box(Modifier.matchParentSize().clickable { show = true })
    }
    if (show) {
        val state = rememberTimePickerState(initialHour = 10, initialMinute = 0, is24Hour = false)
        Dialog(onDismissRequest = { show = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Select time", style = MaterialTheme.typography.titleMedium)
                    Box(Modifier.padding(top = 12.dp)) { TimePicker(state = state) }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { show = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            onChange(formatPickedTime(state.hour, state.minute))
                            show = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}
