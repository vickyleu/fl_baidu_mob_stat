package fl.baidu.mob.stat

import android.app.Activity
import android.content.Context
import com.baidu.mobstat.StatService
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

@Suppress("unused")
class BaiduMobStatPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel

    private var mActivity: Activity? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "fl_baidu_mob_stat")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        @Suppress("UNREACHABLE_CODE", "IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")
        val act: Activity = mActivity ?: return.apply {
            result.error("no_activity", "no_activity", null)
            return@apply
        }
        if (isMainProcess(act)) {
            when (call.method) {
                "init" -> {
                    val appKey = call.argument<String>("appKey")
                    val appChannel = call.argument<String>("appChannel")
                    val versionName = call.argument<String>("versionName")
                    val debuggable = call.argument<String>("debuggable")

                    // SDK初始化，该函数不会采集用户个人信息，也不会向百度移动统计后台上报数据
                    StatService.init(act, appKey, appChannel)
                    StatService.setAppVersionName(act, versionName)
                    StatService.setDebugOn(debuggable == "true")
                    StatService.setAuthorizedState(act, false)
                    result.success(true)
                }

                "privilegeGranted" -> {
                    // 通过该接口可以控制敏感数据采集，true表示可以采集，false表示不可以采集，
                    // 该方法一定要最优先调用，请在StatService.start(this)之前调用，采集这些数据可以帮助App运营人员更好的监控App的使用情况，
                    // 建议有用户隐私策略弹窗的App，用户未同意前设置false,同意之后设置true
                    StatService.setAuthorizedState(act, true)
                    StatService.platformType(2)
                    // 如果没有页面和自定义事件统计埋点，此代码一定要设置，否则无法完成统计
                    // 进程第一次执行此代码，会导致发送上次缓存的统计数据；若无上次缓存数据，则发送空启动日志
                    // 由于多进程等可能造成Application多次执行，建议此代码不要埋点在Application中，否则可能造成启动次数偏高
                    // 建议此代码埋点在统计路径触发的第一个页面中，若可能存在多个则建议都埋点
                    StatService.start(act)
                    result.success(true)
                }

                "logEvent" -> {
                    val eventId = call.argument<String>("eventId")
                    val attributes = call.argument<MutableMap<String, String>>("attributes")
                    StatService.onEvent(act, eventId, "", 1, attributes)
                    result.success(true)
                }

                "logDurationEvent" -> {
                    val eventId = call.argument<String>("eventId")
                    val label = call.argument<String>("label")
                    val duration = call.argument<Int>("duration")
                    val attributes = call.argument<MutableMap<String, String>?>("attributes")
                    if (duration != null) {
                        StatService.onEventDuration(
                            act,
                            eventId,
                            label,
                            duration.toLong(),
                            attributes
                        )
                    }
                    result.success(true)
                }
                "eventStart" -> {
                    StatService.onEventStart(act, call.argument("eventId"), call.argument("label"))
                    result.success(true)
                }
                "eventEnd" -> {
                    val eventId = call.argument<String>("eventId")
                    val label = call.argument<String>("label")
                    val attributes = call.argument<MutableMap<String, String>?>("attributes")
                    StatService.onEventEnd(act, eventId, label, attributes)
                    result.success(true)
                }
                "pageStart" -> {
                    StatService.onPageStart(act, call.arguments as String)
                    result.success(true)
                }
                "pageEnd" -> {
                    StatService.onPageEnd(act, call.arguments as String)
                    result.success(true)
                }
                else -> {
                    result.notImplemented()
                }
            }
        } else {
            result.error("not_main_process", "not_main_process", null)
            return
        }
    }


    override fun onAttachedToActivity(p0: ActivityPluginBinding) {
        mActivity = p0.activity
    }

    override fun onDetachedFromActivity() {
        mActivity = null
    }

    private fun isMainProcess(act: Activity): Boolean {
        val pid = android.os.Process.myPid()
        val activityManager =
            act.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningAppProcesses = activityManager.runningAppProcesses
        runningAppProcesses.forEach {
            if (it.pid == pid) {
                return it.processName == act.packageName
            }
        }
        return false
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        if (::channel.isInitialized) {
            channel.setMethodCallHandler(null)
        }
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(p0: ActivityPluginBinding) {
        onAttachedToActivity(p0)
    }
}