package ua.parking.app.ui.parking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ua.parking.app.R
import ua.parking.app.databinding.FragmentParkingListBinding
import ua.parking.app.domain.FilterType

class ParkingListFragment : Fragment() {

    private var _binding: FragmentParkingListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ParkingListViewModel by viewModels()
    private lateinit var adapter: ParkingAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentParkingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ParkingAdapter { parking ->
            val action = ParkingListFragmentDirections.actionParkingListFragmentToParkingDetailFragment(parking.id)
            findNavController().navigate(action)
        }
        binding.recyclerView.adapter = adapter

        binding.etSearch.addTextChangedListener { text ->
            viewModel.search(text?.toString() ?: "")
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val type = when {
                checkedIds.contains(R.id.chip_free) -> FilterType.FREE_ONLY
                checkedIds.contains(R.id.chip_paid) -> FilterType.PAID_ONLY
                else                                -> FilterType.ALL
            }
            viewModel.setFilter(type)
        }

        viewModel.filteredParkings.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
