package com.aifactory.appmessagecapture.ui

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aifactory.appmessagecapture.service.BillNotificationHelper
import com.aifactory.appmessagecapture.ui.theme.AppMessageCaptureTheme

/**
 * 账单快捷编辑的悬浮窗宿主：在**任意界面之上**直接弹出双栏选择窗，
 * 无需跳转进 App（区别于 [BillQuickEditActivity] 的透明 Activity 方案）。
 *
 * 触发链路：通知「完善账单」按钮 → [BillQuickEditOverlayReceiver]（广播
 * PendingIntent，仅在已授予「显示悬浮窗」权限时由 BillNotificationHelper
 * 选择此入口）→ WindowManager 挂载 TYPE_APPLICATION_OVERLAY 窗口，
 * 内容为共享的 [QuickEditDialogContent]。
 *
 * Compose 视图挂在 WindowManager 上需要自备 Lifecycle/ViewModelStore/
 * SavedStateRegistry 宿主（[OverlayLifecycleOwner]）。
 */
object BillQuickEditOverlay {

    private var active: ActiveOverlay? = null

    private class ActiveOverlay(
        val owner: OverlayLifecycleOwner,
        val view: FrameLayout
    )

    /** 主线程调用。已在展示时忽略重复触发 */
    fun show(context: Context, billId: Long, notificationId: Int) {
        val app = context.applicationContext
        if (active != null) return
        if (!Settings.canDrawOverlays(app)) return

        val owner = OverlayLifecycleOwner().apply { onCreate() }
        val container = FrameLayout(app)
        val compose = ComposeView(app).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                AppMessageCaptureTheme {
                    QuickEditDialogContent(
                        billId = billId,
                        showOverlayHint = false,
                        onDismiss = { dismiss() },
                        onCommitted = { cancelNotification(app, notificationId) },
                        onBillMissing = {
                            // 账单已删：清掉残留通知并关闭悬浮窗
                            cancelNotification(app, notificationId)
                            dismiss()
                        }
                    )
                }
            }
        }
        container.addView(
            compose,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // 不抢焦点：系统返回键穿透给底层应用，关闭靠点遮罩/取消按钮
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        wm.addView(container, lp)
        owner.moveToResumed()
        active = ActiveOverlay(owner, container)
    }

    private fun dismiss() {
        val current = active ?: return
        active = null
        val wm = (current.view.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        wm.removeView(current.view)
        current.owner.onDestroy()
    }

    private fun cancelNotification(context: Context, notificationId: Int) {
        if (notificationId >= BillNotificationHelper.NOTIFICATION_ID_BASE) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(notificationId)
        }
    }
}

/**
 * 悬浮窗内 Compose 的最小生命周期宿主。
 * ComposeView 要求 ViewTree 上挂 LifecycleOwner/SavedStateRegistryOwner/
 * ViewModelStoreOwner，普通 View 环境没有，需要手动提供。
 */
private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun moveToResumed() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

/**
 * 「完善账单」按钮的广播入口（悬浮窗通道）。
 * onReceive 在主线程执行且只做窗口挂载，账单/平台数据由弹窗内部异步加载。
 */
class BillQuickEditOverlayReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val billId = intent.getLongExtra(BillNotificationHelper.EXTRA_BILL_ID, -1L)
        val notificationId = intent.getIntExtra(BillNotificationHelper.EXTRA_NOTIFICATION_ID, -1)
        if (billId <= 0) return
        if (!Settings.canDrawOverlays(context)) return

        BillQuickEditOverlay.show(context.applicationContext, billId, notificationId)
    }
}
