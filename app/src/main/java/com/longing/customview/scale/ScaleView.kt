package com.longing.customview.scale

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.longing.customview.R
import com.longing.customview.dp
import kotlin.math.roundToInt

private const val TAG = "ScaleView"
private const val SCALE_COUNTS = 11

class ScaleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textBounds = Rect()
    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 12.dp
        color = Color.WHITE
    }


    private val scaleThickness = 2.dp
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        strokeWidth = scaleThickness
    }
    private val scalingValueBgColor = ContextCompat.getColor(context, R.color.scaling_value_bg)

    private val scaleColor = ContextCompat.getColor(context, R.color.scale_color)
    private val scaleLengthLong = 24.dp
    private val scaleLengthShort = 15.dp


    private var hasScale = false

    //数值和刻度的间隔
    private val columnSpacing = 18.dp

    //缩放倍数的圆圈大小
    private val scalingValueBgSize = 32.dp

    //缩放倍数圆圈的间隔
    private val scalingValueBgSpacing = 38.dp

    //游标
    private var cursorColor = Color.GREEN
    private val cursorLength = scaleLengthLong

    private val resetRunnable = Runnable {
        hasScale = false
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val height = 2 * (scalingValueBgSize + scalingValueBgSpacing)

        var width = if (hasScale) scaleLengthLong + scalingValueBgSize + columnSpacing else scalingValueBgSize
        width += paddingStart + paddingEnd
        Log.d(TAG, "onMeasure:$height,$width")

        setMeasuredDimension(width.roundToInt(), height.roundToInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = scalingValueBgSize / 2
        var cy = radius
        val start = radius + paddingStart
        listOf("10", "2", "1").onEach {
            //画背景圆
            paint.color = scalingValueBgColor
            canvas.drawCircle(start, cy, scalingValueBgSize / 2, paint)
            //画缩放倍率
            val text = it + "x"
            textPaint.getTextBounds(text, 0, text.length, textBounds)
            val tx = radius - textBounds.width() / 2
            val ty = cy + textBounds.height() / 2
            canvas.drawText(text, tx, ty, textPaint)

            cy += radius + scalingValueBgSpacing
        }

        if (hasScale) {
            val spacing = (measuredHeight - scaleThickness * SCALE_COUNTS) / (SCALE_COUNTS - 1)
            var startY = scaleThickness / 2
            val with = measuredWidth.toFloat()
            repeat(SCALE_COUNTS) {
                val scaleLength = if (it % 5 == 0) {
                    paint.color = Color.WHITE
                    scaleLengthLong
                } else {
                    paint.color = scaleColor
                    scaleLengthShort
                }
                canvas.drawLine(with - scaleLength, startY, with, startY, paint)
                startY += spacing + scaleThickness
            }
            paint.color = cursorColor
            canvas.drawLine(with - cursorLength, moveOffset, with, moveOffset, paint)
        }
    }

    private var moveOffset = 0f
    private var lastMoveOffset = 0f
    private var lastPointY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointY = event.x
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!hasScale) {
                    hasScale = true
                    requestLayout()
                } else {
                    removeCallbacks(resetRunnable)
                }
                lastPointY = pointY
            }

            MotionEvent.ACTION_MOVE -> {
                moveOffset = (pointY - lastPointY) * 3.5f + lastMoveOffset
                if (moveOffset < 0) moveOffset = 0f
                else if (moveOffset > measuredHeight) moveOffset = measuredHeight.toFloat()
                invalidate()
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                lastMoveOffset = moveOffset
                postDelayed(resetRunnable, 2000)
            }
        }
        return true
    }

}