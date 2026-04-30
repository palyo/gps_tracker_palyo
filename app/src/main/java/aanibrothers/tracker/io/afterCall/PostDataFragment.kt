package aanibrothers.tracker.io.afterCall

import aanibrothers.tracker.io.databinding.PostCallDataFragmentBinding
import aanibrothers.tracker.io.ui.updates.HomeActivity
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class PostDataFragment : Fragment() {

    private var _binding: PostCallDataFragmentBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PostCallDataFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.initListener()
    }


    private fun PostCallDataFragmentBinding.initListener() {
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



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}