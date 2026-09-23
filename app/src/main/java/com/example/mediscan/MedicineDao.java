package com.example.mediscan;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MedicineDao {

    @Insert
    long insert(Medicine medicine);

    @Update
    void update(Medicine medicine);

    @Delete
    void delete(Medicine medicine);


    @Query("Select * from medicines order by expiryDate ASC")
    List<Medicine> getAllOrderedByExpiry();

    @Query("SELECT * FROM medicines WHERE name LIKE :searchTerm ORDER BY expiryDate ASC")
    List<Medicine> searchByName(String searchTerm);

    @Query("SELECT * FROM medicines") // adjust table name if different
    List<Medicine> getAllSync();


}
