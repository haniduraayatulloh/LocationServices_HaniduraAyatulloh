package com.example.praktikum8_haniduraayatulloh_225150207111005

import android.app.Application
import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale


data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "Menunggu lokasi..."
)

sealed class LocationState {
    object Idle : LocationState()
    object Loading : LocationState()
    data class Success(val location: LocationData) : LocationState()
    data class Error(val message: String) : LocationState()
}
class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val _locationState = mutableStateOf<LocationState>(LocationState.Idle)
    val locationState: State<LocationState> = _locationState


    private val _addressResult = mutableStateOf("Mencari alamat...")
    val addressResult: State<String> = _addressResult

    fun updateLocationStateToError(errorMessage: String) {
        _locationState.value = LocationState.Error(errorMessage)
    }


    fun startLocationFetch(fusedLocationClient: FusedLocationProviderClient) {
        if (locationState.value == LocationState.Loading) return
        _locationState.value = LocationState.Loading

        fetchLastLocation(fusedLocationClient)
    }


    @Suppress("MissingPermission")
    private fun fetchLastLocation(fusedLocationClient: FusedLocationProviderClient) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {

                val locationData = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                _locationState.value = LocationState.Success(locationData)

                viewModelScope.launch {
                    val context = application.applicationContext
                    val address = getAddressFromCoordinates(context, location)
                    _addressResult.value = address
                }
            } else {

                _locationState.value = LocationState.Error("Lokasi tidak ditemukan. Pastikan GPS aktif.")
            }
        }.addOnFailureListener { e ->
            _locationState.value = LocationState.Error("Gagal mengambil lokasi: ${e.message}")
        }
    }


    private suspend fun getAddressFromCoordinates(context: Context, location: Location): String =
        withContext(Dispatchers.IO) {
            val geocoder = Geocoder(context, Locale.getDefault())
            return@withContext try {
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (addresses.isNullOrEmpty().not()) {
                    addresses!![0].getAddressLine(0) ?: "Alamat tidak dapat diurai"
                } else {
                    "Alamat tidak ditemukan (Internet mungkin bermasalah)"
                }
            } catch (e: IOException) {
                "Layanan Geocoder tidak tersedia/Koneksi Internet putus."
            } catch (e: Exception) {
                "Kesalahan Geocoding: ${e.message}"
            }
        }
}