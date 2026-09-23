package com.example.mediscan;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.ViewHolder> {

    public interface OnDeleteClickListener {
        void onDelete(Medicine medicine);
    }

    private List<Medicine> medicines;
    private final OnDeleteClickListener deleteListener;

    public MedicineAdapter(List<Medicine> medicines, OnDeleteClickListener deleteListener) {
        this.medicines = medicines;
        this.deleteListener = deleteListener;
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

        YearMonth expiry = YearMonth.parse(medicine.expiryDate);
        LocalDate expiryDate = expiry.atEndOfMonth();
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);

        String expiryLabel;
        int color;
        if (daysLeft < 0) {
            expiryLabel = "EXPIRED";
            color = Color.parseColor("#C0392B");
        } else if (daysLeft <= 30) {
            expiryLabel = "Expires " + medicine.expiryDate + " (soon)";
            color = Color.parseColor("#E2725B");
        } else if (daysLeft <= 90) {
            expiryLabel = "Expires " + medicine.expiryDate;
            color = Color.parseColor("#D4A017");
        } else {
            expiryLabel = "Expires " + medicine.expiryDate;
            color = Color.parseColor("#2E8B57");
        }

        holder.tvItemExpiry.setText(expiryLabel);

        GradientDrawable dot = (GradientDrawable) holder.urgencyDot.getBackground().mutate();
        dot.setColor(color);

        holder.btnDelete.setOnClickListener(v -> deleteListener.onDelete(medicine));
    }

    @Override
    public int getItemCount() {
        return medicines.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvItemExpiry;
        View urgencyDot;
        android.widget.Button btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemExpiry = itemView.findViewById(R.id.tvItemExpiry);
            urgencyDot = itemView.findViewById(R.id.urgencyDot);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}