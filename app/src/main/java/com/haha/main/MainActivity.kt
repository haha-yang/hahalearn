package com.haha.main

//import com.haha.hahalearn.IProcessStub
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.Service
import android.app.usage.NetworkStatsManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.transition.Slide
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.GsonUtils
import com.haha.animation.AnimationActivity
import com.haha.baseui.BaseActivity
import com.haha.bean.SubDeviceResultBean
import com.haha.binder.TestService
import com.haha.binding.BindingAdapterActivity
import com.haha.bluetooth.BluetoothActivity
import com.haha.contract.router.RoutePath
import com.haha.coroutineScope.CoroutineScopeActivity
import com.haha.coroutineScope.EatGame
import com.haha.dynamicTextView.DynamicTextViewActivity
import com.haha.easyswipemenulayout.EasySwipeMenuActivity
import com.haha.flutter.FlutterIntegrationActivity
import com.haha.gps.GpsActivity
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityMainBinding
import com.haha.kmp.KmpLearnActivity
import com.haha.liveData.LiveDataActivity
import com.haha.log.DOFLogUtil
import com.haha.main.broadcast.MyReceiver
import com.haha.main.broadcast.MyReceiver1
import com.haha.main.glide.SinglePreviewActivity
import com.haha.main.logMonitor.UiPerfMonitor
import com.haha.main.network.NetworkIp
import com.haha.main.pieChartView.CircleProgressView
import com.haha.main.timeMonitor.TimeMonitorConfig
import com.haha.main.timeMonitor.TimeMonitorManager
import com.haha.mviFrame.main.MainMVIActivity
import com.haha.network.NetworkActivity
import com.haha.recyclerview.RecyclerViewActivity
import com.haha.scene.CustomSceneFirstActivity
import com.haha.scene.SceneFirstActivity
import com.haha.selector.SelectorActivity
import com.haha.service.accessibility.CustomAccessibilityService
import com.haha.servicerouter.core.DOFRouter
import com.haha.splash.SplashAd
import com.haha.splash.SplashAdCache
import com.haha.splash.SplashAdLandingActivity
import com.haha.splash.SplashAdOverlay
import com.haha.splash.SplashAdSession
import com.haha.transparency.TransparencyActivity
import com.haha.util.DpOrSpToPxTransfer
import com.haha.util.PermissionUtils
import com.haha.volume.ui.VolumeActivity
import com.haha.waterfall.WaterFallActivity
import com.haha.wifi.WifiActivity


class MainActivity : BaseActivity(), View.OnClickListener {

    companion object {
        private const val KEY_SPLASH_SHOWING = "key_splash_showing"
        private const val KEY_SPLASH_REMAIN_SEC = "key_splash_remain_sec"
    }

    /**
     * A native method that is implemented by the 'HahaLearn' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String?

    private val mAccessibilityManager: AccessibilityManager by lazy {
        getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }

    private val mConnectivityManager: ConnectivityManager by lazy {
        getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private var mNetworkIp: NetworkIp? = null

    /** 品牌系统 Splash 只盖进程创建，开屏层挂上后立刻放开。 */
    private var splashBrandReleased = false
    private var splashRestoreState: Bundle? = null
    private var homeContainer: FrameLayout? = null
    private var splashAdCache: SplashAdCache? = null
    private var splashOverlay: SplashAdOverlay? = null
    private var _binding: ActivityMainBinding? = null
    private val mViewDataBinding: ActivityMainBinding
        get() = _binding ?: error("首页 binding 未就绪")
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null

    val config: EatGame by lazy(LazyThreadSafetyMode.NONE) {
        EatGame() // 非线程安全，但初始化更快
    }

    private val receiver = MyReceiver()
    private val receiver1 = MyReceiver1()
    private var isBroadcastReceiverRegistered = false

    private var mWifiConnectCallback: ConnectivityManager.NetworkCallback? = null
    private var mAccessibilityStateListener: AccessibilityManager.AccessibilityStateChangeListener? =
        null

    override fun getLayoutId(): Int = R.layout.activity_main

