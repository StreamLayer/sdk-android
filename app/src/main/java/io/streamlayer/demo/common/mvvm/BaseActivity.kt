package io.streamlayer.demo.common.mvvm

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

private const val TAG = "BaseActivity"

abstract class BaseActivity : AppCompatActivity(), BaseView, HasAndroidInjector {

    // DI
    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector

    //@Inject
    //lateinit var viewModelFactory: ViewModelProvider.Factory

    /**
     * Lifecycle
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun showError(errorMessage: String) {
        showShortInfo(errorMessage)
    }

    override fun showError(stringResourceId: Int) {
        showShortInfo(getString(stringResourceId))
    }

    override fun showShortInfo(info: String) {
        Toast.makeText(this, info, Toast.LENGTH_SHORT).show()
    }

    override fun showShortInfo(stringResourceId: Int) {
        showShortInfo(getString(stringResourceId))
    }

    override fun showLoader(show: Boolean) {
        Log.i(TAG, "Loading: $show")
    }

    override fun hideKeyboard(view: View) {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).apply {
            hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}