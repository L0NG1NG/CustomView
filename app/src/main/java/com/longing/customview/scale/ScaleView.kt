package com.longing.customview.scale

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.longing.customview.dp
import com.longing.customview.sp
import kotlin.math.log

private const val TAG = "ScaleView"
private const val SCALE_COUNTS = 10

class ScaleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textPaint = Paint().apply {
        isAntiAlias = true
        val size = 14.sp
        textSize = size
        color = Color.BLUE
    }

    private val textHeight: Float by lazy {
        val fm = textPaint.fontMetrics
        fm.descent - fm.ascent
    }
    private val textBgRadius get() = textHeight + 8.dp

    private val scalePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        strokeWidth = 4f
        color = Color.GRAY
    }


    private val defaultScaleSpacing = 12.dp
    private val scaleLength = 24.dp
    private val scaleThickness = 2.dp

    private var textScaleSize = 0f

    private var hasScale = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val height = defaultScaleSpacing * SCALE_COUNTS + textBgRadius
        textScaleSize = defaultScaleSpacing * 5f

        val width = if (hasScale) scaleLength + textBgRadius else textBgRadius
        Log.d(TAG, "onMeasure:$height,$width,$defaultScaleSpacing")

        setMeasuredDimension(width.toInt() + 40, height.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var y = textHeight / 2
        repeat(3) {
            canvas.drawText(it.toString(), 0f, y, textPaint)
            y += textScaleSize
        }

        var startY = scaleThickness.toFloat()
        val with = measuredWidth.toFloat()
        if (hasScale) {
            repeat(SCALE_COUNTS) {
                canvas.drawLine(with - scaleLength, startY, with, startY, scalePaint)
                startY += defaultScaleSpacing + scaleThickness
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                hasScale = true
                requestLayout()
            }

            MotionEvent.ACTION_UP -> {
                postDelayed({
                    hasScale = false
                    requestLayout()
                }, 500)
            }
        }

        return true
    }
}