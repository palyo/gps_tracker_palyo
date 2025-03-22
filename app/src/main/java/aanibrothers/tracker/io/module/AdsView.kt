package aanibrothers.tracker.io.module

import aanibrothers.tracker.io.*
import android.content.*
import android.util.*
import android.view.*
import android.widget.*

class AdsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AdsView, defStyleAttr, 0)
        val layoutResId = typedArray.getResourceId(R.styleable.AdsView_shimmer_preview_layout, 0)
        if (layoutResId != 0) {
            LayoutInflater.from(context).inflate(layoutResId, this, true)
        }
        typedArray.recycle()
    }
}