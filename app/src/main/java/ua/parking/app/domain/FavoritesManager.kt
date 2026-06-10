package ua.parking.app.domain

class FavoritesManager {
    private val favorites = mutableSetOf<String>()

    fun addFavorite(parkingId: String) { favorites.add(parkingId) }
    fun removeFavorite(parkingId: String) { favorites.remove(parkingId) }
    fun isFavorite(parkingId: String): Boolean = favorites.contains(parkingId)
    fun getFavorites(): Set<String> = favorites.toSet()
}
