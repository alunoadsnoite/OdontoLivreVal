package br.com.odontolivreval.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalConfiguration
import java.time.format.FormatStyle
import java.util.Locale

private val portuguese = Locale.forLanguageTag("pt-BR")


@Composable
internal fun MonthNavigation(month: YearMonth, onMonth: (YearMonth) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val pattern = if (locale == Locale.US) "MMMM uuuu" else "MMMM 'de' uuuu"
    val monthLabel = DateTimeFormatter.ofPattern(pattern, locale)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(month.format(monthLabel), style = MaterialTheme.typography.titleLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onMonth(month.minusMonths(1)) }) {
                Text(stringResource(R.string.schedule_previous_month))
            }
            TextButton(onClick = { onMonth(month.plusMonths(1)) }) {
                Text(stringResource(R.string.schedule_next_month))
            }
        }
    }
}

@Composable
internal fun MonthCalendar(
    month: YearMonth,
    selectedDay: LocalDate,
    activeCounts: Map<LocalDate, Int>,
    onDay: (LocalDate) -> Unit,
) {
    val firstColumn = month.atDay(1).dayOfWeek.value % 7
    val weeks = (firstColumn + month.lengthOfMonth() + 6) / 7
    val weekdays = stringArrayResource(R.array.schedule_weekdays)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = maxWidth.coerceAtLeast(336.dp)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            Column(Modifier.width(width)) {
                Row(Modifier.fillMaxWidth()) {
                    weekdays.forEach { label ->
                        Text(label, Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
                repeat(weeks) { week ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { column ->
                            val number = week * 7 + column - firstColumn + 1
                            val modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            if (number in 1..month.lengthOfMonth()) {
                                val date = month.atDay(number)
                                CalendarDay(date, date == selectedDay, activeCounts[date] ?: 0, modifier, onDay)
                            } else {
                                Spacer(modifier)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    selected: Boolean,
    count: Int,
    modifier: Modifier,
    onDay: (LocalDate) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val dayLabel = if (locale == portuguese) {
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' uuuu", locale)
    } else {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    }
    val countLabel = pluralStringResource(R.plurals.schedule_month_appointments, count, count)
    val label = stringResource(R.string.schedule_month_day_description, date.format(dayLabel), countLabel)
    Column(
        modifier
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .selectable(selected, role = Role.Button, onClick = { onDay(date) })
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(date.dayOfMonth.toString(), Modifier.clearAndSetSemantics { contentDescription = label })
        if (count > 0) {
            Text(
                count.toString(),
                Modifier.clearAndSetSemantics {},
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
