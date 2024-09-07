package com.longing.customview

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.longing.customview.scale.CursorMoveListener
import com.longing.customview.scale.ScaleValue
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
            override fun onCursorMove(progress: Float) {
                textView.text = "${progress}%"
                if (0.48f < progress && progress < 0.52f) {
                    scaleView.selectedValue = ScaleValue.Middle
                } else if (progress < 0.02f) {
                    scaleView.selectedValue = ScaleValue.Low
                } else if (progress > 0.98f) {
                    scaleView.selectedValue = ScaleValue.High
                }

            }

            override fun onCursorMoveEnd(progress: Float) {

            }

        }
    }
}