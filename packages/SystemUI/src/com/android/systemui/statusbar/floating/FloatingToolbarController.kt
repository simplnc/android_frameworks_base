package com.android.systemui.statusbar.floating

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.android.systemui.CoreStartable
import com.android.systemui.res.R
import javax.inject.Inject

class FloatingToolbarController @Inject constructor(
    private val context: Context
) : CoreStartable {

    private val windowManager: WindowManager by lazy {
        context.getSystemService(WindowManager::class.java)
    }
    private val contentResolver: ContentResolver by lazy { context.contentResolver }
    private val mainHandler = Handler(Looper.getMainLooper())

    private var toolbarView: View? = null

    private val settingUri: Uri = Settings.Secure.getUriFor(SETTING_ENABLED)
    private val positionUri: Uri = Settings.Secure.getUriFor(SETTING_POSITION)

    private val observer = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean, uris: MutableCollection<Uri>, flags: Int, userId: Int) {
            super.onChange(selfChange, uris, flags, userId)
            update()
        }

        override fun onChange(selfChange: Boolean) {
            update()
        }
    }

    override fun start() {
        contentResolver.registerContentObserver(settingUri, false, observer)
        contentResolver.registerContentObserver(positionUri, false, observer)
        update()
    }

    private fun update() {
        val enabled = Settings.Secure.getInt(context.contentResolver, SETTING_ENABLED, 0) == 1
        if (enabled) show() else hide()
    }

    private fun show() {
        if (toolbarView != null) return
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.floating_toolbar, null)

        // Simple actions
        view.findViewById<View>(R.id.btn_settings)?.setOnClickListener {
            val intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            kotlin.runCatching { context.startActivity(intent) }
        }

        // Drag to move support
        var lastX = 0f
        var lastY = 0f
        var paramX = 0
        var paramY = 0
        val layoutParams = createLayoutParams().also { lp ->
            paramX = lp.x
            paramY = lp.y
        }
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    lastY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - lastX).toInt()
                    val dy = (event.rawY - lastY).toInt()
                    lastX = event.rawX
                    lastY = event.rawY
                    paramX += dx
                    paramY += dy
                    layoutParams.x = paramX
                    layoutParams.y = paramY
                    runCatching { windowManager.updateViewLayout(v, layoutParams) }
                    true
                }
                else -> false
            }
        }

        toolbarView = view
        runCatching { windowManager.addView(view, layoutParams) }
    }

    private fun hide() {
        toolbarView?.let { v ->
            runCatching { windowManager.removeView(v) }
        }
        toolbarView = null
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val position = Settings.Secure.getString(context.contentResolver, SETTING_POSITION)
        val gravity = if (position == POSITION_BOTTOM) Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        else Gravity.TOP or Gravity.CENTER_HORIZONTAL

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_STATUS_BAR_ADDITIONAL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = gravity
        lp.y = if (gravity and Gravity.BOTTOM == Gravity.BOTTOM) 0 else 0
        lp.setTitle("FloatingToolbar")
        return lp
    }

    companion object {
        const val SETTING_ENABLED = "floating_toolbar_enabled"
        const val SETTING_POSITION = "dual_status_bar_position" // "top" or "bottom"
        const val POSITION_BOTTOM = "bottom"
    }
}