package com.example.budgetasyougo

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetasyougo.databinding.LoadingBinding

class load : AppCompatActivity() {

    private lateinit var binding: LoadingBinding
    private val handler = Handler(Looper.getMainLooper())

    private val spinnerColors = listOf(
        0xFFE91E63.toInt(),
        0xFF3F51B5.toInt(),
        0xFFFF9800.toInt(),
        0xFF4CAF50.toInt(),
        0xFF9C27B0.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startColorCycle()
        startZoomAnimation()
        startTimeout()
    }

    private fun startColorCycle() {
        var index = 0
        val interval = 1000L

        val runnable = object : Runnable {
            override fun run() {
                val color = spinnerColors[index % spinnerColors.size]
                binding.loadingSpinner.indeterminateDrawable
                    .setColorFilter(color, PorterDuff.Mode.SRC_IN)
                index++
                handler.postDelayed(this, interval)
            }
        }

        handler.post(runnable)
    }

    private fun startZoomAnimation() {
        val zoomInOutX = ObjectAnimator.ofFloat(binding.loadingSpinner, "scaleX", 1.0f, 1.2f, 1.0f)
        val zoomInOutY = ObjectAnimator.ofFloat(binding.loadingSpinner, "scaleY", 1.0f, 1.2f, 1.0f)

        listOf(zoomInOutX, zoomInOutY).forEach { animator ->
            animator.duration = 1000
            animator.repeatCount = ValueAnimator.INFINITE
            animator.repeatMode = ValueAnimator.RESTART
            animator.start()
        }
    }

    private fun startTimeout() {
        handler.postDelayed({


            val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val email = sharedPref.getString("email", "")
            val userName = sharedPref.getString("name", "")

            if (!email.isNullOrEmpty() && !userName.isNullOrEmpty()) {
                // Both name and email are present, open another activity
                val intent = Intent(this, greeting::class.java)
                startActivity(intent)
                finish()
            }else {


                startActivity(Intent(this, login::class.java))
                finish()
            }
        }, 10_000L)
    }
}
