package io.streamlayer.demo.utils

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

fun View.setOnDoubleClickListener(onClickListener: (View) -> Unit) {
    val gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent?): Boolean {
                onClickListener.invoke(this@setOnDoubleClickListener)
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent?): Boolean = true
            override fun onDown(e: MotionEvent?): Boolean = true
        })

    setOnTouchListener { view, motionEvent ->
        gestureDetector.onTouchEvent(motionEvent)
    }
}