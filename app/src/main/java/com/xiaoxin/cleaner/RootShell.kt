package com.xiaoxin.cleaner

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object RootShell {
    fun execute(command: String): String {
        val output = StringBuilder()
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes(command + "\n")
            os.writeBytes("exit\n")
            os.flush()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            var errLine: String?
            while (errReader.readLine().also { errLine = it } != null) {
                output.append("[ERROR] ").append(errLine).append("\n")
            }
            process.waitFor()
        } catch (e: Exception) {
            output.append("[异常] ").append(e.message)
        }
        return output.toString()
    }
}
