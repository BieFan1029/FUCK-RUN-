package com.uy_li.runhook;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps);

        RecyclerView rvApps = findViewById(R.id.rv_apps);
        rvApps.setLayoutManager(new LinearLayoutManager(this));

        List<AppInfo> apps = new ArrayList<>();
        apps.add(new AppInfo("闪动校园", "com.huachenjie.shandong_school"));
        apps.add(new AppInfo("闪动校园PRO", "com.huachenjie.shandong_school_pro"));
        apps.add(new AppInfo("体适能", "com.bxkj.student"));
        apps.add(new AppInfo("运动世界校园", "com.zjwh.android_wh_physicalfitness"));
        apps.add(new AppInfo("宥马运动", "android.youma.com"));

        rvApps.setAdapter(new AppAdapter(apps));
    }

    public static class AppInfo {
        public String name;
        public String packageName;
        public AppInfo(String name, String packageName) {
            this.name = name;
            this.packageName = packageName;
        }
    }
}
