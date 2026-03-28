package aanibrothers.tracker.io.afterCall.ui.fragment

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.PostCallFragmentMessagesBinding
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import aanibrothers.tracker.io.afterCall.ui.adapter.QuickMessageItemClickListener
import aanibrothers.tracker.io.afterCall.ui.adapter.QuickReplyAdapter
import aanibrothers.tracker.io.afterCall.ui.adapter.QuickReplyModel

class MessagesFragment : Fragment() {

    private var _binding: PostCallFragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private var adapter: QuickReplyAdapter? = null

    private val dataList = arrayListOf<QuickReplyModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = PostCallFragmentMessagesBinding.inflate(inflater, container, false)

        val ctx = requireContext()

        dataList.add(QuickReplyModel("0", ctx.getString(R.string.quick_reply_1)))
        dataList.add(QuickReplyModel("1", ctx.getString(R.string.quick_reply_2)))
        dataList.add(QuickReplyModel("2", ctx.getString(R.string.quick_reply_3)))
        dataList.add(QuickReplyModel("3", ctx.getString(R.string.quick_reply_4)))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listener = requireActivity() as QuickMessageItemClickListener

        setupRecycler(listener)
        setupInput(listener)
    }

    private fun setupRecycler(listener: QuickMessageItemClickListener) {

        binding.rvClipboard.layoutManager = LinearLayoutManager(requireContext())

        adapter = QuickReplyAdapter(
            requireContext(), dataList
        ) { position ->

            listener.onQuickMsgItemClick(dataList[position].name)
        }

        binding.rvClipboard.adapter = adapter
    }

    private fun setupInput(listener: QuickMessageItemClickListener) {
        binding.icSend.visibility = if (binding.edtMsg.text.isNullOrEmpty()) View.GONE else View.VISIBLE

        binding.edtMsg.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {

                binding.icSend.visibility =
                    if (s.isNullOrEmpty()) View.GONE else View.VISIBLE

                adapter?.let {
                    if (it.selectPos != -1) {
                        it.selectionClear()
                    }
                }
            }
        })

        binding.icSend.setOnClickListener {
            val text = binding.edtMsg.text.toString()
            if (text.isNotEmpty()) {
                listener.onQuickMsgItemClick(text)
            }
        }

        val accentColor = ContextCompat.getColor(requireActivity(), R.color.colorAccent)

        binding.edtMsg.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.ivEdit.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
                  adapter?.let {
                    if (it.selectPos != -1) {
                        it.selectionClear()
                    }
                }

            } else {
                binding.ivEdit.clearColorFilter()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}