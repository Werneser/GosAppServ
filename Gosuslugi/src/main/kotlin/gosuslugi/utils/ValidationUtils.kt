package gosuslugi.utils

import java.util.regex.Pattern

fun String.isValidPhoneNumber(): Boolean {
    val phonePattern: Pattern = Pattern.compile("^\\+?\\d{10,15}\$")
    return phonePattern.matcher(this).matches()
}