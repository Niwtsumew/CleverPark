package ua.parking.app.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ua.parking.app.databinding.FragmentAdminPanelBinding

class AdminPanelFragment : Fragment() {

    private var _binding: FragmentAdminPanelBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()
    private lateinit var adapter: AdminAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminPanelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminAdapter(
            onEdit = { parking ->
                val action = AdminPanelFragmentDirections.actionAdminPanelFragmentToAddEditParkingFragment(parking.id)
                findNavController().navigate(action)
            },
            onDelete = { parking ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Видалити парковку?")
                    .setMessage("${parking.name} буде видалено назавжди.")
                    .setPositiveButton("Видалити") { _, _ -> viewModel.deleteParking(parking.id) }
                    .setNegativeButton("Скасувати", null)
                    .show()
            }
        )
        binding.recyclerView.adapter = adapter

        binding.fabAdd.setOnClickListener {
            val action = AdminPanelFragmentDirections.actionAdminPanelFragmentToAddEditParkingFragment("")
            findNavController().navigate(action)
        }

        viewModel.parkings.observe(viewLifecycleOwner) { adapter.submitList(it) }

        viewModel.operationResult.observe(viewLifecycleOwner) { msg ->
            msg ?: return@observe
            Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
            viewModel.clearResult()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
