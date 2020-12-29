package net.wingao.xue.auto

import android.graphics.Rect
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.stardust.automator.UiObjectCollection.Companion.EMPTY
import com.stardust.automator.UiObject.Companion.createRoot
import com.stardust.automator.UiObjectCollection.Companion.of

import com.stardust.automator.UiGlobalSelector
import com.stardust.view.accessibility.AccessibilityNodeInfoAllocator
import com.stardust.automator.UiObjectCollection
import com.stardust.concurrent.VolatileBox
import com.stardust.automator.UiObject
import com.stardust.automator.ActionArgument
import com.stardust.automator.ActionArgument.IntActionArgument
import com.stardust.automator.ActionArgument.CharSequenceActionArgument
import com.stardust.automator.ActionArgument.FloatActionArgument
import com.stardust.automator.filter.BooleanFilter
import com.stardust.automator.filter.Filter
import com.stardust.automator.filter.TextFilters
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.wingao.xue.BuildConfig
import org.slf4j.LoggerFactory
import java.lang.Error
import java.util.ArrayList

/**
 * Created by Stardust on 2017/3/9.
 */
class UiSelector : UiGlobalSelector {
    val logger = LoggerFactory.getLogger(this.javaClass)
    private lateinit var mAccessibilityBridge: AccessibilityBridge
    private var mAllocator: AccessibilityNodeInfoAllocator? = null

    constructor(accessibilityBridge: AccessibilityBridge) {
        mAccessibilityBridge = accessibilityBridge
    }

    constructor(accessibilityBridge: AccessibilityBridge, allocator: AccessibilityNodeInfoAllocator?) {
        mAccessibilityBridge = accessibilityBridge
        mAllocator = allocator
    }

    private val automator: SimpleActionAutomator by lazy {
        SimpleActionAutomator(this.mAccessibilityBridge)
    }

    protected fun find(max: Int): UiObjectCollection {
        ensureAccessibilityServiceEnabled()
        if (mAccessibilityBridge.flags and AccessibilityBridge.FLAG_FIND_ON_UI_THREAD != 0
            && Looper.myLooper() != Looper.getMainLooper()
        ) {
            val result = VolatileBox<UiObjectCollection>()
            mAccessibilityBridge.post { result.setAndNotify(findImpl(max)) }
            return result.blockedGet()
        }
        return findImpl(max)
    }

    fun find(): UiObjectCollection {
        return find(Int.MAX_VALUE)
    }

    // 找到最大的
    fun findBiggest(): UiObject {
        return find().biggest()
    }

    protected fun findImpl(max: Int): UiObjectCollection {
        val roots = mAccessibilityBridge.windowRoots()
        logger.debug("find: roots = $roots")
        if (roots.isEmpty()) {
            return EMPTY
        }
        val result: MutableList<UiObject?> = ArrayList()
        for (root in roots) {
            if (root == null) {
                continue
            }
            //            if (root.getPackageName() != null && mAccessibilityBridge.getConfig().whiteListContains(root.getPackageName().toString())) {
//                Log.d(TAG, "package in white list, return null");
//                return UiObjectCollection.Companion.getEMPTY();
//            }
            result.addAll(findAndReturnList(createRoot(root, mAllocator), max - result.size))
            if (result.size >= max) {
                break
            }
        }
        return of(result)
    }

    fun classNameX(className: String): UiSelector {
        return super.className(className) as UiSelector
    }

    fun textMatchesX(regex: String): UiSelector {
        return super.textMatches(convertRegex(regex)) as UiSelector
    }

    // TODO: 2018/1/30 更好的实现方式。
    private fun convertRegex(regex: String): String {
        return if (regex.startsWith("/") && regex.endsWith("/") && regex.length > 2) {
            regex.substring(1, regex.length - 1)
        } else regex
    }

    override fun classNameMatches(regex: String): UiGlobalSelector {
        return super.classNameMatches(convertRegex(regex))
    }

    override fun idMatches(regex: String): UiGlobalSelector {
        return super.idMatches(convertRegex(regex))
    }

    override fun packageNameMatches(regex: String): UiGlobalSelector {
        return super.packageNameMatches(convertRegex(regex))
    }

    override fun descMatches(regex: String): UiGlobalSelector {
        return super.descMatches(convertRegex(regex))
    }

    fun filterX(filter: BooleanFilter.BooleanSupplier): UiSelector {
        return this.filter(filter) as UiSelector
    }

    private fun ensureAccessibilityServiceEnabled() {
        mAccessibilityBridge.ensureServiceEnabled()
    }

    fun untilFind(): UiObjectCollection {
        ensureNonUiThread()
        var uiObjectCollection = find()
        while (uiObjectCollection.empty()) {
            if (Thread.currentThread().isInterrupted) {
                throw Error()
            }
            try {
                Thread.sleep(50)
            } catch (e: InterruptedException) {
                throw Error()
            }
            uiObjectCollection = find()
        }
        return uiObjectCollection
    }

