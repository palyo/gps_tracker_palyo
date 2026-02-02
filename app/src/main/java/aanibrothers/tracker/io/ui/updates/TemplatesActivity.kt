package aanibrothers.tracker.io.ui.updates

import aanibrothers.tracker.io.databinding.ActivityTemplatesBinding
import androidx.activity.addCallback
import coder.apps.space.library.base.BaseActivity

class TemplatesActivity :
    BaseActivity<ActivityTemplatesBinding>(ActivityTemplatesBinding::inflate) {
    override fun ActivityTemplatesBinding.initExtra() {
        updateSelection()
    }

    override fun ActivityTemplatesBinding.initListeners() {
        templateDefault.setOnClickListener {
            tinyDB?.putString("template", "default")
            updateSelection()
        }
        templateClassic.setOnClickListener {
            tinyDB?.putString("template", "classic")
            updateSelection()
        }
        templateSquarise.setOnClickListener {
            tinyDB?.putString("template", "squarise")
            updateSelection()
        }
    }

    private fun ActivityTemplatesBinding.updateSelection() {
        templateDefault.isSelected = tinyDB?.getString("template", "default") == "default"
        templateClassic.isSelected = tinyDB?.getString("template", "default") == "classic"
        templateSquarise.isSelected = tinyDB?.getString("template", "default") == "squarise"
    }

    override fun ActivityTemplatesBinding.initView() {
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        onBackPressedDispatcher.addCallback { finish() }
    }
}