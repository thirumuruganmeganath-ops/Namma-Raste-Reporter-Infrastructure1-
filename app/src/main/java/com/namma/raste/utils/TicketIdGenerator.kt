package com.namma.raste.utils

import java.text.SimpleDateFormat
import java.util.*

object TicketIdGenerator {
    fun generate(count: Int): String {
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val seq  = String.format("%04d", count + 1)
        return "NR-$date-$seq"
    }
}
