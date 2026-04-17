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
import android.widget.LinearLayout;
import android.widget.TextView;

public class FloatingService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private boolean isWorking = true;
    private long lastClickTime = 0;

    // 主题色
    private static final int COLOR_ON = Color.parseColor("#4A90D9");
    private static final int COLOR_OFF = Color.parseColor("#90A4AE");

    // 辅助方法：dp 转 px
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PX,
            dp * getResources().getDisplayMetrics().density,
            getResources().getDisplayMetrics()
        );
    }

    private int dpToPx2(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 创建悬浮窗容器 - 圆角半透明卡片风格
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);

        // 半透明毛玻璃背景
        final GradientDrawable bgShape = new GradientDrawable();
        int radius = dpToPx2(18);
        bgShape.setCornerRadii(new float[]{
            radius, radius, radius, radius,
            radius, radius, radius, radius
        });
        bgShape.setColor(Color.argb(180, 74, 144, 217)); // #4A90D9 at 70% opacity
        container.setBackground(bgShape);

        container.setPadding(
            dpToPx2(14),
            dpToPx2(10),
            dpToPx2(14),
            dpToPx2(10)
        );

        // 图标文字
        TextView tvIcon = new TextView(this);
        tvIcon.setText("👟");
        tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvIcon.setTextColor(Color.WHITE);

        // 状态文字
        final TextView tvStatus = new TextView(this);
        tvStatus.setText("ON");
        tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setPadding(dpToPx2(6), 0, 0, 0);

        container.addView(tvIcon);
        container.addView(tvStatus);

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20;
        params.y = 300;

        container.setOnTouchListener(new View.OnTouchListener() {
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

                        // 点击时加深背景
                        bgShape.setColor(Color.argb(220, 74, 144, 217));
                        container.setBackground(bgShape);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(deltaX) > 8 || Math.abs(deltaY) > 8) {
                            isClick = false;
                        }
                        params.x = initialX + deltaX;
                        params.y = initialY + deltaY;
                        windowManager.updateViewLayout(floatingView, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (isClick) {
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastClickTime < 500) {
                                return true;
                            }
                            lastClickTime = currentTime;

                            isWorking = !isWorking;

                            int currentColor = isWorking ? COLOR_ON : COLOR_OFF;

                            // 更新半透明背景
                            bgShape.setColor(Color.argb(180,
                                Color.red(currentColor), Color.green(currentColor), Color.blue(currentColor)));
                            container.setBackground(bgShape);

                            // 更新文字
                            tvStatus.setText(isWorking ? "ON" : "OFF");

                            SharedPreferences sharedPrefs = getSharedPreferences("JoyrunConfig", MODE_PRIVATE);
                            float minCadence = sharedPrefs.getFloat("min_cadence", 172.0f);
                            float maxCadence = sharedPrefs.getFloat("max_cadence", 182.0f);

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

        floatingView = container;
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
