package ua.parking.app.ui.auth

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import ua.parking.app.R

public class LoginFragmentDirections private constructor() {
  public companion object {
    public fun actionLoginFragmentToRegisterFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_loginFragment_to_registerFragment)

    public fun actionLoginFragmentToParkingListFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_loginFragment_to_parkingListFragment)
  }
}
