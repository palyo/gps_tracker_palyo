package aanibrothers.tracker.io.ui.updates

import aanibrothers.tracker.io.databinding.ActivityPromptBinding
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

class PromptActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPromptBinding

    public override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        binding = ActivityPromptBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        binding.layoutParent.setOnClickListener {
            finish()
        }

    }
}