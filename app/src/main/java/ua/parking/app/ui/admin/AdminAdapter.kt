package ua.parking.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ua.parking.app.data.model.ParkingModel
import ua.parking.app.databinding.ItemAdminParkingBinding

class AdminAdapter(
    private val onEdit: (ParkingModel) -> Unit,
    private val onDelete: (ParkingModel) -> Unit
) : ListAdapter<ParkingModel, AdminAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemAdminParkingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(parking: ParkingModel) {
            binding.tvName.text = parking.name
            binding.tvAddress.text = parking.address
            binding.tvSpots.text = "${parking.totalSpots} місць"
            binding.btnEdit.setOnClickListener { onEdit(parking) }
            binding.btnDelete.setOnClickListener { onDelete(parking) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemAdminParkingBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private class DiffCallback : DiffUtil.ItemCallback<ParkingModel>() {
        override fun areItemsTheSame(old: ParkingModel, new: ParkingModel) = old.id == new.id
        override fun areContentsTheSame(old: ParkingModel, new: ParkingModel) = old == new
    }
}
