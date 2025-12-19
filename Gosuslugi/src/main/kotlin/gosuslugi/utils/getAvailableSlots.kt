import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun getAvailableSlots(date: LocalDate): List<LocalDateTime> {
    val slots = mutableListOf<LocalDateTime>()
    val startHour = 9 // пример начала рабочего дня
    val endHour = 17 // пример окончания рабочего дня

    for (hour in startHour until endHour) {
        slots.add(LocalDateTime.of(date.year, date.month, date.dayOfMonth, hour, 0))
        slots.add(LocalDateTime.of(date.year, date.month, date.dayOfMonth, hour, 30))
    }
    return slots
}