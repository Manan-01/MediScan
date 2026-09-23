package com.example.mediscan;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MedicineListFragment extends Fragment implements MedicineAdapter.OnMedicineActionListener {

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
        adapter = new MedicineAdapter(new ArrayList<>(), this);
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
            List<Medicine> all = db.medicineDao().getAllOrderedByExpiry();
            List<Medicine> filtered = new ArrayList<>();

            String query = (searchQuery == null) ? "" : searchQuery.trim().toLowerCase();

            for (Medicine m : all) {
                if (query.isEmpty()) {
                    filtered.add(m);
                } else {
                    boolean matchName = m.name != null && m.name.toLowerCase().contains(query);
                    boolean matchAssigned = m.assignedTo != null && m.assignedTo.toLowerCase().contains(query);
                    boolean matchLocation = m.location != null && m.location.toLowerCase().contains(query);
                    boolean matchBatch = m.batchNumber != null && m.batchNumber.toLowerCase().contains(query);
                    if (matchName || matchAssigned || matchLocation || matchBatch) {
                        filtered.add(m);
                    }
                }
            }

            requireActivity().runOnUiThread(() -> {
                adapter.updateList(filtered);
                tvEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    @Override
    public void onDelete(Medicine medicine) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            db.medicineDao().delete(medicine);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Removed: " + medicine.name, Toast.LENGTH_SHORT).show();
                loadMedicines(etSearch.getText().toString());
            });
        });
    }

    @Override
    public void onMarkUsed(Medicine medicine) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            medicine.status = "USED";
            db.medicineDao().update(medicine);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Marked as Used/Disposed: " + medicine.name, Toast.LENGTH_SHORT).show();
                loadMedicines(etSearch.getText().toString());
            });
        });
    }
}
