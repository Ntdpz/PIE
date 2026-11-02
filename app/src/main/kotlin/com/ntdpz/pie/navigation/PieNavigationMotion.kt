package com.ntdpz.pie.navigation

internal fun navigationDirection(from: String?, to: String?): Int {
    val source = PieDestination.entries.indexOfFirst { it.route == from }
    val target = PieDestination.entries.indexOfFirst { it.route == to }
    return if (source >= 0 && target >= 0 && target < source) -1 else 1
}
