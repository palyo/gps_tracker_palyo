package aanibrothers.tracker.io.locations

class LocationRepository(private val dao: SavedLocationDao) {
    suspend fun getLocations() = dao.getAll()

    suspend fun saveLocation(location: SavedLocationEntity) {
        dao.insert(location)
    }

    suspend fun setDefaultLocation(id: Long) {
        dao.clearDefault()
        dao.setDefault(id)
    }

    suspend fun getDefaultLocation(): SavedLocationEntity? =
        dao.getDefault()

}
