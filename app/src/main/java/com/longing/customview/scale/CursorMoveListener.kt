package com.longing.customview.scale

interface CursorMoveListener {

    /**
     * 游标在移动
     */
    fun onCursorMove(progress:Float)

    /**
     * 移动结束
     */
    fun onCursorMoveEnd(progress:Float)

}