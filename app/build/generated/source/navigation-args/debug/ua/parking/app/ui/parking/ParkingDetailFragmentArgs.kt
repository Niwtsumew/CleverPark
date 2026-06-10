package ua.parking.app.ui.parking

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import java.lang.IllegalArgumentException
import kotlin.String
import kotlin.jvm.JvmStatic

public data class ParkingDetailFragmentArgs(
  public val parkingId: String,
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
    public fun fromBundle(bundle: Bundle): ParkingDetailFragmentArgs {
      bundle.setClassLoader(ParkingDetailFragmentArgs::class.java.classLoader)
      val __parkingId : String?
      if (bundle.containsKey("parkingId")) {
        __parkingId = bundle.getString("parkingId")
        if (__parkingId == null) {
          throw IllegalArgumentException("Argument \"parkingId\" is marked as non-null but was passed a null value.")
        }
      } else {
        throw IllegalArgumentException("Required argument \"parkingId\" is missing and does not have an android:defaultValue")
      }
      return ParkingDetailFragmentArgs(__parkingId)
    }

    @JvmStatic
    public fun fromSavedStateHandle(savedStateHandle: SavedStateHandle): ParkingDetailFragmentArgs {
      val __parkingId : String?
      if (savedStateHandle.contains("parkingId")) {
        __parkingId = savedStateHandle["parkingId"]
        if (__parkingId == null) {
          throw IllegalArgumentException("Argument \"parkingId\" is marked as non-null but was passed a null value")
        }
      } else {
        throw IllegalArgumentException("Required argument \"parkingId\" is missing and does not have an android:defaultValue")
      }
      return ParkingDetailFragmentArgs(__parkingId)
    }
  }
}
