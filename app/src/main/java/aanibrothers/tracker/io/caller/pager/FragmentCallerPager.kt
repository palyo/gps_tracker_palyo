package aanibrothers.tracker.io.caller.pager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class FragmentCallerPager(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
    var registeredFragments: MutableList<Fragment> = mutableListOf()
    override fun createFragment(position: Int): Fragment {
        return registeredFragments[position]
    }

    override fun getItemCount(): Int {
        return registeredFragments.count()
    }

    fun update(fragments: MutableList<Fragment>) {
        registeredFragments = fragments
        notifyItemRangeChanged(0, itemCount)
    }
}
