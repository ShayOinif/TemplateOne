package edu.shayo.templateone.utils

import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun FusedLocationProviderClient.locationFlow(
    interval: Long,
    fastestInterval: Long?,
    maxWaitTime: Long?,
    priority: Int?,
) = callbackFlow {
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            try {
                trySend(result.lastLocation)
            } catch (e: Exception) {
                Log.d("Shay", e.message.toString())
            }
        }
    }

    val locationRequest = LocationRequest.create().apply {
        this.interval = interval

        fastestInterval?.let {
            this.fastestInterval = fastestInterval
        }

        maxWaitTime?.let {
            this.maxWaitTime = maxWaitTime
        }

        priority?.let {
            this.priority = priority
        }
    }

    requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        .addOnFailureListener { e ->
            Log.d("Shay: Location callback extension", e.message.toString())
            close(e)
        }

    awaitClose {
        Log.d("Shay", "Location callback closed")
        removeLocationUpdates(locationCallback)
    }
}