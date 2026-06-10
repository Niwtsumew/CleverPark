package ua.parking.app.ui.parking

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int
import kotlin.String
import ua.parking.app.R

public class ParkingListFragmentDirections private constructor() {
  private data class ActionParkingListFragmentToParkingDetailFragment(
    public val parkingId: String,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_parkingListFragment_to_parkingDetailFragment

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("parkingId", this.parkingId)
        return result
      }
  }

  public companion object {
    public fun actionParkingListFragmentToParkingDetailFragment(parkingId: String): NavDirections =
        ActionParkingListFragmentToParkingDetailFragment(parkingId)
  }
}
