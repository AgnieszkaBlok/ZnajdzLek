package com.example.znajdzlek

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.example.znajdzlek.databinding.ActivityMainBinding
import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
//to json
import org.json.JSONObject
import java.io.File
import java.io.FileWriter


class MainActivity : AppCompatActivity() {

    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    lateinit var binding: ActivityMainBinding

    private val REQUEST_IMAGE_CAPTURE=1

    private var imageBitmap: Bitmap? =null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding= DataBindingUtil.setContentView(this,R.layout.activity_main)

        binding.apply {

            captureImage.setOnClickListener {

                takeImage()

                textView.text = ""


            }

            detectTextImageBtn.setOnClickListener {

                processImage()

            }

            search.setOnClickListener {

                processImage()

            }

        }


    }



    private fun takeImage(){

        val intent= Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        try {

            startActivityForResult(intent,REQUEST_IMAGE_CAPTURE)

        }
        catch (e:Exception){



        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode==REQUEST_IMAGE_CAPTURE && resultCode== RESULT_OK){

            val extras: Bundle? = data?.extras

            imageBitmap= extras?.get("data") as Bitmap

            if (imageBitmap!=null) {

                binding.imageView.setImageBitmap(imageBitmap)

            }



        }


    }




private fun processImage() {

    if (imageBitmap != null) {



        val image = imageBitmap?.let {

            InputImage.fromBitmap(it, 0)

        }

        image?.let {
            recognizer.process(it)
                .addOnSuccessListener { visionText ->

                    // Identify the largest letters
                    val largestText = findLargestLetters(visionText)

                    // Save the largest text to JSON file
                    saveTextToJsonFile(largestText)

                    // Display the largest letters
                    binding.textView.text = largestText


                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Text recognition failed", Toast.LENGTH_SHORT).show()
                }
        }


    } else {

        Toast.makeText(this, "Please select a photo", Toast.LENGTH_SHORT).show()

    }
}

    private fun findLargestLetters(visionText: Text): String {
        var largestText = ""
        var largestArea = 0

        for (block in visionText.textBlocks) {
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

        return largestText
    }

    private fun saveTextToJsonFile(text: String) {
        try {
            // Tworzymy obiekt JSON
            val json = JSONObject()
            json.put("medication", text)

            // Pobieramy ścieżkę do katalogu aplikacji
            val directory = this.filesDir

            // Tworzymy plik JSON w katalogu aplikacji
            val file = File(directory, "output.json")

            // Zapisujemy obiekt JSON do pliku
            val fileWriter = FileWriter(file)
            fileWriter.use {
                it.write(json.toString())
            }

            // Powiadomienie użytkownika o zapisaniu pliku
            Toast.makeText(this, "Text saved to JSON file", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // Obsługa błędów
            Toast.makeText(this, "Error saving text to JSON file", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

}