    override fun shouldInflateContentInOnCreate(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        splashRestoreState = savedInstanceState
        setTheme(R.style.Theme_App_Starting)
        installSplashScreen().setKeepOnScreenCondition { !splashBrandReleased }
        super.onCreate(savedInstanceState)
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                handlePermissionResult(result)
            }
    }

    override fun hideTitleAndActionBar() {
        // Android 12 以上由 SplashScreen 处理标题栏
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            super.hideTitleAndActionBar()
        }
    }

    override fun extraConfig() {
        super.extraConfig()
        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("AppStartActivity_create")
    }

    override fun onWindowReady() {
        val container = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        homeContainer = container
        setContentView(container)

        val cache = SplashAdCache(this)
        splashAdCache = cache
        attachCachedSplashIfNeeded(container, cache)

        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("AsyncLayoutInflater_start")
        // 首页在开屏底下预热；等待期由开屏层 / windowBackground 顶住
        AsyncLayoutInflater(this).inflate(R.layout.activity_main, container) { view, _, _ ->
            if (isFinishing || isDestroyed) {
                return@inflate
            }
            TimeMonitorManager.getInstance()
                .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
                .recodingTimeTag("AsyncLayoutInflater_callback")
            bindHomeContentView(view, container)
            TimeMonitorManager.getInstance()
                .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
                .recodingTimeTag("AsyncLayoutInflater_bind_done")
            onHomeContentReady()
            runResumeBindingTasks()
            TimeMonitorManager.getInstance()
                .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
                .end("AppStartActivity_contentReady", false)
        }

        splashBrandReleased = true
        registerSplashBackCallback()
    }

    private fun attachCachedSplashIfNeeded(container: FrameLayout, cache: SplashAdCache) {
        val restore = splashRestoreState
        val restoreShowing = restore?.getBoolean(KEY_SPLASH_SHOWING, false) == true
        val restoreRemain = restore?.getInt(KEY_SPLASH_REMAIN_SEC, 0) ?: 0
        val ad = cache.getReadyAd() ?: return
        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("SplashAd_show")
        cache.markShown()
        val overlay = SplashAdOverlay(this, lifecycleScope)
        overlay.callback = object : SplashAdOverlay.Callback {
            override fun onSkip() = onSplashClosed("skip")
            override fun onTimeout() = onSplashClosed("timeout")
            override fun onAdClick(ad: SplashAd) {
                onSplashClosed("click")
                SplashAdLandingActivity.start(this@MainActivity, ad.landingUrl)
            }
        }
        splashOverlay = overlay
        overlay.attach(
            parent = container,
            ad = ad,
            remainingSec = if (restoreShowing) restoreRemain else null,
        )
    }

    private fun onSplashClosed(reason: String) {
        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("SplashAd_close_$reason")
        SplashAdSession.dismissed = true
        splashAdCache?.preloadNext()
        splashOverlay = null
        if (_binding != null) {
            launchHomePermissions()
        }
    }

    private fun bindHomeContentView(contentView: View, container: FrameLayout) {
        _binding = DataBindingUtil.bind(contentView)
            ?: error("DataBinding bind 失败，确认 activity_main 根节点是 <layout>")
        _binding?.lifecycleOwner = this
        container.addView(
            contentView,
            0,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        handleNavigationVAndStatusVisibility()
    }

    private fun onHomeContentReady() {
        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("MainActivity_homeReady")
        mViewDataBinding.root.background = ContextCompat.getDrawable(mContext, R.color.white)
        if (!isShowStatus()) {
            addStatusBarView()
        }
        if (splashOverlay?.isShowing != true) {
            launchHomePermissions()
        }
        initView()
        initData()
    }

    private fun launchHomePermissions() {
        permissionLauncher?.launch(requestPermissionArray())
    }

    private fun registerSplashBackCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val overlay = splashOverlay
                if (overlay?.isShowing == true) {
                    overlay.skip()
                    return
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val showing = splashOverlay?.isShowing == true
        outState.putBoolean(KEY_SPLASH_SHOWING, showing)
        if (showing) {
            outState.putInt(KEY_SPLASH_REMAIN_SEC, splashOverlay?.remainingSec ?: 0)
        }
    }

    private fun initView() {
        mViewDataBinding.btnSkipGps.setOnClickListener(this)
        mViewDataBinding.btnSkipRemoteView.setOnClickListener(this)
        mViewDataBinding.btnSkipNetwork.setOnClickListener(this)
        mViewDataBinding.btnRefresh.setOnClickListener(this)
        mViewDataBinding.btnTest.setOnClickListener(this)
        mViewDataBinding.btnTest1.setOnClickListener(this)
        mViewDataBinding.btnPermission.setOnClickListener(this)
        mViewDataBinding.btnDialogFragment.setOnClickListener(this)
        mViewDataBinding.btnAnimation.setOnClickListener(this)
        mViewDataBinding.btnActivityScene.setOnClickListener(this)
        mViewDataBinding.btnActivityCoroutineScope.setOnClickListener(this)
        mViewDataBinding.btnActivityCustomScene.setOnClickListener(this)
        mViewDataBinding.btnActivityCustomScene2.setOnClickListener(this)
        mViewDataBinding.btnActivityRecyclerview.setOnClickListener(this)
        mViewDataBinding.btnActivityWaterfall.setOnClickListener(this)
        mViewDataBinding.btnActivitySelector.setOnClickListener(this)
        mViewDataBinding.btnActivityVolume.setOnClickListener(this)
        mViewDataBinding.btnActivitySpeechCaption.setOnClickListener(this)
        mViewDataBinding.btnActivityLlmChat.setOnClickListener(this)
        mViewDataBinding.btnActivitySlide.setOnClickListener {
            val intent = Intent(mContext, EasySwipeMenuActivity::class.java)
            startActivity(intent)
        }
        mViewDataBinding.btnActivityBluetooth.setOnClickListener {
            val intent = Intent(mContext, BluetoothActivity::class.java)
            intent.putExtras(Intent())
            startActivity(intent)
        }
        // CircleProgressView / PieChartView 改由 ViewStub 首帧后加载，见 inflateHeavyCustomViews()
        mViewDataBinding.btnActivityDynamic.setOnClickListener {
            val intent = Intent(mContext, DynamicTextViewActivity::class.java)
            startActivity(intent)
        }
//        val picList = mutableListOf<PieChartBean>()
//        val colors: IntArray = intArrayOf(
//            ContextCompat.getColor(mContext, R.color.circle_gradual_end),
//            ContextCompat.getColor(mContext, R.color.circle_gradual_end)
//        )
//        val gradient = LinearGradient(0f, 0f, 100f, 100f, colors, null, Shader.TileMode.CLAMP)
//        picList.add(PieChartBean("0", 25f, gradient))
//        picList.add(PieChartBean("1", 25f, gradient))
//        picList.add(PieChartBean("2", 25f, gradient))
//        picList.add(PieChartBean("3", 25f, gradient))
//        mViewDataBinding.csv.setDate(picList)
        mViewDataBinding.btnActivityLiveData.setOnClickListener {
            startActivity(Intent(mContext, LiveDataActivity::class.java))
        }
        mViewDataBinding.btnActivityRecyclerView.setOnClickListener {
            startActivity(Intent(mContext, RecyclerViewActivity::class.java))
        }
        mViewDataBinding.btnActivityRecyclerView.post {
            val options = BitmapFactory.Options()
            options.inMutable = true
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.net_ic_phone, options)
            bitmap.setConfig(Bitmap.Config.RGB_565)
            val byteCount = bitmap.byteCount // 直接获取内存占用字节数
            DOFLogUtil.d("Memory", "Bitmap size: $byteCount bytes")
        }
        mViewDataBinding.btnActivityPlugin.setOnClickListener(this)
        mViewDataBinding.btnUiMonitor.setOnClickListener {
            //TODO 目前版本问题，导致获取权限不到，无法使用，后续优化
            changeMonitorPerf()
        }
        mViewDataBinding.btnActivityMviFrame.setOnClickListener(this)
        mViewDataBinding.btnActivityFlutterIntegration.setOnClickListener(this)
        mViewDataBinding.btnActivityKmpLearn.setOnClickListener(this)
        mViewDataBinding.btnActivityBindingAdapter.setOnClickListener(this)

        // 首帧后再加载自定义重控件，缩短 activity_main inflate 时间
        mViewDataBinding.root.post {
            inflateHeavyCustomViews()
        }

        val builder =
            SpannableStringBuilder("请注意将温度传感器靠近网关\n若长时间未搜索到，可尝试重新操作设备")
        val str = "重新操作设备"
        val span = ForegroundColorSpan(Color.RED)
        builder.setSpan(object : ClickableSpan() {
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.RED
                ds.isUnderlineText = false
            }

            override fun onClick(widget: View) {
                Toast.makeText(this@MainActivity, "nihao", Toast.LENGTH_SHORT).show()
            }

        }, builder.length - str.length, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        mViewDataBinding.tvSpan.text = builder
        mViewDataBinding.tvSpan.movementMethod = LinkMovementMethod.getInstance()

        /**
        if (isSatisfiedAndroidVersion(Build.VERSION_CODES.R)) {
        mConnectivityDiagnosticsManager = getSystemService(Context.CONNECTIVITY_DIAGNOSTICS_SERVICE) as ConnectivityDiagnosticsManager
        val request = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        mConnectivityDiagnosticsManager.registerConnectivityDiagnosticsCallback(
        request,
        Executors.newSingleThreadExecutor(),
        mConnectivityDiagnodsticsCallback
        )

        mNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
        super.onBlockedStatusChanged(network, blocked)
        DOFLogUtil.d("haha", "onBlockedStatusChanged Network = $network \t blocked = $blocked")
        }

        override fun onAvailable(network: Network) {
        super.onAvailable(network)
        DOFLogUtil.d("haha", "onAvailable Network = $network")
        }

        override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities
        ) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        DOFLogUtil.d(
        "haha",
        "onCapabilitiesChanged Network = $network \t networkCapabilities = $networkCapabilities"
        )
        }

        override fun onLinkPropertiesChanged(
        network: Network,
        linkProperties: LinkProperties
        ) {
        super.onLinkPropertiesChanged(network, linkProperties)
        val addresses = linkProperties.linkAddresses;
        var hostAddress: String? = null
        for (address in addresses) {
        if (address.address.hostAddress.contains(".") == true) {
        hostAddress = address.address.hostAddress
        DOFLogUtil.d(TAG, "hostAddress = $hostAddress")
        break
        }
        }
        DOFLogUtil.d(TAG, "linkProperties: ${linkProperties.linkAddresses}")
        pingforInetAddresss(hostAddress ?: "")
        //                pingForCMD("163.177.151.110")
        DOFLogUtil.d(
        "haha",
        "onLinkPropertiesChanged Network = $network \t linkProperties = $linkProperties"
        )
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
        super.onLosing(network, maxMsToLive)
        DOFLogUtil.d("haha", "onLosing Network = $network \t maxMsToLive = $maxMsToLive")
        }

        override fun onLost(network: Network) {
        super.onLost(network)
        DOFLogUtil.d("haha", "onLost Network = $network")
        }

        override fun onUnavailable() {
        super.onUnavailable()
        DOFLogUtil.d("haha", "onUnavailable")
        }
        }

        mConnectivityManager.registerNetworkCallback(request, mNetworkCallback)
        }

        val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermissions()
        ) { permissions ->
        when {
        permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
        // Precise location access granted.
        }
        permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
        // Only approximate location access granted.
        } else -> {
        // No location access granted.
        }
        }
        }
        locationPermissionRequest.launch(arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION))
         **/
    }

    private fun testService() {
        val intent = Intent(this, TestService::class.java)
        bindService(intent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//                val stub = IProcessStub.Stub.asInterface(service)
//                try {
//                    stub.request(" ")
//                } catch (e: RemoteException) {
//                    e.printStackTrace()
//                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {

            }

        }, Context.MODE_PRIVATE.or(Context.BIND_AUTO_CREATE))
    }

    private fun changeMonitorPerf() {
        if (UiPerfMonitor.getInstance().isMonitoring()) {
            UiPerfMonitor.getInstance().stopMonitor()
            mViewDataBinding.btnUiMonitor.text = resources.getText(R.string.monitor_control_start)
        } else {
            UiPerfMonitor.getInstance().startMonitor()
            mViewDataBinding.btnUiMonitor.text = resources.getText(R.string.monitor_control_stop)
        }
    }

    private fun requestPermissionArray(): Array<String> {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        }
    }

    private fun handlePermissionResult(permissionResultMap: Map<String, Boolean>) {
        permissionResultMap.forEach { (k, v) ->
            DOFLogUtil.d(TAG, "$k ----->>>>>  $v")
        }
    }

    /**
     * 延迟 inflate 自定义重控件（ViewStub），避免拖慢首页 setContentView。
     * DataBinding 对 ViewStub 生成的是 ViewStubProxy。
     */
    private fun inflateHeavyCustomViews() {
        val stubCpv = mViewDataBinding.stubCpv
        if (!stubCpv.isInflated) {
            val cpv = stubCpv.viewStub?.inflate() as? CircleProgressView
            cpv?.apply {
                setProgress(20f)
                setCircleBgColor(Color.RED)
                setProgressColor(Color.BLACK)
                setCenterText("哈哈哈哈")
                setCenterTextColor(Color.GREEN)
                setCenterTextSize(DpOrSpToPxTransfer.sp2px(mContext, 18).toFloat())
                invalidate()
            }
        }

        val stubPcv = mViewDataBinding.stubPcv
        if (!stubPcv.isInflated) {
            stubPcv.viewStub?.inflate()
        }
    }

    private fun initData() {
        setupWindowAnimations()

        val filter = IntentFilter("com.haha.main.broadcast.intent.action.MyReceiver")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
            registerReceiver(receiver1, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
            registerReceiver(receiver1, filter)
        }
        isBroadcastReceiverRegistered = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            accessibilityTest()
        }
        DOFLogUtil.d(TAG, "C++ = ${stringFromJNI()}")
