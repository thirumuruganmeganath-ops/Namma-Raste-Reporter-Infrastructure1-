package com.namma.raste.ui.preview

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.namma.raste.R
import com.namma.raste.databinding.FragmentPreviewBinding
import com.namma.raste.utils.LocationHelper
import com.namma.raste.utils.showToast
import kotlinx.coroutines.*
import java.io.File

class PreviewFragment : Fragment() {
    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PreviewViewModel by viewModels()
    private val args: PreviewFragmentArgs by navArgs()

    private var selectedType     = "POTHOLE"
    private var selectedSeverity = "MEDIUM"
    private var capturedLat      = 0.0
    private var capturedLng      = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Glide.with(this).load(File(args.photoPath)).into(binding.ivPhoto)
        fetchLocation()

        // Issue type buttons
        setIssueType("POTHOLE")
        binding.btnPothole.setOnClickListener {
            setIssueType("POTHOLE")
            binding.tilOtherIssue.visibility = View.GONE
        }
        binding.btnStreetlight.setOnClickListener {
            setIssueType("STREETLIGHT")
            binding.tilOtherIssue.visibility = View.GONE
        }
        binding.btnOther.setOnClickListener {
            setIssueType("OTHER")
            binding.tilOtherIssue.visibility = View.VISIBLE
        }

        binding.rgSeverity.setOnCheckedChangeListener { _, id ->
            selectedSeverity = when (id) {
                binding.rbLow.id  -> "LOW"
                binding.rbHigh.id -> "HIGH"
                else              -> "MEDIUM"
            }
        }

        binding.btnSubmit.setOnClickListener {
            // If Other selected, use the custom text as issue type
            if (selectedType == "OTHER") {
                val otherText = binding.etOtherIssue.text.toString().trim()
                if (otherText.isEmpty()) {
                    requireContext().showToast("Please describe the issue")
                    return@setOnClickListener
                }
                selectedType = "OTHER: $otherText"
            }
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSubmit.isEnabled    = false
            val description = binding.etDescription.text.toString().trim()
            viewModel.submitReport(
                args.photoPath, selectedType, selectedSeverity, capturedLat, capturedLng, description
            )
        }

        viewModel.submitResult.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            binding.btnSubmit.isEnabled    = true
            result.onSuccess  { ticketId -> showSuccessDialog(ticketId) }
            result.onFailure  { e -> requireContext().showToast("Failed: ${e.message}") }
        }
    }

    private fun fetchLocation() {
        binding.tvLocation.text = "📍 Fetching location..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val loc = LocationHelper.getCurrentLocation(requireContext())
                capturedLat = loc.latitude
                capturedLng = loc.longitude

                // Reverse geocoding — convert GPS to readable address
                val geocoder = android.location.Geocoder(
                    requireContext(), java.util.Locale.getDefault()
                )
                val addresses = geocoder.getFromLocation(capturedLat, capturedLng, 1)

                val readableAddress = if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    buildString {
                        // Area / locality
                        address.subLocality?.let { append(it).append(", ") }
                        // City
                        address.locality?.let { append(it).append(", ") }
                        // State
                        address.adminArea?.let { append(it) }
                    }
                } else {
                    // Fallback to coordinates if geocoding fails
                    "${"%.5f".format(capturedLat)}, ${"%.5f".format(capturedLng)}"
                }

                withContext(Dispatchers.Main) {
                    binding.tvLocation.text = "📍 $readableAddress"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvLocation.text = "📍 Location unavailable"
                }
            }
        }
    }

    private fun showSuccessDialog(ticketId: String) {
        // Dismiss keyboard if open
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)

        MaterialAlertDialogBuilder(requireContext())
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle("✅ Report Submitted!")
            .setMessage(
                "Your report has been successfully submitted.\n\n" +
                        "🎫  Ticket ID:\n" +
                        "$ticketId\n\n" +
                        "📌 Save this Ticket ID to track\n" +
                        "the status of your report."
            )
            .setCancelable(false)
            .setPositiveButton("🏠  Go to Home") { _, _ ->
                findNavController().popBackStack(R.id.homeFragment, false)
            }
            .setNeutralButton("📋  Copy Ticket ID") { dialog, _ ->
                val clipboard = requireContext().getSystemService(
                    android.content.Context.CLIPBOARD_SERVICE
                ) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Ticket ID", ticketId)
                clipboard.setPrimaryClip(clip)
                requireContext().showToast("✅ Ticket ID copied!")
                dialog.dismiss()
                findNavController().popBackStack(R.id.homeFragment, false)
            }
            .show()
    }
    private fun setIssueType(type: String) {
        selectedType = type
        val selected   = R.drawable.bg_issue_selected
        val unselected = R.drawable.bg_issue_unselected

        binding.btnPothole.setBackgroundResource(
            if (type == "POTHOLE") selected else unselected)
        binding.btnStreetlight.setBackgroundResource(
            if (type == "STREETLIGHT") selected else unselected)
        binding.btnOther.setBackgroundResource(
            if (type == "OTHER") selected else unselected)

        // Update text colors
        binding.btnPothole.getChildAt(1)?.let {
            (it as? android.widget.TextView)?.setTextColor(
                if (type == "POTHOLE") android.graphics.Color.parseColor("#1A5CA8")
                else android.graphics.Color.parseColor("#888888"))
        }
        binding.btnStreetlight.getChildAt(1)?.let {
            (it as? android.widget.TextView)?.setTextColor(
                if (type == "STREETLIGHT") android.graphics.Color.parseColor("#1A5CA8")
                else android.graphics.Color.parseColor("#888888"))
        }
        binding.btnOther.getChildAt(1)?.let {
            (it as? android.widget.TextView)?.setTextColor(
                if (type == "OTHER") android.graphics.Color.parseColor("#1A5CA8")
                else android.graphics.Color.parseColor("#888888"))
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}