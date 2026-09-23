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
    public String quantity;
    public String batchNumber;
    public String assignedTo;
    public String location;
    public String status; // "ACTIVE", "USED", "DISPOSED"

    public boolean notifiedSoon;
    public boolean notifiedExpired;
}
