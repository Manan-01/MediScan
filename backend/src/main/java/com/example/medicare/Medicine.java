package com.example.medicare;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

@Entity
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String name;
    public LocalDate expiryDate;
    public String dosage;
    public String quantity;
    public String batchNumber;
    public String assignedTo;
    public String location;

    @Transient
    public Long daysUntilExpiry;

    public Medicine() {

    }
}
