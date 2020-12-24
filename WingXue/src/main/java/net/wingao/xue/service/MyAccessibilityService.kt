package net.wingao.xue.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast
import androidx.annotation.RequiresApi
import net.wingao.xue.Consts.Companion.XuePackagename
import org.slf4j.LoggerFactory

/**
 * User: Wing
 * Date: 2020/12/24
 */
class MyAccessibilityService : AccessibilityService() {
    companion object {

        lateinit var instant: MyAccessibilityService
    }

    init {
        instant = this
    }

    val logger = LoggerFactory.getLogger(this.javaClass)
    var mLatestPackage = ""
    var mLatestActivity = ""
    var isOnXueApp = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event != null) {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    // 记录窗口
                    val window = this.getWindow(event.windowId)
                    if (window?.isFocused != false) {
                        //TODO 排除launch
                        mLatestPackage = event.packageName.toString()
                        mLatestActivity = event.className.toString()
                        logger.info("当前window = ${mLatestPackage}/${mLatestActivity}")
                        onXueApp(mLatestPackage == XuePackagename)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
    }

    // 是否是学习强国app在前台
    fun onXueApp(active: Boolean) {
        isOnXueApp = active
    }

    // 是否在主页
    fun isMainPage(){
        rootInActiveWindow
    }

    fun startTask() {
        if(!isOnXueApp){
            Toast.makeText(applicationContext,"请打开学习强国",Toast.LENGTH_LONG).show()
            return
        }
    }

    // 开启我的
    fun openMe() {
        // comm_head_xuexi_mine
    }

    fun getCurrentActivity() {

    }
}

private fun AccessibilityService.getWindow(windowId: Int): AccessibilityWindowInfo? {
    windows.forEach {
        if (it.id == windowId) {
            return it
        }
    }
    return null
}
