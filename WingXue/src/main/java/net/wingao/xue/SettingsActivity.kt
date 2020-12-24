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
import com.yhao.floatwindow.FloatWindow
import com.yhao.floatwindow.PermissionListener
import com.yhao.floatwindow.Screen
import com.yhao.floatwindow.ViewStateListener
import net.wingao.xue.service.MyAccessibilityService
import net.wingao.xue.service.MyAccessibilityService2
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.timer


class SettingsActivity : AppCompatActivity() {
    companion object {
        lateinit var instant: SettingsActivity
    }

    val logger = LoggerFactory.getLogger(this.javaClass)
    var hasFloatWindow = false
    override fun onCreate(savedInstanceState: Bundle?) {
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

        this.onResume()
        instant = this
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
        val floatView = LayoutInflater.from(this).inflate(R.layout.float_window, null);
        FloatWindow
            .with(getApplicationContext())
            .setView(floatView)
            .setWidth(200)                               //设置控件宽高
            .setHeight(Screen.width, 0.2f)
            .setX(100)                                   //设置控件初始位置
            .setY(Screen.height, 0.3f)
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
                MyAccessibilityService2.instant.startTask()
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
            startService(Intent(this, MyAccessibilityService2::class.java))
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
