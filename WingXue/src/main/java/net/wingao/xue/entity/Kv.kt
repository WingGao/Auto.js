package net.wingao.xue.entity

import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

/**
 * User: Wing
 * Date: 2020/12/28
 */
open class Kv : RealmObject() {
    companion object {
        fun get(key: String): Kv? {
            return Realm.getDefaultInstance().where(Kv::class.java).equalTo("key", key).findFirst()
        }
    }

    @PrimaryKey
    var key: String = ""
    var postName: String? = null

    var createTime: Date = Date()
}
