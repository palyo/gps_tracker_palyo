package aanibrothers.tracker.io.caller.frags

import aanibrothers.tracker.io.databinding.FragmentOptionBinding
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import coder.apps.space.library.base.BaseFragment

class OptionFragment : BaseFragment<FragmentOptionBinding>(FragmentOptionBinding::inflate) {

    override fun FragmentOptionBinding.viewCreated() {
    }

    override fun FragmentOptionBinding.initListeners() {
        activity?.apply context@{
            buttonContact.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = ContactsContract.Contacts.CONTENT_URI
                }
                startActivity(intent)
            }
            buttonCalendar.setOnClickListener {
                val calendarIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                }
                startActivity(calendarIntent)
            }
            buttonMail.setOnClickListener {
                val gmailIntent = packageManager.getLaunchIntentForPackage("com.google.android.gm")
                if (gmailIntent != null) {
                    startActivity(gmailIntent)
                } else {
                    Toast.makeText(this, "app not installed", Toast.LENGTH_SHORT).show()
                }

            }
            buttonMessage.setOnClickListener {
                val smsIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MESSAGING)
                }
                startActivity(smsIntent)
            }
        }
    }

    override fun FragmentOptionBinding.initView() {
    }

    override fun create() {
    }

    companion object {
        private const val TAG = "OptionFragment"
        fun newInstance() = OptionFragment().apply {
            arguments = Bundle().apply {}
        }
    }
}
