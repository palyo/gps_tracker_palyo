package aanibrothers.tracker.io.ui.updates

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.ActivityFeedbackBinding
import aanibrothers.tracker.io.extension.firebaseASOEvent
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.net.toUri
import coder.apps.space.library.base.BaseActivity
import coder.apps.space.library.helper.TinyDB

class FeedbackActivity : BaseActivity<ActivityFeedbackBinding>(ActivityFeedbackBinding::inflate) {
    var selectedText = ""

    override fun ActivityFeedbackBinding.initExtra() {
        firebaseASOEvent("rate_view_feeedback")
    }

    override fun ActivityFeedbackBinding.initListeners() {
        txt1.setOnClickListener {
            resetAll()
            selectedText = txt1.text.toString()
            txt1.isSelected =true
        }
        txt2.setOnClickListener {
            resetAll()
            selectedText = txt2.text.toString()
            txt2.isSelected =true
        }
        txt3.setOnClickListener {
            resetAll()
            selectedText = txt3.text.toString()
            txt3.isSelected =true
        }
        txt4.setOnClickListener {
            resetAll()
            selectedText = txt4.text.toString()
            txt4.isSelected =true
        }
        txt5.setOnClickListener {
            resetAll()
            selectedText = txt5.text.toString()
            txt5.isSelected =true
        }
        txt6.setOnClickListener {
            resetAll()
            selectedText = txt6.text.toString()
            txt6.isSelected =true
        }
        submitButton.setOnClickListener {
            if(selectedText.isEmpty()){
                Toast.makeText(this@FeedbackActivity, "please select feedback type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (feedbackInput.text.isNullOrEmpty()) {
                Toast.makeText(this@FeedbackActivity, "please write a feedback", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (feedbackInput.text.length < 6) {
                Toast.makeText(this@FeedbackActivity, "minimum 6 characters required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            hideKeyboardScreen()
            sendFeedback(feedbackInput.text.toString())
        }
    }

    override fun ActivityFeedbackBinding.initView() {
        backButton.setOnClickListener {
            finish()
        }
    }


    fun ActivityFeedbackBinding.resetAll() {
        binding.apply {
            txt1.isSelected = false
            txt2.isSelected = false
            txt3.isSelected = false
            txt4.isSelected = false
            txt5.isSelected = false
            txt6.isSelected = false
        }
    }

    fun sendFeedback(userMessage: String) {
        val model = Build.MANUFACTURER + " " + Build.MODEL
        val osVersion = Build.VERSION.RELEASE
        val apiLevel = Build.VERSION.SDK_INT

        val body = """
                ${getString(R.string.app_name)}

                System Detail:
                Model: $model
                OS Version: $osVersion
                OS API Level: $apiLevel

                Report[${selectedText}]:
                $userMessage
        """.trimIndent()

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf("jayanichhaya@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            TinyDB(this@FeedbackActivity).putBoolean("isRated", true)
            startActivity(Intent.createChooser(emailIntent, "Send feedback using"))
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    fun Activity.hideKeyboardScreen() {
        try {
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            if (inputMethodManager.isActive) {
                inputMethodManager.hideSoftInputFromWindow(window.currentFocus!!.windowToken, 0)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}