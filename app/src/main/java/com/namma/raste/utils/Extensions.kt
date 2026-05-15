package com.namma.raste.utils

import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

fun Context.showToast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Long.toReadableDate(): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(this))
fun getAddressFromLocation(context: Context, latitude: Double, longitude: Double): String {
    return try {
        val geocoder = android.location.Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val parts = mutableListOf<String>()
            // Build a readable address from components
            if (!address.subLocality.isNullOrEmpty())    parts.add(address.subLocality)
            if (!address.locality.isNullOrEmpty())       parts.add(address.locality)
            if (!address.adminArea.isNullOrEmpty())      parts.add(address.adminArea)
            if (parts.isEmpty()) address.getAddressLine(0) ?: "Unknown location"
            else parts.joinToString(", ")
        } else {
            "📍 ${"%.4f".format(latitude)}, ${"%.4f".format(longitude)}"
        }
    } catch (e: Exception) {
        "📍 ${"%.4f".format(latitude)}, ${"%.4f".format(longitude)}"
    }
}