package com.example.zerogrid.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.zerogrid.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Material 3 calendar date picker modal styled with ZeroGrid dark theme.
 * Restricts date selection strictly to past dates (from 1900 up to today).
 * Formats selected date into standardized "YYYY-MM-DD" UTC string.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeroGridDatePickerDialog(
    initialDateMillis: Long? = null,
    onDateSelected: (formattedDate: String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Cannot be born in the future
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        onDateSelected(formatter.format(Date(millis)))
                    }
                    onDismiss()
                }
            ) {
                Text("Confirm", color = PrimaryCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = CardBackground
        )
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                containerColor = CardBackground,
                titleContentColor = TextPrimary,
                headlineContentColor = TextPrimary,
                weekdayContentColor = TextSecondary,
                subheadContentColor = TextSecondary,
                yearContentColor = TextPrimary,
                currentYearContentColor = PrimaryCyan,
                selectedYearContentColor = CardBackground,
                selectedYearContainerColor = PrimaryCyan,
                dayContentColor = TextPrimary,
                selectedDayContentColor = CardBackground,
                selectedDayContainerColor = PrimaryCyan,
                todayContentColor = PrimaryCyan,
                todayDateBorderColor = PrimaryCyan
            )
        )
    }
}
