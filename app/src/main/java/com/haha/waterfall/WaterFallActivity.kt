package com.haha.waterfall

import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityWaterfallBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.Collator
import java.util.Locale
import kotlin.math.min
import kotlin.random.Random


/**
 *     author : yangzy33
 *     time   : 2024-02-21
 *     desc   :
 *     version: 1.0
 */
class WaterFallActivity : BaseMvvmActivity<ActivityWaterfallBinding, BaseViewModel>() {

    companion object {
        const val BASE_CONTENT = "你了解发号施令旅339ajk,]】；国妮啊后发交电话费盖提亚武器先擦"

        /** 防止一次创建过多 TextView 卡死主线程 */
        private const val MAX_VIEW_COUNT = 200
    }

    private val tvMap = mutableMapOf<View, Boolean>()

    private val mWaterFallLayout: WaterFallLayout by lazy {
        findViewById(R.id.lt_waterfall)
    }

    private val contentList = mutableListOf<String>()

    private var mPopupWindow: MaskPopupWindow? = null

    private lateinit var mTvParams: LinearLayout.LayoutParams

    private val mMap: MutableMap<String, String> = mutableMapOf()

    private val mViewCount: Button by lazy {
        findViewById(R.id.btn_view_count)
    }

    private val mRv: CustomRecyclerView by lazy {
        findViewById(R.id.rv)
    }

    private lateinit var mCustomViewAdapter: CustomViewAdapter

    private var loadJob: Job? = null

    private var pendingPopupShow: Runnable? = null

    private var popupShowGeneration = 0

    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun getLayoutId(): Int = R.layout.activity_waterfall

    override fun initView() {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        mRv.layoutManager = layoutManager
        mCustomViewAdapter = CustomViewAdapter(this, contentList)
        mRv.adapter = mCustomViewAdapter
        mRv.addItemDecoration(VerticalScrollBarDecoration())

        val etViewCount = findViewById<EditText>(R.id.et_view_count)

        mTvParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        mTvParams.gravity = Gravity.CENTER

        mViewCount.setOnClickListener {
            if (mPopupWindow?.isShowing == true) {
                cancelPendingPopupShow()
                mPopupWindow?.dismiss()
                return@setOnClickListener
            }

            val rawCount = etViewCount.text.toString().toIntOrNull()
            if (rawCount == null || rawCount <= 0) {
                showPopupOnNextFrame()
                return@setOnClickListener
            }

            val viewCount = min(rawCount, MAX_VIEW_COUNT)
            if (rawCount > MAX_VIEW_COUNT) {
                Toast.makeText(
                    this,
                    "数量过大，已限制为 $MAX_VIEW_COUNT",
                    Toast.LENGTH_SHORT
                ).show()
            }

            loadWaterFallContent(viewCount)
        }
    }

    override fun initData() {
    }

    private fun loadWaterFallContent(viewCount: Int) {
        loadJob?.cancel()
        cancelPendingPopupShow()
        loadJob = lifecycleScope.launch {
            mViewCount.isEnabled = false
            try {
                val sorted = withContext(Dispatchers.Default) {
                    val random = Random(0)
                    val list = ArrayList<String>(viewCount)
                    val len = BASE_CONTENT.length
                    for (i in 0 until viewCount) {
                        ensureActive()
                        var startIndex = random.nextInt(len)
                        var endIndex = random.nextInt(len)
                        while (startIndex == endIndex) {
                            startIndex = random.nextInt(len)
                            endIndex = random.nextInt(len)
                        }
                        if (startIndex > endIndex) {
                            val temp = startIndex
                            startIndex = endIndex
                            endIndex = temp
                        }
                        list.add(BASE_CONTENT.substring(startIndex, endIndex))
                    }
                    val collator = Collator.getInstance(Locale.CHINA)
                    list.sortWith { o1, o2 -> collator.compare(o1, o2) }
                    list
                }

                ensureActive()
                contentList.clear()
                contentList.addAll(sorted)
                mCustomViewAdapter.notifyDataSetChanged()

                bindWaterFallViews(sorted)
                showPopupOnNextFrame()

                withContext(Dispatchers.IO) {
                    readJson()
                }
            } finally {
                mViewCount.isEnabled = true
            }
        }
    }

