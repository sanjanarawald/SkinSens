package com.example.skinapiapp
import androidx.activity.compose.rememberLauncherForActivityResult
import android.app.Activity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toFile
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.File

class MainActivity : ComponentActivity() {
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var imageUri by remember { mutableStateOf<Uri?>(null) }
            var resultText by remember { mutableStateOf("") }

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                imageUri = uri
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(onClick = { launcher.launch("image/*") }) {
                        Text("Select Image")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    imageUri?.let {
                        Image(
                            painter = rememberAsyncImagePainter(it),
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                val prediction = uploadImageToServer(it)
                                resultText = prediction ?: "Failed to get prediction"
                            }
                        }) {
                            Text("Send to Server")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(resultText)
                    }
                }
            }
        }
    }

    private fun uploadImageToServer(uri: Uri): String? {
        val file = File(getPathFromUri(uri) ?: return null)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", file.name,
                RequestBody.create("image/*".toMediaTypeOrNull(), file)
            )
            .build()

        val request = Request.Builder()
            .url("http://10.0.2.2:5000/predict") // 10.0.2.2 = localhost for emulator
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    "Error: ${response.code}"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Exception: ${e.message}"
        }
    }

    private fun getPathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        return cursor?.use {
            val idx = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            it.moveToFirst()
            it.getString(idx)
        }
    }
}