    private fun ensureNonUiThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // TODO: 2018/11/1 配置字符串
            throw IllegalThreadStateException("不能在ui线程执行阻塞操作, 请在子线程或子脚本执行findOne()或untilFind()")
        }
    }

    fun findOne(timeout: Long): UiObject? {
        var uiObjectCollection = find(1)
        val start = SystemClock.uptimeMillis()
        while (uiObjectCollection.empty()) {
            if (Thread.currentThread().isInterrupted) {
                throw Error()
            }
            if (timeout > 0 && SystemClock.uptimeMillis() - start > timeout) {
                return null
            }
            try {
                Thread.sleep(50)
            } catch (e: InterruptedException) {
                throw Error()
            }
            uiObjectCollection = find(1)
        }
        return uiObjectCollection[0]
    }

    fun findOnce(index: Int = 0): UiObject? {
        val uiObjectCollection = find(index + 1)
        return if (index >= uiObjectCollection.size()) {
            null
        } else uiObjectCollection[index]
    }

    fun findOne(): UiObject? {
        return untilFindOne()
    }

    fun exists(): Boolean {
        val collection = find()
        return collection.nonEmpty()
    }

    fun untilFindOne(): UiObject? {
        return findOne(-1)
    }

    fun waitFor() {
        untilFind()
    }

    override fun id(id: String): UiSelector {
        if (!id.contains(":")) {
            addFilter(object : Filter {
                override fun filter(node: UiObject): Boolean {
                    val fullId = mAccessibilityBridge.infoProvider.latestPackage + ":id/" + id
                    return fullId == node.viewIdResourceName
                }

                override fun toString(): String {
                    return "id(\"$id\")"
                }
            })
        } else {
            super.id(id)
        }
        return this
    }

    override fun idStartsWith(prefix: String): UiGlobalSelector {
        if (!prefix.contains(":")) {
            addFilter(object : Filter {
                override fun filter(nodeInfo: UiObject): Boolean {
                    val fullIdPrefix = mAccessibilityBridge.infoProvider.latestPackage + ":id/" + prefix
                    val id = nodeInfo.viewIdResourceName
                    return id != null && id.startsWith(fullIdPrefix)
                }

                override fun toString(): String {
                    return "idStartsWith(\"$prefix\")"
                }
            })
        } else {
            super.idStartsWith(prefix)
        }
        return this
    }

    override fun text(text: String): UiSelector {
        super.text(text)
        return this
    }

    private fun performAction(action: Int, vararg arguments: ActionArgument): Boolean {
        return untilFind().performAction(action, *arguments)
    }

    fun click(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
    }

    fun longClick(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.ACTION_LONG_CLICK)
    }

    fun accessibilityFocus(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS)
    }

    fun clearAccessibilityFocus(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS)
    }

    fun focus(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.ACTION_FOCUS)
    }

    fun clearFocus(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.ACTION_CLEAR_FOCUS)
    }

    fun copy(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.ACTION_COPY)
    }

    fun paste(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.ACTION_PASTE)
    }

    fun select(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.ACTION_SELECT)
    }

    fun cut(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.ACTION_CUT)
    }

    fun collapse(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.ACTION_COLLAPSE)
    }

    fun expand(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.ACTION_EXPAND)
    }

    fun dismiss(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.ACTION_DISMISS)
    }

    fun show(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SHOW_ON_SCREEN.id)
    }

    fun scrollForward(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)
    }

    fun scrollBackward(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD)
    }

    fun scrollUp(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_UP.id)
    }

    fun scrollDown(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_DOWN.id)
    }

    fun scrollLeft(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_LEFT.id)
    }

    fun scrollRight(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_RIGHT.id)
    }

    fun contextClick(): Boolean {
        return performAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CONTEXT_CLICK.id)
    }

    fun setSelection(s: Int, e: Int): Boolean {
        return performAction(
            AccessibilityNodeInfoCompat.ACTION_SET_SELECTION,
            IntActionArgument(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_START_INT, s),
            IntActionArgument(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_END_INT, e)
        )
    }

    fun setText(text: String?): Boolean {
        return performAction(
            AccessibilityNodeInfoCompat.ACTION_SET_TEXT,
            CharSequenceActionArgument(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text!!)
        )
    }

    fun setProgress(value: Float): Boolean {
        return performAction(
            AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SET_PROGRESS.id,
            FloatActionArgument(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_PROGRESS_VALUE, value)
        )
    }

    fun scrollTo(row: Int, column: Int): Boolean {
        return performAction(
            AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_TO_POSITION.id,
            IntActionArgument(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_ROW_INT, row),
            IntActionArgument(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_COLUMN_INT, column)
        )
    }

    fun clickScreen(delayMs: Int = 0) {
        untilFind().toArray().forEach {
            if (it != null) it.clickScreen()
        }
        if (delayMs > 0) {
            runBlocking {
                delay(delayMs.toLong())
            }
        }
    }
}


