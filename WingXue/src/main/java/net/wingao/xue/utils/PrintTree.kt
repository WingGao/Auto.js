package net.wingao.xue.utils

import com.stardust.view.accessibility.NodeInfo
import net.wingao.xue.utils.PrintTree
import java.io.File
import java.lang.StringBuilder

/**
 * User: Wing
 * Date: 2020/12/29
 */
object PrintTree {
    fun printDirectoryTree(folder: NodeInfo): String {
        val indent = 0
        val sb = StringBuilder()
        printDirectoryTree(folder, indent, sb)
        return sb.toString()
    }

    private fun printDirectoryTree(
        folder: NodeInfo, indent: Int,
        sb: StringBuilder
    ) {
        sb.append(getIndentString(indent))
        sb.append("+--")
        sb.append(folder)
        sb.append("/")
        sb.append("\n")
        for (file in folder.getChildren()) {
            if (file.getChildren().isNotEmpty()) {
                printDirectoryTree(file, indent + 1, sb)
            } else {
                printFile(file, indent + 1, sb)
            }
        }
    }

    private fun printFile(file: NodeInfo, indent: Int, sb: StringBuilder) {
        sb.append(getIndentString(indent))
        sb.append("+--")
        // 简化
//        sb.append(file)
        sb.append(file.simple())
        sb.append("\n")
    }

    private fun getIndentString(indent: Int): String {
        val sb = StringBuilder()
        for (i in 0 until indent) {
            sb.append("|  ")
        }
        return sb.toString()
    }
}
