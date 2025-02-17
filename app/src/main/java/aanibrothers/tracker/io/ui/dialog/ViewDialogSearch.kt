package aanibrothers.tracker.io.ui.dialog

import aanibrothers.tracker.io.adapter.*
import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.extension.*
import android.location.*
import android.os.*
import android.text.*
import android.view.*
import androidx.core.widget.*
import androidx.recyclerview.widget.*
import coder.apps.space.library.base.*

class ViewDialogSearch : BaseDialog<ViewDialogSearchBinding>(ViewDialogSearchBinding::inflate) {
    private var suggestionLocationAdapter: SuggestionLocationAdapter? = null

    override fun create() {}

    private fun handleBackPress() {
        dismiss()
    }

    override fun ViewDialogSearchBinding.viewCreated() {
        setupBackPressed()
        setupAdapter()
    }

    private fun ViewDialogSearchBinding.setupAdapter() {
        activity?.apply context@{
            recyclerView.apply {
                layoutManager = LinearLayoutManager(this@context, RecyclerView.VERTICAL, false)
                suggestionLocationAdapter = SuggestionLocationAdapter(this@context) {
                    listener?.invoke(it)
                    dialog?.dismiss()
                }
                adapter = suggestionLocationAdapter
            }
        }
    }

    private fun setupBackPressed() {
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                handleBackPress()
                true
            } else {
                false
            }
        }
    }

    override fun ViewDialogSearchBinding.initView() {}

    override fun ViewDialogSearchBinding.initListeners() {
        editSearch.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        editSearch.doAfterTextChanged {
            val query = editSearch.text.toString()
            filterQuery(query)
        }
    }

    private fun filterQuery(query: String) {
        activity?.apply context@{
            if (TextUtils.isEmpty(query)) {
                binding?.recyclerView?.post {
                    suggestionLocationAdapter?.updateData(mutableListOf(), query, "#000000")
                }
            } else {
            }
        }
    }

    companion object {
        private var listener: ((Address) -> Unit)? = null

        fun newInstance(listener: (Address) -> Unit): ViewDialogSearch {
            this.listener = listener
            return ViewDialogSearch().apply {
                arguments = Bundle().apply {}
            }
        }
    }
}