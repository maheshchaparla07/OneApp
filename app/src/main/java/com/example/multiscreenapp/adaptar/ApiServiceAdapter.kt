package com.example.multiscreenapp.adaptar

// In adapter/ApiServiceAdapter.kt
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.multiscreenapp.R
import com.example.multiscreenapp.api.ApiService

class ApiServiceAdapter : ListAdapter<ApiService, ApiServiceAdapter.ApiServiceViewHolder>(DiffCallback()) {

    class ApiServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.service_title)
        private val descTextView: TextView = itemView.findViewById(R.id.service_description)

        fun bind(service: ApiService) {
            titleTextView.text = service.name
            descTextView.text = service.description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApiServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service, parent, false)
        return ApiServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApiServiceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }


    private class DiffCallback : DiffUtil.ItemCallback<ApiService>() {
        override fun areItemsTheSame(oldItem: ApiService, newItem: ApiService): Boolean {
            return oldItem.id == newItem.id
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: ApiService, newItem: ApiService): Boolean {
            return oldItem == newItem
        }
    }
}