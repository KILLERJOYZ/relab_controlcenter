package com.example.relab_tool.ui.cit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.relab_tool.R
import com.example.relab_tool.databinding.ItemCitTestTileBinding

class CITAdapter(
    private val onTestClick: (CITTestRoute) -> Unit
) : RecyclerView.Adapter<CITAdapter.ViewHolder>() {

    private var items: List<CITTestItem> = emptyList()

    fun submitList(newItems: List<CITTestItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCitTestTileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemCitTestTileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CITTestItem) {
            binding.tvName.text = item.name
            // We need to map ImageVector to drawable resource if we want to use XML ImageView
            // But since the icons are from Icons.Default, they are ImageVectors.
            // For now, I'll use a placeholder or update CITTestItem to include resource IDs.
            // Alternatively, I can use ComposeView inside the item tile to render the Icon.
            
            // To keep it simple and consistent with the requirement of XML layout, 
            // I'll assume we have a way to map them or just use a generic icon for now.
            // Or I'll update CITTestItem to use resource IDs.
            
            binding.ivIcon.setImageResource(R.drawable.baseline_bluetooth_24) // Placeholder
            
            val context = binding.root.context
            val (bgColor, contentColor, statusText) = when (item.status) {
                CITTestResult.NOT_TESTED -> Triple(
                    com.google.android.material.R.attr.colorSurfaceVariant,
                    com.google.android.material.R.attr.colorOnSurfaceVariant,
                    context.getString(R.string.cit_not_tested)
                )
                CITTestResult.PASS -> Triple(
                    R.color.status_green, // Using app's status_green
                    android.R.color.white,
                    context.getString(R.string.cit_pass)
                )
                CITTestResult.FAIL -> Triple(
                    com.google.android.material.R.attr.colorError,
                    com.google.android.material.R.attr.colorOnError,
                    context.getString(R.string.cit_fail)
                )
            }
            
            // Set colors using theme attributes where possible
            // For colors from colors.xml, use ContextCompat
            
            binding.chipStatus.text = statusText
            
            binding.root.setOnClickListener { onTestClick(item.id) }
        }
    }
}
