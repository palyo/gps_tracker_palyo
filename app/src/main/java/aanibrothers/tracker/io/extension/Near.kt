package aanibrothers.tracker.io.extension

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.model.NearLocation
import android.content.Context

fun nearLocations(context: Context): MutableList<Any> {
    return mutableListOf(
        context.getString(R.string.near_category_food_drink),
        NearLocation(R.drawable.ic_location_near_restaurant, context.getString(R.string.near_place_restaurant)),
        NearLocation(R.drawable.ic_location_near_bars, context.getString(R.string.near_place_bars)),
        NearLocation(R.drawable.ic_location_near_coffee, context.getString(R.string.near_place_coffee)),
        NearLocation(R.drawable.ic_location_near_delivery, context.getString(R.string.near_place_delivery)),
        context.getString(R.string.near_category_services),
        NearLocation(R.drawable.ic_location_near_hotel, context.getString(R.string.near_place_hotels)),
        NearLocation(R.drawable.ic_location_near_atm, context.getString(R.string.near_place_atm)),
        NearLocation(R.drawable.ic_location_near_saloon, context.getString(R.string.near_place_saloon)),
        NearLocation(R.drawable.ic_location_near_bank, context.getString(R.string.near_place_bank)),
        NearLocation(R.drawable.ic_location_near_dry_clean, context.getString(R.string.near_place_dry_clean)),
        NearLocation(R.drawable.ic_location_near_hospital, context.getString(R.string.near_place_hospital)),
        NearLocation(R.drawable.ic_location_near_medical, context.getString(R.string.near_place_medical)),
        NearLocation(R.drawable.ic_location_near_gas, context.getString(R.string.near_place_gas_station)),
        context.getString(R.string.near_category_think_to_do),
        NearLocation(R.drawable.ic_location_near_park, context.getString(R.string.near_place_park)),
        NearLocation(R.drawable.ic_location_near_gym, context.getString(R.string.near_place_gym)),
        NearLocation(R.drawable.ic_location_near_amusement, context.getString(R.string.near_place_amusement)),
        NearLocation(R.drawable.ic_location_near_nightfire, context.getString(R.string.near_place_nightfire)),
        NearLocation(R.drawable.ic_location_near_art, context.getString(R.string.near_place_art)),
        NearLocation(R.drawable.ic_location_near_movie, context.getString(R.string.near_place_movie)),
        NearLocation(R.drawable.ic_location_near_museum, context.getString(R.string.near_place_museum)),
        context.getString(R.string.near_category_shopping),
        NearLocation(R.drawable.ic_location_near_grocery, context.getString(R.string.near_place_grocery)),
        NearLocation(R.drawable.ic_location_near_book_store, context.getString(R.string.near_place_book)),
        NearLocation(R.drawable.ic_location_near_home, context.getString(R.string.near_place_home)),
        NearLocation(R.drawable.ic_location_near_garage, context.getString(R.string.near_place_car_dealer)),
        NearLocation(R.drawable.ic_location_near_clothes, context.getString(R.string.near_place_clothes)),
        NearLocation(R.drawable.ic_location_near_shop, context.getString(R.string.near_place_shop)),
        NearLocation(R.drawable.ic_location_near_electronics, context.getString(R.string.near_place_electronics)),
        NearLocation(R.drawable.ic_location_near_jewellery, context.getString(R.string.near_place_jewellery)),
    )
}
