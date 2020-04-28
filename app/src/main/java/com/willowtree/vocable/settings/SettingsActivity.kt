package com.willowtree.vocable.settings

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.willowtree.vocable.BaseActivity
import com.willowtree.vocable.BaseViewModelFactory
import com.willowtree.vocable.R
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.customviews.PointerView
import com.willowtree.vocable.databinding.ActivitySettingsBinding
import com.willowtree.vocable.facetracking.FaceTrackFragment

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val allViews = mutableListOf<View>()
    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(
            this,
            BaseViewModelFactory(
                getString(R.string.category_123_id),
                getString(R.string.category_my_sayings_id)
            )
        ).get(SettingsViewModel::class.java)

        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (supportFragmentManager.findFragmentById(R.id.settings_fragment_container) == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_fragment_container, SettingsFragment())
                .commit()
        }

        binding.settingsFragmentContainer.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            allViews.clear()
        }
    }

    override fun subscribeToViewModel() {
        super.subscribeToViewModel()
        viewModel.headTrackingEnabled.observe(this, Observer {
            it?.let {
                val faceFragment = supportFragmentManager.findFragmentById(R.id.face_fragment)
                if (faceFragment is FaceTrackFragment) {
                    faceFragment.enableFaceTracking(it)
                }
            }
        })
    }

    override fun getErrorView(): View = binding.errorView.root

    override fun getPointerView(): PointerView = binding.pointerView

    override fun getAllViews(): List<View> {
        return when {
            allViews.isNotEmpty() -> allViews
            else -> {
                getAllChildViews(binding.parentLayout)
                allViews
            }
        }
    }

    fun resetAllViews() {
        allViews.clear()
    }

    override fun getLayout(): Int =
        R.layout.activity_settings

    private fun getAllChildViews(viewGroup: ViewGroup) {
        viewGroup.children.forEach {
            if (it is PointerListener) {
                allViews.add(it)
            } else if (it is ViewGroup) {
                getAllChildViews(it)
            }
        }
    }
}