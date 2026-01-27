package aanibrothers.tracker.io.caller.frags

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.FragmentMessageBinding
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import coder.apps.space.library.base.BaseFragment
import com.google.android.material.checkbox.MaterialCheckBox

class MessageFragment : BaseFragment<FragmentMessageBinding>(FragmentMessageBinding::inflate) {

    override fun FragmentMessageBinding.viewCreated() {
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun FragmentMessageBinding.initListeners() {
        val sendIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_send)

        editMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrEmpty()
                val endDrawable = if (hasText) sendIcon else null
                editMessage.setCompoundDrawablesWithIntrinsicBounds(null, null, endDrawable, null)
            }
        })

        editMessage.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = editMessage.compoundDrawables[2] // index 2 = end drawable
                drawableEnd?.let {
                    val drawableWidth = it.bounds.width()
                    val clickX = event.x
                    val viewWidth = editMessage.width
                    if (clickX >= viewWidth - drawableWidth - editMessage.paddingEnd) {
                        val share = Intent("android.intent.action.SEND")
                        share.type = "text/plain"
                        share.putExtra(
                            "android.intent.extra.TEXT", "${editMessage.text}".trimIndent()
                        )
                        startActivity(Intent.createChooser(share, "Share Application"))
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }

        buttonCantTalk.setOnCheckedChangeListener { _, isChecked ->
            updateSelection(1)
            val endDrawable = if (isChecked) sendIcon else null
            buttonCantTalk.setCompoundDrawablesWithIntrinsicBounds(null, null, endDrawable, null)
        }

        buttonCallYouLater.setOnCheckedChangeListener { _, isChecked ->
            updateSelection(2)
            val endDrawable = if (isChecked) sendIcon else null
            buttonCallYouLater.setCompoundDrawablesWithIntrinsicBounds(null, null, endDrawable, null)
        }

        buttonOnMyWay.setOnCheckedChangeListener { _, isChecked ->
            updateSelection(3)
            val endDrawable = if (isChecked) sendIcon else null
            buttonOnMyWay.setCompoundDrawablesWithIntrinsicBounds(null, null, endDrawable, null)
        }

    }

    fun FragmentMessageBinding.updateSelection(selected: Int) {
        val sendIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_send)
        val buttons = listOf(buttonCantTalk, buttonCallYouLater, buttonOnMyWay)
        buttons.forEach { it.setOnCheckedChangeListener(null) }

        buttons.forEachIndexed { index, button ->
            val isSelected = (index + 1) == selected
            button.isChecked = isSelected
            button.isSelected = isSelected
            val endDrawable = if (isSelected) sendIcon else null
            button.setCompoundDrawablesWithIntrinsicBounds(null, null, endDrawable, null)
        }

        setupCheckButtons()
    }

    private fun FragmentMessageBinding.setupCheckButtons() = with(binding) {
        setupButton(buttonCantTalk, 1)
        setupButton(buttonCallYouLater, 2)
        setupButton(buttonOnMyWay, 3)
    }

    private fun FragmentMessageBinding.setupButton(button: MaterialCheckBox, index: Int) {
        val sendIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_send)

        button.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateSelection(index)
            }
            val endDrawable = if (isChecked) sendIcon else null
            button.setCompoundDrawablesWithIntrinsicBounds(null, null, endDrawable, null)
        }

        // Detect clicks on drawableEnd (send icon)
        button.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP && button.isChecked) {
                val drawableEnd = button.compoundDrawables[2]
                drawableEnd?.let {
                    val drawableWidth = it.bounds.width()
                    val clickX = event.x
                    val viewWidth = button.width

                    if (clickX >= viewWidth - drawableWidth - button.paddingEnd) {
                        Toast.makeText(v.context, "Send clicked for button $index", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "setupButton: Send clicked for button $index" )
                        when (index) {
                            1 -> {
                                val share = Intent("android.intent.action.SEND")
                                share.type = "text/plain"
                                share.putExtra(
                                    "android.intent.extra.TEXT", getString(R.string.can_t_talk_right_now).trimIndent()
                                )
                                startActivity(Intent.createChooser(share, "Share Application"))
                            }

                            2 -> {
                                val share = Intent("android.intent.action.SEND")
                                share.type = "text/plain"
                                share.putExtra(
                                    "android.intent.extra.TEXT", getString(R.string.i_ll_call_you_later).trimIndent()
                                )
                                startActivity(Intent.createChooser(share, "Share Application"))
                            }

                            3 -> {
                                val share = Intent("android.intent.action.SEND")
                                share.type = "text/plain"
                                share.putExtra(
                                    "android.intent.extra.TEXT", getString(R.string.i_m_on_my_way).trimIndent()
                                )
                                startActivity(Intent.createChooser(share, "Share Application"))
                            }
                        }
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }


    override fun FragmentMessageBinding.initView() {
    }

    override fun create() {
    }

    companion object {
        private const val TAG = "MessageFragment"
        fun newInstance() = MessageFragment().apply {
            arguments = Bundle().apply {}
        }
    }
}
