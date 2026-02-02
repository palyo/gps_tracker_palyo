package aanibrothers.tracker.io.locations

import aanibrothers.tracker.io.adapter.BaseXAdapter
import aanibrothers.tracker.io.databinding.LayoutRowItemCustomLocationBinding
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class LocationsAdapter(private val onItemSelected: (SavedLocationEntity) -> Unit) : BaseXAdapter<SavedLocationEntity, LayoutRowItemCustomLocationBinding>(
    bindingFactory = { inflater, parent, attachToRoot, _ ->
        LayoutRowItemCustomLocationBinding.inflate(inflater, parent, attachToRoot)
    }) {
    private var selectedId: Long? = null
    override fun LayoutRowItemCustomLocationBinding.bind(
        item: SavedLocationEntity, position: Int
    ) {

        textTitleLocation.text = item.title
        textAddressLocation.text = item.address
        textLatitudeLocationValue.text = item.latitude.toString()
        textLongitudeLocationValue.text = item.longitude.toString()

        isCheckLocation.visibility = if (selectedId == item.id) View.VISIBLE else View.GONE

        mapLocation.apply {
            onCreate(null)
            onResume() // VERY important

            getMapAsync { googleMap ->
                googleMap.uiSettings.setAllGesturesEnabled(false)

                val latLng = LatLng(item.latitude, item.longitude)

                googleMap.clear()
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(latLng, 14f)
                )
                googleMap.addMarker(
                    MarkerOptions().position(latLng)
                )
            }
        }

        root.setOnClickListener {
            setSelected(item.id)
            onItemSelected(item)
        }
    }

    fun clearSelection() {
        selectedId = null
        notifyDataSetChanged()
    }

    fun setSelected(id: Long) {
        selectedId = id
        notifyDataSetChanged()
    }
}
