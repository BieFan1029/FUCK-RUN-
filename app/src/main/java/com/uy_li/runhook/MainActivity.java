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
            tvSliderValue.setText((int) min + " - " + (int) max + " 步/分钟");
            
            // 实时保存到 SharedPreferences
            sharedPrefs.edit()
                    .putFloat("min_cadence", min)
                    .putFloat("max_cadence", max)
                    .apply();
        });

        // 注册悬浮窗授权回调
        overlayPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Settings.canDrawOverlays(this)) {
                            startFloatingServiceAndFinish();
                        } else {
                            Toast.makeText(this, "需要悬浮窗权限", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // 启动按钮
        MaterialButton btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(v -> checkOverlayPermissionAndStart());

        // 使用说明按钮
        MaterialButton btnHelp = findViewById(R.id.btn_help);
        btnHelp.setOnClickListener(v -> {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
        });
    }

    private void checkOverlayPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingServiceAndFinish();
                return;
            }

            // 尝试 Root 静默授权
            boolean rootGranted = grantOverlayPermissionViaRoot();
            
            if (rootGranted) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "已自动授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                    startFloatingServiceAndFinish();
                    return;
                }
            }

            Toast.makeText(this, "请手动开启悬浮窗权限", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            overlayPermissionLauncher.launch(intent);
            
        } else {
            startFloatingServiceAndFinish();
        }
    }

    private boolean grantOverlayPermissionViaRoot() {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            
            String command = "appops set " + getPackageName() + " SYSTEM_ALERT_WINDOW allow\n";
            os.writeBytes(command);
            os.writeBytes("exit\n");
            os.flush();
            
            int exitValue = process.waitFor();
            return exitValue == 0;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (os != null) os.close();
                if (process != null) process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startFloatingServiceAndFinish() {
        Intent intent = new Intent(this, FloatingService.class);
        startService(intent);
        Toast.makeText(this, "悬浮窗已启动", Toast.LENGTH_SHORT).show();
        moveTaskToBack(true);
    }
}
