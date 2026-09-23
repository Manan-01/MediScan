package com.example.mediscan;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
public class AiUsageTracker {
    private static final int DAILY_SOFT_LIMIT = 15;

    public static boolean canUseAiToday(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ai_usage", Context.MODE_PRIVATE);
        String today = LocalDate.now().toString();
        String savedDate = prefs.getString("date", "");
        int count = savedDate.equals(today) ? prefs.getInt("count", 0) : 0;
        return count < DAILY_SOFT_LIMIT;
    }

    public static void recordAiCall(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ai_usage", Context.MODE_PRIVATE);
        String today = LocalDate.now().toString();
        String savedDate = prefs.getString("date", "");
        int count = savedDate.equals(today) ? prefs.getInt("count", 0) : 0;
        prefs.edit().putString("date", today).putInt("count", count + 1).apply();
    }
}
