package net.wingao.xue.auto

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.stardust.automator.GlobalActionAutomator
import com.stardust.automator.UiObject
import com.stardust.automator.simple_action.ActionFactory
import com.stardust.automator.simple_action.ActionTarget
import com.stardust.automator.simple_action.SimpleAction
import com.stardust.util.DeveloperUtils
import com.stardust.util.ScreenMetrics

/**
 * Created by Stardust on 2017/4/2.
 */

class SimpleActionAutomator(
    private val mAccessibilityBridge: AccessibilityBridge
) {

    private lateinit var mGlobalActionAutomator: GlobalActionAutomator

    private var mScreenMetrics: ScreenMetrics? = null

    private val isRunningPackageSelf: Boolean
        get() = DeveloperUtils.isSelfPackage(mAccessibilityBridge.infoProvider.latestPackage)


    fun text(text: String, i: Int): ActionTarget {
        return ActionTarget.TextActionTarget(text, i)
    }


    fun bounds(left: Int, top: Int, right: Int, bottom: Int): ActionTarget {
        return ActionTarget.BoundsActionTarget(Rect(left, top, right, bottom))
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)

    fun editable(i: Int): ActionTarget {
        return ActionTarget.EditableActionTarget(i)
    }


    fun id(id: String): ActionTarget {
        return ActionTarget.IdActionTarget(id)
    }


    fun click(target: ActionTarget): Boolean {
        return performAction(target.createAction(AccessibilityNodeInfo.ACTION_CLICK))
    }


    fun longClick(target: ActionTarget): Boolean {
        return performAction(target.createAction(AccessibilityNodeInfo.ACTION_LONG_CLICK))
    }


    fun scrollUp(target: ActionTarget): Boolean {
        return performAction(target.createAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD))
    }


    fun scrollDown(target: ActionTarget): Boolean {
        return performAction(target.createAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
    }


    fun scrollBackward(i: Int): Boolean {
        return performAction(ActionFactory.createScrollAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, i))
    }


    fun scrollForward(i: Int): Boolean {
        return performAction(ActionFactory.createScrollAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, i))
    }


    fun scrollMaxBackward(): Boolean {
        return performAction(ActionFactory.createScrollMaxAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD))
    }


    fun scrollMaxForward(): Boolean {
        return performAction(ActionFactory.createScrollMaxAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
    }


    fun focus(target: ActionTarget): Boolean {
        return performAction(target.createAction(AccessibilityNodeInfo.ACTION_FOCUS))
    }


    fun select(target: ActionTarget): Boolean {
        return performAction(target.createAction(AccessibilityNodeInfo.ACTION_SELECT))
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun setText(target: ActionTarget, text: String): Boolean {
        return performAction(target.createAction(AccessibilityNodeInfo.ACTION_SET_TEXT, text))
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun appendText(target: ActionTarget, text: String): Boolean {
        return performAction(target.createAction(UiObject.ACTION_APPEND_TEXT, text))
    }


    fun back(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }


    fun home(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun powerDialog(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
    }


    fun notifications(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
    }


    fun quickSettings(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
    }


    fun recents(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    fun splitScreen(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    fun gesture(start: Long, duration: Long, vararg points: IntArray): Boolean {
        prepareForGesture()
        return mGlobalActionAutomator.gesture(start, duration, *points)
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    fun gestureAsync(start: Long, duration: Long, vararg points: IntArray) {
        prepareForGesture()
        mGlobalActionAutomator.gestureAsync(start, duration, *points)
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    fun gestures(strokes: Any): Boolean {
        prepareForGesture()
        @Suppress("UNCHECKED_CAST")
        return mGlobalActionAutomator.gestures(*strokes as Array<GestureDescription.StrokeDescription>)
    }

    //如果这里用GestureDescription.StrokeDescription[]为参数，安卓7.0以下会因为找不到这个类而报错
    @RequiresApi(api = Build.VERSION_CODES.N)
    fun gesturesAsync(strokes: Any) {
        prepareForGesture()
        @Suppress("UNCHECKED_CAST")
        mGlobalActionAutomator.gesturesAsync(*strokes as Array<GestureDescription.StrokeDescription>)
    }

    private fun prepareForGesture() {
        if (!::mGlobalActionAutomator.isInitialized) {
            mGlobalActionAutomator = GlobalActionAutomator(Handler(Looper.getMainLooper())) {
                ensureAccessibilityServiceEnabled()
                return@GlobalActionAutomator mAccessibilityBridge.service!!
            }
        }
        mGlobalActionAutomator.setScreenMetrics(mScreenMetrics)
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    fun click(x: Int, y: Int): Boolean {
        prepareForGesture()
        return mGlobalActionAutomator.click(x, y)
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    fun press(x: Int, y: Int, delay: Int): Boolean {
        prepareForGesture()
        return mGlobalActionAutomator.press(x, y, delay)
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    fun longClick(x: Int, y: Int): Boolean {
        prepareForGesture()
        return mGlobalActionAutomator.longClick(x, y)
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, delay: Int): Boolean {
        prepareForGesture()
        return mGlobalActionAutomator.swipe(x1, y1, x2, y2, delay.toLong())
    }

    private fun performGlobalAction(action: Int): Boolean {
        ensureAccessibilityServiceEnabled()
        val service = mAccessibilityBridge.service ?: return false
        return service.performGlobalAction(action)
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)

    fun paste(target: ActionTarget): Boolean {
        return performAction(target.createAction(AccessibilityNodeInfo.ACTION_PASTE))
    }

    private fun ensureAccessibilityServiceEnabled() {
        mAccessibilityBridge.ensureServiceEnabled()
    }

    private fun performAction(simpleAction: SimpleAction): Boolean {
        ensureAccessibilityServiceEnabled()
        if (isRunningPackageSelf) {
            return false
        }
        val roots = mAccessibilityBridge.windowRoots().filter { it != null }
        if (roots.isEmpty())
            return false
        var succeed = true
        for (root in roots) {
            succeed = succeed and simpleAction.perform(UiObject.createRoot(root))
        }
        return succeed
    }

    fun setScreenMetrics(metrics: ScreenMetrics) {
        mScreenMetrics = metrics
    }

}
