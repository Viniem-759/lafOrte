package com.example.laforte20

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.widget.ShareDialog
import java.io.File
import java.io.FileOutputStream

class DashboardActivity : AppCompatActivity() {

    private lateinit var callbackManager: CallbackManager
    private lateinit var shareDialog: ShareDialog
    private val ATTACH_REQUEST_CODE = 101
    private var selectedUri: Uri? = null

    private fun requestRequiredPermissions() {
        val permissions = listOf(
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO
        )
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 200)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this, "Permission needed to access media.", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveMediaToCache(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val fileName = "temp_${System.currentTimeMillis()}"
        val file = File(cacheDir, fileName)
        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        Toast.makeText(this, "Stored in cache: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestRequiredPermissions()

        callbackManager = CallbackManager.Factory.create()
        shareDialog = ShareDialog(this)

        findViewById<ImageView>(R.id.back_arrow).setOnClickListener {
            startActivity(Intent(this, loginActivity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.attach_icon).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            }
            startActivityForResult(intent, ATTACH_REQUEST_CODE)
        }

        val postInput = findViewById<EditText>(R.id.post_input)
        findViewById<Button>(R.id.post_button).setOnClickListener {
            val messageText = postInput.text.toString().trim()
            if (messageText.isEmpty()) {
                Toast.makeText(this, "Please enter a message to post.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val content = ShareLinkContent.Builder()
                .setContentUrl(Uri.parse("https://laforte.example.com")) // Optional: attach user media link
                .setQuote(messageText)
                .build()

            if (ShareDialog.canShow(ShareLinkContent::class.java)) {
                shareDialog.show(content)
            } else {
                Toast.makeText(this, "Facebook ShareDialog cannot be shown.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ATTACH_REQUEST_CODE && resultCode == RESULT_OK) {
            selectedUri = data?.data
            selectedUri?.let { uri ->
                Toast.makeText(this, "Media selected!", Toast.LENGTH_SHORT).show()
                saveMediaToCache(uri)
            }
        }
    }
}