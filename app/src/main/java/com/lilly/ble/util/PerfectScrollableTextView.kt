package com.lilly.ble.util

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.TextView


class PerfectScrollableTextView : TextView {
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        isVerticalScrollBarEnabled = true
        setHorizontallyScrolling(false)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        isVerticalScrollBarEnabled = true
        setHorizontallyScrolling(false)
    }

    constructor(context: Context?) : super(context) {
        isVerticalScrollBarEnabled = true
        setHorizontallyScrolling(false)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        if (focused) super.onFocusChanged(focused, direction, previouslyFocusedRect)
    }

    override fun onWindowFocusChanged(focused: Boolean) {
        if (focused) super.onWindowFocusChanged(focused)
    }

    override fun isFocused(): Boolean {
        return true
    }
}