//        Thread {
//            synchronizedTest()
//        }.start()

        // TODO 该Service存在内存泄漏
//        testService()

        TimeMonitorManager.getInstance()
            .getTimeMonitor(TimeMonitorConfig.TIME_MONITOR_ID_APPLICATION_START)
            .recodingTimeTag("AppStartActivity_createOver")
    }

    @Synchronized
    private fun synchronizedTest() {
        synchronized(this) {
            val startTime = System.currentTimeMillis()
            DOFLogUtil.d(TAG, "synchronizedTest: start time = $startTime")
            Thread.sleep(21000)
            DOFLogUtil.d(
                TAG,
                "synchronizedTest: end time = ${System.currentTimeMillis() - startTime}"
            )
        }
    }

    override fun onStart() {
        super.onStart()
        // 首页走异步 inflate，完整耗时在 contentReady 回调里 end，避免 onStart 时内容尚未就绪
    }

    override fun onResume() {
//        synchronizedTest()
        super.onResume()
        runResumeBindingTasks()
    }

    private fun runResumeBindingTasks() {
        if (_binding == null) {
            return
        }
        TestLearnUtils.test(mContext)
        TestLearnUtils.test(mViewDataBinding.btnSkipGps)
        TestLearnUtils.test()
    }

    override fun getStatusBarColor(): Int {
        return R.color.transparent
    }

    override fun getNavigationBarColor(): Int {
        return R.color.transparent
    }

    override fun isBlackStatusText(): Boolean {
        return true
    }

    override fun isShowStatus(): Boolean {
        return false
    }

    override fun isShowNavigation(): Boolean {
        return false
    }

    override fun getRootViewId(): Int {
        return R.id.cl_main
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun accessibilityTest() {
//        mViewDataBinding.btnSkipGps.isEnabled = true
//        mViewDataBinding.btnSkipNetwork.isEnabled = true
//        mViewDataBinding.btnSkipRemoteView.isEnabled = true
//        mViewDataBinding.btnSkipRefresh.isEnabled = true
//        mViewDataBinding.btnSkipGps.isFocusable = true
//        mViewDataBinding.btnSkipNetwork.isFocusable = true
//        mViewDataBinding.btnSkipRemoteView.isFocusable = true
//        mViewDataBinding.btnSkipRefresh.isFocusable = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            mViewDataBinding.btnSkipGps.focusable = View.FOCUSABLE
//            mViewDataBinding.btnSkipNetwork.focusable = View.FOCUSABLE
//            mViewDataBinding.btnSkipRemoteView.focusable = View.FOCUSABLE
//            mViewDataBinding.btnSkipRefresh?Network.focusable = View.FOCUSABLE

//            val event = AccessibilityEvent()
//            event.eventType = AccessibilityEvent.TYPE_VIEW_FOCUSED
//            mViewDataBinding.btnSkipGps.onInitializeAccessibilityEvent(event)

//            val btnSkipGPSNode = mViewDataBinding.btnSkipGps.createAccessibilityNodeInfo()
//            val btnSkipNetwork = mViewDataBinding.btnSkipNetwork.createAccessibilityNodeInfo()
//            val btnSkipRemoteView = mViewDataBinding.btnSkipRemoteView.createAccessibilityNodeInfo()
//            val btnRefreshNetwork = mViewDataBinding.btnSkipRefresh.createAccessibilityNodeInfo()
//
//            btnSkipGPSNode.setTraversalAfter(mViewDataBinding.btnSkipNetwork?)
//            btnSkipNetwork.setTraversalAfter(mViewDataBinding.btnSkipRemoteView?)
//            btnSkipRemoteView.setTraversalAfter(mViewDataBinding.btnSkipRefresh?)

//            mViewDataBinding.btnSkipGps.accessibilityTraversalBefore = R.id.btn_skip_network
//            mViewDataBinding.btnSkipNetwork.accessibilityTraversalBefore = R.id.btn_skip_remote_view
//            mViewDataBinding.btnSkipRemoteView.accessibilityTraversalBefore = R.id.btn_refresh

//            if (mAccessibilityManager.isEnabled) {
//                mViewDataBinding.btnSkipGps.sendAccessibilityEventUnchecked(event)
//            }
        }

//        mViewDataBinding.btnSkipGps.findUserSetNextFocus(mViewDataBinding.clMain?, View.FOCUS_DOWN)


//        mViewDataBinding.btnSkipGps.nextFocusDownId = R.id.btn_skip_network
//        mViewDataBinding.btnSkipGps.nextFocusDownId = R.id.btn_skip_network
//        mViewDataBinding.btnSkipNetwork.nextFocusDownId = R.id.btn_skip_remote_view
//        mViewDataBinding.btnSkipNetwork.nextFocusDownId = R.id.btn_skip_remote_view
//        mViewDataBinding.btnSkipNetwork.nextFocusUpId = R.id.btn_skip_gps
//        mViewDataBinding.btnSkipRemoteView.nextFocusUpId = R.id.btn_skip_network
//        mViewDataBinding.btnSkipRemoteView.nextFocusDownId = R.id.btn_refresh

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            val btnSkipGPSNodeInfo = AccessibilityNodeInfo(mViewDataBinding.btnSkipGps?)
//            val btnSkipRemoteViewNodeInfo = AccessibilityNodeInfo(mViewDataBinding.btnSkipRemoteView?)
//            val btnSkipNetworkNodeInfo = AccessibilityNodeInfo(mViewDataBinding.btnSkipNetwork?)
//            btnSkipGPSNodeInfo.setTraversalAfter(mViewDataBinding.btnSkipNetwork?)
//            btnSkipNetworkNodeInfo.setTraversalBefore(mViewDataBinding.btnSkipGps?)
//            btnSkipNetworkNodeInfo.setTraversalAfter(mViewDataBinding.btnSkipRemoteView?)
//            btnSkipRemoteViewNodeInfo.setTraversalBefore(mViewDataBinding.btnSkipNetwork?)
//
//            DOFLogUtil.d(TAG, "btnSkipGPSNodeInfo = $btnSkipGPSNodeInfo")
//        } else {
//            DOFLogUtil.d(TAG, "version = ${Build.VERSION.SDK_INT}")
//        }

        /*
        val instance: BaseService = BaseService.getInstance()
        instance.init(this)
        if (!instance.checkAccessibilityEnabled("暗黑魔心的无障碍服务")) {
            instance.goAccess()
        }
         */
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun test1() {
        val binding = _binding ?: return
        val accessibilityNodeInfo = AccessibilityNodeInfo(binding.clMain)
        val list = accessibilityNodeInfo.actionList

        list.let {
            for (accessibilityAction in list) {
                DOFLogUtil.d(
                    TAG,
                    "id = ${accessibilityAction.id}, label = ${accessibilityAction.label}"
                )
            }
        }

        val mBtnGPSProvider = object : AccessibilityNodeProvider() {
            override fun createAccessibilityNodeInfo(virtualViewId: Int): AccessibilityNodeInfo? {
                return mViewDataBinding.btnSkipGps.let { AccessibilityNodeInfo(it) }
            }

            override fun findFocus(focus: Int): AccessibilityNodeInfo? {
                return mViewDataBinding.btnSkipGps.let { AccessibilityNodeInfo(it) }
            }
        }
        mBtnGPSProvider.createAccessibilityNodeInfo(AccessibilityNodeProvider.HOST_VIEW_ID)
    }

    /**
     * 启动无障碍服务
     */
    private fun startAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun setupWindowAnimations() {
//        val slide: Slide =
//            TransitionInflater.from(this).inflateTransition(R.transition.activity_slide) as Slide
        val slide = Slide()
        slide.duration = 1000
        window.exitTransition = slide
    }

    @SuppressLint("WrongConstant")
    fun getUid(context: Context): Int? {
        try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(context.packageName, PackageManager.GET_ACTIVITIES)
            DOFLogUtil.d("UID", "getUid:" + ai.uid + "\t ${context.packageName}")
            return ai.uid
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermission() {
//        val requestPermissionLauncher: RequestPermissionLauncher? = null
//        when {
//            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                    == PackageManager.PERMISSION_GRANTED -> {
//
//            }
//            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
//
//            }
//            else -> {
//                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
//            }
//        }
    }

    private fun wifiInfo() {
        if (ActivityCompat.checkSelfPermission(
                this,
                GpsActivity.ACCESS_FIND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val dialog = AlertDialog.Builder(this)
            dialog.apply {
                setTitle("请求精准定位权限")
                setMessage("获取位置经纬度需要获取精准权限")
                setPositiveButton("同意") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(GpsActivity.ACCESS_FIND_LOCATION),
                        200
                    )
                }
                setNegativeButton("拒绝", null)
            }
            dialog.show()
            return
        }

        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val list = wifiManager.scanResults
        for (i in list.indices) {
            val scanResult = list[i]
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                DOFLogUtil.d(
                    "haha", "ssid = " + scanResult.SSID +
                            ", capabilities = " + scanResult.capabilities + ", wifiStandard = " + scanResult.wifiStandard
                )
            }
        }
    }

    private fun connect(ssid: String, context: Context) {
        DOFLogUtil.i("haha", "try connect to $ssid")
        val appContext = context.applicationContext
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nr = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(ssid)
            .build()

        // 先注销旧回调，避免重复注册导致泄漏
        mWifiConnectCallback?.let {
            try {
                cm.unregisterNetworkCallback(it)
            } catch (_: Exception) {
            }
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                DOFLogUtil.i("haha", "onAvailable $network ${network.javaClass.name}")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                try {
                    DOFLogUtil.i(
                        "haha",
                        "onCapabilitiesChanged ${networkCapabilities.linkDownstreamBandwidthKbps}"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                try {
                    DOFLogUtil.i("haha", "onLinkPropertiesChanged " + linkProperties.interfaceName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        mWifiConnectCallback = callback
        DOFLogUtil.i("haha", "requestNetwork!")
        cm.registerNetworkCallback(nr, callback)
    }

    private fun inspectNetworks() {
        val connectivity = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val ns = connectivity.allNetworks
        for (n in ns) {
            val c = connectivity.getNetworkCapabilities(n)
            DOFLogUtil.i("haha", "inspectNetworks: network = $n \t capabilities = $c")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onClick(v: View?) {
        v?.let {
            when (v.id) {
                R.id.btn_skip_gps -> {
                    DOFRouter.create(RoutePath.GPS).navigate(this)
                    val resultList =
                        "{\"uid\":\"469f7d2494b94b28ad5ce5edb61ef632\",\"level\":\"0\",\"subDevices\":\"[{\\\"enterpriseCode\\\":\\\"0000\\\",\\\"modelId\\\":\\\"midea.switch.011.003\\\",\\\"errorCode\\\":0,\\\"subType\\\":\\\"1104\\\",\\\"sn\\\":\\\"9035EAFFFE842AEC\\\",\\\"type\\\":\\\"0x21\\\",\\\"spid\\\":10001698,\\\"deviceId\\\":177021372099829,\\\"deviceName\\\":\\\"midea\\\",\\\"errorMsg\\\":null}]\",\"transId\":\"DFB2190D5220A53DC35AAF2A1F4FD13B\",\"appId\":\"900\",\"pubTs\":\"1688024614\",\"targetUid\":\"469f7d2494b94b28ad5ce5edb61ef632\",\"exp\":\"2023-07-01 15:43:34\",\"pushTime\":\"2023-06-29 15:43:34\",\"gatewayId\":\"177021372100423\",\"pushType\":\"gateway\\/subAppliance\\/bind\"}"
                    var bean: SubDeviceResultBean? = null
                    try {
                        bean = GsonUtils.fromJson(resultList, SubDeviceResultBean::class.java)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    DOFLogUtil.d(TAG, "bean = $bean")
//                    startActivity(Intent(this, TestActivity::class.java))
                }

                R.id.btn_skip_remote_view -> {
//                    val intent = Intent(this, RemoteViewActivity::class.java);
//                    startActivity(intent)
//                    val c = getSystemService(NETWORK_POLICY_SERVICE) as NetworkPolicyManager

                    mViewDataBinding.ivSuccess.visibility = View.VISIBLE
                    val animation = AnimationUtils.loadAnimation(this, R.anim.success_up_anim)
                    mViewDataBinding.ivSuccess.startAnimation(animation)
                }

                R.id.btn_skip_network -> {
                    val intent = Intent(this, NetworkActivity::class.java);
                    startActivity(intent)
//                    mViewDataBinding.btnSkipGps.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)
//
//                    val constructor = AccessibilityEvent::class.java.getDeclaredConstructor()
//                    constructor.isAccessible = true
//                    val accessibilityEvent = constructor.newInstance()
//                    accessibilityEvent.eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
//                    accessibilityEvent.className = mViewDataBinding.btnSkipGps.javaClass.name
//                    DOFLogUtil.d(TAG, "accessibilityEvent = $accessibilityEvent")
//                    DOFLogUtil.d(
//                        TAG,
//                        "accessibilityEvent = ${mViewDataBinding.btnSkipGps.accessibilityTraversalAfter}"
//                    )
                }

                R.id.btn_refresh -> {
//                    val panelIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
//                    startActivityForResult(panelIntent, -1)
//                    runOnUiThread {
//                        isNetworkOnline()
//                    }
                    /*
                    val connectivityManager =
                        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val wifiManager =
                        applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                    val wifiInfo = wifiManager.connectionInfo
                    val wifiName = wifiInfo.ssid
                    DOFLogUtil.d("haha", wifiName)
                    DOFLogUtil.d("haha", "speed: ${wifiInfo.linkSpeed}")
                     */

                    val intent = Intent(this, CustomAccessibilityService::class.java)
                    startService(intent)
                }

                R.id.btn_test -> {
                    if (!isSatisfiedAndroidVersion(Build.VERSION_CODES.M)) return@let

                    val uid = getUid(this)

                    val wifiManager =
                        applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                    uid?.let {
                        val networkInfo =
                            mConnectivityManager.getNetworkInfo(mConnectivityManager.activeNetwork)
                        networkInfo?.let {
                            DOFLogUtil.d(
                                "haha",
                                "${it.isConnectedOrConnecting} \t ${it.detailedState}"
                            )
                        }
                        val networkCapabilities = mConnectivityManager.getNetworkCapabilities(
                            mConnectivityManager.activeNetwork
                        )

                        val b = mConnectivityManager.isDefaultNetworkActive
                        DOFLogUtil.d("haha", "isDefaultNetworkActive = $b")

                        val networkInfo1 = mConnectivityManager.activeNetworkInfo
                        networkInfo1?.let { info ->
                            DOFLogUtil.d(TAG, "detailedState = " + info.detailedState.toString())
                        }

                        if (isSatisfiedAndroidVersion(Build.VERSION_CODES.M)) {
                            mConnectivityManager.activeNetwork?.let { network ->
                                DOFLogUtil.d(TAG, "netId = $network")
                            }
                        }

                        val proxy = mConnectivityManager.defaultProxy
                        proxy?.let { proxy ->
                            DOFLogUtil.d(
                                TAG,
                                "proxy: host = ${proxy.host} \t port = ${proxy.port} \t isValid = ${proxy.isValid}"
                            )
                        }
                    }
                    val nsm = getSystemService(NETWORK_STATS_SERVICE) as NetworkStatsManager
//                    if (hasPermissionToReadNetworkStats()) {
//                        Thread {
//                            var bucket: NetworkStats.Bucket? = null
//                            try{
//                                bucket = nsm.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, "", 0, System.currentTimeMillis())
//                            } catch (e: RemoteException) {
//                                e.printStackTrace()
//                            }
//                            bucket.let {
//                                val total = it.rxBytes + it.txBytes
//                                DOFLogUtil.d("haha", "Total = $total")
//                                DOFLogUtil.d("haha", "rxBytes = ${it.rxBytes}")
//                                DOFLogUtil.d("haha", "txBytes = ${it.txBytes}")
//                            }
//                        }.start()
//                    }


                    Thread {
                        val wifiManager =
                            getApplicationContext().getSystemService(WIFI_SERVICE) as WifiManager
                        connect(wifiManager.connectionInfo.ssid, this)
                    }.start()

//        Thread {
//            val isConnect = cm.getConnectionOwnerUid(
//                IPPROTO_TCP,
//                InetSocketAddress(InetAddress.getByName("10.74.35.6"), 80),
//                InetSocketAddress(InetAddress.getByName("163.177.151.110"), 80)
//            )
//            DOFLogUtil.d("haha", "isConnect = $isConnect")
//        }.start()

                    Thread {
                        val network = mConnectivityManager.boundNetworkForProcess
                        DOFLogUtil.d("haha", "boundNetworkForProcess = $network")

                        mNetworkIp?.pingforInetAddresss("163.177.151.110")
                        mNetworkIp?.pingForCMD("163.177.151.110")
                    }.start()
                }

                R.id.btn_test1 -> {
//                    if (mNetworkIp == null) {
//                        mNetworkIp = NetworkIp()
//                    }
//                    mNetworkIp?.isWifiConnected(this, mConnectivityManager.activeNetworkInfo)
//
//                    NetWorkUtils.requestNetwork(this)
//                    val b =
//                        mConnectivityManager.bindProcessToNetwork(mConnectivityManager.activeNetwork)
//                    DOFLogUtil.d("haha", "bindProcessToNetwork: $b")
                    createMemoryChurn()
//                    testANRService()
//                    createRunnableChurn()

                    val intent = Intent()
                    intent.setClass(mContext, SinglePreviewActivity::class.java)
//                    intent.setClass(mContext, MultiPreviewActivity::class.java)
                    startActivity(intent)
                }

                R.id.btn_permission -> {
                    val intent = Intent(this, TransparencyActivity::class.java)
                    val permissions = Array(1) { Manifest.permission.ACCESS_FINE_LOCATION }
                    intent.putExtra("permissions", permissions)
                    startActivity(intent)
                }

                R.id.btn_dialog_fragment -> {
//                    val dialog = DialogFragment()
//                    dialog.let {
//                        it.isCancelable = false
//                        it.isCancelable = false
//                        it.show(supportFragmentManager, "")
//                    }

                    val intent = Intent();
                    intent.setAction("com.haha.main.broadcast.intent.action.MyReceiver")
                    sendOrderedBroadcast(intent, null)//有序广播需要用sendOrderedBroadcast()方法发送
                }

                R.id.btn_animation -> {
                    val intent = Intent(this, AnimationActivity::class.java)
//                    startActivity(
//                        intent,
//                        ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
//                    )
                    startActivity(intent)
                }

                R.id.btn_activity_scene -> {
                    val intent = Intent(this, SceneFirstActivity::class.java)
                    startActivity(intent)
                }

                R.id.btn_activity_coroutineScope -> {
                    val intent = Intent(this, CoroutineScopeActivity::class.java)
                    startActivity(intent)
                }

                R.id.btn_activity_custom_scene -> {
                    val intent = Intent(this, CustomSceneFirstActivity::class.java)
                    startActivity(intent)
                }

                R.id.btn_activity_custom_scene2 -> {
//                    val intent = Intent(this, CustomSceneSecondActivity::class.java)
//                    startActivity(intent)
//                    PermissionUtil.gotoPermission(this)
                    com.blankj.utilcode.util.PermissionUtils.launchAppDetailsSettings()
                }

                R.id.btn_activity_recyclerview -> {
                    if (!PermissionUtils.checkGPSPermission()) {
                        PermissionUtils.requestPermissions(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    } else {

                        val wifiManager =
                            applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
                        wifiManager?.let {
                            val result = it.scanResults
                        }

                        val intent = Intent(this, WifiActivity::class.java)
                        startActivity(intent)
                    }
                }

                R.id.btn_activity_waterfall -> {
                    val intent = Intent(this, WaterFallActivity::class.java)
                    startActivity(intent)
                }

                R.id.btn_activity_selector -> {
                    val intent = Intent(this, SelectorActivity::class.java)
                    startActivity(intent)
                }

                R.id.btn_activity_volume -> {
                    val intent = Intent(this, VolumeActivity::class.java)
                    startActivity(intent)
                }

                R.id.btn_activity_speech_caption -> {
                    DOFRouter.create(RoutePath.SPEECH_CAPTION).navigate(this)
                }

                R.id.btn_activity_llm_chat -> {
                    DOFRouter.create(RoutePath.LLM_CHAT).navigate(this)
                }

                R.id.btn_activity_plugin -> {
                    val intent = Intent()
                    intent.setComponent(
                        ComponentName(
                            "com.haha.pluginapp",
                            "com.haha.pluginapp.PluginActivity"
                        )
                    )
                    startActivity(intent)
                }

                R.id.btn_activity_mvi_frame -> {
                    val intent = Intent(this, MainMVIActivity::class.java)
                    startActivity(intent)
                }

                R.id.btn_activity_flutter_integration -> {
                    startActivity(Intent(this, FlutterIntegrationActivity::class.java))
                }

                R.id.btn_activity_kmp_learn -> {
                    startActivity(Intent(this, KmpLearnActivity::class.java))
                }

                R.id.btn_activity_binding_adapter -> {
                    startActivity(Intent(this, BindingAdapterActivity::class.java))
                }

                else -> {

                }
            }
        }
    }

    private fun createMemoryChurn() {
        for (i in 0..10000) {
            var result: String? = ""
            for (j in 0..99) {
                result += j // 每次+=都会创建新的StringBuilder和String
            }
        }
    }

    private fun badCollectionUsage() {
        val data: MutableList<String> = ArrayList()
        for (i in 0..9999) {
            data.add("item$i")


            // 创建临时List进行筛选
            val filtered: MutableList<String> = ArrayList()
            for (item in data) {
                if (item.contains("5")) {
                    filtered.add(item)
                }
            }
        }
    }

    private fun createRunnableChurn() {
        for (i in 0..999) {
            // 每次循环都创建新Runnable
            Handler().postDelayed({
                // do something
            }, 1000)
        }
    }

    private fun testOOM() {
        val list = mutableListOf<ByteArray>()
        try {
            while (true) {
                // 每次循环分配1MB内存
                list.add(ByteArray(1024 * 1024))
            }
        } catch (e: OutOfMemoryError) {
            // 捕获到OOM，进行后续处理或日志记录
            DOFLogUtil.e("OOM_TEST", "OutOfMemoryError caught!")
        }
    }

    private fun TestANRSleep() {
        Thread.sleep(20 * 1000)
    }

    private fun testANRLock() {
        synchronizedTest2()
    }

    private fun synchronizedTest2() {
        Thread { synchronizedInThread() }.start()
        runOnUiThread { synchronizedInMain() }
    }

    @Synchronized
    fun synchronizedInThread() {
        SystemClock.sleep(30000)
    }

    @Synchronized
    fun synchronizedInMain() {
    }

    private fun testANRService() {
        produceANRByService()
    }

    /**
     * 点击后，启动一个 Service，在 Service 内阻塞主线程
     *
     * 20s 左右会有 ANR 的log，再过 10s 左右有 ANR 的弹框
     */
    private fun produceANRByService(view: View?) {
        val intent = Intent(this, MyService::class.java)
        startService(intent)
    }

    /**
     * 点击后，阻塞主线程，再启动一个 Service
     *
     * 20s 左右会有 ANR 的log，再过 10s 左右有 ANR 的弹框
     */
    private fun produceANRByService() {
        Thread {
            SystemClock.sleep(3000)
            val intent = Intent(this, MyService::class.java)
            startService(intent)
        }.start()

        sleepTest()
    }

    private fun sleepTest() {
        SystemClock.sleep(100000)
    }


    private fun accessibilityManagerTest() {
        val accessibilityManager =
            getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        mAccessibilityStateListener?.let {
            accessibilityManager.removeAccessibilityStateChangeListener(it)
        }
        val listener = AccessibilityManager.AccessibilityStateChangeListener { enabled ->
            if (enabled) {
            }
        }
        mAccessibilityStateListener = listener
        accessibilityManager.addAccessibilityStateChangeListener(listener)
    }

    private fun unregisterBroadcastReceiversIfNeeded() {
        if (!isBroadcastReceiverRegistered) {
            return
        }
        try {
            unregisterReceiver(receiver1)
        } catch (_: IllegalArgumentException) {
        }
        try {
            unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
        }
        isBroadcastReceiverRegistered = false
    }

    override fun onDestroy() {
        mWifiConnectCallback?.let {
            try {
                mConnectivityManager.unregisterNetworkCallback(it)
            } catch (_: Exception) {
            }
            mWifiConnectCallback = null
        }
        mAccessibilityStateListener?.let {
            mAccessibilityManager.removeAccessibilityStateChangeListener(it)
            mAccessibilityStateListener = null
        }
        unregisterBroadcastReceiversIfNeeded()
        splashOverlay?.destroy()
        splashOverlay = null
        homeContainer = null
        _binding?.unbind()
        _binding = null
        super.onDestroy()
        val intent = Intent(this, CustomAccessibilityService::class.java)
        stopService(intent)

        val testIntent = Intent(this, TestService::class.java)
        stopService(testIntent)
        TestLearnUtils.destroy()
//        if (isSatisfiedAndroidVersion(Build.VERSION_CODES.R)) {
//            mConnectivityDiagnosticsManager.unregisterConnectivityDiagnosticsCallback(
//                mConnectivityDiagnosticsCallback
//            )
//        }
//        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback)
    }

    fun hasPermissionToReadNetworkStats(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        var mode = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mode = appOps.unsafeCheckOpRaw(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        if (mode == AppOpsManager.MODE_ALLOWED) {
            return true
        }

        requestReadNetworkStats()
        return false
    }

    private fun requestReadNetworkStats() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

    private fun isSatisfiedAndroidVersion(version: Int): Boolean = Build.VERSION.SDK_INT >= version

}

class MyService : Service() {

    override fun onCreate() {
        super.onCreate()
        SystemClock.sleep(100000)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

class OrderedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 获取传递的数据
        val data = resultData
        val bundle = getResultExtras(true);

        // 修改广播数据（只有有序广播可以）
        setResultData("Modified data");
        bundle.putString("extra", "new value");
        setResultExtras(bundle);

        // 中止广播（阻止传递给低优先级的接收器）
        abortBroadcast()

        // 获取优先级
        val priority = resultCode
    }
}