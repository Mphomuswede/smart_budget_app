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

class register_user : AppCompatActivity() {

    private lateinit var nameInputLayout: TextInputLayout
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var nameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var registerButton: Button
    private lateinit var loginTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_page)


        nameInputLayout = findViewById(R.id.nameInputLayout)
        emailInputLayout = findViewById(R.id.emailInputLayout)
        passwordInputLayout = findViewById(R.id.passwordInputLayout)
        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        registerButton = findViewById(R.id.registerButton)
        loginTextView = findViewById(R.id.loginText)

        setupLoginText()
        setupRegisterButton()
    }

    private fun setupLoginText() {
        val fullText = "Already have an account? Login in now"
        val spannable = SpannableString(fullText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Open Login Activity
                val intent = Intent(this@register_user, login::class.java)
                startActivity(intent)
                finish()
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.color = getColor(R.color.purple_200)
                ds.isUnderlineText = false
            }
        }

        val startIndex = fullText.indexOf("Login in now")
        val endIndex = startIndex + "Login in now".length

        spannable.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        loginTextView.text = spannable
        loginTextView.movementMethod = LinkMovementMethod.getInstance()
        loginTextView.highlightColor = Color.TRANSPARENT
    }

    private fun setupRegisterButton() {
        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            var valid = true

            if (name.isEmpty()) {
                nameInputLayout.error = "Name is required"
                valid = false
            } else {
                nameInputLayout.error = null
            }

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
                val db = FirebaseFirestore.getInstance()
                val rootView = findViewById<View>(android.R.id.content)

                // Check if email already exists
                db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            Snackbar.make(rootView, "Email already registered", Snackbar.LENGTH_LONG).show()
                        } else {
                            // Save new user
                            val user = hashMapOf(
                                "name" to name,
                                "email" to email,
                                "password" to password
                            )

                            db.collection("users")
                                .add(user)
                                .addOnSuccessListener {
                                    Snackbar.make(rootView, "Registration successful. Please login.", Snackbar.LENGTH_LONG).show()
                                    // Redirect to login page
                                    startActivity(Intent(this, login::class.java))
                                    finish()
                                }
                                .addOnFailureListener { exception ->
                                    Snackbar.make(rootView, "Error: ${exception.message}", Snackbar.LENGTH_LONG).show()
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Snackbar.make(rootView, "Error: ${exception.message}", Snackbar.LENGTH_LONG).show()
                    }
            }
        }
    }
}
