package net.bokumin45.archivedownloader
enum class HotPeriod(val displayName: String, val value: String) {
    DAY("Last 24 Hours", "day"),
    WEEK("Last Week", "week"),
    MONTH("Last Month", "month"),
    YEAR("Last Year", "year")
}