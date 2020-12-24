package net.wingao.xue.service

import android.widget.Toast
import com.stardust.view.accessibility.AccessibilityService.Companion.addDelegate
import net.wingao.xue.Consts
import net.wingao.xue.SettingsActivity
import net.wingao.xue.auto.ActivityInfoProvider
import org.slf4j.LoggerFactory

/**
 * User: Wing
 * Date: 2020/12/24
 */
class MyAccessibilityService2 : com.stardust.view.accessibility.AccessibilityService() {
    val logger = LoggerFactory.getLogger(this.javaClass)

    companion object {
        lateinit var instant: MyAccessibilityService2
    }

    init {
        logger.info("init")
        instant = this
    }

    override fun onCreate() {
        super.onCreate()

        addAccessibilityServiceDelegates()
    }

    val mActivityInfoProvider = ActivityInfoProvider(SettingsActivity.instant)

    fun isOnXueApp(): Boolean {
        return mActivityInfoProvider.latestPackage == Consts.XuePackagename
    }

    fun startTask() {
        if (isOnXueApp()) {

        } else {
            Toast.makeText(SettingsActivity.instant, "请打开学习强国", Toast.LENGTH_SHORT)
        }
    }


    private fun addAccessibilityServiceDelegates() {
        addDelegate(100, mActivityInfoProvider)
//        addDelegate(200, mNotificationObserver)
//        addDelegate(300, mAccessibilityActionRecorder)
    }

    fun isMainPage(){
        
    }
}
