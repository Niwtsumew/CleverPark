package ua.parking.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import ua.parking.app.data.model.ParkingModel
import ua.parking.app.databinding.FragmentAddEditParkingBinding
import ua.parking.app.domain.ParkingPriceValidator

class AddEditParkingFragment : Fragment() {

    private var _binding: FragmentAddEditParkingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()
    private val args: AddEditParkingFragmentArgs by navArgs()
    private val priceValidator = ParkingPriceValidator()
    private var editingParking: ParkingModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddEditParkingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parkingId = args.parkingId
        if (parkingId.isNotEmpty()) {
            viewModel.parkings.observe(viewLifecycleOwner) { list ->
                list.find { it.id == parkingId }?.let { parking ->
                    editingParking = parking
                    binding.etName.setText(parking.name)
                    binding.etAddress.setText(parking.address)
                    binding.etPhotoUrl.setText(parking.photoUrl)
                    binding.etSchedule.setText(parking.schedule)
                    binding.etTotalSpots.setText(parking.totalSpots.toString())
                    parking.pricePerHour?.let { binding.etPricePerHour.setText(it.toString()) }
                }
            }
        }

        binding.btnSave.setOnClickListener { save() }
    }

    private fun save() {
        val name = binding.etName.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val photoUrl = binding.etPhotoUrl.text.toString().trim()
        val schedule = binding.etSchedule.text.toString().trim()
        val totalSpots = binding.etTotalSpots.text.toString().toIntOrNull() ?: 0
        val priceRaw = binding.etPricePerHour.text.toString()

        if (name.isBlank()) { binding.tilName.error = "Поле не може бути порожнім"; return }

        val (priceResult, price) = priceValidator.validate(priceRaw)
        if (!priceResult.isValid) {
            binding.tilPrice.error = priceResult.errorMessage
            return
        }
        binding.tilPrice.error = null

        val existing = editingParking
        if (existing != null) {
            viewModel.updateParking(
                existing.copy(name = name, address = address, photoUrl = photoUrl,
                    schedule = schedule, totalSpots = totalSpots, pricePerHour = price)
            )
        } else {
            viewModel.addParking(
                ParkingModel(name = name, address = address, photoUrl = photoUrl,
                    schedule = schedule, totalSpots = totalSpots, pricePerHour = price)
            )
        }
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
