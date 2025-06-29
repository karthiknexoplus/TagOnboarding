package com.nexoplus.tagonboarding

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.github.chrisbanes.photoview.PhotoView
import android.graphics.BitmapFactory

class ImagePreviewActivity : AppCompatActivity() {
    private lateinit var photoView: PhotoView
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private lateinit var plateText: android.widget.TextView
    private lateinit var timeText: android.widget.TextView
    private var imagePaths: List<String> = emptyList()
    private var plates: List<String> = emptyList()
    private var times: List<String> = emptyList()
    private var currentIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        photoView = findViewById(R.id.photoView)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)
        plateText = findViewById(R.id.plateText)
        timeText = findViewById(R.id.timeText)

        imagePaths = intent.getStringArrayListExtra("imagePaths") ?: emptyList()
        plates = intent.getStringArrayListExtra("plates") ?: emptyList()
        times = intent.getStringArrayListExtra("times") ?: emptyList()
        currentIndex = intent.getIntExtra("currentIndex", 0)

        showImage()

        prevButton.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                showImage()
            }
        }
        nextButton.setOnClickListener {
            if (currentIndex < imagePaths.size - 1) {
                currentIndex++
                showImage()
            }
        }
    }

    private fun showImage() {
        if (imagePaths.isNotEmpty() && currentIndex in imagePaths.indices) {
            val path = imagePaths[currentIndex]
            val bmp = BitmapFactory.decodeFile(path)
            photoView.setImageBitmap(bmp)
        } else {
            photoView.setImageResource(android.R.drawable.ic_menu_report_image)
        }
        plateText.text = if (plates.isNotEmpty() && currentIndex in plates.indices) plates[currentIndex] else ""
        timeText.text = if (times.isNotEmpty() && currentIndex in times.indices) times[currentIndex] else ""
        prevButton.isEnabled = currentIndex > 0
        nextButton.isEnabled = currentIndex < imagePaths.size - 1
    }
} 