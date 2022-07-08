package edu.shayo.templateone.navigator

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import edu.shayo.templateone.ui.home.HomeFragment
import java.lang.ref.WeakReference
import javax.inject.Inject

class NavigatorImpl @Inject constructor(
    private val homeFragmentNavigator: HomeFragmentNavigator
) : Navigator {
    private var fragment: WeakReference<Fragment>? = null
    private var state: FragmentState? = null
    private var params: List<Any>? = null

    override fun from(fragment: Fragment): Navigator {
        this.fragment = WeakReference(fragment)

        return this
    }

    override fun withState(state: FragmentState): Navigator {
        this.state = state

        return this
    }

    override fun withParams(params: List<Any>): Navigator {
        this.params = params

        return this
    }

    override fun commit() {
        fragment?.apply {
            get()?.apply {
                state?.let { currentState ->
                    val directions = when (this) {
                        is HomeFragment -> {
                            homeFragmentNavigator.getDirections(currentState, params)
                        }
                        else -> null
                    }

                    directions?.let {
                        findNavController().navigate(directions)
                    }
                }

            }
        }

        cleanUp()
    }

    private fun cleanUp() {
        fragment = null
        state = null
        params = null
    }
}