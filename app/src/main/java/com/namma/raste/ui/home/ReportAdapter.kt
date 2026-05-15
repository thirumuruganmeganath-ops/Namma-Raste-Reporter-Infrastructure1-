package com.namma.raste.ui.home

import android.view.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.namma.raste.R
import com.namma.raste.data.model.Report
import com.namma.raste.databinding.ItemReportBinding
import com.namma.raste.utils.getAddressFromLocation
import com.namma.raste.utils.toReadableDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReportAdapter(
    private val onDeleteClick: (String) -> Unit
) : ListAdapter<Report, ReportAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            ItemReportBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onDeleteClick
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    class ViewHolder(
        private val b: ItemReportBinding,
        private val onDeleteClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(b.root) {
        fun bind(report: Report) {
            val ctx = b.root.context
            val photoFile = java.io.File(report.photoPath)
            if (photoFile.exists()) {
                com.bumptech.glide.Glide.with(ctx)
                    .load(photoFile)
                    .centerCrop()
                    .transform(com.bumptech.glide.load.resource.bitmap.RoundedCorners(24))
                    .placeholder(android.R.color.darker_gray)
                    .into(b.ivReportPhoto)
            } else {
                b.ivReportPhoto.setBackgroundColor(
                    android.graphics.Color.parseColor("#F0F0F0")
                )
            }
            b.tvTicketId.text = report.ticketId
            b.tvDate.text = report.timestamp.toReadableDate()
            b.tvIssueType.text = when {
                report.issueType == "POTHOLE"              -> "🕳️  Pothole"
                report.issueType == "STREETLIGHT"          -> "💡  Streetlight"
                report.issueType.startsWith("OTHER:")      -> "📢  ${report.issueType.removePrefix("OTHER:").trim()}"
                else                                       -> "📢  ${report.issueType}"
            }
            // Show coordinates immediately, then replace with address
            b.tvLocation.text = "📍 fetching address..."
            CoroutineScope(Dispatchers.IO).launch {
                val address = getAddressFromLocation(
                    b.root.context, report.latitude, report.longitude
                )
                withContext(Dispatchers.Main) {
                    b.tvLocation.text = "📍 $address"
                }
            }
            // Description — show only if not empty
            if (report.description.isNotEmpty()) {
                b.tvDescription.visibility = View.VISIBLE
                b.tvDescription.text = "📝  ${report.description}"
            } else {
                b.tvDescription.visibility = View.GONE
            }

            // Status badge
            b.tvStatus.text = report.status
            val statusColor = when (report.status) {
                "IN_PROGRESS" -> android.graphics.Color.parseColor("#FF6D00")
                "RESOLVED"    -> android.graphics.Color.parseColor("#2E7D32")
                else          -> android.graphics.Color.parseColor("#1A5CA8")
            }
            b.tvStatus.setBackgroundColor(statusColor)  

            // Severity badge
            b.tvSeverity.text = report.severity
            val severityColor = when (report.severity) {
                "HIGH" -> android.graphics.Color.parseColor("#C62828")
                "MEDIUM" -> android.graphics.Color.parseColor("#FF6D00")
                else -> android.graphics.Color.parseColor("#2E7D32")
            }
            b.tvSeverity.setBackgroundColor(severityColor)
            // Long press ticket ID to copy
            b.tvTicketId.setOnLongClickListener {
                val clipboard = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Ticket ID", report.ticketId)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(ctx, "✅ Ticket ID copied!", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
            // Share button
            b.btnShare.setOnClickListener {
                val issueEmoji = if (report.issueType == "POTHOLE") "🕳️" else "💡"
                val severityEmoji = when (report.severity) {
                    "HIGH"   -> "🔴"
                    "MEDIUM" -> "🟡"
                    else     -> "🟢"
                }

                // Fetch readable address first then share
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val address = try {
                        val geocoder  = android.location.Geocoder(ctx, java.util.Locale.getDefault())
                        val addresses = geocoder.getFromLocation(report.latitude, report.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val a     = addresses[0]
                            val parts = mutableListOf<String>()
                            if (!a.subLocality.isNullOrEmpty()) parts.add(a.subLocality)
                            if (!a.locality.isNullOrEmpty())    parts.add(a.locality)
                            if (!a.adminArea.isNullOrEmpty())   parts.add(a.adminArea)
                            if (parts.isEmpty()) a.getAddressLine(0) ?: "${report.latitude}, ${report.longitude}"
                            else parts.joinToString(", ")
                        } else {
                            "${"%.4f".format(report.latitude)}, ${"%.4f".format(report.longitude)}"
                        }
                    } catch (e: Exception) {
                        "${"%.4f".format(report.latitude)}, ${"%.4f".format(report.longitude)}"
                    }

                    val shareText = """
            🚧 Namma-Raste Infrastructure Report
            
            $issueEmoji Issue Type  : ${report.issueType}
            $severityEmoji Severity    : ${report.severity}
            🎫 Ticket ID   : ${report.ticketId}
            📍 Location    : $address
            🕐 Reported    : ${report.timestamp.toReadableDate()}
            📋 Status      : ${report.status}
            ${if (report.description.isNotEmpty()) "📝 Description : ${report.description}" else ""}
            
            Reported via Namma-Raste App
            Track status using Ticket ID above.
        """.trimIndent()

                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type    = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            putExtra(
                                android.content.Intent.EXTRA_SUBJECT,
                                "Road Issue Report - ${report.ticketId}"
                            )
                        }
                        ctx.startActivity(
                            android.content.Intent.createChooser(intent, "Share report via")
                        )
                    }
                }
            }
            // Delete button with confirmation
            b.btnDelete.setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(ctx)
                    .setTitle("Mark as Resolved?")
                    .setMessage("This will move ${report.ticketId} to the Resolved section.\n\nThe report will not be deleted — it will be kept as a record.")
                    .setPositiveButton("✅ Mark Resolved") { _, _ ->
                        onDeleteClick(report.ticketId)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }


    }
    class DiffCallback : DiffUtil.ItemCallback<Report>() {
        override fun areItemsTheSame(a: Report, b: Report) = a.ticketId == b.ticketId
        override fun areContentsTheSame(a: Report, b: Report) = a == b
    }
}
