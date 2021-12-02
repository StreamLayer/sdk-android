package io.streamlayer.demo.common.mvvm

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import io.streamlayer.sdk.R

abstract class BaseFragment(@LayoutRes val contentLayoutId: Int) : Fragment(contentLayoutId) {

    private var progressDialog: Dialog? = null

    override fun onDestroyView() {
        showLoader(false)
        super.onDestroyView()
    }

    protected fun showLoader(show: Boolean) {
        if (show) {
            if (progressDialog?.isShowing == true) return
            if (progressDialog == null) progressDialog = requireContext().buildLoaderDialog()
            progressDialog?.show()
        } else progressDialog?.let {
            it.dismiss()
            progressDialog = null
        }
    }
}

fun Context.buildLoaderDialog(): Dialog = Dialog(this, R.style.StreamLayerSDK_LoadingDialog).apply {
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    setContentView(R.layout.slr_dialog_loader)
    window?.setFlags(
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    )
    setCancelable(false)
}