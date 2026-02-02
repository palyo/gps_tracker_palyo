package aanibrothers.tracker.io.helper

import aanibrothers.tracker.io.ui.updates.HomeActivity
import android.os.Message
import android.provider.Settings
import coder.apps.space.library.helper.LeakGuardHandlerWrapper

class HandleSettingPreview internal constructor(activity: HomeActivity) :
        LeakGuardHandlerWrapper<HomeActivity>(activity) {

        fun cancelPollingImeSettings() {
            removeMessages(0)
        }

        override fun handleMessage(message: Message) {
            val ownerInstance = ownerInstance
            if (ownerInstance != null && message.what == 0) {
                if (Settings.canDrawOverlays(ownerInstance)) {
                    ownerInstance.invokeSetupWizardOfThisIme()
                } else {
                    startPollingImeSettings()
                }
            }
        }

        fun startPollingImeSettings() {
            sendMessageDelayed(obtainMessage(0), 200L)
        }
    }