package com.example.mediscan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.PeriodicWorkRequest;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.WorkManager;
import androidx.work.OneTimeWorkRequest;

import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationHelper.createChannel(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ExpiryCheckWorker.class, 1, TimeUnit.DAYS)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "expiry_check_daily",
                ExistingPeriodicWorkPolicy.KEEP,
                request);

        setContentView(R.layout.activity_main);
        BottomNavigationView bottomNavigationView=findViewById(R.id.bottomNav);

        if(savedInstanceState==null){
            switchFragment(new ScanFragment());
        }

        bottomNavigationView.setOnItemSelectedListener(item->{
            int id=item.getItemId();
            if(id==R.id.nav_scan){
                switchFragment(new ScanFragment());
                return true;
            } else if (id==R.id.nav_medicines) {
                switchFragment(new MedicineListFragment());
                return true;
            }
            return false;
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Medicine> all = AppDatabase.getInstance(this).medicineDao().getAllSync();
            YearMonth currentMonth = YearMonth.now();
            long expiredCount = all.stream().filter(m -> {
                try {
                    return YearMonth.parse(m.expiryDate).compareTo(currentMonth) <= 0;
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
     getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer,fragment).commit();
    }
}