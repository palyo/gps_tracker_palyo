package aanibrothers.tracker.io.ui.dialog

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.LayoutDialogAddCustomLocationBinding
import aanibrothers.tracker.io.locations.SavedLocationEntity
import android.location.Geocoder
import android.os.Bundle
import android.view.KeyEvent
import androidx.lifecycle.lifecycleScope
import coder.apps.space.library.base.BaseDialog
import coder.apps.space.library.extension.showToast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AddCustomLocation(
    private val currentLat: Double? = null, private val currentLng: Double? = null
) : BaseDialog<LayoutDialogAddCustomLocationBinding>(LayoutDialogAddCustomLocationBinding::inflate) {
    private var googleMap: GoogleMap? = null
    private var radiusCircle: Circle? = null
    private val radiusMeters = 100.0
    override fun create() {}

    private fun handleBackPress() {
        dismiss()
    }

    override fun LayoutDialogAddCustomLocationBinding.viewCreated() {
        setupBackPressed()
        setupMap()
    }

    private fun LayoutDialogAddCustomLocationBinding.setupMap() {

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.view?.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
        mapFragment.getMapAsync { map ->
            googleMap = map
            map.uiSettings.apply {
                isMyLocationButtonEnabled = false
                isCompassEnabled = true
                isScrollGesturesEnabled = true
                isZoomGesturesEnabled = true
            }

            val startLatLng = if (currentLat != null && currentLng != null) {
                LatLng(currentLat, currentLng)
            } else {
                LatLng(28.6139, 77.2090) // fallback
            }

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 16f))

            // Update text fields immediately
            updateLocationFromCenter(startLatLng)

            // Draw radius circle
            drawRadius(startLatLng)

            map.setOnCameraIdleListener {
                val center = map.cameraPosition.target
                updateLocationFromCenter(center)
            }
        }
    }

    private fun moveCamera(latLng: LatLng) {
        googleMap?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(latLng, 16f)
        )

    }

    private fun LayoutDialogAddCustomLocationBinding.updateLocationFromCenter(
        latLng: LatLng
    ) {
        editLat.setText(latLng.latitude.toString())
        editLong.setText(latLng.longitude.toString())

        drawRadius(latLng)

        fetchAddress(latLng)
    }

    private fun drawRadius(center: LatLng) {
        radiusCircle?.remove()

        radiusCircle = googleMap?.addCircle(
            CircleOptions().center(center).radius(radiusMeters).strokeWidth(2f)
                .strokeColor(0x55007AFF).fillColor(0x22007AFF)
        )
    }

    private fun LayoutDialogAddCustomLocationBinding.fetchAddress(latLng: LatLng) {
        lifecycleScope.launch(Dispatchers.IO) {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(
                latLng.latitude, latLng.longitude, 1
            )

            val addressText = if (!addresses.isNullOrEmpty()) {
                val a = addresses[0]
                listOfNotNull(
                    a.featureName, a.subLocality, a.locality, a.adminArea, a.countryName
                ).joinToString(", ")
            } else {
                ""
            }

            withContext(Dispatchers.Main) {
                editAddress.setText(addressText)
            }
        }
    }

    private fun setupBackPressed() {
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                handleBackPress()
                true
            } else {
                false
            }
        }
    }

    override fun LayoutDialogAddCustomLocationBinding.initView() {}

    override fun LayoutDialogAddCustomLocationBinding.initListeners() {
        actionContinue.setOnClickListener {

            val lat = editLat.text.toString().toDoubleOrNull()
            val lng = editLong.text.toString().toDoubleOrNull()
            val address = editAddress.text.toString()
            val title = editTitle.text.toString()

            if (title.isEmpty()) {
                activity?.showToast(getString(R.string.message_title_validation))
                return@setOnClickListener
            }

            if (lat == null || lng == null || address.isEmpty()) return@setOnClickListener

            val entity = SavedLocationEntity(
                title = title, latitude = lat, longitude = lng, address = address, isDefault = true
            )

            listener?.invoke(entity)
            dismiss()
        }
    }

    companion object {
        private var listener: ((SavedLocationEntity) -> Unit)? = null

        fun newInstance(
            currentLat: Double?, currentLng: Double?, listener: (SavedLocationEntity) -> Unit
        ): AddCustomLocation {
            this.listener = listener
            return AddCustomLocation(currentLat, currentLng).apply {
                arguments = Bundle()
            }
        }
    }
}