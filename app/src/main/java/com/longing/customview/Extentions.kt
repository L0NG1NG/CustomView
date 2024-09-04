package com.longing.customview

import android.content.res.Resources


val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()
val Int.sp: Float get() = this * Resources.getSystem().displayMetrics.scaledDensity