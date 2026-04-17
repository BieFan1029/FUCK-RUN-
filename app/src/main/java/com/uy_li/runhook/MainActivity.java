package com.uy_li.runhook;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.RangeSlider;

import java.io.DataOutputStream;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> overlayPermissionLauncher;
    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        sharedPrefs = getSharedPreferences("JoyrunConfig", MODE_PRIVATE);

        // 初始化步频配置 UI
        RangeSlider rangeSlider = findViewById(R.id.range_slider);
        TextView tvSliderValue = findViewById(R.id.tv_slider_value);
        
        float minVal = sharedPrefs.getFloat("min_cadence", 172.0f);
        float maxVal = sharedPrefs.getFloat("max_cadence", 182.0f);
        rangeSlider.setValues(minVal, maxVal);
        tvSliderValue.setText((int) minVal + " - " + (int) maxVal);

        rangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            float min = slider.getValues().get(0);
            float max = slider.getValues().get(1);
            tvSliderValue.setText((int) min + " - " + (int) max);
            
            // 实时保存到 SharedPreferences
            sharedPrefs.edit()
                    .putFloat("min_cadence", min)
                    .putFloat("max_cadence", max)
                    .apply();
        });

        // 注册现代的 ActivityResultCallback 处理悬浮窗授权回调
        overlayPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.canDrawOverlays(this)) {
                            // 授权成功
                            startFloatingServiceAndFinish();
                        } else {
                            // 拒绝授权
                            Toast.makeText(this, "需要悬浮窗权限才能显示控制按钮", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // 绑定按钮事件
        MaterialButton btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(v -> checkOverlayPermissionAndStart());

        // 适配应用按钮
        MaterialButton btnApps = findViewById(R.id.btn_apps);
        btnApps.setOnClickListener(v -> {
            Intent intent = new Intent(this, AppsActivity.class);
            startActivity(intent);
        });
    }

    private void checkOverlayPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 第一步：检查是否已经有权限
            if (Settings.canDrawOverlays(this)) {
                startFloatingServiceAndFinish();
                return;
            }

            // 第二步：尝试通过 Root 静默授权
            boolean rootGranted = grantOverlayPermissionViaRoot();
            
            if (rootGranted) {
                try {
                    // 第三步：等待 300ms 让系统状态刷新
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                // 再次检查权限
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "已通过 Root 自动授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                    startFloatingServiceAndFinish();
                    return;
                }
            }

            // 第四步：优雅降级，跳转到系统设置页面手动授权
            Toast.makeText(this, "Root 授权失败，请手动开启悬浮窗", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            overlayPermissionLauncher.launch(intent);
            
        } else {
            // Android 6.0 以下默认拥有悬浮窗权限
            startFloatingServiceAndFinish();
        }
    }

    /**
     * 尝试通过 Root 执行 appops 命令静默授予悬浮窗权限
     */
    private boolean grantOverlayPermissionViaRoot() {
        Process process = null;
        DataOutputStream os = null;
        try {
            // 请求 Root 权限
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            
            // 执行 appops 提权命令
            String command = "appops set " + getPackageName() + " SYSTEM_ALERT_WINDOW allow\n";
            os.writeBytes(command);
            os.writeBytes("exit\n");
            os.flush();
            
            // 等待命令执行完毕并获取退出状态码
            int exitValue = process.waitFor();
            return exitValue == 0;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startFloatingServiceAndFinish() {
        Intent intent = new Intent(this, FloatingService.class);
        startService(intent);
        Toast.makeText(this, "悬浮窗已开启", Toast.LENGTH_SHORT).show();
        // 启动完毕后调用 moveTaskToBack(true) 将程序退到后台而不是直接销毁，保留界面状态
        moveTaskToBack(true);
    }
}