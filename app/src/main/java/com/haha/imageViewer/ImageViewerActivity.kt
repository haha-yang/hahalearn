package com.haha.imageViewer

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityImageviewerBinding

/**
 * @auther: haha
 * @Date:   2026/1/23
 * @Detail:
 */
class ImageViewerActivity : BaseMvvmActivity<ActivityImageviewerBinding, BaseViewModel>() {
    private var mMinScale: Float = 0.1f
    private var mMaxScale: Float = 5f

    private var mScaleFactor = 1.0f
    private var mPivotX = 1f
    private var mPivotY = 1f
    private var mTranslateX: Float = 0f
    private var mTranslateY: Float = 0f

    private var mOriginalPivotX = 1f
    private var mOriginalPivotY = 1f
    private var mIsFirst = false

    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun getLayoutId(): Int = R.layout.activity_imageviewer

    override fun initView() {
    }

    override fun initData() {
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            println("onScaleBegin")
            // 获取缩放中心点（两个手指的中心）
            mPivotX = detector.focusX
            mPivotY = detector.focusY

            // 设置缩放中心
            mViewDataBinding.imageview.pivotX = mPivotX
            mViewDataBinding.imageview.pivotY = mPivotY
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {

            // 累积缩放因子
            val previousScale: Float = mScaleFactor
            mScaleFactor *= detector.scaleFactor
            mScaleFactor = mScaleFactor.coerceIn(mMinScale, mMaxScale)

            setScaleFactor(mScaleFactor)

            // 调整位置，使缩放更自然
            adjustPosition(detector, previousScale);
            println("onScale: ${detector.scaleFactor}, mScaleFactor = $mScaleFactor")
            return true
        }

        private fun adjustPosition(detector: ScaleGestureDetector, previousScale: Float) {
            // 计算缩放中心的变化
            val focusX = detector.focusX
            val focusY = detector.focusY

            // 调整位置，让缩放中心保持在手指位置
            val deltaX: Float = (focusX - mPivotX) * (1 - mScaleFactor / previousScale)
            val deltaY: Float = (focusY - mPivotY) * (1 - mScaleFactor / previousScale)

            mViewDataBinding.imageview.translationX += deltaX
            mViewDataBinding.imageview.translationY += deltaY
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            println("onScaleEnd")
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onShowPress(e: MotionEvent) {
            println("onShowPrecess")
        }

        override fun onLongPress(e: MotionEvent) {
            println("保存照片")
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            // 滑动时移动图片
            mTranslateX -= distanceX
            mTranslateY -= distanceY
            val x = mViewDataBinding.imageview.x
            val y = mViewDataBinding.imageview.y
            println("before: x = $x, y = $y")
            println("mTranslateX = $mTranslateX, mTranslateY = $mTranslateY")
            mViewDataBinding.imageview.translationX = mTranslateX
            mViewDataBinding.imageview.translationY = mTranslateY
//            println("after: x = ${mViewDataBinding.imageview.x}, y = ${mViewDataBinding.imageview.y}")
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (mScaleFactor < 2.5f) {
                mScaleFactor = 2.5f
            } else if (mScaleFactor < 5f) {
                mScaleFactor = 5f
            } else {
                mScaleFactor = 1f
            }
            setScaleFactor(mScaleFactor)
            return true
        }
    }

    private fun setScaleFactor(scaleFactor: Float) {
        mViewDataBinding.imageview.scaleX = scaleFactor
        mViewDataBinding.imageview.scaleY = scaleFactor
    }
}