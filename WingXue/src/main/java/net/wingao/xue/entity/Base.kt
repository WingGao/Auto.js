package net.wingao.xue.entity

import io.realm.Realm
import io.realm.RealmObject

/**
 * User: Wing
 * Date: 2020/12/28
 */

fun RealmObject.save() {
    val realm = Realm.getDefaultInstance()
    realm.beginTransaction()
    realm.copyToRealm(this)
    realm.commitTransaction()
}
