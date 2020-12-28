package net.wingao.xue.auto

import com.stardust.automator.UiObject
import com.stardust.util.ScreenMetrics
import net.wingao.xue.service.MyAccessibilityService2

/**
 * User: Wing
 * Date: 2020/12/28
 */
fun UiObject.clickScreen() {
    val bound = bounds()
    MyAccessibilityService2.automator.click(bound.centerX(), bound.centerY())
}

//将组建移动到追上（类似于翻页）
fun UiObject.scrollToTop() {
    val bound = bounds()
    MyAccessibilityService2.automator.swipe(
        bound.centerX(), bound.centerY(),
        ScreenMetrics.getDeviceScreenWidth() / 3, 100, 2000
    )
}

fun UiObject.isOnScreen(): Boolean {
    val bound = bounds()
    return bound.centerX() > 0 && bound.centerX() < ScreenMetrics.getDeviceScreenWidth()
            && bound.centerY() > 0 && bound.centerY() < ScreenMetrics.getDeviceScreenHeight()
}