    /**
     * 批量挂 View：removeAllViewsInLayout + addViewInLayout(preventRequestLayout=true)，
     * 结束后只 requestLayout 一次，避免每个 addView 都触发全量 onMeasure。
     * 不使用 API 29+ 的 [android.view.ViewGroup.suppressLayout]，以兼容 minSdk 24。
     */
    private fun bindWaterFallViews(contents: List<String>) {
        mWaterFallLayout.removeAllViewsNoRequestLayout()
        tvMap.clear()
        for (content in contents) {
            val tv = createSingleTextView(content)
            val lp = LinearLayout.LayoutParams(mTvParams)
            mWaterFallLayout.addViewNoRequestLayout(tv, lp)
        }
        mWaterFallLayout.requestLayout()
    }

    private fun createSingleTextView(content: String): TextView {
        val tv = TextView(this)
        tv.layoutParams = mTvParams
        tv.background = getDrawable(R.drawable.bg_28_gray)
        tv.text = content
        tv.textSize = 22F
        tv.id = tv.hashCode()
        tv.maxLines = 1
        tv.ellipsize = TextUtils.TruncateAt.END
        tv.setTextColor(resources.getColor(R.color.b3ffffff))
        tv.setOnClickListener {
            if (tvMap[tv] == false) {
                tv.background = getDrawable(R.drawable.bg_28_orange)
                tv.setTextColor(resources.getColor(R.color.white))
                tvMap[tv] = true
            } else {
                tv.background = getDrawable(R.drawable.bg_28_gray)
                tv.setTextColor(resources.getColor(R.color.b3ffffff))
                tvMap[tv] = false
            }
        }
        tvMap[tv] = false
        return tv
    }

    private fun readJson() {
        mMap.clear()
        val jsonStr = ReadJsonUtil.readJson("DeviceType.json", this) ?: return
        val json = JSONArray(jsonStr)
        try {
            var i = 0
            while (true) {
                val jsonObject: JSONObject
                try {
                    jsonObject = json.getJSONObject(i++)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    break
                }
                val key = jsonObject.get("category").toString()
                val value = jsonObject.get("categoryName").toString()
                mMap[key] = value
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val sb = java.lang.StringBuilder()
        val iter = mMap.keys.iterator()
        while (iter.hasNext()) {
            val key = iter.next()
            sb.append("key = $key, value = ${mMap[key]}\n")
        }
        Log.d(this.javaClass.simpleName, sb.toString())
    }

    /**
     * 等当前帧 traversal 结束后再 addView。
     * 避免瀑布流 requestLayout 与 PopupWindow 新建窗口落在同一次 doFrame。
     */
    private fun showPopupOnNextFrame() {
        cancelPendingPopupShow()
        val generation = popupShowGeneration
        val show = Runnable {
            if (generation != popupShowGeneration) return@Runnable
            showPopupWindowNow()
        }
        pendingPopupShow = show
        mWaterFallLayout.postOnAnimation {
            if (generation != popupShowGeneration) return@postOnAnimation
            mWaterFallLayout.post(show)
        }
    }

    private fun cancelPendingPopupShow() {
        popupShowGeneration++
        pendingPopupShow?.let { mWaterFallLayout.removeCallbacks(it) }
        pendingPopupShow = null
    }

    private fun showPopupWindowNow() {
        pendingPopupShow = null
        if (isFinishing || isDestroyed) return
        if (mPopupWindow == null) {
            mPopupWindow = MaskPopupWindow(this)
            mPopupWindow?.animationStyle = R.style.add_popup_anim
        }
        if (mPopupWindow?.isShowing == true) return
        mPopupWindow?.showAsDropDown(mViewCount, 0, 32, Gravity.START)
    }

    override fun onDestroy() {
        mWaterFallLayout.removeCallbacks(pendingPopupShow)
        mPopupWindow?.dismiss()
        mPopupWindow = null
        super.onDestroy()
    }

    override fun getNavigationBarColor(): Int {
        return R.color.black
    }
}
