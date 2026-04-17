package com.uy_li.runhook;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Build;
import android.os.SystemClock;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class JoyrunHook implements IXposedHookLoadPackage {

    private static boolean isModifyCadence = true;
    private static long mockStartTime = 0; // 用于计算平滑正弦波的起点时间
    private static float baseStepOffset = -1f; // 记录第一次拦截到的真实步数
    private static int lastLoggedSec = -1; // 上次打印日志的秒数（避免刷屏）
    
    // 动态步频配置
    private static float minCadenceConfig = 172f;
    private static float maxCadenceConfig = 182f;
    private static float currentCadence = 176f; // 当前随机产生的步频值

    // 用于记录已经成功 Hook 过的 Listener 类，避免重复 Hook 导致性能损耗或循环
    private static final Set<String> hookedListenerClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        String pkg = lpparam.packageName;

        // 排除列表：不应该被 Hook 的包名
        // - 模块自身
        // - 系统 UI
        // - 常见的系统组件
        String[] excludePackages = {
            "com.uy_li.runhook",      // 模块自身
            "com.android.systemui",   // 系统 UI
            "com.android.launcher",   // 桌面
            "com.android.settings",   // 设置
            "com.android.packageinstaller", // 安装器
        };

        for (String exclude : excludePackages) {
            if (exclude.equals(pkg)) {
                return;
            }
        }

        XposedBridge.log("JoyrunHook loaded for package: " + pkg + " (LSPosed 作用域模式)");

        // ==========================================
        // 1. 悬浮窗广播控制开关
        // ==========================================
        try {
            XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @SuppressLint("UnspecifiedRegisterReceiverFlag")
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Application app = (Application) param.thisObject;

                    BroadcastReceiver receiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if ("com.uy_li.TOGGLE_CADENCE".equals(intent.getAction())) {
                                boolean state = intent.getBooleanExtra("STATE", true);
                                
                                // 获取用户在主界面设定的动态步频区间
                                minCadenceConfig = intent.getFloatExtra("MIN_CADENCE", 172f);
                                maxCadenceConfig = intent.getFloatExtra("MAX_CADENCE", 182f);
                                
                                if (isModifyCadence == state) return;
                                
                                isModifyCadence = state;
                                
                                if (isModifyCadence) {
                                    // 每次开启时重置状态，生成新的区间内随机步频
                                    mockStartTime = System.currentTimeMillis();
                                    baseStepOffset = -1f;
                                    currentCadence = minCadenceConfig + (float) (Math.random() * (maxCadenceConfig - minCadenceConfig));
                                }
                                
                                String statusMsg = isModifyCadence ? ("已开启 (范围 " + (int)minCadenceConfig + "-" + (int)maxCadenceConfig + ")") : "已关闭";
                                Toast.makeText(context, "FuckRun " + statusMsg, Toast.LENGTH_SHORT).show();
                                XposedBridge.log("JoyrunHook: " + pkg + " 收到广播，步频模拟 " + statusMsg);
                            }
                        }
                    };

                    IntentFilter filter = new IntentFilter("com.uy_li.TOGGLE_CADENCE");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        app.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
                    } else {
                        app.registerReceiver(receiver, filter);
                    }
                    
                    if (isModifyCadence && mockStartTime == 0) {
                        mockStartTime = System.currentTimeMillis();
                        baseStepOffset = -1f;
                        currentCadence = minCadenceConfig + (float) (Math.random() * (maxCadenceConfig - minCadenceConfig));
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("JoyrunHook: Broadcast hook skipped for " + pkg + ": " + t.getMessage());
        }

        // ==========================================
        // 2. 万能接口 Hook：动态锁定所有的 SensorEventListener
        // ==========================================
        try {
            // 尝试 Hook 标准的 SensorManager 和 SystemSensorManager
            hookRegisterListenerMethods(android.hardware.SensorManager.class);
            try {
                Class<?> sysManagerClass = XposedHelpers.findClass("android.hardware.SystemSensorManager", lpparam.classLoader);
                hookRegisterListenerMethods(sysManagerClass);
            } catch (Throwable ignored) {}

            // 关键适配：ColorOS / OnePlus OplusSensorManager
            try {
                Class<?> oplusManagerClass = XposedHelpers.findClass("com.oplus.sensor.OplusSensorManager", lpparam.classLoader);
                if (oplusManagerClass != null) {
                    hookRegisterListenerMethods(oplusManagerClass);
                    XposedBridge.log("JoyrunHook: 发现并准备 Hook OplusSensorManager in " + pkg);
                }
            } catch (Throwable ignored) {}

            XposedBridge.log("JoyrunHook: 成功建立系统级万能接口分发拦截器 in " + pkg);
        } catch (Throwable t) {
            XposedBridge.log("JoyrunHook: 建立万能接口拦截器失败: " + t.getMessage());
        }
    }

    /**
     * 遍历并 Hook 所有名为 registerListener 的方法
     */
    private void hookRegisterListenerMethods(Class<?> managerClass) {
        if (managerClass == null) return;
        
        for (Method method : managerClass.getDeclaredMethods()) {
            if ("registerListener".equals(method.getName())) {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.args.length > 0 && param.args[0] != null && param.args[0] instanceof SensorEventListener) {
                            SensorEventListener listener = (SensorEventListener) param.args[0];
                            hookSensorEventListener(listener);
                        }
                    }
                });
            }
        }
    }

    /**
     * 对具体的 SensorEventListener 类进行 onSensorChanged 拦截 (咽喉锁死)
     */
    private void hookSensorEventListener(SensorEventListener listener) {
        if (listener == null) return;
        
        Class<?> listenerClass = listener.getClass();
        String className = listenerClass.getName();
        
        if (!hookedListenerClasses.contains(className)) {
            hookedListenerClasses.add(className);
            try {
                XposedHelpers.findAndHookMethod(listenerClass, "onSensorChanged", SensorEvent.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!isModifyCadence) return;

                        SensorEvent event = (SensorEvent) param.args[0];
                        if (event == null || event.sensor == null || event.values == null) return;
                        
                        int type = event.sensor.getType();

                        // 拦截加速度(1) 和 计步器(19)、计步检测(18)
                        if (type == Sensor.TYPE_ACCELEROMETER || type == Sensor.TYPE_STEP_COUNTER || type == Sensor.TYPE_STEP_DETECTOR || type == 19) {
                            
                            long now = System.currentTimeMillis();
                            if (mockStartTime == 0) {
                                mockStartTime = now;
                                baseStepOffset = -1f;
                                currentCadence = minCadenceConfig + (float) (Math.random() * (maxCadenceConfig - minCadenceConfig));
                            }
                            
                            // 用时间流逝计算正弦波和步数
                            float elapsedSecs = (now - mockStartTime) / 1000f;
                            float freq = currentCadence / 60.0f; // 动态计算 Hz (如 175步/分钟 -> ~2.91Hz)

                            if (type == Sensor.TYPE_ACCELEROMETER && event.values.length >= 3) {
                                // 物理仿真计算：振幅微调，符合真实跑步
                                float zWave = 9.81f + (float) (Math.sin(elapsedSecs * Math.PI * 2 * freq) * 3.5f);
                                float yWave = (float) (Math.cos(elapsedSecs * Math.PI * 2 * freq) * 1.5f);
                                
                                float jitterY = (float) (Math.random() * 0.3f - 0.15f);
                                float jitterZ = (float) (Math.random() * 0.3f - 0.15f);
                                
                                event.values[0] = 0.0f;
                                event.values[1] = yWave + jitterY;
                                event.values[2] = zWave + jitterZ;
                                
                                // 每秒打印一次加速度日志（避免刷屏）
                                if ((int)elapsedSecs != lastLoggedSec) {
                                    lastLoggedSec = (int)elapsedSecs;
                                    XposedBridge.log("JoyrunHook: 加速度伪造中 t=" + (int)elapsedSecs + "s, z=" + String.format("%.2f", zWave) + ", 步频=" + (int)currentCadence);
                                }

                            } else if ((type == Sensor.TYPE_STEP_COUNTER || type == 19) && event.values.length >= 1) {
                                // 计步器修改
                                if (baseStepOffset < 0) {
                                    baseStepOffset = event.values[0];
                                    XposedBridge.log("JoyrunHook: 捕获到初始步数基准 baseStepOffset = " + baseStepOffset);
                                }
                                
                                float currentFakeSteps = baseStepOffset + (elapsedSecs * freq);
                                event.values[0] = currentFakeSteps;

                            } else if (type == Sensor.TYPE_STEP_DETECTOR && event.values.length >= 1) {
                                event.values[0] = 1.0f;
                            }

                            // 关键操作：伪造真实的时间戳
                            event.timestamp = SystemClock.elapsedRealtimeNanos();
                        }
                    }
                });
                XposedBridge.log("JoyrunHook: Successfully locked onto onSensorChanged for: " + className);
            } catch (Throwable t) {
                XposedBridge.log("JoyrunHook: Failed to hook onSensorChanged for: " + className + " -> " + t.getMessage());
            }
        }
    }
}