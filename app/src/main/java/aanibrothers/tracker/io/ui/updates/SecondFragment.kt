package aanibrothers.tracker.io.ui.updates

import aanibrothers.tracker.io.databinding.FragmentSecondBinding
import aanibrothers.tracker.io.module.viewNativeMedium
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imgNext.setOnClickListener {
            (activity as? OnboardingActivity)?.goToNext()
        }

        activity?.viewNativeMedium(binding.adNative)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
