package com.example.multiscreenapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class WebAppAdapter(
    private val apps: MutableList<HomeActivity.WebApp>,
    private val onClick: (HomeActivity.WebApp) -> Unit
) : RecyclerView.Adapter<WebAppAdapter.WebAppViewHolder>() {

    class WebAppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val appName: TextView = view.findViewById(R.id.appName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebAppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_web_app, parent, false)
        return WebAppViewHolder(view)
    }

    override fun onBindViewHolder(holder: WebAppViewHolder, position: Int) {
        val app = apps[position]
        holder.appName.text = app.name

        // Safe icon loading with multiple fallbacks
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

        holder.itemView.setOnClickListener { onClick(app) }
    }

    private fun generateFaviconUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            "${uri.scheme}://${uri.host}/favicon.ico"
        } catch (e: Exception) {
            "" // Will trigger the error fallback
        }
    }

    override fun getItemCount() = apps.size
}