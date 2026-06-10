package ua.parking.app.ui.favorites

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int
import kotlin.String
import ua.parking.app.R

public class FavoritesFragmentDirections private constructor() {
  private data class ActionFavoritesFragmentToParkingDetailFragment(
    public val parkingId: String,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_favoritesFragment_to_parkingDetailFragment

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("parkingId", this.parkingId)
        return result
      }
  }

  public companion object {
    public fun actionFavoritesFragmentToParkingDetailFragment(parkingId: String): NavDirections =
        ActionFavoritesFragmentToParkingDetailFragment(parkingId)
  }
}
