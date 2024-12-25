package com.informatika.doneit.ui.account

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.informatika.doneit.LoginActivity
import com.informatika.doneit.R
import com.informatika.doneit.databinding.FragmentAccountBinding
import com.informatika.doneit.ui.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    var user: FirebaseFirestore = FirebaseFirestore.getInstance()
    val viewModel: AuthViewModel by viewModels()

    private var doubleBackToExitPressedOnce = false
    private val handler = Handler(Looper.getMainLooper())

    val TAG = "Acc Fragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            user.collection("user").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val displayName = document.getString("name") // Adjust the field name as per your Firestore structure

                        // Set the retrieved data to the TextViews
                        binding.nameTextField.text = ("Hello, " + displayName) ?: "No Name"
                    } else {
                        Log.d(TAG, "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Get failed with ", exception)
                }
        } else {
            Log.w(TAG, "User ID is null")
        }

        binding.logoutButton.setOnClickListener {
            viewModel.logout {
                startActivity(Intent(requireContext(), LoginActivity::class.java))
            }
        }

        setupBackPress()
    }

    private fun setupBackPress() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun handleBackPress() {
        if (doubleBackToExitPressedOnce) {
            // Exit the app
            requireActivity().finishAffinity()
            return
        }

        // Set flag to true
        doubleBackToExitPressedOnce = true

        // Show toast message
        Toast.makeText(requireContext(), "Press again to exit", Toast.LENGTH_SHORT).show()

        // Reset flag after 2 seconds
        handler.postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
