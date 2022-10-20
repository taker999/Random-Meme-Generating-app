package com.example.memesharing

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
//import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.internal.NoOpContinuation.context
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.URL
//import kotlin.coroutines.jvm.internal.CompletedContinuation.context

//import kotlin.coroutines.jvm.internal.CompletedContinuation.context

class MainActivity : AppCompatActivity() {
//    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadMeme()
    }

    private var currentMemeUrl: String? = null
    private var i=0

    private var prev= listOf<String>()

    private fun loadMeme() {
        nextButton.isEnabled = false
        shareButton.isEnabled = false
        progressBar.visibility = View.VISIBLE
        val url = "https://meme-api.herokuapp.com/gimme"

        // Request a string response from the provided URL.
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                currentMemeUrl = response.getString("url")

                Glide.with(this).load(currentMemeUrl).listener(object : RequestListener<Drawable>{
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        nextButton.isEnabled = true
                        shareButton.isEnabled = true
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        return false
                    }
                }).into(memeImageView)
            },
            Response.ErrorListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
            })

        // Add the request to the RequestQueue.
        prev= listOf("$currentMemeUrl")+prev
        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }

    fun showNextMeme(view: View) {
        i=0
        loadMeme()
    }

    fun shareMeme(view: View) {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_TEXT, "Hey, checkout this meme: $currentMemeUrl")
        startActivity(Intent.createChooser(i, "Share this meme with..."))
    }

    private fun galleryAddPic(imagePath: String?) {
        imagePath?.let { path ->
//            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val f = File(path)
            MediaScannerConnection.scanFile(this, arrayOf(f.toString()),
                null, null)
//            val contentUri: Uri = Uri.fromFile(f)
//            mediaScanIntent.data = contentUri
//            sendBroadcast(mediaScanIntent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveImage(image: Bitmap): String? {
        var savedImagePath: String? = null
        val startTime: Long = System.currentTimeMillis()

        val imageFileName: String = if((currentMemeUrl?.last()) == 'g'){
            "$startTime.jpg"
        }   else{
            "$startTime.gif"
        }
        val storageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + "/YOUR_FOLDER_NAME"
        )
        var success = true
        if (!storageDir.exists()) {
            success = storageDir.mkdirs()
        }

        if (success) {
            val imageFile = File(storageDir, imageFileName)

            @SuppressLint("ResourceType")
            val inputStream: InputStream? =
                URL("$currentMemeUrl").openStream()
            val bufferedInputStream = BufferedInputStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()

            var current = 0
            savedImagePath = imageFile.absolutePath
            try {
                do {
                    current = bufferedInputStream.read()
                    byteArrayOutputStream.write(current)
                    if(current == -1){
                        break
                    }
                } while(true)
                val fOut: OutputStream = FileOutputStream(imageFile)
                fOut.write(byteArrayOutputStream.toByteArray())
                fOut.flush()
//                image.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                fOut.close()
                inputStream!!.close()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "$current", Toast.LENGTH_LONG).show()
            }

            // Add the image to the system gallery
            galleryAddPic(savedImagePath)
//            Toast.makeText(this, "IMAGE SAVED", Toast.LENGTH_LONG).show() // to make this working, need to manage coroutine, as this execution is something off the main thread
        }
        return savedImagePath
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun downloadMeme(view: View) {
        Toast.makeText(this, "SAVED", Toast.LENGTH_LONG).show()
        CoroutineScope(Dispatchers.IO).launch {
            saveImage(withContext(Dispatchers.IO) {
                Glide.with(this@MainActivity)
                    .asBitmap()
                    .load(currentMemeUrl) // sample image
                    .placeholder(android.R.drawable.progress_indeterminate_horizontal) // need placeholder to avoid issue like glide annotations
                    .error(android.R.drawable.stat_notify_error) // need error to avoid issue like glide annotations
                    .submit()
                    .get()
            })
        }
    }


    fun prevMeme(view: View) {
        progressBar.visibility = View.VISIBLE

        if(i==prev.size-1){
            progressBar.visibility = View.GONE
            Toast.makeText(this, "No More Memes", Toast.LENGTH_LONG).show()
        }
        else{
            currentMemeUrl=prev[i]
            Glide.with(this)
                .load(prev[i]).listener(object : RequestListener<Drawable>{
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        nextButton.isEnabled = true
                        shareButton.isEnabled = true
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        return false
                    }
                }) // image url
                .into(memeImageView)  // imageview object
            i++
        }

    }
}