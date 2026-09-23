package com.example.mediscan;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "medicines")
public class Medicine {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String groupKey;
    public String expiryDate;
    public boolean notifiedSoon;
    public boolean notifiedExpired;
}