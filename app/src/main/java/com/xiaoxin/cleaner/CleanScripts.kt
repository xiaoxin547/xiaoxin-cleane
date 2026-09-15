
package com.xiaoxin.cleaner

object CleanScripts {
    fun buildScript(game: GameItem): String {
        return """
            #!/system/bin/sh
            echo ">>> 启动清理程序: ${game.name}"
            echo ">>> 正在扫描包名..."
            
            USER_IDS=${'$'}(pm list users 2>/dev/null | grep -oE '[0-9]+' | sort -u)
            if [ -z "${'$'}USER_IDS" ]; then USER_IDS="0"; fi
            
            PACKAGES=${'$'}(pm list packages 2>/dev/null | grep -iE "${game.pattern}" | cut -d: -f2)
            if [ -z "${'$'}PACKAGES" ]; then
                echo "[!] 警告: 未检测到 ${game.name} 的安装包，请确认已安装。"
                exit 1
            fi
            
            for uid in ${'$'}USER_IDS; do
                for pkg in ${'$'}PACKAGES; do
                    echo ">>> 正在处理: ${'$'}pkg (用户空间: ${'$'}uid)"
                    
                    for data_dir in "/data/user/${'$'}uid/${'$'}pkg" "/data/user_de/${'$'}uid/${'$'}pkg"; do
                        if [ -d "${'$'}data_dir" ]; then
                            if [ -d "${'$'}data_dir/shared_prefs" ]; then
                                echo "[✔] 清除账号登录状态"
                                rm -rf "${'$'}data_dir/shared_prefs"
                            fi
                            if [ -d "${'$'}data_dir/cache" ]; then
                                echo "[✔] 清空本地缓存文件"
                                rm -rf "${'$'}data_dir/cache"/*
                            fi
                            
                            if [ "${game.id}" != "2" ]; then
                                if [ -d "${'$'}data_dir/databases" ]; then
                                    echo "[✔] 深度清理数据库残留"
                                    find "${'$'}data_dir/databases" -type f -size -1M 2>/dev/null | while read f; do rm -f "${'$'}f"; done
                                fi
                                if [ -d "${'$'}data_dir/files" ]; then
                                    echo "[✔] 清理文件残留"
                                    find "${'$'}data_dir/files" -type f -size -1M 2>/dev/null | while read f; do rm -f "${'$'}f"; done
                                fi
                            fi
                            
                            if [ "${game.id}" == "5" ] || [ "${game.id}" == "8" ] || [ "${game.id}" == "11" ]; then
                                echo "[✔] 执行反作弊深度清理"
                                [ -d "${'$'}data_dir/code_cache" ] && rm -rf "${'$'}data_dir/code_cache"/*
                                [ -d "${'$'}data_dir/no_backup" ] && rm -rf "${'$'}data_dir/no_backup"/*
                                [ -d "${'$'}data_dir/app_webview" ] && rm -rf "${'$'}data_dir/app_webview"/*
                                [ -d "${'$'}data_dir/app_textures" ] && rm -rf "${'$'}data_dir/app_textures"/*
                            fi
                        fi
                    done
                    
                    if [ "${'$'}uid" = "0" ]; then ext_bases="/sdcard /storage/emulated/0"; else ext_bases="/storage/emulated/${'$'}uid /sdcard/${'$'}uid"; fi
                    for base in ${'$'}ext_bases; do
                        for sub in "data" "obb"; do
                            ext_dir="${'$'}base/Android/${'$'}sub/${'$'}pkg"
                            if [ -d "${'$'}ext_dir" ]; then
                                echo "[✔] 清理外部存储残留"
                                [ -d "${'$'}ext_dir/cache" ] && rm -rf "${'$'}ext_dir/cache"/*
                                if [ "${game.id}" != "2" ]; then
                                    find "${'$'}ext_dir" -type f -size -1M 2>/dev/null -exec rm -f {} \;
                                fi
                            fi
                        done
                    done
                    
                    rm -f /data/dalvik-cache/*${'$'}{pkg}* 2>/dev/null
                    rm -f /data/system/package_cache/*${'$'}{pkg}* 2>/dev/null
                done
            done
            
            echo ">>> 执行全局系统清理..."
            rm -rf /data/local/tmp/* /cache/* /data/system/dropbox/* /data/system/tencent 2>/dev/null
            rm -rf /sdcard/tencent /storage/emulated/0/tencent 2>/dev/null
            for uid in ${'$'}USER_IDS; do rm -rf /data/user/${'$'}uid/tencent 2>/dev/null; done
            
            echo ">>> 重写设备标识..."
            NEW_ID=${'$'}(cat /dev/urandom | tr -dc 'a-f0-9' | head -c 16)
            settings put secure android_id "${'$'}NEW_ID" 2>/dev/null && echo "[✔] Android ID 修改成功"
            pm clear com.google.android.gms >/dev/null 2>&1
            pm clear com.google.android.gsf >/dev/null 2>&1
            echo "[✔] 广告 ID 重置成功"
            
            WIF=${'$'}(ip link 2>/dev/null | grep wlan | head -1 | awk -F: '{print ${'$'}2}' | tr -d ' ')
            if [ -n "${'$'}WIF" ]; then
                NMAC=${'$'}(cat /dev/urandom | tr -dc 'a-f0-9' | head -c 10 | sed 's/../&:/g;s/:${'$'}//')
                ip link set "${'$'}WIF" address "${'$'}NMAC" 2>/dev/null && echo "[✔] WiFi MAC 修改成功"
            fi
            for uid in ${'$'}USER_IDS; do rm -f "/data/system/users/${'$'}uid/settings_ssaid.xml" 2>/dev/null; done
            echo "[✔] SSAID 缓存清除成功"
            
            echo ">>> 清理任务全部完成！"
            echo ">>> 共耗时: ${'$'}(date +%s) 秒"
        """.trimIndent()
    }
}
