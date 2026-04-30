package com.post.call.info

import androidx.fragment.app.Fragment

/**
 * One-time configuration the host app provides to the :postcall library.
 *
 * Set this from your Application.onCreate(), before any call can be received.
 *
 *     // In :app — BaseApp.onCreate():
 *     PostCallConfig.dataFragmentClass = DataFragment::class.java
 *
 * The library reads this when it shows PostCallActivity. If left null,
 * the first ViewPager tab falls back to an empty DefaultMsgFragment
 * (which will throw at inflation time — so don't leave it null).
 */
object PostCallConfig {

    /**
     * Fragment shown in the first ("data") tab of PostCallActivity.
     * Must be a public class with a public no-arg constructor.
     */
    @JvmStatic
    var dataFragmentClass: Class<out Fragment>? = null
}
