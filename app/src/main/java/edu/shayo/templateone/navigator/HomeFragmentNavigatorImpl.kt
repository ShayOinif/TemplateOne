package edu.shayo.templateone.navigator

import androidx.navigation.NavDirections
import edu.shayo.templateone.data.MyModel
import edu.shayo.templateone.ui.home.HomeFragmentDirections
import javax.inject.Inject

class HomeFragmentNavigatorImpl @Inject constructor() : HomeFragmentNavigator {
    override fun getDirections(
        state: FragmentState,
        params: List<Any>?
    ): NavDirections? =
        try {
            when (state) {
                is HomeFragmentNextScreen ->
                    params?.let {
                        HomeFragmentDirections.actionHomeFragmentSelf(
                            params[0] as Int,
                            params[1] as MyModel,
                        )
                    }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
}