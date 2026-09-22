package com.haha.splash

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivitySplashLandingBinding

/**
 * 开屏点击后的站内落地页（Demo 打开百度），不拉起系统浏览器。
 */
class SplashAdLandingActivity : BaseMvvmActivity<ActivitySplashLandingBinding, BaseViewModel>() {

    override fun getLayoutId(): Int = R.layout.activity_splash_landing

    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    @SuppressLint("SetJavaScriptEnabled")
    override fun initView() {
        mViewDataBinding.tvLandingClose.setOnClickListener { finish() }
        val webView = mViewDataBinding.webSplashLanding
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        val url = intent.getStringExtra(EXTRA_URL)?.takeIf { it.isNotBlank() }
            ?: SplashAdCache.DEFAULT_LANDING
        webView.loadUrl(url)
    }

    override fun initData() = Unit

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val webView = mViewDataBinding.webSplashLanding
        if (webView.canGoBack()) {
            webView.goBack()
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        if (isBindingInitialized()) {
            mViewDataBinding.webSplashLanding.apply {
                stopLoading()
                (parent as? ViewGroup)?.removeView(this)
                destroy()
            }
        }
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_URL = "extra_splash_landing_url"

        fun start(context: Context, url: String) {
            context.startActivity(
                Intent(context, SplashAdLandingActivity::class.java)
                    .putExtra(EXTRA_URL, url),
            )
        }
    }
}
