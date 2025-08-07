package com.example.laforte20

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.EditText
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        callbackManager = CallbackManager.Factory.create()

        // 🔗 Facebook callback setup
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    Toast.makeText(this@MainActivity, "Facebook Authenticated!", Toast.LENGTH_SHORT).show()

                    val accessToken = result.accessToken
                    val request = GraphRequest.newMeRequest(accessToken) { obj, _ ->
                        val name = obj?.optString("name") ?: ""
                        val email = obj?.optString("email") ?: ""
                        val id = obj?.optString("id") ?: ""
                        val fbData = mapOf("fb_name" to name, "fb_email" to email, "fb_id" to id)

                        val userId = auth.currentUser?.uid
                        userId?.let {
                            val dbRef = FirebaseDatabase.getInstance().reference
                            dbRef.child("users").child(it).updateChildren(fbData)
                        }
                    }

                    request.parameters = Bundle().apply {
                        putString("fields", "id,name,email")
                    }
                    request.executeAsync()
                }

                override fun onCancel() {
                    Toast.makeText(this@MainActivity, "Facebook login canceled", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: FacebookException) {
                    Toast.makeText(this@MainActivity, "Facebook login error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })

        // 🔘 Facebook auth trigger button
        val fbAuthBtn = findViewById<Button>(R.id.Fb_Auth_Btn)
        fbAuthBtn.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this,
                listOf("email", "public_profile", "pages_show_list") // Supported permission added
            )
        }

        // 🔄 Navigation to login screen
        val go_login_page = findViewById<TextView>(R.id.go_login_screen)
        go_login_page.setOnClickListener {
            startActivity(Intent(this, loginActivity::class.java))
            finish()
        }

        // 🔑 Firebase Auth setup
        auth = FirebaseAuth.getInstance()

        val emailField = findViewById<EditText>(R.id.email_input)
        val passwordField = findViewById<EditText>(R.id.password_input)
        val confirmField = findViewById<EditText>(R.id.confirm_password_input)
        val continueBtn = findViewById<Button>(R.id.continue_btn)

        // 📝 Laforte user registration
        continueBtn.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val confirmPassword = confirmField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            val database = FirebaseDatabase.getInstance().reference
                            userId?.let {
                                val userMap = mapOf("email" to email)
                                database.child("users").child(it).setValue(userMap)
                            }
                            Toast.makeText(this, "Account created and saved!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, loginActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        // ⬅️ Back navigation from toolbar
        val backArrow = findViewById<ImageView>(R.id.back_arrow2)
        backArrow.setOnClickListener {
            startActivity(Intent(this, loginActivity::class.java))
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}