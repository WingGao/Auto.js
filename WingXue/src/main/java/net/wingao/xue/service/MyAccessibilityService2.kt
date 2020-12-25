package net.wingao.xue.service

import android.graphics.Rect
import android.widget.Toast
import com.stardust.util.UiHandler
import com.stardust.view.accessibility.AccessibilityNotificationObserver
import com.stardust.view.accessibility.AccessibilityService
import com.stardust.view.accessibility.AccessibilityService.Companion.addDelegate
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.wingao.xue.Consts
import net.wingao.xue.SettingsActivity
import net.wingao.xue.auto.*
import org.slf4j.LoggerFactory

/**
 * User: Wing
 * Date: 2020/12/24
 */
class MyAccessibilityService2 : com.stardust.view.accessibility.AccessibilityService() {
    val logger = LoggerFactory.getLogger(this.javaClass)
    lateinit var bridge: AccessibilityBridge
    lateinit var mActivityInfoProvider: ActivityInfoProvider
    lateinit var automator: SimpleActionAutomator
    var connected = false


    val idBtnMainTab = "home_bottom_tab_icon_large" //主页 - 底部大红按钮
    //剩余要获取积分


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


        super.onServiceConnected()
    }

    fun connect() {
        if (connected) return
        connected = true
        bridge = AccessibilityBridgeImpl()
        mActivityInfoProvider = ActivityInfoProvider(SettingsActivity.instant!!)
        automator = SimpleActionAutomator(bridge)
        addAccessibilityServiceDelegates()
    }


    fun isOnXueApp(): Boolean {
        logger.info("最新pkg ${mActivityInfoProvider.latestPackage}/${mActivityInfoProvider.latestActivity}")
        return mActivityInfoProvider.latestPackage == Consts.XuePackagename
    }

    suspend fun startTask() {
        if (isOnXueApp()) {
            logger.info("在强国")
//            toGetMyScore()
            readTask.start()
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


    fun getCurrentPage(): XuePage {
        when (mActivityInfoProvider.latestActivity) {
            "android.widget.FrameLayout" -> { //大概率主页
                if (newSelector().id(idBtnMainTab).exists()) {
                    return XuePage.Main
                }
            }
            "com.alibaba.lightapp.runtime.activity.CommonWebViewActivity" -> { // 我的积分啥的

            }
        }
        return XuePage.None
    }

    fun setCurrentPage(p: XuePage) {
        if (getCurrentPage() == p) {
            return
        }
        when (p) {
            XuePage.MyScore -> {
                newSelector().id("comm_head_xuexi_score").click()
            }
        }
    }

    // 查找我的积分
    suspend fun toGetMyScore() {
        setCurrentPage(XuePage.MyScore)
        val todayScoreV = (newSelector().textStartsWith("今日已累积") as UiSelector).untilFindOne()
        if (todayScoreV != null) {
            val todayScore = parseInt(todayScoreV.text())
            logger.info("今日积分 ${todayScore}")
            return
        }
        logger.error("TODO 没有找到积分")
    }


    fun logAllViewInWindow() {

    }

    fun parseInt(t: String): Int? {
        val r = Regex("\\d+").find(t)
        if (r == null) return null
        return r.value.toInt()
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

    enum class XuePage {
        Main,
        MyScore,
        None
    }


    // 阅读
    val readTask = Score(0, 12, suspend handler@{
        setCurrentPage(XuePage.Main)
        newSelector().id(idBtnMainTab).click()
        newSelector().text("推荐").click()
        // general_card_title_id 标题
        val cards = newSelector().id("general_card_title_id").find()
        for (i in 0..cards.size()) {
            val card = cards.get(i)
            if (card != null) {
                var screenBound = Rect()
                card.getBoundsInScreen(screenBound)
                if (screenBound.left >= 0 && screenBound.left < 100 && screenBound.top > 0) {
                    // 在首页的card
                    logger.info("开始阅读 ${card.text()}")
                    //TODO 判断今日已读时间
                    card.click()
                    // 阅读 6分钟
                    delay((6 * 60 + 20) * 1000)
                    logger.info("阅读结束")
                    return@handler true
                }
            }
        }
        logger.info("card ${cards.size()}")
        // 点击要闻
        true
    })
//        val VideoNum = Score(1, 6)
//        val VideoTime = Score(2, 6)
//        val DailyAnswer = Score(3, 5)
//        val WeeklyAnswer = Score(4, 5)
//        val SpecialAnswer = Score(5, 10)
//        val Tiaozhan = Score(6, 6)
//        val Zhenshangyou = Score(7, 5)
//        val ShuangRen = Score(8, 2)
//        val DingYue = Score(9, 2)
//        val FenXiang = Score(10, 1)
//        val FaBiao = Score(11, 1)
//        val BenDi = Score(12, 1)

    class Score(var type: Int, var max: Int, var doHandler: ScoreDoHandler) {
        var reset = max

        suspend fun start() {
            doHandler()
        }
    }

}

typealias ScoreDoHandler = suspend () -> Boolean
