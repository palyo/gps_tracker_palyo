package aanibrothers.tracker.io.adapter

import aanibrothers.tracker.io.databinding.*
import android.content.*
import android.view.*
import coder.apps.space.library.extension.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

class CustomInfoWindowAdapter(private val context: Context, backgroundColor: Int, textColor: Int) : GoogleMap.InfoWindowAdapter {
    override fun getInfoWindow(marker: Marker): View {
        val view = LayoutMapMarkerInfoWindowBinding.inflate(LayoutInflater.from(context), null, false)
        view.infoTitle.text = marker.title
        view.infoSnippet.beVisibleIf(marker.snippet?.isNotEmpty() == true)
        if (marker.snippet?.isNotEmpty() == true) view.infoSnippet.text = marker.snippet
        return view.root
    }

    override fun getInfoContents(marker: Marker): View? {
        return null
    }
}
