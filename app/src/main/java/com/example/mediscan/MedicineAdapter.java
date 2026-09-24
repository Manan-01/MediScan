package com.example.mediscan;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.ViewHolder> {

    public interface OnMedicineActionListener {
        void onDelete(Medicine medicine);
        void onMarkUsed(Medicine medicine);
    }

    private List<Medicine> medicines;
    private final OnMedicineActionListener listener;

    public MedicineAdapter(List<Medicine> medicines, OnMedicineActionListener listener) {
        this.medicines = medicines;
        this.listener = listener;
    }

    public void updateList(List<Medicine> newList) {
        this.medicines = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicine, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Medicine medicine = medicines.get(position);
        holder.tvItemName.setText(medicine.name);

        if (medicine.assignedTo != null && !medicine.assignedTo.isEmpty()) {
            holder.tvAssignedTo.setText(medicine.assignedTo);
            holder.tvAssignedTo.setVisibility(View.VISIBLE);
        } else {
            holder.tvAssignedTo.setVisibility(View.GONE);
        }

        if (medicine.location != null && !medicine.location.isEmpty()) {
            holder.tvLocation.setText("Location: " + medicine.location);
            holder.tvLocation.setVisibility(View.VISIBLE);
        } else {
            holder.tvLocation.setVisibility(View.GONE);
        }

        if (medicine.quantity != null && !medicine.quantity.isEmpty()) {
            holder.tvQuantity.setText("Qty: " + medicine.quantity);
            holder.tvQuantity.setVisibility(View.VISIBLE);
        } else {
            holder.tvQuantity.setVisibility(View.GONE);
        }

        if (medicine.batchNumber != null && !medicine.batchNumber.isEmpty()) {
            holder.tvBatchNumber.setText("Batch: " + medicine.batchNumber);
            holder.tvBatchNumber.setVisibility(View.VISIBLE);
        } else {
            holder.tvBatchNumber.setVisibility(View.GONE);
        }

        boolean isUsed = "USED".equalsIgnoreCase(medicine.status) || "DISPOSED".equalsIgnoreCase(medicine.status);

        YearMonth currentMonth = YearMonth.now();
        YearMonth expiry = YearMonth.parse(medicine.expiryDate);
        LocalDate expiryDate = expiry.atEndOfMonth();
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);

        String displayDate;
        try {
            String[] parts = medicine.expiryDate.split("-");
            displayDate = parts[1] + "/" + parts[0];
        } catch (Exception e) {
            displayDate = medicine.expiryDate;
        }

        String expiryLabel;
        int color;
        if (isUsed) {
            expiryLabel = "Status: Used / Disposed";
            color = Color.parseColor("#7F8C8D"); // Grey
        } else if (expiry.isBefore(currentMonth)) {
            expiryLabel = "EXPIRED (" + displayDate + ")";
            color = Color.parseColor("#C0392B"); // Red
        } else if (expiry.equals(currentMonth) || daysLeft <= 30) {
            expiryLabel = "Expires " + displayDate + " (Expiring soon)";
            color = Color.parseColor("#E2725B"); // Orange
        } else if (daysLeft <= 90) {
            expiryLabel = "Expires " + displayDate;
            color = Color.parseColor("#D4A017"); // Yellow
        } else {
            expiryLabel = "Expires " + displayDate;
            color = Color.parseColor("#2E8B57"); // Green
        }

        holder.tvItemExpiry.setText(expiryLabel);

        GradientDrawable dot = (GradientDrawable) holder.urgencyDot.getBackground().mutate();
        dot.setColor(color);

        if (isUsed) {
            holder.btnMarkUsed.setVisibility(View.GONE);
        } else {
            holder.btnMarkUsed.setVisibility(View.VISIBLE);
            holder.btnMarkUsed.setOnClickListener(v -> listener.onMarkUsed(medicine));
        }

        holder.btnDelete.setOnClickListener(v -> listener.onDelete(medicine));
    }

    @Override
    public int getItemCount() {
        return medicines.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvItemExpiry, tvAssignedTo, tvLocation, tvQuantity, tvBatchNumber;
        View urgencyDot;
        Button btnDelete, btnMarkUsed;

        ViewHolder(View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemExpiry = itemView.findViewById(R.id.tvItemExpiry);
            tvAssignedTo = itemView.findViewById(R.id.tvAssignedTo);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvBatchNumber = itemView.findViewById(R.id.tvBatchNumber);
            urgencyDot = itemView.findViewById(R.id.urgencyDot);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnMarkUsed = itemView.findViewById(R.id.btnMarkUsed);
        }
    }
}
