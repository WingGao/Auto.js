package net.wingao.xue.service

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.stardust.app.OnActivityResultDelegate.DelegateHost
import com.stardust.autojs.core.image.capture.ScreenCaptureRequestActivity
import com.stardust.autojs.core.image.capture.ScreenCaptureRequester
import com.stardust.autojs.core.image.capture.ScreenCaptureRequester.AbstractScreenCaptureRequester
import com.stardust.autojs.core.image.capture.ScreenCaptureRequester.ActivityScreenCaptureRequester
import com.stardust.autojs.core.image.capture.ScreenCapturer
import com.stardust.autojs.core.util.ScriptPromiseAdapter
import com.stardust.autojs.runtime.ScriptRuntime
import com.stardust.autojs.runtime.api.Images
import com.stardust.automator.UiObject
import com.stardust.automator.filter.BooleanFilter
import com.stardust.util.ScreenMetrics
import com.stardust.util.UiHandler
import com.stardust.view.accessibility.AccessibilityNotificationObserver
import com.stardust.view.accessibility.AccessibilityService
import com.stardust.view.accessibility.AccessibilityService.Companion.addDelegate
import com.stardust.view.accessibility.LayoutInspector
import com.stardust.view.accessibility.NodeInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.wingao.xue.App
import net.wingao.xue.Consts
import net.wingao.xue.SettingsActivity
import net.wingao.xue.auto.*
import net.wingao.xue.entity.Kv
import net.wingao.xue.entity.save
import net.wingao.xue.utils.PrintTree
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

/**
 * User: Wing
 * Date: 2020/12/24
 */
class MyAccessibilityService2 : com.stardust.view.accessibility.AccessibilityService() {
    val logger = LoggerFactory.getLogger(this.javaClass)

    lateinit var bridge: AccessibilityBridge
    lateinit var mActivityInfoProvider: ActivityInfoProvider
    lateinit var scriptRuntime: ScriptRuntime

    var connected = false
    var currentTask: Score? = null
    var schedule = Timer()


    val idBtnMainTab = "home_bottom_tab_icon_large" //主页 - 底部大红按钮
    val idBtnBottomTab = "home_bottom_tab_icon_group" //主页 - 底部小按钮
    //剩余要获取积分


    companion object {
        lateinit var instant: MyAccessibilityService2
        lateinit var automator: SimpleActionAutomator
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

        // 监控当前task
        schedule.scheduleAtFixedRate(1, 1000, {
            //TODO 退出
        })
        scriptRuntime = ScriptRuntime.Builder()
            .setUiHandler(UiHandler(this))
            .setScreenCaptureRequester(ScreenCaptureRequesterImpl())
            .build()
        scriptRuntime.init()
    }


    fun isOnXueApp(): Boolean {
        logger.info("最新pkg ${mActivityInfoProvider.latestPackage}/${mActivityInfoProvider.latestActivity}")
        return mActivityInfoProvider.latestPackage == Consts.XuePackagename
    }

