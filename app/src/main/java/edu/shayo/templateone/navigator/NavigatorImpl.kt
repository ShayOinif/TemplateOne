package edu.shayo.templateone.navigator

import android.os.Parcelable
import javax.inject.Inject

class NavigatorImpl @Inject constructor() : Navigator {
    private var currentFragment = CurrentFragment.HOME_FRAGMENT

    //override lateinit var navController: NavController

    override fun navigate(param: Parcelable) {
        when (currentFragment) {
            CurrentFragment.HOME_FRAGMENT -> {
                currentFragment = CurrentFragment.SECOND_FRAGMENT

               /* navController.navigate(
                    HomeFragmentDirections.actionHomeFragmentToSecondFragment(
                        param as MyModel
                    )
                )*/
            }
            CurrentFragment.SECOND_FRAGMENT -> {}
        }
    }
}

enum class CurrentFragment {
    HOME_FRAGMENT,
    SECOND_FRAGMENT
}

data class NavigationParams(val param: Parcelable)