package com.xiaoxin.cleaner

object CleanScripts {

    // 根据选择的游戏，生成对应的清理脚本
    fun buildScript(game: GameItem): String {
        return """
            #!/system/bin/sh
            echo ">>> 开始清理 ${game.name}..."
            
            # 获取所有用户ID（包括分身）
            USER_IDS=${'$'}(pm list users 2>/dev/null | grep -oE '[0-9]+' | sort -u)
            if [ -z "${'$'}USER_IDS" ]; then
                USER_IDS="0"
            fi
            
            # 获取匹配的游戏包名
            PACKAGES=${'$'}(pm list packages 2>/dev/null | grep -iE "${game.pattern}" | cut -d: -f2)
            if [ -z "${'$'}PACKAGES" ]; then
                echo "未检测到 ${game.name} 的包名，请确认已安装。"
                exit 1
            fi
            
            for uid in ${'$'}USER_IDS; do
                for pkg in ${'$'}PACKAGES; do
                    echo "  [处理] 包名: ${'$'}pkg  用户: ${'$'}uid"
                    
                    # 内部数据目录
                    for data_dir in "/data/user/${'$'}uid/${'$'}pkg" "/data/user_de/${'$'}uid/${'$'}pkg"; do
                        if [ -d "${'$'}data_dir" ]; then
                            # 1. 清除登录状态
                            if [ -d "${'$'}data_dir/shared_prefs" ]; then
                                echo "    - 清除登录状态"
                                rm -rf "${'$'}data_dir/shared_prefs"
                            fi
                            
                            # 2. 清空缓存
                            if [ -d "${'$'}data_dir/cache" ]; then
                                echo "    - 清空缓存"
                                rm -rf "${'$'}data_dir/cache"/*
                            fi
                            
                            # 3. 判断是否跳过 databases/files (王者荣耀 ID=2 跳过，避免资源抽取失败)
                            if [ "${game.id}" != "2" ]; then
                                if [ -d "${'$'}data_dir/databases" ]; then
                                    find "${'$'}data_dir/databases" -type f -size -1M 2>/dev/null | while read f; do
                                        rm -f "${'$'}f"
                                    done
                                fi
                                if [ -d "${'$'}data_dir/files" ]; then
                                    find "${'$'}data_dir/files" -type f -size -1M 2>/dev/null | while read f; do
                                        rm -f "${'$'}f"
                                    done
                                fi
                            fi
                            
                            # 4. 深度清理 (和平精英 ID=5, PUBG ID=8, CF手游 ID=11)
                            if [ "${game.id}" == "5" ] || [ "${game.id}" == "8" ] || [ "${game.id}" == "11" ]; then
                                echo "    - 深度清理模式"
                                [ -d "${'$'}data_dir/code_cache" ] && rm -rf "${'$'}data_dir/code_cache"/*
                                [ -d "${'$'}data_dir/no_backup" ] && rm -rf "${'$'}data_dir/no_backup"/*
                                [ -d "${'$'}data_dir/app_webview" ] && rm -rf "${'$'}data_dir/app_webview"/*
                                [ -d "${'$'}data_dir/app_textures" ] && rm -rf "${'$'}data_dir/app_textures"/*
                            fi
                        fi
                    done
                    
                    # 外部存储目录
                    if [ "${'$'}uid" = "0" ]; then
                        ext_bases="/sdcard /storage/emulated/0"
                    else
                        ext_bases="/storage/emulated/${'$'}uid /mnt/user/${'$'}uid/emulated/0 /sdcard/${'$'}uid"
                    fi
                    for base in ${'$'}ext_bases; do
                        for sub in "data" "obb"; do
                            ext_dir="${'$'}base/Android/${'$'}sub/${'$'}pkg"
                            if [ -d "${'$'}ext_dir" ]; then
                                [ -d "${'$'}ext_dir/cache" ] && rm -rf "${'$'}ext_dir/cache"/*
                                if [ "${game.id}" != "2" ]; then
                                    find "${'$'}ext_dir" -type f -size -1M 2>/dev/null -exec rm -f {} \;
                                fi
                            fi
                        done
                    done
                    
                    # Dalvik 缓存
                    rm -f /data/dalvik-cache/*${'$'}{pkg}* 2>/dev/null
                    rm -f /data/system/package_cache/*${'$'}{pkg}* 2>/dev/null
                done
            done
            
            echo ">>> 系统级清理..."
            rm -rf /data/local/tmp/* /cache/* /data/system/dropbox/* /data/system/tencent 2>/dev/null
            rm -rf /sdcard/tencent /storage/emulated/0/tencent 2>/dev/null
            for uid in ${'$'}USER_IDS; do
                rm -rf /data/user/${'$'}uid/tencent 2>/dev/null
            done
            
            echo ">>> 重写设备标识..."
            NEW_ID=${'$'}(cat /dev/urandom | tr -dc 'a-f0-9' | head -c 16)
            settings put secure android_id "${'$'}NEW_ID" 2>/dev/null && echo "  - Android ID 已修改"
            
            pm clear com.google.android.gms >/dev/null 2>&1
            pm clear com.google.android.gsf >/dev/null 2>&1
            echo "  - 广告 ID 已重置"
            
            WIF=${'$'}(ip link 2>/dev/null | grep wlan | head -1 | awk -F: '{print ${'$'}2}' | tr -d ' ')
            if [ -n "${'$'}WIF" ]; then
                NMAC=${'$'}(cat /dev/urandom | tr -dc 'a-f0-9' | head -c 10 | sed 's/../&:/g;s/:${'$'}//')
                ip link set "${'$'}WIF" address "${'$'}NMAC" 2>/dev/null && echo "  - WiFi MAC 已修改"
            fi
            
            for uid in ${'$'}USER_IDS; do
                rm -f "/data/system/users/${'$'}uid/settings_ssaid.xml" 2>/dev/null
            done
            echo "  - SSAID 缓存已清除"
            
            echo ">>> 清理完成！"
        """.trimIndent()
    }
}
