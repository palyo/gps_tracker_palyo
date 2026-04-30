package com.post.call.info.receiver

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.post.call.info.R
import com.post.call.info.databinding.PostLayoutWindowBinding
import com.post.call.info.ui.adapter.QuickReplyAdapter
import com.post.call.info.ui.adapter.QuickReplyModel

class CallerWidgetWindow(private val context: Context) {

    private var audioManager: AudioManager? = null
    private lateinit var binding: PostLayoutWindowBinding
    private var currentSound = 0
    private var dataList = ArrayList<QuickReplyModel>()
    private var isVoiceMute = false
    private var mainView: View? = null
    private var measuredWidth = 0

    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        binding = PostLayoutWindowBinding.inflate(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)

        mainView = binding.root

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        currentSound = audioManager?.getStreamVolume(AudioManager.STREAM_RING)?:0

        measuredWidth = windowManager.defaultDisplay.width

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            277741985,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.END or Gravity.CENTER

        windowManager.addView(mainView, params)

        shoMainPopupLayout()
        setMuteIcon()

        if (PhoneCallReceiver.isVoiceOptionEnabled) {
            binding.icSoundMute.visibility = View.VISIBLE
            binding.icMute.visibility = View.GONE
        } else {
            binding.icSoundMute.visibility = View.GONE
            binding.icMute.visibility = View.VISIBLE
        }

        binding.loutMain.setOnTouchListener { _, _ -> true }

        binding.icCalendar.setOnClickListener {
            try {

                val currentTimeMillis = System.currentTimeMillis()

                val builder = CalendarContract.CONTENT_URI.buildUpon()
                builder.appendPath("time")
                ContentUris.appendId(builder, currentTimeMillis)

                val intent = Intent(Intent.ACTION_VIEW).setData(builder.build())
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                disabledOpenAds()
                context.startActivity(intent)

            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.no_app_found), Toast.LENGTH_SHORT).show()
            }
        }

        binding.icMute.setOnClickListener {
            muteMicrophone(!(audioManager?.isMicrophoneMute ?: false))
        }

        binding.icSoundMute.setOnClickListener {
            muteSoundMicrophone(!isVoiceMute)
        }

        binding.icMessage.setOnClickListener {
            binding.loutCallPopup.visibility = View.GONE
            binding.loutMessageMain.visibility = View.VISIBLE
        }

        binding.icClose.setOnClickListener {
            shoMainPopupLayout()
        }

        var lastY = 0f

        binding.loutCallPopup.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastY = event.rawY
                    binding.loutCallPopup.alpha = 0.6f
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dy = event.rawY - lastY
                    val params1 = mainView?.layoutParams as WindowManager.LayoutParams
                    params1.y += dy.toInt()
                    windowManager.updateViewLayout(mainView, params1)
                    lastY = event.rawY
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    binding.loutCallPopup.alpha = 1f
                    false
                }

                else -> false
            }
        }

        initMessage()
    }

    private fun setMuteIcon() {

        val muteDrawable = ContextCompat.getDrawable(
            context,
            if (audioManager?.isMicrophoneMute == true)
                R.drawable.post_ic_mute_caller
            else
                R.drawable.post_ic_unmute_caller
        )

        binding.icMute.setImageDrawable(muteDrawable)

        val voiceDrawable = ContextCompat.getDrawable(
            context,
            if (isVoiceMute)
                R.drawable.post_ic_voice_unmute_caller
            else
                R.drawable.post_ic_voice_mute_caller
        )

        binding.icSoundMute.setImageDrawable(voiceDrawable)
    }

    private fun initMessage() {
        dataList.add(QuickReplyModel("0", context.getString(R.string.quick_reply_1)))
        dataList.add(QuickReplyModel("1", context.getString(R.string.quick_reply_2)))
        dataList.add(QuickReplyModel("2", context.getString(R.string.quick_reply_3)))
        dataList.add(QuickReplyModel("3", context.getString(R.string.quick_reply_4)))

        binding.loutMessageMain.layoutParams.width = measuredWidth
        binding.loutMessageMain.requestLayout()

        binding.quickResponseList.adapter = QuickReplyAdapter(context, dataList) { position ->

            val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("sms", "", null))
            intent.putExtra("sms_body", dataList[position].name)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            disabledOpenAds()

            context.startActivity(intent)

            shoMainPopupLayout()
        }
    }

    private fun shoMainPopupLayout() {
        binding.loutCallPopup.visibility = View.VISIBLE
        binding.loutMessageMain.visibility = View.GONE
    }

    private fun disabledOpenAds() {
        // new PostCallApplication().disabledOpenAds()
    }

    private fun muteMicrophone(mute: Boolean) {

        audioManager?.setMicrophoneMute(mute)

        val drawable = ContextCompat.getDrawable(
            context,
            if (audioManager?.isMicrophoneMute == true)
                R.drawable.post_ic_mute_caller
            else
                R.drawable.post_ic_unmute_caller
        )

        binding.icMute.setImageDrawable(drawable)
    }

    private fun muteSoundMicrophone(mute: Boolean) {
        isVoiceMute = mute
        try {
            if (mute) {
                audioManager?.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
            } else {
                audioManager?.setStreamVolume(AudioManager.STREAM_RING, currentSound, 0)
            }

        } catch (_: Exception) {
        }

        val drawable = ContextCompat.getDrawable(
            context,
            if (isVoiceMute)
                R.drawable.post_ic_voice_unmute_caller
            else
                R.drawable.post_ic_voice_mute_caller
        )

        binding.icSoundMute.setImageDrawable(drawable)
    }

    fun hide() {
        try {
            mainView?.let {
                onClearData()
                windowManager.removeView(it)
            }
        } catch (_: Exception) {
        }
    }

    fun onClearData() {
        if (isVoiceMute) {
            audioManager?.setStreamVolume(AudioManager.STREAM_RING, currentSound, 0)
        }
    }

    fun showMuteOption() {

        if (!binding.root.isAttachedToWindow) {
            Log.e("WindowView", "View not attached to window. Skipping showMuteOption()")
            return
        }

        if (PhoneCallReceiver.isVoiceOptionEnabled) {
            binding.icSoundMute.visibility = View.VISIBLE
            binding.icMute.visibility = View.GONE
        } else {
            binding.icSoundMute.visibility = View.GONE
            binding.icMute.visibility = View.VISIBLE
        }

        setMuteIcon()
    }
}