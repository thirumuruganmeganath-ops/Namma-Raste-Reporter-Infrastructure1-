package com.namma.raste.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.namma.raste.R
import com.namma.raste.databinding.FragmentProfileBinding
import com.namma.raste.ui.home.HomeViewModel
import com.namma.raste.utils.toReadableDate

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user  = FirebaseAuth.getInstance().currentUser
        val email = user?.email ?: "Unknown"
        val uid   = user?.uid   ?: "—"
        val name  = email.substringBefore("@")

        // Header
        binding.tvProfileAvatar.text = name.first().uppercaseChar().toString()
        binding.tvProfileName.text   = name
        binding.tvProfileEmail.text  = email

        // Member since — from Firebase creation time
        val creationTime = user?.metadata?.creationTimestamp ?: 0L
        binding.tvMemberSince.text = if (creationTime > 0)
            "Member since ${creationTime.toReadableDate()}"
        else "Member since 2026"

        // Account details
        binding.tvDetailEmail.text  = email
        binding.tvDetailUserId.text = uid

        // Observe reports for stats
        viewModel.allReports.observe(viewLifecycleOwner) { reports ->
            val total      = reports.size
            val potholes   = reports.count { it.issueType == "POTHOLE" }
            val resolved   = reports.count { it.status == "RESOLVED" }
            val lastReport = reports.firstOrNull()

            binding.tvTotalCount.text   = total.toString()
            binding.tvPotholeCount.text = potholes.toString()
            binding.tvResolvedCount.text= resolved.toString()

            binding.tvLastReport.text = if (lastReport != null)
                lastReport.timestamp.toReadableDate()
            else
                "No reports yet"
        }

        // Logout
        binding.btnProfileLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.homeFragment, true)
                .build()
            findNavController().navigate(
                R.id.action_profileFragment_to_loginFragment, null, navOptions
            )
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}