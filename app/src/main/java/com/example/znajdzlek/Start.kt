package com.example.znajdzlek

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity



class Start : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val btnNavigate = findViewById<Button>(R.id.btnStart)


        btnNavigate.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)

            startActivity(intent)
        }



    }
}