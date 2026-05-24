package com.example.relab_tool.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.relab_tool.databinding.ItemBluetoothFeatureRowBinding
import com.example.relab_tool.databinding.ItemBluetoothSectionHeaderBinding
import com.example.relab_tool.model.BluetoothFeature

class BluetoothFeaturesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_FEATURE = 1
    }

    private val items = mutableListOf<Any>()

    fun submitData(featureGroups: List<com.example.relab_tool.model.BluetoothFeatureGroup>) {
        items.clear()
        for (group in featureGroups) {
            items.add(group.titleRes) // Header as resource ID
            items.addAll(group.features) // Features
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is Int) TYPE_HEADER else TYPE_FEATURE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val binding = ItemBluetoothSectionHeaderBinding.inflate(inflater, parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = ItemBluetoothFeatureRowBinding.inflate(inflater, parent, false)
            FeatureViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is HeaderViewHolder && item is Int) {
            holder.bind(item)
        } else if (holder is FeatureViewHolder && item is BluetoothFeature) {
            holder.bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class HeaderViewHolder(private val binding: ItemBluetoothSectionHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(titleRes: Int) {
            binding.tvSectionTitle.setText(titleRes)
        }
    }

    inner class FeatureViewHolder(private val binding: ItemBluetoothFeatureRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(feature: BluetoothFeature) {
            binding.tvFeatureName.setText(feature.nameRes)
            
            // Following existing design from Compose
            if (feature.isSupported) {
                binding.tvFeatureName.alpha = 1.0f
                binding.ivIcon.alpha = 1.0f
                binding.ivIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(binding.root.context, com.example.relab_tool.R.color.status_green))
                binding.tvNotSupported.visibility = View.GONE
            } else {
                binding.tvFeatureName.alpha = 0.5f
                binding.ivIcon.alpha = 0.5f
                binding.ivIcon.setColorFilter(android.graphics.Color.GRAY)
                binding.tvNotSupported.visibility = View.VISIBLE
            }
        }
    }
}
