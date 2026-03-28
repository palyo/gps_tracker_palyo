package aanibrothers.tracker.io.afterCall.ui.fragment

import aanibrothers.tracker.io.R
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import aanibrothers.tracker.io.afterCall.extensions.EXTRA_MOBILE_NUMBER
import aanibrothers.tracker.io.databinding.PostCallFragmentDefaultMsgBinding
import aanibrothers.tracker.io.ui.updates.HomeActivity
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DefaultMsgFragment : Fragment() {

    private var _binding: PostCallFragmentDefaultMsgBinding? = null
    private val binding get() = _binding!!

    private var phoneAccountHandleList: List<PhoneAccountHandle>? = null
    private var strCallType: String? = null
    private var strName: String? = null
    private var strNumber: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PostCallFragmentDefaultMsgBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        phoneAccountHandleList = emptyList()
        initCallDetail()
        binding.initListener()
    }


    private fun PostCallFragmentDefaultMsgBinding.initListener() {
        actionHome.setOnClickListener {
            Intent(requireContext(), HomeActivity::class.java).apply {
                putExtra("action", 0)
                startActivity(this)
            }
            activity?.totalDead()
        }
        actionTemplates.setOnClickListener {
            Intent(requireContext(), HomeActivity::class.java).apply {
                putExtra("action", 1)
                startActivity(this)
            }
            activity?.totalDead()
        }
        actionMapData.setOnClickListener {
            Intent(requireContext(), HomeActivity::class.java).apply {
                putExtra("action", 2)
                startActivity(this)
            }
            activity?.totalDead()
        }
        actionTools.setOnClickListener {
            Intent(requireContext(), HomeActivity::class.java).apply {
                putExtra("action",3)
                startActivity(this)
            }
            activity?.totalDead()
        }
    }

    private fun Activity.totalDead() {
        try {
            finish()
        } catch (e2: Exception) {
            e2.printStackTrace()
        }
    }

    private fun initCallDetail() {
        activity?.apply {
            strNumber = intent.getStringExtra(EXTRA_MOBILE_NUMBER)

            if (!strNumber.isNullOrEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    val contactName = withContext(Dispatchers.IO) {
                        getContactName(strNumber)
                    }
                    strName = contactName.ifEmpty { null }
                }
            }

            val stringExtra2 = intent.getStringExtra("state")
            strCallType = when (stringExtra2) {
                "3" -> getString(R.string.label_missed_call)
                "2" -> getString(R.string.label_call_end)
                else -> getString(R.string.label_call_end)
            }
        }
    }

    fun Context.getContactName(phoneNumber: String?): String {
        try {
            val queryUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor = contentResolver.query(queryUri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayName = it.getString(it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME))
                    return displayName
                }
            }
            return getString(R.string.label_unknown)
        } catch (e: Exception) {
            return ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
