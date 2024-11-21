package com.talhaatif.financeapk

import android.annotation.SuppressLint
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Handler
import android.view.animation.AnimationUtils
import com.google.firebase.FirebaseApp
import com.talhaatif.financeapk.databinding.ActivitySplashScreenBinding
import com.talhaatif.financeapk.firebase.Util

@SuppressLint("CustomSplashScreen")
@Suppress("DEPRECATION")
class SplashScreen : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding
    private val utils = Util()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseApp.initializeApp(this)

        // Load animations
        val logoAnimation = AnimationUtils.loadAnimation(this, R.anim.logo_fade_in)
        val textAnimation = AnimationUtils.loadAnimation(this, R.anim.text_slide_up)

        // Start animations
        binding.logo.startAnimation(logoAnimation)
        binding.slogan.startAnimation(textAnimation)

        // Move to the next screen after 3 seconds
        Handler().postDelayed({
            val authStatus = utils.getLocalData(this, "auth")
            val targetClass = if (authStatus == "true") MainActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this, targetClass))
            finish()
        }, 3000)
    }
}
