package com.aifactory.appmessagecapture.birthday.ui

import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aifactory.appmessagecapture.birthday.service.BirthdayAlarmReceiver
import com.aifactory.appmessagecapture.ui.theme.AppMessageCaptureTheme
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog

/**
 * 全屏生日闹钟 Activity。
 *
 * 触发时：
 * 1. 点亮屏幕并在锁屏上显示
 * 2. 持续播放默认闹钟铃声
 * 3. 展示亲友姓名和将满岁数
 * 4. 用户点击"知道了"后关闭铃声并结束 Activity
 */
class AlarmActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BirthdayLog.i("[AlarmActivity] onCreate")

        // 锁屏显示 + 唤醒屏幕 + 保持屏幕常亮 + 全屏覆盖
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        val name = intent.getStringExtra(BirthdayAlarmReceiver.EXTRA_BIRTHDAY_NAME) ?: "亲友"
        val ageTurning = intent.getIntExtra(BirthdayAlarmReceiver.EXTRA_AGE_TURNING, -1)

        BirthdayLog.logMethodCall(
            "[AlarmActivity] received",
            mapOf("name" to name, "ageTurning" to ageTurning)
        )

        // 播放铃声
        playAlarmSound()

        setContent {
            AppMessageCaptureTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(96.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "🎂 生日提醒",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val subText = if (ageTurning > 0) {
                            "今天过生日，即将满 $ageTurning 岁！"
                        } else {
                            "今天过生日！"
                        }
                        Text(
                            text = subText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                        Button(
                            onClick = { dismissAlarm() },
                            modifier = Modifier
                                .fillMaxSize(0.7f)
                                .height(56.dp)
                        ) {
                            Text(
                                text = "知道了",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
        BirthdayLog.i("[AlarmActivity] onDestroy")
    }

    private fun playAlarmSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            ringtone?.play()
            BirthdayLog.i("[AlarmActivity] Alarm sound started.")
        } catch (e: Exception) {
            BirthdayLog.logException("[AlarmActivity] playAlarmSound", e)
        }
    }

    private fun stopAlarmSound() {
        try {
            ringtone?.stop()
            ringtone = null
            BirthdayLog.i("[AlarmActivity] Alarm sound stopped.")
        } catch (e: Exception) {
            BirthdayLog.logException("[AlarmActivity] stopAlarmSound", e)
        }
    }

    private fun dismissAlarm() {
        stopAlarmSound()
        finish()
    }
}
