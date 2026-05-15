package com.namma.raste.ui.home

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.namma.raste.R
import com.namma.raste.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: ReportAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }
        // Set avatar and welcome name
        val email = FirebaseAuth.getInstance().currentUser?.email ?: "user@email.com"
        binding.tvWelcome.text = email.substringBefore("@")
        binding.tvAvatar.text  = email.first().uppercaseChar().toString()

        // Setup RecyclerView
        adapter = ReportAdapter { ticketId -> viewModel.markAsResolved(ticketId) }
        binding.rvReports.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReports.adapter       = adapter

        // Observe reports + apply filter
        viewModel.allReports.observe(viewLifecycleOwner) { reports ->

            val total        = reports.size
            val potholes     = reports.count { it.issueType == "POTHOLE" }
            val streetlights = reports.count { it.issueType == "STREETLIGHT" }

            android.util.Log.d("STATS", "Total=$total Potholes=$potholes Streetlights=$streetlights")

            // Update stats
            binding.tvTotalReports.text = total.toString()
            binding.tvPotholes.text     = potholes.toString()
            binding.tvStreetlights.text = streetlights.toString()
            binding.tvReportCount.text  = total.toString() + " total"

            // Apply current filter
            val currentFilter = viewModel.activeFilter.value ?: "ALL"
            val filtered      = viewModel.getFilteredReports(reports, currentFilter)
            updateList(filtered)
        }

        // Filter chip listener
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when {
                checkedIds.contains(R.id.chipPothole)     -> "POTHOLE"
                checkedIds.contains(R.id.chipStreetlight) -> "STREETLIGHT"
                checkedIds.contains(R.id.chipHigh)        -> "HIGH"
                checkedIds.contains(R.id.chipMedium)      -> "MEDIUM"
                checkedIds.contains(R.id.chipLow)         -> "LOW"
                checkedIds.contains(R.id.chipResolved)    -> "RESOLVED"
                checkedIds.contains(R.id.chipOther)       -> "OTHER"
                else                                      -> "ALL"
            }
            viewModel.setFilter(filter)
            val reports  = viewModel.allReports.value ?: emptyList()
            val filtered = viewModel.getFilteredReports(reports, filter)
            updateList(filtered)
        }

        binding.fabReport.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_cameraFragment)
        }
        binding.btnTrack.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_trackerFragment)
        }
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.homeFragment, true)
                .build()
            findNavController().navigate(
                R.id.action_homeFragment_to_loginFragment, null, navOptions
            )
        }
    }

    private fun updateList(filtered: List<com.namma.raste.data.model.Report>) {
        adapter.submitList(filtered)
        if (filtered.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvReports.visibility        = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvReports.visibility        = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}