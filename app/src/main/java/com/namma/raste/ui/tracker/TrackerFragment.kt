package com.namma.raste.ui.tracker

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.namma.raste.R
import com.namma.raste.databinding.FragmentTrackerBinding
import com.namma.raste.utils.showToast
import com.namma.raste.utils.toReadableDate

class TrackerFragment : Fragment() {
    private var _binding: FragmentTrackerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TrackerViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTrackerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cardResult.visibility = View.GONE

        binding.btnSearch.setOnClickListener {
            val id = binding.etTicketId.text.toString().trim()
            if (id.isEmpty()) { requireContext().showToast("Enter a Ticket ID"); return@setOnClickListener }
            viewModel.searchTicket(id)
        }

        viewModel.report.observe(viewLifecycleOwner) { report ->
            if (report == null) {
                binding.cardResult.visibility = View.GONE
                requireContext().showToast("No report found for this Ticket ID")
            } else {
                binding.cardResult.visibility = View.VISIBLE
                binding.tvResultTicket.text   = report.ticketId
                binding.tvResultType.text     = "Issue: ${report.issueType}"
                binding.tvResultSeverity.text = "Severity: ${report.severity}"
                binding.tvResultDate.text     = "Reported: ${report.timestamp.toReadableDate()}"
                if (report.description.isNotEmpty()) {
                    binding.tvResultDescription.visibility = View.VISIBLE
                    binding.tvResultDescription.text       = "📝 ${report.description}"
                } else {
                    binding.tvResultDescription.visibility = View.GONE
                }
                binding.tvResultStatus.text   = report.status
                binding.tvResultStatus.setTextColor(requireContext().getColor(when (report.status) {
                    "IN_PROGRESS" -> R.color.status_in_progress
                    "RESOLVED"    -> R.color.status_resolved
                    else          -> R.color.status_submitted
                }))
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
