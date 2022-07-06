package edu.shayo.templateone.ui.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.shayo.templateone.navigator.Navigator
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val navigator: Navigator
) : ViewModel() {
    /*fun navigate() {
        val myModel = MyModel("Test")

        navigator.navigate(myModel)
    }*/
}