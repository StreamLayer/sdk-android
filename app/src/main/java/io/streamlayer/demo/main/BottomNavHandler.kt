package io.streamlayer.demo.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.streamlayer.demo.R
import io.streamlayer.demo.databinding.LayoutMainContentBinding
import io.streamlayer.demo.utils.gone
import io.streamlayer.demo.utils.setDrawable
import io.streamlayer.demo.utils.visible

class BottomNavHandler(private val binding: LayoutMainContentBinding) :
    BottomNavigationView.OnNavigationItemSelectedListener, NavController.OnDestinationChangedListener {

    private val navController: NavController by lazy { binding.navHostFragment.findNavController() }

    init {
        with(binding.bnv) {
            itemIconTintList = null
            titleColor(R.id.extraFragment, R.color.menu_item_dark)
        }
        navController.addOnDestinationChangedListener(this)
    }

    private var isDarkTheme: Boolean = false

    override fun onDestinationChanged(c: NavController, d: NavDestination, a: Bundle?) {
        //set bottom nav color
        setBottomNavColor(d.id)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        navController.navigate(item.itemId)
        return true
    }

    private fun setBottomNavColor(id: Int) {
        Log.d("BottomNavHandler", "item id $id")
        var toolbarColor: Int = 0
        when (id) {
            R.id.extraFragment -> {
                toolbarColor = android.R.color.transparent
                binding.bnv.setTheme(true)
            }
            R.id.homeFragment, R.id.scoreFragment, R.id.moreFragment, R.id.playerActivity -> {
                toolbarColor = android.R.color.black
                binding.bnv.setTheme(false)
            }
        }
        binding.toolbar.setBackgroundColor(ContextCompat.getColor(binding.root.context, toolbarColor))
        //set title iv and tv
        with(binding) {
            downloadIB.gone()
            toolbar.menu.findItem(R.id.menu_calendar).isVisible = false
            titleIV.gone()
            titleTV.gone()
            when (id) {
                R.id.moreFragment -> {
                    titleTV.setText(R.string.menu_more)
                    titleTV.visible()
                }
                R.id.homeFragment -> {
                    titleIV.setDrawable(R.drawable.ic_logo)
                }
                R.id.extraFragment -> {
                    toolbar.setBackgroundColor(
                        ContextCompat.getColor(
                            binding.root.context,
                            android.R.color.transparent
                        )
                    )
                    titleIV.setDrawable(R.drawable.ic_stars)
                    downloadIB.visible()
                    toolbar.menu.findItem(R.id.menu_calendar).isVisible = true
                }
                R.id.scoreFragment -> {
                    titleTV.setText(R.string.menu_scores)
                    titleTV.visible()
                }
            }
        }
    }

    private fun BottomNavigationView.setTheme(isDark: Boolean) {
        if (isDark == isDarkTheme) return
        isDarkTheme = isDark
        setBackgroundColor(
            ContextCompat.getColor(
                this.context,
                if (isDark) R.color.bottom_menu_dark else R.color.bottom_menu_light
            )
        )
        menu.findItem(R.id.scoreFragment).icon = ContextCompat.getDrawable(
            this.context,
            if (isDark) R.drawable.ic_score_dark else R.drawable.ic_score_light
        )
    }

    @SuppressLint("RestrictedApi")
    private fun BottomNavigationView.titleColor(@IdRes idRes: Int, @ColorRes colorId: Int) {
        val view = this.getChildAt(0)
        if (view is BottomNavigationMenuView) {
            for (t in 0 until view.childCount) {
                val child = view.getChildAt(t)
                if (child is BottomNavigationItemView) {
                    val id = child.itemData.itemId
                    if (id == idRes) {
                        child.setTextColor(ContextCompat.getColorStateList(this.context, colorId))
                        return
                    }
                }
            }
        }
    }
}