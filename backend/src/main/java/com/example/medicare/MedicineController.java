package com.example.medicare;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MedicineController {

    private final MedicineRepository repository;

    public MedicineController(MedicineRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/medicines")
    public List<Medicine> getAll() {
        return repository.findAll();
    }

    @DeleteMapping("/medicines/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @GetMapping("/alerts")
    public List<Medicine> getAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);
        List<Medicine> alerts = new ArrayList<>();
        for (Medicine m : repository.findAll()) {
            if (!m.expiryDate.isAfter(threshold)) {
                m.daysUntilExpiry = ChronoUnit.DAYS.between(today, m.expiryDate);
                alerts.add(m);
            }
        }
        return alerts;
    }

    @PostMapping("/medicines")
    public List<Medicine> add(@RequestBody List<Medicine> medicines) {
        for (Medicine m : medicines) {
            if (m.name == null || m.name.isBlank() || m.expiryDate == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name and expiry date are required");
            }
        }
        return repository.saveAll(medicines);
    }

}
