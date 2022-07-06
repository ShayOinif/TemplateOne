package edu.shayo.templateone.navigator

import android.os.Parcelable

interface Navigator {
    fun navigate(
        param: Parcelable,
    )
}