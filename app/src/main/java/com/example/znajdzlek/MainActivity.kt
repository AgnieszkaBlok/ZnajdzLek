package com.example.znajdzlek

import android.content.Intent
import android.graphics.Bitmap
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
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {


    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private lateinit var binding: ActivityMainBinding

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
                    }
                }
                processImage()
            }

            search.setOnClickListener {
                thread {
                    handleResult(apiDataFetcher.getMedicationData(largestText))
                }
            }
        }
    }

    private fun handleResult(result: Result<MedicationResponse>) {
        when (result) {
            is Result.Success -> {
                val medicationData = result.data.result
                // Update UI with medicationData
                runOnUiThread {
                    showDescriptionOnUI(medicationData)
                }

            }

            is Result.Error -> {
                val exception = result.exception
                runOnUiThread {
                    showDescriptionOnUI("Error: ${exception.message}")
                }
                // Handle error

            }
        }
    }

    private fun showDescriptionOnUI(message: String) {
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
                        binding.textView.text = largestText
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
}