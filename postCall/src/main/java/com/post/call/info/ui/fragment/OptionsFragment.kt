package com.post.call.info.ui.fragment

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.post.call.info.databinding.PostFragmentOptionsBinding

class OptionsFragment : Fragment() {

    private var _binding: PostFragmentOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        _binding = PostFragmentOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClicks()
    }

    private fun setupClicks() {

        binding.editIcon.setOnClickListener { editContactMethod() }
        binding.editTxt.setOnClickListener { editContactMethod() }

        binding.messageIcon.setOnClickListener { messageContactMethod() }
        binding.messageTxt.setOnClickListener { messageContactMethod() }

        binding.mailIcon.setOnClickListener { mailContactMethod() }
        binding.mailTxt.setOnClickListener { mailContactMethod() }

        binding.calendarIcon.setOnClickListener { calendarContactMethod() }
        binding.calendarTxt.setOnClickListener { calendarContactMethod() }

        binding.webIcon.setOnClickListener { webContactMethod() }
        binding.webTxt.setOnClickListener { webContactMethod() }
    }

    private fun editContactMethod() {
        val uri = ContentUris.withAppendedId(
            ContactsContract.Contacts.CONTENT_URI,
            10L
        )

        try {
            val intent = Intent(Intent.ACTION_EDIT).apply {
                data = uri
                putExtra("finishActivityOnSaveCompleted", true)
            }

            disabledOpenAds()
            startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun messageContactMethod() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:")).apply {
                putExtra("sms_body", "")
            }

            disabledOpenAds()
            startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mailContactMethod() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, "mailto:your_email".toUri()).apply {
                putExtra(Intent.EXTRA_SUBJECT, "your_subject")
                putExtra(Intent.EXTRA_TEXT, "your_text")
            }

            disabledOpenAds()
            startActivity(intent)

        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun calendarContactMethod() {

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra("title", "New Event")
        }

        if (intent.resolveActivity(requireContext().packageManager) != null) {
            disabledOpenAds()
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "No calendar app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun webContactMethod() {

        val intent = Intent(Intent.ACTION_VIEW, "https://www.google.com".toUri())

        try {
            disabledOpenAds()
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun disabledOpenAds() {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}