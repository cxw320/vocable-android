package com.willowtree.vocable.presets

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import androidx.viewpager2.widget.ViewPager2
import com.willowtree.vocable.*
import com.willowtree.vocable.customviews.PointerListener
import com.willowtree.vocable.databinding.FragmentPresetsBinding
import com.willowtree.vocable.keyboard.KeyboardFragment
import com.willowtree.vocable.room.Category
import com.willowtree.vocable.room.Phrase
import com.willowtree.vocable.settings.SettingsActivity
import com.willowtree.vocable.utils.SpokenText
import com.willowtree.vocable.utils.VocableFragmentStateAdapter
import com.willowtree.vocable.utils.VocableTextToSpeech

class PresetsFragment : BaseFragment<FragmentPresetsBinding>() {

    override val bindingInflater: BindingInflater<FragmentPresetsBinding> = FragmentPresetsBinding::inflate
    private val allViews = mutableListOf<View>()

    private var maxCategories = 1
    private var maxPhrases = 1

    private lateinit var presetsViewModel: PresetsViewModel
    private lateinit var categoriesAdapter: CategoriesPagerAdapter
    private lateinit var phrasesAdapter: PhrasesPagerAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        maxCategories = resources.getInteger(R.integer.max_categories)

        binding.categoryForwardButton.action = {
            when (val currentPosition = binding.categoryView.currentItem) {
                categoriesAdapter.itemCount - 1 -> {
                    binding.categoryView.setCurrentItem(0, true)
                }
                else -> {
                    binding.categoryView.setCurrentItem(currentPosition + 1, true)
                }
            }
        }

        binding.categoryBackButton.action = {
            when (val currentPosition = binding.categoryView.currentItem) {
                0 -> {
                    binding.categoryView.setCurrentItem(categoriesAdapter.itemCount - 1, true)
                }
                else -> {
                    binding.categoryView.setCurrentItem(currentPosition - 1, true)
                }
            }
        }

        binding.phrasesForwardButton.action = {
            when (val currentPosition = binding.phrasesView.currentItem) {
                phrasesAdapter.itemCount - 1 -> {
                    binding.phrasesView.setCurrentItem(0, true)
                }
                else -> {
                    binding.phrasesView.setCurrentItem(currentPosition + 1, true)
                }
            }
        }

        binding.phrasesBackButton.action = {
            when (val currentPosition = binding.phrasesView.currentItem) {
                0 -> {
                    binding.phrasesView.setCurrentItem(phrasesAdapter.itemCount - 1, true)
                }
                else -> {
                    binding.phrasesView.setCurrentItem(currentPosition - 1, true)
                }
            }
        }

