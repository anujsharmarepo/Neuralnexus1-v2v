package com.example.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

// Lightweight coordinate data model to fully decouple from Play Services
data class LatLng(val latitude: Double, val longitude: Double)

object MapsService {
    private val client = OkHttpClient()
    
    // Concurrent map to cache place suggestions and their coordinates
    private val suggestionsCache = ConcurrentHashMap<String, LatLng>()

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager != null) {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        return false
    }

    // 1. Fetch autocomplete predictions using Nominatim
    fun fetchPlaceSuggestions(query: String, apiKey: String? = null): List<String> {
        if (query.isBlank()) return emptyList()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=10&addressdetails=1"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AbhayaSafetyApp/1.0 (anujsharma1555r@gmail.com)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val jsonArray = JSONArray(body)
                val suggestions = mutableListOf<String>()
                
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val displayName = obj.optString("display_name", "")
                    val latStr = obj.optString("lat", "0.0")
                    val lonStr = obj.optString("lon", "0.0")
                    
                    if (displayName.isNotBlank()) {
                        val latLng = LatLng(latStr.toDoubleOrNull() ?: 0.0, lonStr.toDoubleOrNull() ?: 0.0)
                        suggestionsCache[displayName] = latLng
                        suggestions.add(displayName)
                    }
                }
                return suggestions
            }
        } catch (e: Exception) {
            Log.e("MapsService", "Error fetching place suggestions from Nominatim", e)
            return emptyList()
        }
    }

    // Helper to geocode a single destination if not in cache
    fun geocodeDestination(destination: String): LatLng? {
        val cached = suggestionsCache[destination]
        if (cached != null) return cached

        try {
            val encodedQuery = URLEncoder.encode(destination, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=1"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AbhayaSafetyApp/1.0 (anujsharma1555r@gmail.com)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val jsonArray = JSONArray(body)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    val latStr = obj.optString("lat", "0.0")
                    val lonStr = obj.optString("lon", "0.0")
                    val latLng = LatLng(latStr.toDoubleOrNull() ?: 0.0, lonStr.toDoubleOrNull() ?: 0.0)
                    suggestionsCache[destination] = latLng
                    return latLng
                }
            }
        } catch (e: Exception) {
            Log.e("MapsService", "Error geocoding destination: $destination", e)
        }
        return null
    }

    // 2. Directions API using OSRM (Open Source Routing Machine)
    data class DirectionsResult(
        val polylinePoints: List<LatLng>,
        val distanceKm: Double,
        val durationMins: Int,
        val destinationLatLng: LatLng? = null
    )

    fun fetchDirections(origin: LatLng, destination: String, apiKey: String? = null): DirectionsResult? {
        try {
            val destLatLng = geocodeDestination(destination) ?: return null
            
            // OSRM coordinates are formatted as {longitude},{latitude}
            val url = "https://router.project-osrm.org/route/v1/driving/${origin.longitude},${origin.latitude};${destLatLng.longitude},${destLatLng.latitude}?overview=full&geometries=geojson"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AbhayaSafetyApp/1.0 (anujsharma1555r@gmail.com)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val status = json.optString("code")
                if (status != "Ok") {
                    Log.e("MapsService", "OSRM Directions error status: $status")
                    return null
                }
                val routes = json.optJSONArray("routes") ?: return null
                if (routes.length() == 0) return null
                val route = routes.getJSONObject(0)
                
                // Get distance (meters) and duration (seconds)
                val distanceMeters = route.optDouble("distance", 0.0)
                val durationSeconds = route.optDouble("duration", 0.0)
                
                // Extract GeoJSON polyline coordinates
                val geometry = route.optJSONObject("geometry")
                val coordinates = geometry?.optJSONArray("coordinates") ?: JSONArray()
                val polylinePoints = mutableListOf<LatLng>()
                
                for (i in 0 until coordinates.length()) {
                    val coord = coordinates.getJSONArray(i)
                    val lon = coord.getDouble(0)
                    val lat = coord.getDouble(1)
                    polylinePoints.add(LatLng(lat, lon))
                }

                return DirectionsResult(
                    polylinePoints = polylinePoints,
                    distanceKm = distanceMeters / 1000.0,
                    durationMins = (durationSeconds / 60.0).toInt(),
                    destinationLatLng = destLatLng
                )
            }
        } catch (e: Exception) {
            Log.e("MapsService", "Error calling OSRM directions", e)
            return null
        }
    }

    // 3. Nearby police search using Nominatim
    data class PoliceStation(
        val name: String,
        val latLng: LatLng,
        val vicinity: String
    )

    fun fetchNearbyPoliceStations(location: LatLng, apiKey: String? = null): List<PoliceStation> {
        try {
            // Biased around user's coordinates
            val url = "https://nominatim.openstreetmap.org/search?q=police&format=json&lat=${location.latitude}&lon=${location.longitude}&limit=10&addressdetails=1"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "AbhayaSafetyApp/1.0 (anujsharma1555r@gmail.com)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val jsonArray = JSONArray(body)
                val list = mutableListOf<PoliceStation>()
                
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val displayName = obj.optString("display_name", "")
                    val latStr = obj.optString("lat", "0.0")
                    val lonStr = obj.optString("lon", "0.0")
                    
                    val parts = displayName.split(",")
                    val name = parts.firstOrNull()?.trim() ?: "Police Station"
                    val vicinity = if (parts.size > 1) parts.drop(1).joinToString(",").trim() else displayName
                    
                    val stationLatLng = LatLng(latStr.toDoubleOrNull() ?: 0.0, lonStr.toDoubleOrNull() ?: 0.0)
                    list.add(PoliceStation(name, stationLatLng, vicinity))
                }
                return list
            }
        } catch (e: Exception) {
            Log.e("MapsService", "Error searching nearby police stations from Nominatim", e)
            return emptyList()
        }
    }
}
