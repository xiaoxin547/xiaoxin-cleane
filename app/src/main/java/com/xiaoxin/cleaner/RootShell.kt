package com.xiaoxin.cleaner

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object RootShell {

    // 执行 root 命令并返回日志
    fun execute(command: String): String {
        val output = StringBuilder()
        try {
            // 申请 root 权限
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            
            // 写入我们的清理命令
            os.writeBytes(command + "\n")
            os.writeBytes("exit\n")
            os.flush()

            // 读取执行过程中的输出
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            process.waitFor()
        } catch (e: Exception) {
            output.append("执行出错: ").append(e.message)
        }
        return output.toString()
    }
}
