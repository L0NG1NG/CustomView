package com.longing.customview

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.longing.customview.scale.CursorMoveListener
import com.longing.customview.scale.ScaleView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val textView = findViewById<TextView>(R.id.scale_tv)
        val scaleView = findViewById<ScaleView>(R.id.scale_view)
        scaleView.cursorMoveListener = object : CursorMoveListener {
            override fun onCursorMove(progress: Float, scale: Float) {
                textView.text = "$scale"

            }

            override fun onCursorMoveEnd(progress: Float, scale: Float) {

            }

        }
        scaleView.setOnClickListener {
            Toast.makeText(this, "ScaleView Clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.add_button).setOnClickListener {
            scaleView.setScaleValue(scaleView.selectedValue + 0.1f)

        }

        findViewById<Button>(R.id.minus_button).setOnClickListener {
            scaleView.setScaleValue(scaleView.selectedValue - 0.1f)
        }
    }
}