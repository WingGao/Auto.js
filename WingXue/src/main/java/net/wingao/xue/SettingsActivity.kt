package net.wingao.xue

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import android.text.TextUtils
import android.text.TextUtils.SimpleStringSplitter
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.stardust.util.ScreenMetrics
import com.yhao.floatwindow.FloatWindow
import com.yhao.floatwindow.PermissionListener
import com.yhao.floatwindow.Screen
import com.yhao.floatwindow.ViewStateListener
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.wingao.xue.service.MyAccessibilityService
import net.wingao.xue.service.MyAccessibilityService2
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.timer


class SettingsActivity : AppCompatActivity() {
    companion object {
        @JvmStatic
        var instant: SettingsActivity? = null
    }


    val logger = LoggerFactory.getLogger(this.javaClass)
    var hasFloatWindow = false

    init {
        instant = this
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        logger.info("创建")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<Button>(R.id.btnStart).also {
            it.setOnClickListener { startTask() }
        }
        initFloatWindow()

        ScreenMetrics.initIfNeeded(this)
        this.onResume()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }

    // 开始学习任务
    fun startTask() {
        val launchIntent = packageManager.getLaunchIntentForPackage(Consts.XuePackagename)
        startActivity(launchIntent)
    }

    override fun onResume() {
        super.onResume()
        openAccessibility("net.wingao.xue.service.MyAccessibilityService2", this)
    }

    fun initFloatWindow() {
        if (FloatWindow.get() != null) return
        val floatView = LayoutInflater.from(this).inflate(R.layout.float_window, null);
        val widthR = 0.3f
        FloatWindow
            .with(getApplicationContext())
            .setView(floatView)
            .setWidth(Screen.width, widthR)                               //设置控件宽高
            .setHeight(Screen.height, 0.3f)
            .setX(Screen.width, 1 - widthR)  //设置控件初始位置,在右边，防止
            .setY(Screen.height, 0.8f)
            .setDesktopShow(true)                        //桌面显示
            .setViewStateListener(object : ViewStateListener {
                override fun onPositionUpdate(p0: Int, p1: Int) {
                }

                override fun onShow() {
                }

                override fun onHide() {
                }

                override fun onDismiss() {
                }

                override fun onMoveAnimStart() {
                }

                override fun onMoveAnimEnd() {
                }

                override fun onBackToDesktop() {
                }
            })    //监听悬浮控件状态改变
            .setPermissionListener(object : PermissionListener {
                override fun onSuccess() {
                }

                override fun onFail() {
                    Toast.makeText(applicationContext, "请手动开启悬浮窗权限", 3000)
                }
            })  //监听权限申请结果
            .build();
        val stateView = floatView.findViewById<TextView>(R.id.txtState)
        floatView.findViewById<Button>(R.id.btnStart).also {
            it.setOnClickListener {
                stateView.text = "开启"
                GlobalScope.launch {
                    MyAccessibilityService2.instant.startTask()
                }
            }
        }
        floatView.findViewById<Button>(R.id.btnDebug1).also {
            it.setOnClickListener {
                GlobalScope.launch {
                    MyAccessibilityService2.instant.logAllViewInWindow()
                }
            }
        }
    }

    /**
     * 跳转到系统设置页面开启辅助功能
     * @param accessibilityServiceName：指定辅助服务名字
     * @param context：上下文
     */
    fun openAccessibility(accessibilityServiceName: String, context: Context) {
        if (!isAccessibilitySettingsOn(accessibilityServiceName, context)) {
            Toast.makeText(this, "请打开无障碍模式", 3000)
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        } else {
//            startService(Intent(this, MyAccessibilityService2::class.java))
            MyAccessibilityService2.instant.connect()
        }
    }

    /**
     * 该辅助功能开关是否打开了
     * @param accessibilityServiceName：指定辅助服务名字
     * @param context：上下文
     * @return
     */
    private fun isAccessibilitySettingsOn(accessibilityServiceName: String, context: Context): Boolean {
        var accessibilityEnable = 0
        val serviceName = context.packageName + "/" + accessibilityServiceName
        try {
            accessibilityEnable =
                Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        } catch (e: Exception) {
            logger.error("get accessibility enable failed, the err:" + e.message)
        }
        if (accessibilityEnable == 1) {
            val mStringColonSplitter = SimpleStringSplitter(':')
            val settingValue =
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    val accessibilityService = mStringColonSplitter.next()
                    if (accessibilityService.equals(serviceName, ignoreCase = true)) {
                        logger.info("无障碍开启")
                        return true
                    }
                }
            }
        } else {
            logger.info("无障碍已关闭")
        }
        return false
    }


}
