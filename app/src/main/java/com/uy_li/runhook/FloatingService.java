package com.uy_li.runhook;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class FloatingService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    
    // 明确定义工作状态变量
    private boolean isWorking = true; // 默认与 Hook 类中保持一致开启
    // 防抖动时间戳
    private long lastClickTime = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 创建悬浮按钮
        Button button = new Button(this);
        button.setText("步频\nON");
        button.setTextColor(Color.WHITE);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        
        // 设置圆形背景
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(Color.parseColor("#4CAF50")); // 默认绿色
        button.setBackground(shape);

        // 悬浮窗参数配置
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                size,
                size,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 300; // 初始位置靠下一点，避免挡住顶部状态栏

        // 拖动和点击事件处理
        button.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private boolean isClick;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isClick = true;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                            isClick = false;
                        }
                        params.x = initialX + deltaX;
                        params.y = initialY + deltaY;
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (isClick) {
                            // 增加防抖动逻辑：500毫秒内只能点击一次
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastClickTime < 500) {
                                return true;
                            }
                            lastClickTime = currentTime;

                            // 切换工作状态
                            isWorking = !isWorking;
                            if (isWorking) {
                                shape.setColor(Color.parseColor("#4CAF50")); // 绿色
                                button.setText("步频\nON");
                            } else {
                                shape.setColor(Color.parseColor("#F44336")); // 红色
                                button.setText("步频\nOFF");
                            }
                            // 必须重新设置背景才生效
                            button.setBackground(shape);
                            
                            // 实时读取最新的用户配置步频
                            SharedPreferences sharedPrefs = getSharedPreferences("JoyrunConfig", MODE_PRIVATE);
                            float minCadence = sharedPrefs.getFloat("min_cadence", 172.0f);
                            float maxCadence = sharedPrefs.getFloat("max_cadence", 182.0f);

                            // 发送精准状态的单次广播
                            Intent broadcastIntent = new Intent("com.uy_li.TOGGLE_CADENCE");
                            broadcastIntent.putExtra("STATE", isWorking);
                            broadcastIntent.putExtra("MIN_CADENCE", minCadence);
                            broadcastIntent.putExtra("MAX_CADENCE", maxCadence);
                            sendBroadcast(broadcastIntent);
                        }
                        return true;
                }
                return false;
            }
        });

        floatingView = button;
        windowManager.addView(floatingView, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }
}