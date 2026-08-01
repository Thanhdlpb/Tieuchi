package com.thanh.tieuchi

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.thanh.tieuchi.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textView.text = "Xin chào — Tieuchi Android (Kotlin)"
    }
}