        binding.actionButtonContainer.keyboardButton.action = {
            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, KeyboardFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.actionButtonContainer.settingsButton.action = {
            val intent = Intent(activity, SettingsActivity::class.java)
            startActivity(intent)
        }

        categoriesAdapter = CategoriesPagerAdapter(childFragmentManager)
        phrasesAdapter = PhrasesPagerAdapter(childFragmentManager)

        binding.categoryView.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                activity?.let { activity ->
                    allViews.clear()
                    if (activity is MainActivity) {
                        activity.resetAllViews()
                    }
                }
            }
        })

        binding.phrasesView.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val pageNum = position % phrasesAdapter.numPages + 1
                binding.phrasesPageNumber.text = getString(
                    R.string.phrases_page_number,
                    pageNum,
                    phrasesAdapter.numPages
                )

                activity?.let { activity ->
                    allViews.clear()
                    if (activity is MainActivity) {
                        activity.resetAllViews()
                    }
                }
            }
        })

        SpokenText.postValue(null)

        presetsViewModel =
            ViewModelProviders.of(
                requireActivity(),
                BaseViewModelFactory(
                    getString(R.string.category_123_id),
                    getString(R.string.category_my_sayings_id)
                )
            ).get(PresetsViewModel::class.java)
        subscribeToViewModel()
    }

    override fun onResume() {
        super.onResume()
        presetsViewModel.populateCategories()
    }

    private fun subscribeToViewModel() {
        SpokenText.observe(viewLifecycleOwner, Observer {
            binding.currentText.text = if (it.isNullOrBlank()) {
                getString(R.string.select_something)
            } else {
                it
            }
        })

        VocableTextToSpeech.isSpeaking.observe(viewLifecycleOwner, Observer {
            binding.speakerIcon.isVisible = it
        })

        presetsViewModel.apply {
            categoryList.observe(viewLifecycleOwner, ::handleCategories)
            currentPhrases.observe(viewLifecycleOwner, ::handlePhrases)
        }
    }

    override fun getAllViews(): List<View> {
        if (allViews.isEmpty()) {
            getAllChildViews(binding.presetsParent)
        }
        return allViews
    }

    private fun getAllChildViews(viewGroup: ViewGroup?) {
        viewGroup?.children?.forEach {
            if (it is PointerListener) {
                allViews.add(it)
            } else if (it is ViewGroup) {
                getAllChildViews(it)
            }
        }
    }

    private fun handleCategories(categories: List<Category>) {
        binding.categoryView.apply {
            isSaveEnabled = false
            adapter = categoriesAdapter
            categoriesAdapter.setItems(categories)

            // The categories ViewPager position will initially be set to the middle so that the
            // user can scroll in both directions. Upon subsequent config changes, the position
            // will be set to the closest page to the middle which contains the selected category.
            var targetPosition = categoriesAdapter.itemCount / 2

            if (targetPosition % categoriesAdapter.numPages != 0) {
                targetPosition %= categoriesAdapter.numPages
            }

            presetsViewModel.selectedCategory.value?.let { selectedCategory ->
                for (i in targetPosition until targetPosition + categoriesAdapter.numPages) {
                    val pageCategories = categoriesAdapter.getItemsByPosition(i)

                    if (pageCategories.find { it.categoryId == selectedCategory.categoryId } != null) {
                        targetPosition = i
                        break
                    }
                }
            }

            setCurrentItem(targetPosition, false)
        }
    }

    private fun handlePhrases(phrases: List<Phrase>) {
        binding.phrasesView.apply {
            isSaveEnabled = false
            adapter = phrasesAdapter

            maxPhrases =
                if (presetsViewModel.selectedCategory.value?.categoryId == getString(R.string.category_123_id)) {
                    NumberPadFragment.MAX_PHRASES
                } else {
                    resources.getInteger(R.integer.max_phrases)
                }

            phrasesAdapter.setItems(phrases)
            // Move adapter to middle so user can scroll both directions
            val middle = phrasesAdapter.itemCount / 2
            if (middle % phrasesAdapter.numPages == 0) {
                setCurrentItem(middle, false)
            } else {
                val mod = middle % phrasesAdapter.numPages
                setCurrentItem(
                    middle + (phrasesAdapter.numPages - mod),
                    false
                )
            }
        }
    }

    inner class CategoriesPagerAdapter(fm: FragmentManager) :
        VocableFragmentStateAdapter<Category>(fm, viewLifecycleOwner.lifecycle) {

        override fun getMaxItemsPerPage(): Int = maxCategories

        override fun createFragment(position: Int) = CategoriesFragment.newInstance(getItemsByPosition(position))
    }

    inner class PhrasesPagerAdapter(fm: FragmentManager) :
        VocableFragmentStateAdapter<Phrase>(fm, viewLifecycleOwner.lifecycle) {

        override fun setItems(items: List<Phrase>) {
            super.setItems(items)
            setPagingButtonsEnabled(phrasesAdapter.numPages > 1)
        }

        private fun setPagingButtonsEnabled(enable: Boolean) {
            binding.apply {
                phrasesForwardButton.isEnabled = enable
                phrasesBackButton.isEnabled = enable
                phrasesView.isUserInputEnabled = enable
            }
        }

        override fun getMaxItemsPerPage(): Int = maxPhrases

        override fun createFragment(position: Int): Fragment {
            val phrases = getItemsByPosition(position)

            return if (presetsViewModel.selectedCategory.value?.categoryId == getString(R.string.category_123_id)) {
                NumberPadFragment.newInstance(phrases)
            } else if (presetsViewModel.selectedCategory.value?.categoryId == getString(R.string.category_my_sayings_id) && items.isEmpty()) {
                MySayingsEmptyFragment.newInstance(false)
            } else {
                PhrasesFragment.newInstance(phrases)
            }
        }

    }
}