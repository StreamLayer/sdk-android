package io.streamlayer.demo.common.mvvm

import android.view.View

interface BaseView {

    fun showError(errorMessage: String)

    fun showError(stringResourceId: Int)

    fun showLoader(show: Boolean)

    fun showShortInfo(info: String)

    fun showShortInfo(stringResourceId: Int)

    fun hideKeyboard(view: View)
}