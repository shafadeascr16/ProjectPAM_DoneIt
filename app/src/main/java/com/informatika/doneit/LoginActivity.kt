package com.informatika.doneit

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.informatika.doneit.databinding.ActivityLoginBinding
import com.informatika.doneit.ui.auth.ForgotPasswordFragment
import com.informatika.doneit.ui.auth.LoginFragment
import com.informatika.doneit.ui.auth.RegisterFragment
import com.informatika.doneit.ui.auth.WelcomeFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var doubleBackToExitPressedOnce = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        loadFragment(WelcomeFragment())
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
        when (currentFragment) {
            is WelcomeFragment -> {
                if (doubleBackToExitPressedOnce) {
                    finishAffinity()
                    return
                }

                this.doubleBackToExitPressedOnce = true
                Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show()

                handler.postDelayed({
                    doubleBackToExitPressedOnce = false
                }, 2000)
            }
            is LoginFragment, is RegisterFragment, is ForgotPasswordFragment -> {
                loadFragment(WelcomeFragment())
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
