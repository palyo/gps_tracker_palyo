package aanibrothers.tracker.io.extension

import aanibrothers.tracker.io.*
import aanibrothers.tracker.io.model.*

val nearLocations: MutableList<Any>
    get() {
        return mutableListOf(
            "Food & Drink",
            NearLocation(R.drawable.ic_location_near_restaurant, "Restaurant"),
            NearLocation(R.drawable.ic_location_near_bars, "Bars"),
            NearLocation(R.drawable.ic_location_near_coffee, "Coffee"),
            NearLocation(R.drawable.ic_location_near_delivery, "Delivery"),
            "Services",
            NearLocation(R.drawable.ic_location_near_hotel, "Hotels"),
            NearLocation(R.drawable.ic_location_near_atm, "ATM"),
            NearLocation(R.drawable.ic_location_near_saloon, "Saloon"),
            NearLocation(R.drawable.ic_location_near_bank, "Bank"),
            NearLocation(R.drawable.ic_location_near_dry_clean, "Dry Clean"),
            NearLocation(R.drawable.ic_location_near_hospital, "Hospital"),
            NearLocation(R.drawable.ic_location_near_medical, "Medical"),
            NearLocation(R.drawable.ic_location_near_gas, "Gas Station"),
            "Think to do",
            NearLocation(R.drawable.ic_location_near_park, "Park"),
            NearLocation(R.drawable.ic_location_near_gym, "Gym"),
            NearLocation(R.drawable.ic_location_near_amusement, "Amusement"),
            NearLocation(R.drawable.ic_location_near_nightfire, "Nightfire"),
            NearLocation(R.drawable.ic_location_near_art, "Art"),
            NearLocation(R.drawable.ic_location_near_movie, "Movie"),
            NearLocation(R.drawable.ic_location_near_museum, "Museum"),
            "Shopping",
            NearLocation(R.drawable.ic_location_near_grocery, "Grocery"),
            NearLocation(R.drawable.ic_location_near_book_store, "Book"),
            NearLocation(R.drawable.ic_location_near_home, "Home"),
            NearLocation(R.drawable.ic_location_near_garage, "Car Dealer"),
            NearLocation(R.drawable.ic_location_near_clothes, "Clothes"),
            NearLocation(R.drawable.ic_location_near_shop, "Shop"),
            NearLocation(R.drawable.ic_location_near_electronics, "Electronics"),
            NearLocation(R.drawable.ic_location_near_jewellery, "Jewellery"),
        )
    }