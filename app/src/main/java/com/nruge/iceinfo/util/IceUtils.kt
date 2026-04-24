package com.nruge.iceinfo.util

import com.nruge.iceinfo.R

fun getIceDrawable(tzn: String): Int {
    val number = tzn.removePrefix("ICE").toIntOrNull() ?: return R.drawable.ice
    return when (number) {
        in 701..899 -> R.drawable.ice
        else -> R.drawable.ice
    }
}

fun getIceClass(tzn: String): String {
    val number = tzn.removePrefix("ICE").toIntOrNull() ?: return ""
    return when (number) {
        in 1..59 -> "ICE 1"
        in 60..99 -> "ICE 2"
        in 101..159 -> "ICE 1"
        in 201..299 -> "ICE T (BR 415)"
        in 301..399 -> "ICE T (BR 411)"
        in 401..499 -> "ICE 3"
        in 701..799 -> "ICE 3neo"
        in 801..899 -> "ICE 3neo"
        in 901..999 -> "ICE 4"
        else -> ""
    }
}
