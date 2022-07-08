package edu.shayo.templateone.navigator

import androidx.navigation.NavDirections

interface HomeFragmentNavigator {
    fun getDirections(state: FragmentState, params: List<Any>?) : NavDirections?
}