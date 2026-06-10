package ua.parking.app.ui.admin

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int
import kotlin.String
import ua.parking.app.R

public class AdminPanelFragmentDirections private constructor() {
  private data class ActionAdminPanelFragmentToAddEditParkingFragment(
    public val parkingId: String = "",
  ) : NavDirections {
    public override val actionId: Int = R.id.action_adminPanelFragment_to_addEditParkingFragment

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("parkingId", this.parkingId)
        return result
      }
  }

  public companion object {
    public fun actionAdminPanelFragmentToAddEditParkingFragment(parkingId: String = ""):
        NavDirections = ActionAdminPanelFragmentToAddEditParkingFragment(parkingId)
  }
}
