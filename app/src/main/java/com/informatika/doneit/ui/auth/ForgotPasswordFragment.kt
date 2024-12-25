package com.informatika.doneit.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import com.informatika.doneit.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import com.informatika.doneit.R
import com.informatika.doneit.databinding.FragmentForgotPasswordBinding
import com.informatika.doneit.util.*

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {

    val TAG: String = "ForgotPasswordFragment"
    private lateinit var binding: FragmentForgotPasswordBinding
    val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForgotPasswordBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup toolbar
        setupToolbar()

        // Setup back press handling
        setupBackPress()

        observer()

        binding.forgotPasswordButton.setOnClickListener {
            if (validation()){
                viewModel.forgotPassword(binding.username.text.toString())
            }
        }
    }

    private fun setupToolbar() {
        (requireActivity() as LoginActivity).apply {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            supportActionBar?.title = "Forgot Password"
        }
    }

    private fun setupBackPress() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun navigateBack() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, LoginFragment())
            .commit()
    }

    private fun observer() {
        viewModel.forgotPassword.observe(viewLifecycleOwner) { state ->
            when(state) {
                is UiState.Loading -> {
                    binding.loading.show()
                }
                is UiState.Failure -> {
                    binding.loading.hide()
                    snackbar(state.error ?: "Error occurred")
                }
                is UiState.Success -> {
                    binding.loading.hide()
                    snackbar(state.data)
                    navigateBack()
                }
            }
        }
    }

    fun validation(): Boolean {
        var isValid = true
        if (binding.username.text.isNullOrEmpty()) {
            isValid = false
            snackbar(getString(R.string.enter_email))
        } else {
            if (!binding.username.text.toString().isValidEmail()) {
                isValid = false
                snackbar(getString(R.string.invalid_email))
            }
        }
        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset toolbar title and navigation
        (requireActivity() as LoginActivity).apply {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayShowHomeEnabled(false)
            supportActionBar?.title = ""
        }
    }
}