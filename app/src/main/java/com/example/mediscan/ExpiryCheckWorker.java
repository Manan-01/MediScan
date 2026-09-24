package com.example.mediscan;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.YearMonth;
import java.util.List;

public class ExpiryCheckWorker extends Worker {

    public ExpiryCheckWorker(@NonNull Context context, @NonNull WorkerParameters params){
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork(){
        Context context = getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(context);
        MedicineDao dao = db.medicineDao();

        List<Medicine> medicines = dao.getAllSync();
        YearMonth currentMonth = YearMonth.now();
        int notificationId = 1000;
        for (Medicine m : medicines) {
            if ("USED".equalsIgnoreCase(m.status) || "DISPOSED".equalsIgnoreCase(m.status)) {
                continue;
            }

            YearMonth expiry;
            try {
                expiry = YearMonth.parse(m.expiryDate);
            } catch (Exception e) {
                continue;
            }

            boolean isExpired = expiry.isBefore(currentMonth);
            boolean isExpiringSoon = expiry.equals(currentMonth) || expiry.equals(currentMonth.plusMonths(1));

            if (isExpired && !m.notifiedExpired) {
                NotificationHelper.showExpiredToday(context, m.name, notificationId + m.id);
                m.notifiedExpired = true;
                dao.update(m);
            } else if (isExpiringSoon && !m.notifiedSoon) {
                NotificationHelper.showExpiringSoon(context, m.name, notificationId + m.id);
                m.notifiedSoon = true;
                dao.update(m);
            }
        }
        return Result.success();
    }
}
