package io.github.jqssun.airplay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import io.github.jqssun.airplay.service.AirPlayService

/**
 * 车机侧（灵动屏）控制接收端开关的入口。
 *
 * 为什么需要它：`AirPlayService` 是 `exported=false`，别的 App 既不能 start 也不能 stop 它；
 * 而灵动屏要的就是「一个按钮把接收端服务打开/关掉」，所以由接收端自己暴露一个受控广播口。
 *
 * 契约（灵动屏侧 `core/AirPlayBridge.kt` 的对端）：
 *  - `com.avatr.airplay.START` → 前台服务起监听（端口开始 accept）
 *  - `com.avatr.airplay.STOP`  → 停服务 + 通知界面自己退出
 *
 * 注意：START 只保证**服务**起来。视频画面要的是 Activity 提供的 Surface，
 * 所以灵动屏的「打开」= 拉起本 App 界面 + 发 START，两者都做。
 */
class ControlReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_START -> runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AirPlayService::class.java)
                        .setAction(AirPlayService.ACTION_START_SERVER),
                )
            }
            ACTION_STOP -> {
                // 服务已经在跑时 startService 会直接派发到现有实例，走 STOP_SERVER 分支
                runCatching {
                    context.startService(
                        Intent(context, AirPlayService::class.java).setAction(ACTION_STOP_SERVER),
                    )
                }
                // 让界面自己收起来：只停服务会留一个「服务已停止」的死界面在副屏/中控上
                context.sendBroadcast(Intent(ACTION_FINISHED).setPackage(context.packageName))
            }
        }
    }

    companion object {
        const val ACTION_START = "com.avatr.airplay.START"
        const val ACTION_STOP = "com.avatr.airplay.STOP"

        /** 内部用：派给服务实例的停止指令 */
        const val ACTION_STOP_SERVER = "com.avatr.airplay.STOP_SERVER"

        /** 内部用：通知界面退出 */
        const val ACTION_FINISHED = "com.avatr.airplay.FINISHED"
    }
}
