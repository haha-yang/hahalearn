package com.haha.remoteviews

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.haha.baseui.mvvm.BaseMvvmActivity
import com.haha.baseui.mvvm.BaseViewModel
import com.haha.hahalearn.R
import com.haha.hahalearn.databinding.ActivityRemoteViewBinding


class RemoteViewActivity : BaseMvvmActivity<ActivityRemoteViewBinding, BaseViewModel>(), View.OnClickListener {
    private val mCreateRemoteViewBt: Button by lazy {
        findViewById(R.id.create_remote_view_bt)
    }
    private val mAddRemoteViewBt: Button by lazy {
        findViewById(R.id.add_remote_view_bt)
    }
    private val mRemoveRemoteViewBt: Button by lazy {
        findViewById(R.id.remove_remote_view_bt)
    }
    private val mTvDeviceName: TextView by lazy {
        findViewById(R.id.tv_device_name)
    }
    private val mNotificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Volatile
    private var notificationThreadRunning = false
    private var notificationThread: Thread? = null

    override fun getViewModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun getLayoutId(): Int = R.layout.activity_remote_view

    override fun initView() {
        mCreateRemoteViewBt.setOnClickListener(this)
        mAddRemoteViewBt.setOnClickListener(this)
        mRemoveRemoteViewBt.setOnClickListener(this)
    }

    override fun initData() {
        val name = intent.extras?.getString("device", "") ?: ""
        val content = getString(R.string.refresh_connect_device, name)
        val ssContent = SpannableString(content)
        val s = getString(R.string.refresh_connect)
        val start = content.indexOf(s)
        if (start >= 0) {
            ssContent.setSpan(null, start, start + s.length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE) //修改字体颜色

            val clickableSpan: ClickableSpan = object : ClickableSpan() {
                //添加点击
                override fun onClick(widget: View) {
                    Toast.makeText(this@RemoteViewActivity, "haha", Toast.LENGTH_SHORT).show()
                }

                override fun updateDrawState(ds: TextPaint) {
                    //去除连接下划线
                    ds.color = resources.getColor(R.color.purple_500)
                    ds.isUnderlineText = false
                }
            }
            ssContent.setSpan(clickableSpan, start, start + s.length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        mTvDeviceName.movementMethod = LinkMovementMethod.getInstance()
        mTvDeviceName.text = ssContent
    }

    override fun onClick(v: View?) {
        v?.let {
            when(v.id) {
                R.id.create_remote_view_bt -> {
                    createRemoteView1()
                }
                R.id.add_remote_view_bt -> {

                }
                R.id.remove_remote_view_bt -> {
                    stopNotificationThread()
                }
                else -> {

                }
            }
        }
    }

    private fun createRemoteView1() {
        if (notificationThreadRunning) {
            return
        }
        notificationThreadRunning = true
        val appContext = applicationContext
        val pkg = packageName
        val nm = mNotificationManager
        notificationThread = Thread({
            val remoteView = RemoteViews(pkg, R.layout.layout_custom_remote_views)
            remoteView.setTextViewText(R.id.tv_device_name, "哈哈哈哈")
            remoteView.setImageViewResource(R.id.iv_device_icon, R.mipmap.ic_launcher)

            val builder = Notification.Builder(appContext)
            builder.setSmallIcon(R.mipmap.ic_launcher)
            val notification = builder.build()

            var bitmap: Bitmap?
            var i = 0
            while (notificationThreadRunning && !Thread.currentThread().isInterrupted) {
                remoteView.setTextViewText(R.id.tv_device_name, "Notification, count = $i")
                bitmap = if (i % 2 == 1) {
                    BitmapFactory.decodeResource(appContext.resources, R.mipmap.ic_launcher)
                } else {
                    BitmapFactory.decodeResource(appContext.resources, R.mipmap.ic_launcher_round)
                }
                i++
                remoteView.setImageViewBitmap(R.id.iv_device_icon, bitmap)
                notification.contentView = remoteView
                nm.notify(1, notification)
                try {
                    Thread.sleep(1000)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }, "add Notification").also { it.start() }
    }

    private fun stopNotificationThread() {
        notificationThreadRunning = false
        notificationThread?.interrupt()
        notificationThread = null
    }

    override fun onDestroy() {
        stopNotificationThread()
        super.onDestroy()
    }

    private fun createRemoteView() {
        var remoteView =
            CustomRemoteViews(applicationContext, packageName, R.layout.layout_custom_remote_views)
        remoteView.build(
            "https://midea-file.oss-cn-hangzhou.aliyuncs.com/2021/5/26/18/EknLqOeDxZjshMOoEeZx.png",
            "空调"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelID = "custom_notification_1"
            val channel = NotificationChannel(
                channelID,
                "设备控件",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.enableLights(false)
            channel.enableVibration(false)
            channel.setSound(null, null)
            mNotificationManager.createNotificationChannel(channel)

            val notificationCompatBuilder = NotificationCompat.Builder(this, channelID)
            notificationCompatBuilder.setCustomContentView(remoteView)
                .setContentIntent(remoteView.toNewActivity())
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setOngoing(false)
                .setSmallIcon(R.drawable.me_ic_message)
                .setOnlyAlertOnce(true)

            val notification = notificationCompatBuilder.build()
            mNotificationManager.notify(1, notification)
        } else {
            val notificationCompatBuilder = NotificationCompat.Builder(this)
            notificationCompatBuilder.setCustomContentView(remoteView)
                .setContentIntent(remoteView.toNewActivity())
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setOngoing(false)
                .setSmallIcon(R.drawable.me_ic_message)
                .setOnlyAlertOnce(true)

//            val notification = notificationCompatBuilder.build()
//            notification.flags = Notification.FLAG_ONGOING_EVENT
//            mNotificationManager.notify(1, notification)
        }
    }
}