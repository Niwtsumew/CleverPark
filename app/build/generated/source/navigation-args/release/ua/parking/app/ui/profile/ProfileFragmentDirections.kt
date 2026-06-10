package ua.parking.app.ui.profile

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import ua.parking.app.R

public class ProfileFragmentDirections private constructor() {
  public companion object {
    public fun actionProfileFragmentToLoginFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_profileFragment_to_loginFragment)

    public fun actionProfileFragmentToAdminPanelFragment(): NavDirections =
        ActionOnlyNavDirections(R.id.action_profileFragment_to_adminPanelFragment)
  }
}
