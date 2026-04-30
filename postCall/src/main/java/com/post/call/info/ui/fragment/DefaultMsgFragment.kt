package com.post.call.info.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.post.call.info.databinding.PostFragmentDefaultMsgBinding

/**
 * Base fragment that inflates a layout supplied by the host module.
 *
 * Two ways to use:
 *
 *   1) Direct (no subclass) — inflate any app-module layout:
 *        DefaultMsgFragment.newInstance(R.layout.fragment_data)
 *
 *   2) Subclass in the app module to wire clicks / ViewBinding:
 *        class DataFragment : DefaultMsgFragment() {
 *            override val layoutRes = R.layout.fragment_data
 *            override fun onViewCreated(view: View, s: Bundle?) { ... }
 *        }
 */
class DefaultMsgFragment : Fragment() {

    private var _binding: PostFragmentDefaultMsgBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = PostFragmentDefaultMsgBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
