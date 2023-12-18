package com.example.znajdzlek

//to json

///

import android.content.Intent
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.znajdzlek.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {


    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    lateinit var binding: ActivityMainBinding

    private val REQUEST_IMAGE_CAPTURE = 1

    private var imageBitmap: Bitmap? = null
    private val apiDataFetcher: APIDataFetcher = APIDataFetcher()

    private lateinit var descriptionView: TextView
    @Volatile
    private lateinit var largestText: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        descriptionView = findViewById(R.id.description_view)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.apply {

            captureImage.setOnClickListener {

                takeImage()

                textView.text = ""


            }

            detectTextImageBtn.setOnClickListener {
                lifecycleScope.launch {
                    try {
                        val result = apiDataFetcher.getMedicationData(largestText)
                        handleResult(result)
                    } catch (e: Exception) {
                        println(">>>>Execution exception")
                        // Handle exceptions here
                    }
                }
                processImage()


            }

            search.setOnClickListener {
                thread {
                    handleResult(apiDataFetcher.getMedicationData(largestText))
                }

                //PostRequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }


        }


    }

    private fun handleResult(result: Result<MedicationResponse>) {
        when (result) {
            is Result.Success -> {
                val medicationData = result.data.result
                // Update UI with medicationData
                runOnUiThread {
                    showToast(medicationData)
                }

            }

            is Result.Error -> {
                val exception = result.exception
                runOnUiThread {
                    showToast("Error: ${exception.message}")
                }
                // Handle error

            }
        }
    }

    private fun showToast(message: String) {
        // Use application context to ensure the Toast is displayed even if the activity is destroyed
        // For example, when the user rotates the device.
        // Replace 'applicationContext' with 'this' if you want to tie the Toast to the activity.
        // 'LENGTH_SHORT' can be changed to 'LENGTH_LONG' if you want a longer duration.
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()

        println(">>>>Message: $message")

        binding.descriptionView.text = message
    }


    private fun takeImage() {

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        try {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        } catch (_: Exception) {
            Toast.makeText(this, "Wystąpił problem przy robieniu zdjęcia", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            val extras: Bundle? = data?.extras

            if (extras != null && extras.containsKey("data")) {
                imageBitmap = extras.get("data") as Bitmap

                if (imageBitmap != null) {
                    binding.imageView.setImageBitmap(imageBitmap)
                }
            }
        }
    }


    private fun processImage() {
        if (imageBitmap != null) {
            val image = imageBitmap?.let {
                InputImage.fromBitmap(it, 0)
            }

            image?.let {
                recognizer.process(it).addOnSuccessListener { visionText ->
                    largestText = findLargestLetters(visionText)
                    if (largestText.isNotEmpty()) {
//                            saveTextToJsonFile(largestText)
                        binding.textView.text = largestText
//                            val description = fetchDescriptionForMedication(largestText)
//                            binding.descriptionView.text = description
                    } else {
                        Toast.makeText(this, "Nie znaleziono tekstu", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Rozpoznawanie tekstu nie powiodło się: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(this, "Proszę wybrać zdjęcie", Toast.LENGTH_SHORT).show()
        }
    }


    private fun findLargestLetters(visionText: Text?): String {
        var largestText = ""
        var largestArea = 0

        visionText?.let {
            for (block in it.textBlocks) {
                for (line in block.lines) {
                    for (element in line.elements) {
                        val boundingBox = element.boundingBox

                        boundingBox?.let {
                            val area = it.width() * it.height()

                            if (area > largestArea) {
                                largestText = element.text
                                largestArea = area
                            }
                        }
                    }
                }
            }
        }

        return largestText


    }


    inner class PostRequestTask : AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg params: Void): String? {
            try {
                // Construct the JSON object
                val jsonParam = JSONObject()
                jsonParam.put("medication", largestText)

                // Specify the API endpoint URL (replace with your actual endpoint)
                val url = URL("http://185.47.65.159:5000/api/medication")


                // Open connection
                val urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.setRequestProperty("Content-Type", "application/json")
                urlConnection.setRequestProperty("Accept", "application/json")
                urlConnection.doOutput = true
                urlConnection.connectTimeout = 5000 // 5 sekundy timeout
                urlConnection.readTimeout = 10000 // 10 sekund timeout odczytu


                // Write data to the connection
                urlConnection.outputStream.use { os ->
                    val input = jsonParam.toString().toByteArray(StandardCharsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                // Get the response from the server

                val responseCode = urlConnection.responseCode
                println("Odpowiedź z serwera: $responseCode")
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read and return the response
                    return urlConnection.inputStream.bufferedReader().readText()

                } else {
                    println("Błąd HTTP: $responseCode")
                    return null


                }
            } catch (e: IOException) {
                e.printStackTrace()
                println("IOException: ${e.message}")
                return null
            } catch (e: JSONException) {
                e.printStackTrace()
                println("JSONException: ${e.message}")
                return null
            }

        }

        override fun onPostExecute(wynik: String?) {
            // Handle the response here and update the UI

            if (wynik != null) {
                try {
                    // Parse the JSON response
                    val jsonResponse = JSONObject(wynik.trim())
                    val medication = jsonResponse.getString("medication")
                    // val description = jsonResponse.getString("description")
                    val result = jsonResponse.getString("result")

                    //Display the result in the TextView
                    descriptionView.text = "Medication: $medication\n Description: $result"

                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@MainActivity,
                        "Błąd parsowania odpowiedzi JSON ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    this@MainActivity, "Pusta odpowiedź z serwera", Toast.LENGTH_SHORT
                ).show()
            }

        }

    }

}