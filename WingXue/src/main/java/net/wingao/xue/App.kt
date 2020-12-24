package net.wingao.xue

import android.app.Application
import org.slf4j.LoggerFactory

/**
 * User: Wing
 * Date: 2020/12/24
 */
class App : Application() {
    companion object {
        lateinit var instant: Application
    }

    val logger = LoggerFactory.getLogger(this.javaClass)
    override fun onCreate() {
        super.onCreate()

        instant = this
        logger.info("onCreate instant=${instant}")
    }
}
