package com.example.znajdzlek

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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
    private var largestText: String = ""

//
      private lateinit var url : String
      private fun sendToast(message: String){
          Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
      }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        descriptionView = findViewById(R.id.description_view)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.apply {

            captureImage.setOnClickListener {
                takeImage()
                descriptionView.text = "To check information about your medicine \n 1. Click CAPTURE button to take a photo. \n 2. After uploading the photo, click the DETECT button to read the name of your medication from the photo.\n 3. If the name is correct, click the SEARCH button to access medication information. If not, take another photo.\n 4.For more detailed information about the medicine or if you can't find yours, you can click on the laptop search button. \n"
                textView.text = "Your medication"
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
                if(largestText == ""){
                    sendToast("Please perform detection first")
                }
                else{
                    thread {
                        handleResult(apiDataFetcher.getMedicationData(largestText))
                    }
                }
            }
            findMore.setOnClickListener{
                if(largestText == "") {
                    sendToast("Please perform detection first")
                }else{
                    url = "https://ktomalek.pl/l/lek/szukaj?searchInput=$largestText"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
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
                    println("Please perform detection first")
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
            Toast.makeText(this, "The problem occurred while taking a photo.", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this, "Text not found", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Text recognition failed. ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
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