    suspend fun startTask() {
        if (isOnXueApp()) {
            logger.info("在强国")
//            toGetMyScore()
//            videoTask.start()
            readTask.start()
//            dingYueTask.start()
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

    fun userCanSee(obj: UiObject): Boolean {
        val bound = obj.bounds()
        return bound.centerX() > 0 && bound.centerX() < ScreenMetrics.getDeviceScreenWidth()
                // 去掉顶部和底部
                && bound.centerY() > 200 && bound.centerY() < ScreenMetrics.getDeviceScreenHeight() - 200
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

    // 打印空间树
    fun logAllViewInWindow() {
        val inspector = LayoutInspector(this)
        inspector.addCaptureAvailableListener(object : LayoutInspector.CaptureAvailableListener {
            override fun onCaptureAvailable(capture: NodeInfo?) {
                if (capture != null) {
                    //打印dfs
                    val saveFile =
                        File(instant.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "dump-window.txt");
                    saveFile.writeText(PrintTree.printDirectoryTree(capture))
                    logger.info("完成logAllViewInWindow ${saveFile.absolutePath}")
                }
            }
        })
        inspector.captureCurrentWindow()
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

    //找到当前最大的ListView
    fun findMainListView() {

    }

    suspend fun loopCard(onCard: suspend () -> Unit, onEnd: suspend () -> Boolean) {
        // general_card_title_id 标题
        val cards = newSelector().id("general_card_title_id").find()
        var lastCard: UiObject? = null
        logger.info("card ${cards.size()}")
        for (i in 0..cards.size() - 1) {
            val card = cards.get(i)
            if (card != null) {
                if (userCanSee(card)) {
                    lastCard = card
                    // 在首页的card
                    val title = card.text()
                    val key = "post-${title}"
                    val old = Kv.get(key)
                    if (old != null) {
                        logger.info("已阅读 $title")
                        continue
                    }
                    logger.info("开始阅读 ${title}")
                    card.clickScreen()

                    onCard()
                    logger.info("阅读结束")
                    markCard(card)
                    // 返回上一页
                    automator.back()
                    delay(3000)
                    if (onEnd()) {
                        return
                    }
                }
            }
        }
        // 翻页
        if (lastCard != null) lastCard.scrollToTop()
    }

    // 判断卡片是否已读
    fun checkCardCanRead(titleView: UiObject, dateView: UiObject? = null): Boolean {
        var key = "post-${titleView.text()}"
        if (dateView != null) key += "-${dateView.text()}"
        val old = Kv.get(key)
        return old == null
    }

    // 标记文章已完成
    fun markCard(titleView: UiObject, dateView: UiObject? = null) {
        var key = "post-${titleView.text()}"
        if (dateView != null) key += "-${dateView.text()}"
        val record = Kv().also {
            it.postName = titleView.text()
            it.key = key
        }
        record.save()
        logger.info("标记 ${key} 已读")
    }

    // 阅读
    val readTask: Score = Score(0, 12, suspend handler@{
        val task = instant.readTask
        if (!task.needDo()) { //完成
            return@handler true
        }

        setCurrentPage(XuePage.Main)
        newSelector().id(idBtnMainTab).click()
        val bj = newSelector().text("北京")
        if (!bj.exists()) {
            Toast.makeText(App.instant, "请现将地区设置为北京", Toast.LENGTH_SHORT).show()
            logger.error("北京不存在")
            return@handler false
        }
        bj.clickScreen()
        delay(2000)
        // general_card_title_id 标题
        val cards = newSelector().id("general_card_title_id").find()
        var lastCard: UiObject? = null
        logger.info("card ${cards.size()}")
        loopCard({
            // 阅读 1分钟
            val readTime = 1 * 60 + 10
            val delayTime = 10
            for (j in 1..readTime / 10) {
                logger.info("阅读loop $j")
                delay(delayTime * 1000L)
                //下滑一点
                automator.swipe(
                    (ScreenMetrics.getDeviceScreenWidth() * 0.5).toInt(),
                    (ScreenMetrics.getDeviceScreenHeight() * 0.7).toInt(),
                    (ScreenMetrics.getDeviceScreenWidth() * 0.55).toInt(),
                    (ScreenMetrics.getDeviceScreenHeight() * 0.5).toInt(),
                    1000
                )
            }
        }, onEnd@{
            task.reset -= 2
            if (!task.needDo()) { //完成
                return@onEnd true
            }
            false
        })
        // 翻页
        task.doHandler()
    })

    //视听任务
    val videoTimeScore = Score(0, 6, suspend { false })
    val videoTask: Score = Score(0, 6, suspend handler@{
        val task = instant.videoTask
        if (!task.needDo() && !videoTimeScore.needDo()) { //完成
            return@handler true
        }

        setCurrentPage(XuePage.Main)
        newSelector().text("电视台").clickScreen()
        newSelector().text("学习视频").clickScreen()
        newSelector().text("学习新视界").clickScreen()
//        newSelector().text("联播频道").clickScreen()

        while (true) {
            delay(2000)
//        val timeDivs = newSelector().classNameX("TextView").textMatchesX("\\d\\d:\\d\\d").find()
            val listView = newSelector().classNameX("ListView").findBiggest()
            var lastCard: UiObject? = null
            for (frame in listView.children().toArray()) {
                if (frame != null && frame.className() == "android.widget.FrameLayout" && userCanSee(frame)) {
                    val dateDiv = frame.findOne(newSelector().textMatches("\\d{4}-\\d{2}-\\d{2}"))!!
                    val titleDiv = dateDiv.parent()!!.parent()!!.parent()!!.child(0)!!
                    lastCard = dateDiv
                    logger.info("title=${titleDiv.text()} date=${dateDiv.text()}")
                    if (checkCardCanRead(titleDiv, dateDiv)) {
                        titleDiv.clickScreen()
                        //停留1分钟\
                        //TODO 视频重置时间
                        delay(70 * 1000)
                        markCard(titleDiv, dateDiv)
                        instant.videoTask.reset -= 1
                        instant.videoTimeScore.reset -= 1
                        // 返回上一页
                        automator.back()
                        delay(3000)
                        if (!task.needDo() && !videoTimeScore.needDo()) { //完成
                            return@handler true
                        }
                    } else {
                        logger.info("已读")
                    }
                }
            }
//        // 翻页
            automator.scrollList(listView)
        }
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
    //订阅任务
    val dingYueTask: Score = Score(9, 2, suspend handler@{
        val task = instant.dingYueTask
        if (!task.needDo()) { //完成
            return@handler true
        }
        setCurrentPage(XuePage.Main)
//        newSelector().text("我的").clickScreen(1300)
//        newSelector().text("订阅").clickScreen(1300)
//        newSelector().text("添加").clickScreen(1300)
//        newSelector().text("上新").clickScreen(1300)
        val sw = ScreenMetrics.getDeviceScreenWidth()
        //查找list
        val btnList = newSelector().classNameX("ImageView").filterX(object : BooleanFilter.BooleanSupplier {
            override fun get(node: UiObject): Boolean {
                return node.bounds().left > sw * 0.5
            }
        }).find()
        //截屏
        val img = Images(this, scriptRuntime, ScreenCaptureRequesterImpl())
        img.requestScreenCapture(ScreenCapturer.ORIENTATION_AUTO).onResolve(object : ScriptPromiseAdapter.Callback {
            override fun call(arg: Any?) {
                logger.info("requestScreenCapture resolve", arg)
                val imgPath = File(instant.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "a.png")
                logger.info("保存在 $imgPath")
                img.captureScreen(imgPath.absolutePath)
            }
        })
        // 判断颜色
        true
    })
//        val FenXiang = Score(10, 1)
//        val FaBiao = Score(11, 1)
//        val BenDi = Score(12, 1)

    class Score(var type: Int, var max: Int, var doHandler: ScoreDoHandler) {
        var reset: Int = max

        suspend fun start() {
            doHandler()
        }

        fun needDo(): Boolean {
            return reset > 0
        }
    }


    private class ScreenCaptureRequesterImpl : AbstractScreenCaptureRequester() {
        override fun setOnActivityResultCallback(callback: ScreenCaptureRequester.Callback) {
            super.setOnActivityResultCallback { result: Int, data: Intent ->
                mResult = data
                callback.onRequestResult(result, data)
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun request() {
            ScreenCaptureRequestActivity.request(instant, mCallback)
        }
    }
}

typealias ScoreDoHandler = suspend () -> Boolean
