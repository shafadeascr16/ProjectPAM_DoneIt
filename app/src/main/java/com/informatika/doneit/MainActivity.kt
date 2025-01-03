package com.informatika.doneit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.navigation.ui.AppBarConfiguration
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import com.informatika.doneit.databinding.ActivityMainBinding
import com.informatika.doneit.ui.account.AccountFragment
import com.informatika.doneit.ui.home.HomeFragment

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    val user = Firebase.auth.currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (user != null) {
            loadFragment(HomeFragment())
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Menutup MainActivity jika pengguna belum login
        }

        setupOnClickListener()
    }

    private fun setupOnClickListener() {
        binding.bottomNavigation.setOnItemSelectedListener {
            val fragment = when (it.itemId) {
                R.id.home -> HomeFragment()
                R.id.account -> AccountFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
