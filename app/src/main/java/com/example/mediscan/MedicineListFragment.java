package com.example.mediscan;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.Executors;

public class MedicineListFragment extends Fragment {

    private RecyclerView recyclerView;
    private EditText etSearch;
    private TextView tvEmptyState;
    private MedicineAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_medicine_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerView);
        etSearch = view.findViewById(R.id.etSearch);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MedicineAdapter(new java.util.ArrayList<>(), this::onDeleteMedicine);
        recyclerView.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadMedicines(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadMedicines("");
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMedicines(etSearch.getText().toString());
    }

    private void loadMedicines(String searchQuery) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<Medicine> results;
            if (searchQuery == null || searchQuery.trim().isEmpty()) {
                results = db.medicineDao().getAllOrderedByExpiry();
            } else {
                results = db.medicineDao().searchByName("%" + searchQuery.trim() + "%");
            }

            requireActivity().runOnUiThread(() -> {
                adapter.updateList(results);
                tvEmptyState.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void onDeleteMedicine(Medicine medicine) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            db.medicineDao().delete(medicine);
            requireActivity().runOnUiThread(() -> loadMedicines(etSearch.getText().toString()));
        });
    }
}