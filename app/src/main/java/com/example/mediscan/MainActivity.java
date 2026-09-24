package com.example.mediscan;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_THEME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    private BottomNavigationView bottomNavigationView;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean cameraGranted = result.getOrDefault(Manifest.permission.CAMERA, false);
                if (Boolean.TRUE.equals(cameraGranted)) {
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                    if (currentFragment instanceof ScanFragment) {
                        ((ScanFragment) currentFragment).checkAndStartCamera();
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applySavedTheme();
        super.onCreate(savedInstanceState);
        NotificationHelper.createChannel(this);

        List<String> permissionsToRequest = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ExpiryCheckWorker.class, 12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "expiry_check_daily",
                ExistingPeriodicWorkPolicy.UPDATE,
                request);

        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.theme_system) {
                setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                return true;
            } else if (id == R.id.theme_light) {
                setThemeMode(AppCompatDelegate.MODE_NIGHT_NO);
                return true;
            } else if (id == R.id.theme_dark) {
                setThemeMode(AppCompatDelegate.MODE_NIGHT_YES);
                return true;
            }
            return false;
        });

        bottomNavigationView = findViewById(R.id.bottomNav);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_scan) {
                switchFragment(new ScanFragment());
                return true;
            } else if (id == R.id.nav_medicines) {
                switchFragment(new MedicineListFragment());
                return true;
            }
            return false;
        });

        handleNotificationIntent(getIntent(), savedInstanceState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent, null);
    }

    private void handleNotificationIntent(Intent intent, Bundle savedInstanceState) {
        boolean openList = intent != null && intent.getBooleanExtra("open_list", false);
        if (openList) {
            bottomNavigationView.setSelectedItemId(R.id.nav_medicines);
            switchFragment(new MedicineListFragment());
        } else if (savedInstanceState == null) {
            switchFragment(new ScanFragment());
        }
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE);
        int mode = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    private void setThemeMode(int mode) {
        getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_THEME_MODE, mode)
                .apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Medicine> all = AppDatabase.getInstance(this).medicineDao().getAllSync();
            YearMonth currentMonth = YearMonth.now();
            long expiredCount = all.stream().filter(m -> {
                try {
                    return YearMonth.parse(m.expiryDate).isBefore(currentMonth);
                } catch (Exception e) {
                    return false;
                }
            }).count();

            if (expiredCount > 0) {
                NotificationHelper.showPersistingNag(this, (int) expiredCount, 9999);
            } else {
                NotificationManagerCompat.from(this).cancel(9999);
            }
        });
    }

    private void switchFragment(Fragment fragment){
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).commit();
    }
}
