package edu.shayo.templateone.navigator

import androidx.fragment.app.Fragment

interface Navigator {
    fun from(fragment: Fragment): Navigator

    fun withState(state: FragmentState): Navigator

    fun withParams(params: List<Any>): Navigator

    fun commit()
}

sealed class FragmentState

object HomeFragmentNextScreen : FragmentState()

object ExampleAnotherFragmentState : FragmentState()