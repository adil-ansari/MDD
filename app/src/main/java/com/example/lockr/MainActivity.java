package com.example.lockr;

import android.app.AppOpsManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lockr.adapter.LockedAppAdapter;
import com.example.lockr.model.AppModel;
import com.example.lockr.services.BackgroundManager;
import com.example.lockr.shared.SharedPrefUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    static List<AppModel> lockedAppsList = new ArrayList<>();
    static Context context;
    ImageView allAppsBtn;
    LockedAppAdapter lockedAppsAdapter = new LockedAppAdapter(lockedAppsList, context);
    RecyclerView recyclerView;
    LockedAppAdapter adapter;
    ProgressDialog progressDialog;
    LinearLayout emptyLockListInfo, blockingInfoLayout;
    RelativeLayout enableUsageAccess, enableOverlayAccess;
    TextView btnEnableUsageAccess, btnEnableOverlay,blockingScheduleDescription,scheduleMode ;
    ImageView checkBoxOverlay, checkBoxUsage;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(" Locked Apps");
        setTheme(R.style.Theme_Appsift);
        setContentView(R.layout.activity_main);
        BackgroundManager.getInstance().init(this).startService();
        addIconToBar();
        progressDialog = new ProgressDialog(this);
        emptyLockListInfo = findViewById(R.id.emptyLockListInfo);
        allAppsBtn = findViewById(R.id.all_apps_button_img);
        enableOverlayAccess = findViewById(R.id.permissionsBoxDisplay);
        enableUsageAccess = findViewById(R.id.permissionsBoxUsage);
        btnEnableOverlay = findViewById(R.id.enableStatusDisplay);
        btnEnableUsageAccess = findViewById(R.id.enableStatusUsage);
        checkBoxOverlay = findViewById(R.id.checkedIconDisplay);
        checkBoxUsage = findViewById(R.id.checkedIconUsage);
        blockingInfoLayout = findViewById(R.id.blockingInfoLayout);
        blockingScheduleDescription = findViewById(R.id.blockingScheduleDescription);
        scheduleMode = findViewById(R.id.scheduleMode);
        showBlockingInfo();

        allAppsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, ShowAllApps.class);
                startActivity(myIntent);
            }
        });

        final Context context = this;
        getLockedApps(context);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_locked_apps);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_locked_apps:
                        return true;
                    case R.id.nav_all_apps:
                        startActivity(new Intent(getApplicationContext(),
                                ShowAllApps.class));
                        overridePendingTransition(0, 0);
                        return true;
                    case R.id.nav_settings:
                        startActivity(new Intent(getApplicationContext(),
                                About.class));
                        overridePendingTransition(0, 0);
                        return true;
                }
                return false;
            }
        });

        recyclerView = findViewById(R.id.lockedAppsListt);
        adapter = new LockedAppAdapter(lockedAppsList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        progressDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                getLockedApps(context);
            }
        });
        //toggle permissions box
        togglePermissionBox();
        checkAppsFirstTimeLaunch();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showBlockingInfo(){
        SharedPrefUtil prefUtil = SharedPrefUtil.getInstance(this);
        boolean checkSchedule = prefUtil.getBoolean("confirmSchedule");
        String startTimeHour = prefUtil.getStartTimeHour();
        String startTimeMin = prefUtil.getStartTimeMinute();
        String endTimeHour = prefUtil.getEndTimeHour();
        String endTimeMin = prefUtil.getEndTimeMinute();
        List<String> appsList = prefUtil.getLockedAppsList();
        List<String> days = prefUtil.getDaysList();
        List<String> shortDaysName = new ArrayList<>();
        days.forEach(day -> shortDaysName.add(day.substring(0,3)));
        if(appsList.size() > 0){
            if(checkSchedule){
                scheduleMode.setText("Every " +String.join(", ", shortDaysName) +" from "+ startTimeHour+":"+startTimeMin+" to "+endTimeHour+":"+endTimeMin);
            } else {
                scheduleMode.setText("Always Blocking");
            }
        } else {
            blockingInfoLayout.setVisibility(View.GONE);
        }
    }

    private void checkAppsFirstTimeLaunch() {
        /*Intent myIntent = new Intent(MainActivity.this, IntroScreen.class);
        MainActivity.this.startActivity(myIntent);*/
        boolean secondTimePref = SharedPrefUtil.getInstance(this).getBoolean("secondRun");
        if (!secondTimePref) {
            Intent myIntent = new Intent(MainActivity.this, IntroScreen.class);
            MainActivity.this.startActivity(myIntent);
            SharedPrefUtil.getInstance(this).putBoolean("secondRun", true);
        }
    }

    private void togglePermissionBox() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this) || !isAccessGranted()) {
                emptyLockListInfo.setVisibility(View.GONE);
                btnEnableOverlay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        overlayPermission();
                    }
                });
                btnEnableUsageAccess.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        accessPermission();
                    }
                });
                if (Settings.canDrawOverlays(this)) {
                    btnEnableOverlay.setVisibility(View.INVISIBLE);
                    checkBoxOverlay.setColorFilter(Color.GREEN);
                }
                if (isAccessGranted()) {
                    btnEnableUsageAccess.setVisibility(View.INVISIBLE);
                    checkBoxUsage.setColorFilter(Color.GREEN);
                }
            } else {
                enableUsageAccess.setVisibility(View.GONE);
                enableOverlayAccess.setVisibility(View.GONE);
                toggleEmptyLockListInfo(this);
            }
        }
    }

    private void addIconToBar() {
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.mipmap.ic_launcher_zz);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        setContentView(R.layout.activity_main);
    }

    public void getLockedApps(Context ctx) {
        toggleEmptyLockListInfo(ctx);
        List<String> prefAppList = SharedPrefUtil.getInstance(ctx).getLockedAppsList();
        List<ApplicationInfo> packageInfos = ctx.getPackageManager().getInstalledApplications(0);
        lockedAppsList.clear();
        for (int i = 0; i < packageInfos.size(); i++) {
            if (packageInfos.get(i).icon > 0) {
                String name = packageInfos.get(i).loadLabel(ctx.getPackageManager()).toString();
                Drawable icon = packageInfos.get(i).loadIcon(ctx.getPackageManager());
                String packageName = packageInfos.get(i).packageName;
                if (prefAppList.contains(packageName)) {
                    lockedAppsList.add(new AppModel(name, icon, 1, packageName));
                } else {
                    continue;
                }
            }
        }
        lockedAppsAdapter.notifyDataSetChanged();
        progressDialog.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();
        togglePermissionBox();
    }

    public void toggleEmptyLockListInfo(Context ctx) {
        List<String> prefAppList = SharedPrefUtil.getInstance(ctx).getLockedAppsList();
        if (prefAppList.size() > 0) {
            emptyLockListInfo.setVisibility(View.GONE);
        } else {
            emptyLockListInfo.setVisibility(View.VISIBLE);
        }
    }

    private boolean isAccessGranted() {
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
            AppOpsManager appOpsManager = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            }
            int mode = 0;
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.KITKAT) {
                mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        applicationInfo.uid, applicationInfo.packageName);
            }
            return (mode == AppOpsManager.MODE_ALLOWED);

        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void accessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isAccessGranted()) {
                Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivityForResult(intent, 102);
            }
        }
    }

    public void overlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivityForResult(myIntent, 101);
            }
        }
    }

}