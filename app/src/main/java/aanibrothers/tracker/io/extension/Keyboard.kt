package aanibrothers.tracker.io.extension

import android.content.*
import android.view.*
import android.view.inputmethod.*

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}
