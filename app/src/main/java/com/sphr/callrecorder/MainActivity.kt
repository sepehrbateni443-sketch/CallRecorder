package com.sphr.callrecorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE
    )
    private val permissionRequestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.statusText)

        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), permissionRequestCode)
            statusText.text = "در حال درخواست دسترسی‌ها..."
        } else {
            showStatus(statusText)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val statusText = findViewById<TextView>(R.id.statusText)
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            showStatus(statusText)
        } else {
            statusText.text = "بدون این دسترسی‌ها اپ نمی‌تونه تماس‌ها رو ضبط کنه."
        }
    }

    private fun showStatus(statusText: TextView) {
        val dir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "CallRecordings")
        statusText.text = "آماده‌ست. تماس بگیر یا جواب بده — ضبط خودکار شروع می‌شه.\n\n" +
                "فایل‌ها اینجا ذخیره می‌شن:\n${dir.absolutePath}"
    }
}
