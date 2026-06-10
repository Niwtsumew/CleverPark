package ua.parking.app.ui.parking

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ua.parking.app.R
import ua.parking.app.data.model.ParkingModel
import ua.parking.app.databinding.ItemParkingBinding

class ParkingAdapter(private val onItemClick: (ParkingModel) -> Unit) :
    ListAdapter<ParkingModel, ParkingAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemParkingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(parking: ParkingModel) {
            binding.tvName.text = parking.name
            binding.tvAddress.text = parking.address
            binding.tvSchedule.text = parking.schedule
            binding.tvFreeSpots.text = itemView.context.getString(R.string.free_spots_format, parking.freeSpots)

            if (parking.isFull) {
                binding.tvStatus.text = itemView.context.getString(R.string.status_full)
                binding.tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
                binding.tvFreeSpots.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
            } else {
                binding.tvStatus.text = itemView.context.getString(R.string.status_available)
                binding.tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.green))
                binding.tvFreeSpots.setTextColor(ContextCompat.getColor(itemView.context, R.color.green))
            }

            if (parking.isPaid) {
                binding.tvPrice.text = itemView.context.getString(R.string.price_per_hour_format, parking.pricePerHour!!)
                binding.tvPrice.setTextColor(ContextCompat.getColor(itemView.context, R.color.md_theme_primary))
            } else {
                binding.tvPrice.text = itemView.context.getString(R.string.parking_free_label)
                binding.tvPrice.setTextColor(ContextCompat.getColor(itemView.context, R.color.green))
            }

            binding.root.setOnClickListener { onItemClick(parking) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemParkingBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private class DiffCallback : DiffUtil.ItemCallback<ParkingModel>() {
        override fun areItemsTheSame(old: ParkingModel, new: ParkingModel) = old.id == new.id
        override fun areContentsTheSame(old: ParkingModel, new: ParkingModel) = old == new
    }
}
