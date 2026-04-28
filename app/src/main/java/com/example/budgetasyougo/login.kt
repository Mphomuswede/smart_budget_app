package com.example.budgetasyougo

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class login : AppCompatActivity() {

    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var registerTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)

        emailInputLayout = findViewById(R.id.emailInputLayout)
        passwordInputLayout = findViewById(R.id.passwordInputLayout)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        registerTextView = findViewById(R.id.registerText)

        setupRegisterText()
        setupLoginButton()
    }

    private fun setupRegisterText() {
        val fullText = "Don't have an account? Register now"
        val spannable = SpannableString(fullText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Open Register
                val intent = Intent(this@login, register_user::class.java)
                startActivity(intent)
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.color = getColor(R.color.purple_200)
                ds.isUnderlineText = false
            }
        }

        val startIndex = fullText.indexOf("Register now")
        val endIndex = startIndex + "Register now".length

        spannable.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        registerTextView.text = spannable
        registerTextView.movementMethod = LinkMovementMethod.getInstance()
        registerTextView.highlightColor = Color.TRANSPARENT
    }

    private fun setupLoginButton() {
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            var valid = true

            if (email.isEmpty()) {
                emailInputLayout.error = "Email is required"
                valid = false
            } else {
                emailInputLayout.error = null
            }

            if (password.isEmpty()) {
                passwordInputLayout.error = "Password is required"
                valid = false
            } else {
                passwordInputLayout.error = null
            }

            if (valid) {

                val databases = FirebaseFirestore.getInstance();
                val rootView = findViewById<View>(android.R.id.content)

                databases.collection("users")
                    .whereEqualTo("email", email)
                    .whereEqualTo("password", password)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val userDoc = documents.documents[0]
                            val userName = userDoc.getString("name") ?: "User"

                            val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString("email", email)
                                putString("name", userName)
                                apply()
                            }

                            Snackbar.make(rootView, "Login successful. Welcome $userName!", Snackbar.LENGTH_LONG).show()

                            // open the dashboard
                            startActivity(Intent(this, dashboard::class.java))
                            finish()
                        } else {
                            Snackbar.make(rootView, "Invalid email or password", Snackbar.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { exception ->
                        Snackbar.make(rootView, "Error: ${exception.message}", Snackbar.LENGTH_LONG).show()
                    }
            }
        }
    }
}