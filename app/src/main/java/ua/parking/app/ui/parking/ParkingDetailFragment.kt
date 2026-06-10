package ua.parking.app.ui.parking

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import ua.parking.app.R
import ua.parking.app.databinding.FragmentParkingDetailBinding

class ParkingDetailFragment : Fragment() {

    private var _binding: FragmentParkingDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ParkingDetailViewModel by viewModels()
    private val args: ParkingDetailFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentParkingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadParking(args.parkingId)

        viewModel.parking.observe(viewLifecycleOwner) { parking ->
            binding.tvName.text = parking.name
            binding.tvAddress.text = parking.address
            binding.tvSchedule.text = parking.schedule
            binding.tvTotalSpots.text = getString(R.string.total_spots_format, parking.totalSpots)
            binding.tvFreeSpots.text = getString(R.string.free_spots_format, parking.freeSpots)

            if (parking.isPaid) {
                binding.tvPrice.visibility = View.VISIBLE
                binding.tvPrice.text = getString(R.string.price_per_hour_format, parking.pricePerHour!!)
            } else {
                binding.tvPrice.visibility = View.VISIBLE
                binding.tvPrice.text = getString(R.string.parking_free_label)
            }

            if (parking.isFull) {
                binding.tvStatus.text = getString(R.string.status_full)
                binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                binding.btnBook.isEnabled = false
            } else {
                binding.tvStatus.text = getString(R.string.status_available)
                binding.tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                val hasBooking = viewModel.activeBooking.value != null
                binding.btnBook.isEnabled = !hasBooking
            }

            Glide.with(this)
                .load(parking.photoUrl.ifBlank { null })
                .placeholder(R.drawable.ic_parking_placeholder)
                .error(R.drawable.ic_parking_placeholder)
                .centerCrop()
                .into(binding.ivParking)
        }

        viewModel.activeBooking.observe(viewLifecycleOwner) { booking ->
            val hasBooking = booking != null
            binding.btnBook.visibility = if (hasBooking) View.GONE else View.VISIBLE
            binding.layoutActiveBooking.visibility = if (hasBooking) View.VISIBLE else View.GONE
        }

        viewModel.timerText.observe(viewLifecycleOwner) { text ->
            binding.tvBookingTimer.text = getString(R.string.booking_timer_label, text)
        }

        viewModel.costPreview.observe(viewLifecycleOwner) { cost ->
            if (cost.isNullOrEmpty()) {
                binding.tvCostPreview.visibility = View.GONE
            } else {
                binding.tvCostPreview.visibility = View.VISIBLE
                binding.tvCostPreview.text = getString(R.string.cost_preview_label, cost)
            }
        }

        viewModel.isFavorite.observe(viewLifecycleOwner) { isFav ->
            binding.fabFavorite.setImageResource(
                if (isFav) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )
        }

        binding.btnBook.setOnClickListener { viewModel.bookSpot(args.parkingId) }
        binding.btnFree.setOnClickListener { viewModel.freeSpot(args.parkingId) }
        binding.fabFavorite.setOnClickListener { viewModel.toggleFavorite(args.parkingId) }

        viewModel.bookingResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            when (result) {
                is BookingUiResult.InsufficientBalance -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Недостатньо коштів")
                        .setMessage(
                            "Вартість паркування: ${"%.2f".format(result.cost)} грн\n" +
                            "Ваш баланс: ${"%.2f".format(result.balance)} грн\n\n" +
                            "Поповніть баланс у профілі."
                        )
                        .setPositiveButton("Зрозуміло", null)
                        .show()
                }
                is BookingUiResult.Freed -> {
                    val msg = if (result.costFormatted != null)
                        "Місце звільнено. Списано: ${result.costFormatted}"
                    else
                        getString(R.string.booking_freed)
                    Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                }
                else -> {
                    val message = when (result) {
                        is BookingUiResult.Success       -> getString(R.string.booking_success)
                        is BookingUiResult.Full          -> getString(R.string.parking_full_error)
                        is BookingUiResult.AlreadyBooked -> getString(R.string.booking_already_booked)
                        else -> ""
                    }
                    if (message.isNotEmpty())
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                }
            }
            viewModel.clearBookingResult()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
