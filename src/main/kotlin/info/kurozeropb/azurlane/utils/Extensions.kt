package info.kurozeropb.azurlane.utils

fun String.capitalizeName(): String {
    return if (isNotEmpty()) {
        if (this[0] == '(') {
            substring(0, 1) + substring(1, 2).toUpperCase() + substring(2)
        } else {
            substring(0, 1).toUpperCase() + substring(1)
        }
    } else {
        this
    }
}