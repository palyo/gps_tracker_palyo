package aanibrothers.tracker.io.ui.updates

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.caller.alert.AppDatabase
import aanibrothers.tracker.io.databinding.ActivityLocationsBinding
import aanibrothers.tracker.io.locations.LocationPreference
import aanibrothers.tracker.io.locations.LocationRepository
import aanibrothers.tracker.io.locations.LocationsAdapter
import aanibrothers.tracker.io.ui.dialog.AddCustomLocation
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coder.apps.space.library.base.BaseActivity
import coder.apps.space.library.extension.beGone
import coder.apps.space.library.extension.beVisible
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class LocationsActivity :
    BaseActivity<ActivityLocationsBinding>(ActivityLocationsBinding::inflate) {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                fetchCurrentLocation()
            }
        }

    private val db by lazy { AppDatabase.getDatabase(this) }
    private lateinit var repository: LocationRepository
    private lateinit var adapter: LocationsAdapter

    override fun ActivityLocationsBinding.initExtra() {
        repository = LocationRepository(db.savedLocationDao())
        adapter = LocationsAdapter { selected ->
            LocationPreference.saveCustomLocation(
                this@LocationsActivity, selected
            )
            updateLocationSelection()
            setResult(RESULT_OK)
            finish()
        }
        recyclerView.layoutManager =
            LinearLayoutManager(this@LocationsActivity, RecyclerView.VERTICAL, false)
        recyclerView.adapter = adapter

        lifecycleScope.launch(Dispatchers.IO) {
            val saved = repository.getLocations()
            runOnUiThread {
                adapter.updateList(saved.toMutableList())
                updateLocationSelection()
            }
        }
    }

    fun ActivityLocationsBinding.updateLocationSelection() {
        val location = LocationPreference.getSelectedLocation(this@LocationsActivity)
        if (location == null) {
            isCheckCurrentLocation.beVisible()
            adapter.clearSelection()
        } else {
            location.let {
                adapter.setSelected(it.customId)
                isCheckCurrentLocation.beGone()
            }
        }
    }

    override fun ActivityLocationsBinding.initListeners() {
        layoutCurrentLocation.setOnClickListener {
            LocationPreference.markCurrentLocation(this@LocationsActivity)
            updateLocationSelection()
            setResult(RESULT_OK)
            finish()
        }

        actionAdd.setOnClickListener {
            val currentLat = textLatitudeCurrentLocationValue.text.toString().toDoubleOrNull()
            val currentLng = textLongitudeCurrentLocationValue.text.toString().toDoubleOrNull()

            AddCustomLocation.newInstance(
                currentLat = currentLat,
                currentLng = currentLng
            ) { entity ->
                lifecycleScope.launch(Dispatchers.IO) {
                    repository.saveLocation(entity)
                    runOnUiThread {
                        adapter.addItemAtTop(entity)
                        recyclerView.scrollToPosition(0)
                    }
                }
            }.show(
                supportFragmentManager, AddCustomLocation::class.java.simpleName
            )
        }
    }

    override fun ActivityLocationsBinding.initView() {
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        onBackPressedDispatcher.addCallback { finish() }
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this@LocationsActivity)

        if (ContextCompat.checkSelfPermission(
                this@LocationsActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchCurrentLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude

                setLocationData(lat, lng)
            }
        }
    }

    private fun setLocationData(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)

        val addressText = if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            listOfNotNull(
                address.featureName,
                address.subLocality,
                address.locality,
                address.adminArea,
                address.countryName
            ).joinToString(", ")
        } else {
            "Unknown location"
        }

        binding?.apply {
            textTitleCurrentLocation.text = getString(R.string.label_current_location)
            textAddressCurrentLocation.text = addressText

            textLatitudeCurrentLocationValue.text = latitude.toString()
            textLongitudeCurrentLocationValue.text = longitude.toString()
        }
    }
}