package com.haha.dynamicTextView

import android.graphics.Paint
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityDynamicTextviewBinding
import com.haha.util.DpOrSpToPxTransfer

/**
 *
 * @author : haha
 * @date   : 2024-10-31
 * @desc   : 动态TextView展示
 *
 */
class DynamicTextViewActivity: BaseMvvmActivity<ActivityDynamicTextviewBinding, BaseViewModel>() {
    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun initView() {
        mViewDataBinding.apply {
            btnUpdate.setOnClickListener {
                val value = etValue.text.toString()
                if (value.isEmpty()) {
                    Toast.makeText(this@DynamicTextViewActivity, "请输入数值", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val unit = etUnit.text.toString()
                if (unit.isEmpty()) {
                    Toast.makeText(this@DynamicTextViewActivity, "请输入单位", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (value.length % 2 == 0) {
                    ivImage.setImageResource(R.color.cyan)
                } else {
                    ivImage.setImageResource(R.color.red)
                }
                tvValue.text = value
                tvUnit.text = unit
                Log.d(TAG, "")
                val textWidth = calculateDynamicTextViewWidth(tvUnit)
                Log.d(TAG, "setOnClickListener: llValue.width = ${llValue.width}, llValue.measuredWidth = ${llValue.measuredWidth}, 285.dp = ${DpOrSpToPxTransfer.dp2px(mContext, 285)}")
                Log.d(TAG, "setOnClickListener: llValue.width = ${llValue.width}, ivImage.width = ${ivImage.width}, unitWidth = $textWidth")
                dynamicChangeTextViewWidth(llValue, tvValue,llValue.width - ivImage.width - textWidth)
            }
            dynamicLayout.setFontPaddingBottom(false)
            dynamicView.setOnClickListener {
                println("haha, nihao")
            }
        }
    }

    private fun calculateTextViewWidth(ll: LinearLayout, tv: TextView, leaveLength: Int) {
        val width = calculateDynamicTextViewWidth(tv)
        Log.d(TAG, "calculateTextViewWidth: width = $width")
        ll.gravity = Gravity.CENTER_HORIZONTAL.or(Gravity.BOTTOM)
        if (width >= leaveLength) {
            ll.gravity = Gravity.CENTER_HORIZONTAL.or(Gravity.BOTTOM)
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            params.gravity = Gravity.END
            tv.layoutParams = params
            tv.ellipsize = TextUtils.TruncateAt.END
        }
    }

    private fun calculateDynamicTextViewWidth(tv: TextView): Int {
        val paint = Paint()
        paint.setTypeface(tv.typeface)
        paint.textSize = tv.textSize
        paint.textAlign = tv.paint.textAlign
        val textWidth = paint.measureText(tv.text.toString())
        Log.d(TAG, "calculateDynamicTextViewWidth: textWidth = $textWidth")
        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        tv.measure(widthSpec, heightSpec)
        Log.d(TAG,"calculateDynamicTextViewLength: view.class = ${tv::class.simpleName}, width = ${tv.width}, measuredWidth = ${tv.measuredWidth}")
        return tv.measuredWidth
    }

    private fun dynamicChangeTextViewWidth(ll: LinearLayout, tv: TextView, leaveLength: Int) {
        var params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.BOTTOM
        tv.layoutParams = params
        val width = calculateDynamicTextViewWidth(tv)
        Log.d(TAG, "dynamicChangeTextViewWidth: width = $width, leaveLength = $leaveLength")
        ll.gravity = Gravity.CENTER_HORIZONTAL.or(Gravity.BOTTOM)
        tv.ellipsize = TextUtils.TruncateAt.END
        if (width > leaveLength) {
            params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            params.gravity = Gravity.BOTTOM.or(Gravity.START)
            tv.layoutParams = params
            tv.setLines(1)
        }
    }

    override fun initData() {
    }

    override fun getLayoutId(): Int = R.layout.activity_dynamic_textview

}