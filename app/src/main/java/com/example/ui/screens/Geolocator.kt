package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class LocationAccuracy {
    bestForNavigation,
    best,
    medium,
    low
}

object Geolocator {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentPosition(
        context: Context,
        accuracy: LocationAccuracy = LocationAccuracy.bestForNavigation
    ): LatLng = suspendCancellableCoroutine { continuation ->
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    continuation.resume(LatLng(location.latitude, location.longitude))
                } else {
                    val priority = when (accuracy) {
                        LocationAccuracy.bestForNavigation -> Priority.PRIORITY_HIGH_ACCURACY
                        LocationAccuracy.best -> Priority.PRIORITY_HIGH_ACCURACY
                        LocationAccuracy.medium -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
                        LocationAccuracy.low -> Priority.PRIORITY_LOW_POWER
                    }
                    val request = CurrentLocationRequest.Builder()
                        .setPriority(priority)
                        .build()
                    fusedLocationClient.getCurrentLocation(request, null)
                        .addOnSuccessListener { loc: Location? ->
                            if (loc != null) {
                                continuation.resume(LatLng(loc.latitude, loc.longitude))
                            } else {
                                continuation.resumeWithException(Exception("GPS location acquired as null"))
                            }
                        }
                        .addOnFailureListener { e ->
                            continuation.resumeWithException(e)
                        }
                }
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }
    }

    @SuppressLint("MissingPermission")
    fun getPositionStream(
        context: Context,
        accuracy: LocationAccuracy = LocationAccuracy.bestForNavigation
    ): Flow<LatLng> = callbackFlow {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        val priority = when (accuracy) {
            LocationAccuracy.bestForNavigation -> Priority.PRIORITY_HIGH_ACCURACY
            LocationAccuracy.best -> Priority.PRIORITY_HIGH_ACCURACY
            LocationAccuracy.medium -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            LocationAccuracy.low -> Priority.PRIORITY_LOW_POWER
        }
        
        val locationRequest = LocationRequest.Builder(priority, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()
            
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    trySend(LatLng(location.latitude, location.longitude))
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        
        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    fun isGpsEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        return lm?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true
    }
}
