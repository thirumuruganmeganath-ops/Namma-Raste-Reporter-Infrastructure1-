package com.namma.raste.ui.login

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.namma.raste.R
import com.namma.raste.databinding.FragmentLoginBinding
import com.namma.raste.utils.showToast

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        binding.btnLogin.setOnClickListener { handleLogin() }
        binding.btnRegister.setOnClickListener { handleRegister() }
    }

    private fun handleLogin() {
        val email = binding.etEmail.text.toString().trim()
        val pass  = binding.etPassword.text.toString().trim()
        if (email.isEmpty() || pass.isEmpty()) { requireContext().showToast("Enter email and password"); return }
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                requireContext().showToast("Login failed: ${it.message}")
            }
    }

    private fun handleRegister() {
        val email = binding.etEmail.text.toString().trim()
        val pass  = binding.etPassword.text.toString().trim()
        if (email.isEmpty() || pass.length < 6) { requireContext().showToast("Enter valid email & password (min 6 chars)"); return }
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true
                requireContext().showToast("Account created! Now login.")
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true
                requireContext().showToast("Register failed: ${it.message}")
            }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

