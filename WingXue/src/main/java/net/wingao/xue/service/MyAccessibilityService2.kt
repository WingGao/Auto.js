package net.wingao.xue.service

import android.widget.Toast
import com.stardust.util.UiHandler
import com.stardust.view.accessibility.AccessibilityNotificationObserver
import com.stardust.view.accessibility.AccessibilityService
import com.stardust.view.accessibility.AccessibilityService.Companion.addDelegate
import net.wingao.xue.Consts
import net.wingao.xue.SettingsActivity
import net.wingao.xue.auto.AccessibilityBridge
import net.wingao.xue.auto.AccessibilityConfig
import net.wingao.xue.auto.ActivityInfoProvider
import net.wingao.xue.auto.UiSelector
import org.slf4j.LoggerFactory

/**
 * User: Wing
 * Date: 2020/12/24
 */
class MyAccessibilityService2 : com.stardust.view.accessibility.AccessibilityService() {
    val logger = LoggerFactory.getLogger(this.javaClass)
    lateinit var bridge: AccessibilityBridge
    lateinit var mActivityInfoProvider: ActivityInfoProvider

    companion object {
        lateinit var instant: MyAccessibilityService2
    }

    init {
        logger.info("init")
        instant = this
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onServiceConnected() {
        bridge = AccessibilityBridgeImpl()
        mActivityInfoProvider = ActivityInfoProvider(SettingsActivity.instant!!)
        addAccessibilityServiceDelegates()

        super.onServiceConnected()
    }

    fun connect() {
        onServiceConnected()
    }


    fun isOnXueApp(): Boolean {
        logger.info("最新pkg ${mActivityInfoProvider.latestPackage}/${mActivityInfoProvider.latestActivity}")
        return mActivityInfoProvider.latestPackage == Consts.XuePackagename
    }

    fun startTask() {
        if (isOnXueApp()) {
            logger.info("在强国")
            isMainPage()
        } else {
            logger.info("不在强国")
            Toast.makeText(SettingsActivity.instant, "请打开学习强国", Toast.LENGTH_SHORT)
        }
    }

    fun newSelector(): UiSelector {
        return UiSelector(bridge)
    }

    private fun addAccessibilityServiceDelegates() {
        addDelegate(100, mActivityInfoProvider)
//        addDelegate(200, mNotificationObserver)
//        addDelegate(300, mAccessibilityActionRecorder)
    }

    fun isMainPage(): Boolean {
        val ok = newSelector().id("home_bottom_bat_icon_large").exists()
        logger.info("主页按钮 $ok")
        return ok
    }

    class AccessibilityBridgeImpl :
        AccessibilityBridge(SettingsActivity.instant, AccessibilityConfig(), UiHandler(SettingsActivity.instant)) {
        override fun ensureServiceEnabled() {
//            TODO("Not yet implemented")
        }

        override fun waitForServiceEnabled() {
//            TODO("Not yet implemented")
        }

        override fun getService(): AccessibilityService {
            return instant
        }

        override fun getInfoProvider(): ActivityInfoProvider {
            return instant.mActivityInfoProvider
        }

        override fun getNotificationObserver(): AccessibilityNotificationObserver {
            TODO("Not yet implemented")
        }

    }
}
