package com.longing.customview.scale

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import com.longing.customview.R
import com.longing.customview.dp
import kotlin.math.abs
import java.math.RoundingMode
import java.text.NumberFormat
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private const val TAG = "ScaleView"
private const val SCALE_COUNTS = 11


class ScaleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var pressDownTime = 0L
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    //刻度值圆圆背景半径
    private val valueBgRadius = 16.dp

    //刻度值圆环厚度
    private val outlineWith = 1.dp

    //刻度值间距
    private val valueSpacing = 54.dp

    //刻度值背景色
    private val valueBackground = ContextCompat.getColor(context, R.color.scaling_value_bg)

    //刻度值集合
    private val scaleValues = arrayOf(ScaleValue(10f, false), ScaleValue(2f, false), ScaleValue(1f, false))
    private val dynamicScaleValues =
        arrayOf(ScaleValue(10f, false), ScaleValue(2f, false), ScaleValue(1f, true))

    //刻度值坐标y点
    private val scaleValuesCenterY = FloatArray(scaleValues.size)

    //当前刻度
    var selectedValue = 1f

    //刻度值和刻度的间隔
    private val columnSpacing = 18.dp

    //是否展示刻度
    private var hasScale = false

    //刻度颜色
    private val scaleColorSecondary = ContextCompat.getColor(context, R.color.scale_color)
    private val scaleColorMain = Color.WHITE

    //刻度长度
    private val scaleLengthLong = 24.dp
    private val scaleLengthShort = 15.dp

    //游标
    private var cursorColor = Color.GREEN
    private val cursorLength = scaleLengthLong
    private val scaleThickness = 2.dp
    private val cursorMoveRange: Pair<Float, Float> by lazy(LazyThreadSafetyMode.NONE) {
        scaleValuesCenterY.first() to scaleValuesCenterY.last()
    }
    private var cursorOffset = 0f
    private var lastCursorOffset = 0f

    private var lastPointY = 0f

    private val textBounds = Rect()
    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 12.dp
        color = Color.WHITE
    }

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        strokeWidth = scaleThickness
    }

    private val resetRunnable = Runnable {
        hasScale = false
        requestLayout()
    }

    private val nf = NumberFormat.getNumberInstance().apply {
        roundingMode = RoundingMode.HALF_UP
        isGroupingUsed = false
        maximumFractionDigits = 1
    }

    //外部监听事件
    var cursorMoveListener: CursorMoveListener? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val paddingVertical = paddingTop + paddingBottom
        val height = 2 * (valueBgRadius + valueSpacing + outlineWith) + paddingVertical

        val paddingHorizontal = paddingStart + paddingEnd
        var width = 2 * (valueBgRadius + outlineWith) + paddingHorizontal

        if (hasScale) {
            width += scaleLengthLong + columnSpacing
        }
        Log.d(TAG, "onMeasure:$height,$width")

        //设置倍率坐标中心点
        if (scaleValuesCenterY.all { it == 0f }) {
            val cy = valueBgRadius + paddingTop + outlineWith
            for (i in scaleValues.indices) {
                scaleValuesCenterY[i] = cy + valueSpacing * i
            }
            //默认指针在最低处
            lastCursorOffset = scaleValuesCenterY.last()
        }

        setMeasuredDimension(width.roundToInt(), height.roundToInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (hasScale) {
            drawScaleValue(canvas, scaleValues)
            val spacing = (scaleValuesCenterY.last() - scaleValuesCenterY.first()) / (SCALE_COUNTS - 1)
            var startY = scaleValuesCenterY.first()
            val with = measuredWidth.toFloat()
            repeat(SCALE_COUNTS) {
                val scaleLength = if (it % 5 == 0) {
                    paint.color = scaleColorMain
                    scaleLengthLong
                } else {
                    paint.color = scaleColorSecondary
                    scaleLengthShort
                }
                canvas.drawLine(with - scaleLength, startY, with, startY, paint)
                startY += spacing
            }
            paint.color = cursorColor
            canvas.drawLine(
                with - cursorLength, cursorOffset, with, cursorOffset, paint
            )
        } else {
            drawScaleValue(canvas, dynamicScaleValues)
        }
    }

    private fun drawScaleValue(canvas: Canvas, scaleValues: Array<ScaleValue>) {
        val start = valueBgRadius + paddingStart + outlineWith
        scaleValues.onEachIndexed { index, scale ->
            val cy = scaleValuesCenterY[index]
            //画背景圆
            paint.color = valueBackground
            canvas.drawCircle(start, cy, valueBgRadius, paint)
            //画缩放倍率

            val text = "${nf.format(scale.value)}x"
            textPaint.getTextBounds(text, 0, text.length, textBounds)
            val tx = valueBgRadius - textBounds.width() / 2
            val ty = cy + textBounds.height() / 2
            canvas.drawText(text, tx, ty, textPaint)
            //选中圆环
            if (scaleValues[index].isShot) {
                val paintStyle = textPaint.style
                textPaint.style = Paint.Style.STROKE
                textPaint.strokeWidth = outlineWith
                canvas.drawCircle(start, cy, valueBgRadius + outlineWith, textPaint)
                textPaint.style = paintStyle
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointY = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastPointY = pointY
                if (hasScale) {
                    removeCallbacks(resetRunnable)
                }
                pressDownTime = SystemClock.elapsedRealtime()
            }

            MotionEvent.ACTION_MOVE -> {
                val offset = pointY - lastPointY
                if (!hasScale && offset.absoluteValue >= touchSlop) {
                    hasScale = true
                    lastPointY = pointY
                    requestLayout()
                } else if (hasScale) {
                    val offsetY = pointY - lastPointY
                    updateCursorOffset(offsetY + lastCursorOffset)
                    invalidate()
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                lastCursorOffset = cursorOffset
                if (hasScale) {
                    postDelayed(resetRunnable, 2000)
                }
                if (event.action == MotionEvent.ACTION_UP) {
                    if (abs(pointY - lastPointY) < touchSlop
                        && SystemClock.elapsedRealtime() - pressDownTime < 500

                    ) {
                        //点击了
                        for (index in scaleValuesCenterY.indices) {
                            val cy = scaleValuesCenterY[index]
                            if (abs(pointY - cy) <= valueBgRadius) {
                                //点击了刻度值
                                if (dynamicScaleValues[index].isShot) {
                                    if (!hasScale) {
                                        hasScale = true
                                        requestLayout()
                                        postDelayed(resetRunnable, 2000)
                                        break
                                    }
                                } else {
                                    lastCursorOffset = cy
                                    updateCursorOffset(lastCursorOffset)
                                    invalidate()
                                }
                                break
                            }
                        }
                    }
                }
            }
        }
        return true
    }

    private fun updateCursorOffset(offset: Float) {
        cursorOffset = offset

        if (cursorOffset < cursorMoveRange.first) {
            cursorOffset = cursorMoveRange.first
        } else if (cursorOffset > cursorMoveRange.second) {
            cursorOffset = cursorMoveRange.second
        }

        var progress = (cursorOffset - cursorMoveRange.first) /
                (cursorMoveRange.second - cursorMoveRange.first)
        //修正下保留一位小数,使得判断更容易选中
        progress = nf.format(progress).toFloat()
        onProgressChange(progress)
    }

    private fun onProgressChange(progress: Float) {
        //更新选中的刻度数值
        for (i in scaleValues.indices) {
            scaleValues[i].isShot = i * 0.5f == progress
        }
        //计算当前的倍率
        val scale = if (progress >= 0.5) {
            scaleValues[2].value + (scaleValues[1].value - scaleValues[2].value) * (1 - (progress - 0.5) / 0.5)
        } else {
            scaleValues[1].value + (scaleValues[0].value - scaleValues[1].value) * (1 - progress / 0.5)
        }
        selectedValue = nf.format(scale).toFloat()

        val scaleLevel = when {
            selectedValue < scaleValues[1].value -> {
                2
            }

            selectedValue >= scaleValues[0].value * 0.6 -> {
                0
            }

            else -> {
                1
            }
        }
        Log.i(TAG, "onProgressChange: selectedValue-->$selectedValue,level->$scaleLevel")

        for (index in scaleValues.indices) {
            dynamicScaleValues[index].apply {
                if (scaleLevel == index) {
                    value = selectedValue
                    isShot = true
                } else {
                    value = scaleValues[index].value
                    isShot = false
                }
            }
        }

        //通知到外部
        cursorMoveListener?.onCursorMove(progress, selectedValue)
    }
}