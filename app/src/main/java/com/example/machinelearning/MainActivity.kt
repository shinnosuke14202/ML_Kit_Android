package com.example.machinelearning

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.transition.Visibility
import com.example.machinelearning.databinding.ActivityMainBinding
import com.example.machinelearning.scanner.ScannerFragment
import com.example.machinelearning.translator.TranslatorFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val translatorFragment = TranslatorFragment()
        val scannerFragment = ScannerFragment()

        setCurrentFragment(translatorFragment)

        binding.bnvMain.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.miTranslate -> setCurrentFragment(translatorFragment)
                R.id.miScanner -> setCurrentFragment(scannerFragment)
            }
            true
        }
    }

    private fun setCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flMainLayout, fragment)
            commit()
        }
    }

    fun showOrHideBottomNavigation(option : Int) {
        when (option) {
            0 -> binding.bnvMain.visibility = View.GONE
            1 -> binding.bnvMain.visibility = View.VISIBLE
        }
    }
}
