package ua.parking.app.ui.splash

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import ua.parking.app.R

public class SplashFragmentDirections private constructor() {
  public companion object {
    public fun actionSplashFragmentToLoginFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_splashFragment_to_loginFragment)

    public fun actionSplashFragmentToParkingListFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_splashFragment_to_parkingListFragment)
  }
}
