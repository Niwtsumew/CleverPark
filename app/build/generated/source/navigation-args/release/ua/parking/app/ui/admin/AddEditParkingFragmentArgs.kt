package ua.parking.app.ui.admin

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import java.lang.IllegalArgumentException
import kotlin.String
import kotlin.jvm.JvmStatic

public data class AddEditParkingFragmentArgs(
  public val parkingId: String = "",
) : NavArgs {
  public fun toBundle(): Bundle {
    val result = Bundle()
    result.putString("parkingId", this.parkingId)
    return result
  }

  public fun toSavedStateHandle(): SavedStateHandle {
    val result = SavedStateHandle()
    result.set("parkingId", this.parkingId)
    return result
  }

  public companion object {
    @JvmStatic
    public fun fromBundle(bundle: Bundle): AddEditParkingFragmentArgs {
      bundle.setClassLoader(AddEditParkingFragmentArgs::class.java.classLoader)
      val __parkingId : String?
      if (bundle.containsKey("parkingId")) {
        __parkingId = bundle.getString("parkingId")
        if (__parkingId == null) {
          throw IllegalArgumentException("Argument \"parkingId\" is marked as non-null but was passed a null value.")
        }
      } else {
        __parkingId = ""
      }
      return AddEditParkingFragmentArgs(__parkingId)
    }

    @JvmStatic
    public fun fromSavedStateHandle(savedStateHandle: SavedStateHandle):
        AddEditParkingFragmentArgs {
      val __parkingId : String?
      if (savedStateHandle.contains("parkingId")) {
        __parkingId = savedStateHandle["parkingId"]
        if (__parkingId == null) {
          throw IllegalArgumentException("Argument \"parkingId\" is marked as non-null but was passed a null value")
        }
      } else {
        __parkingId = ""
      }
      return AddEditParkingFragmentArgs(__parkingId)
    }
  }
}
