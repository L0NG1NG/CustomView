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
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import com.longing.customview.R
import com.longing.customview.dp
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private const val TAG = "ScaleView"
private const val SCALE_COUNTS = 11


class ScaleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    //倍数背景半径
    private val ratioBgRadius = 16.dp

    //倍数间距
    private val ratiosSpacing = 54.dp

    //倍数背景色
    private val ratioBackgroundColor = ContextCompat.getColor(context, R.color.scaling_value_bg)

    //当前选中的倍数
    private var selectedRatioPosition = 0

    //选中倍数的圆环厚度
    private val ratioOutlineWith = 1.dp

    //倍数和刻度的间隔
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
        (scaleThickness / 2) to (measuredHeight - scaleThickness / 2f)
    }

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

    private val ratioSpec = listOf(10, 2, 1)

    //中点的坐标
    private val ratioSpecY = FloatArray(ratioSpec.size)

    private val resetRunnable = Runnable {
        hasScale = false
        requestLayout()
    }

    //外部监听事件
    var cursorMoveListener: CursorMoveListener? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val paddingVertical = paddingTop + paddingBottom
        val height = 2 * (ratioBgRadius + ratiosSpacing + ratioOutlineWith) + paddingVertical

        val paddingHorizontal = paddingStart + paddingEnd
        var width = 2 * (ratioBgRadius + ratioOutlineWith) + paddingHorizontal

        if (hasScale) {
            width += scaleLengthLong + columnSpacing
        }
        Log.d(TAG, "onMeasure:$height,$width")

        //设置倍率坐标中心点
        var cy = ratioBgRadius + paddingTop + ratioOutlineWith
        for (i in ratioSpec.indices) {
            ratioSpecY[i] = cy
            cy += ratiosSpacing
        }

        setMeasuredDimension(width.roundToInt(), height.roundToInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val start = ratioBgRadius + paddingStart + ratioOutlineWith
        ratioSpec.onEachIndexed { index, scale ->
            val cy = ratioSpecY[index]
            //画背景圆
            paint.color = ratioBackgroundColor
            canvas.drawCircle(start, cy, ratioBgRadius, paint)
            //画缩放倍率
            val text = "${scale}x"
            textPaint.getTextBounds(text, 0, text.length, textBounds)
            val tx = ratioBgRadius - textBounds.width() / 2
            val ty = cy + textBounds.height() / 2
            canvas.drawText(text, tx, ty, textPaint)
            //选中圆环
            if (index == selectedRatioPosition) {
                val paintStyle = textPaint.style
                textPaint.style = Paint.Style.STROKE
                textPaint.strokeWidth = ratioOutlineWith
                canvas.drawCircle(start, cy, ratioBgRadius + ratioOutlineWith, textPaint)
                textPaint.style = paintStyle
            }

        }

        if (hasScale) {
            val spacing = (ratioSpecY[2]-ratioSpecY[0]) / (SCALE_COUNTS - 1)
            var startY = ratioSpecY[0]
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
                startY += spacing + scaleThickness
            }
            paint.color = cursorColor
            canvas.drawLine(
                with - cursorLength, moveOffset.toFloat(), with, moveOffset.toFloat(), paint
            )
        }
    }

    private var moveOffset = 0
    private var lastMoveOffset = 0
    private var lastPointY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointY = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastPointY = pointY
                if (hasScale) {
                    removeCallbacks(resetRunnable)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val offset = pointY - lastPointY
                if (!hasScale && offset.absoluteValue >= touchSlop) {
                    hasScale = true
                    lastPointY = pointY
                    requestLayout()
                } else if (hasScale) {
                    val offsetY = pointY - lastPointY
                    moveOffset = offsetY.toInt() + lastMoveOffset

                    if (moveOffset < cursorMoveRange.first) {
                        moveOffset = cursorMoveRange.first.toInt()
                    } else if (moveOffset > cursorMoveRange.second){
                        moveOffset = cursorMoveRange.second.toInt()
                    }
                    invalidate()
                    cursorMoveListener?.onCursorMove(
                        1-(moveOffset-cursorMoveRange.first)/cursorMoveRange.second)
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                lastMoveOffset = moveOffset
                if (hasScale) {
                    postDelayed(resetRunnable, 2000)
                }

            }
        }
        return true
    }

}