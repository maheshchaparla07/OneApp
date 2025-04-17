package com.example.multiscreenapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class WebAppAdapter(
    private val apps: MutableList<HomeActivity.WebApp>,
    private val onClick: (HomeActivity.WebApp) -> Unit,
    private val onDelete: (HomeActivity.WebApp) -> Unit // New parameter for delete callback
) : RecyclerView.Adapter<WebAppAdapter.WebAppViewHolder>() {

    class WebAppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val appName: TextView = view.findViewById(R.id.appName)
        val btnClose: ImageButton = view.findViewById(R.id.btnClose)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebAppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_web_app, parent, false)
        return WebAppViewHolder(view)
    }

    override fun onBindViewHolder(holder: WebAppViewHolder, position: Int) {
        val app = apps[position]
        holder.appName.text = app.name

        // Safe icon loading with multiple fallbacks (existing code preserved)
        when {
            !app.iconUrl.isNullOrEmpty() -> {
                Glide.with(holder.itemView.context)
                    .load(app.iconUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_broken_image)
                    .fallback(R.drawable.ic_no_image)
                    .into(holder.appIcon)
            }
            else -> {
                // If no iconUrl is provided, try to generate favicon
                val faviconUrl = generateFaviconUrl(app.url)
                Glide.with(holder.itemView.context)
                    .load(faviconUrl)
                    .error(R.drawable.ic_no_image)
                    .into(holder.appIcon)
            }
        }

        // Existing click listener
        holder.itemView.setOnClickListener { onClick(app) }

        // New long click listener for delete functionality
        holder.itemView.setOnLongClickListener {
            showDeleteConfirmationDialog(holder.itemView.context, app)
            true
        }
        holder.btnClose.setOnClickListener {
            onDelete(app)
        }
    }

    private fun generateFaviconUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            "${uri.scheme}://${uri.host}/favicon.ico"
        } catch (e: Exception) {
            "" // Will trigger the error fallback
        }
    }

    private fun showDeleteConfirmationDialog(context: android.content.Context, app: HomeActivity.WebApp) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Delete App")
            .setMessage("Are you sure you want to delete ${app.name}?")
            .setPositiveButton("Delete") { _, _ ->
                onDelete(app) // Call the delete callback
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun getItemCount() = apps.size
}