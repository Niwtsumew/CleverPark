package ua.parking.app.ui.auth

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import ua.parking.app.R

public class RegisterFragmentDirections private constructor() {
  public companion object {
    public fun actionRegisterFragmentToParkingListFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_registerFragment_to_parkingListFragment)
  }
}
