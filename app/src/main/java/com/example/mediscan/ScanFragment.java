package com.example.mediscan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanFragment extends Fragment {

    private static final int MODE_TEXT = 0;
    private static final int BURST_SIZE = 3;

    private int currentMode = MODE_TEXT;

    private TabLayout modeTabs;
    private TextView tvHint;
    private PreviewView previewView;
    private Button btnScan, btnConfirmName, btnConfirmExpiry, btnSave, btnRescan;
    private ImageView ivPreview;
    private TextView tvResult, tvNameStatus, tvExpiryStatus;
    private EditText etName, etExpiry, etQuantity, etBatchNumber;
    private Spinner spinnerAssignedTo, spinnerLocation;

    private boolean nameConfirmed = false;
    private boolean expiryConfirmed = false;
    private String confirmedName = "";
    private String confirmedExpiry = "";

    private ImageCapture imageCapture;
    private final Executor cameraExecutor = Executors.newSingleThreadExecutor();
    private final TextRecognizer textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    private final BarcodeScanner barcodeScanner = BarcodeScanning.getClient();

    private int burstIndex = 0;
    private final List<String> burstRawTexts = new ArrayList<>();
    private final List<String> burstNameGuesses = new ArrayList<>();
    private final List<String> burstExpiryGuesses = new ArrayList<>();

    public void checkAndStartCamera() {
        if (isAdded() && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAndStartCamera();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        modeTabs = view.findViewById(R.id.modeTabs);
        tvHint = view.findViewById(R.id.tvHint);
        previewView = view.findViewById(R.id.previewView);
        btnScan = view.findViewById(R.id.btnScan);
        btnRescan = view.findViewById(R.id.btnRescan);
        btnConfirmName = view.findViewById(R.id.btnConfirmName);
        btnConfirmExpiry = view.findViewById(R.id.btnConfirmExpiry);
        btnSave = view.findViewById(R.id.btnSave);
        ivPreview = view.findViewById(R.id.ivPreview);
        tvResult = view.findViewById(R.id.tvResult);
        tvNameStatus = view.findViewById(R.id.tvNameStatus);
        tvExpiryStatus = view.findViewById(R.id.tvExpiryStatus);
        etName = view.findViewById(R.id.etName);
        etExpiry = view.findViewById(R.id.etExpiry);
        etQuantity = view.findViewById(R.id.etQuantity);
        etBatchNumber = view.findViewById(R.id.etBatchNumber);
        spinnerAssignedTo = view.findViewById(R.id.spinnerAssignedTo);
        spinnerLocation = view.findViewById(R.id.spinnerLocation);

        setupSpinners();
        checkAndStartCamera();

        modeTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentMode = tab.getPosition();
                updateHintText();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnScan.setOnClickListener(v -> {
            if (currentMode == MODE_TEXT) startBurstCapture();
            else captureBarcodeOnce();
        });

        btnRescan.setOnClickListener(v -> {
            nameConfirmed = false;
            expiryConfirmed = false;
            etName.setEnabled(true);
            etExpiry.setEnabled(true);
            btnConfirmName.setText("Confirm Name");
            btnConfirmExpiry.setText("Confirm Expiry");
            tvNameStatus.setVisibility(View.GONE);
            tvExpiryStatus.setVisibility(View.GONE);
            updateSaveButtonState();
            if (currentMode == MODE_TEXT) startBurstCapture();
            else captureBarcodeOnce();
        });

        btnConfirmName.setOnClickListener(v -> {
            if (nameConfirmed) {
                nameConfirmed = false;
                etName.setEnabled(true);
                btnConfirmName.setText("Confirm Name");
                tvNameStatus.setVisibility(View.GONE);
            } else {
                confirmedName = etName.getText().toString().trim();
                if (confirmedName.isEmpty()) {
                    Toast.makeText(requireContext(), "Name can't be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                nameConfirmed = true;
                etName.setEnabled(false);
                btnConfirmName.setText("Edit Name");
                tvNameStatus.setText("Confirmed: " + confirmedName);
                tvNameStatus.setVisibility(View.VISIBLE);
            }
            updateSaveButtonState();
        });

        btnConfirmExpiry.setOnClickListener(v -> {
            if (expiryConfirmed) {
                expiryConfirmed = false;
                etExpiry.setEnabled(true);
                btnConfirmExpiry.setText("Confirm Expiry");
                tvExpiryStatus.setVisibility(View.GONE);
            } else {
                confirmedExpiry = etExpiry.getText().toString().trim();
                if (confirmedExpiry.isEmpty()) {
                    Toast.makeText(requireContext(), "Expiry can't be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                expiryConfirmed = true;
                etExpiry.setEnabled(false);
                btnConfirmExpiry.setText("Edit Expiry");
                tvExpiryStatus.setText("Confirmed: " + confirmedExpiry);
                tvExpiryStatus.setVisibility(View.VISIBLE);
            }
            updateSaveButtonState();
        });

        btnSave.setOnClickListener(v -> {
            String sortableExpiry;
            try {
                String[] parts = confirmedExpiry.split("/");
                String month = parts[0].trim();
                if (month.length() == 1) month = "0" + month;
                String year = parts[1].trim();
                if (year.length() == 2) year = "20" + year;
                sortableExpiry = year + "-" + month;
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Expiry Date Format looks wrong (MM/YYYY) - please fix it and confirm again", Toast.LENGTH_LONG).show();
                return;
            }

            YearMonth expiryYearMonth = YearMonth.parse(sortableExpiry);
            YearMonth currentYearMonth = YearMonth.now();

            if (expiryYearMonth.isBefore(currentYearMonth)) {
                Toast.makeText(requireContext(), "This Medicine already expired (" + confirmedExpiry + ") - not saving it", Toast.LENGTH_LONG).show();
                return;
            }

            Medicine medicine = new Medicine();
            medicine.name = confirmedName;
            medicine.groupKey = confirmedName.trim().toLowerCase();
            medicine.expiryDate = sortableExpiry;
            medicine.quantity = etQuantity.getText().toString().trim();
            medicine.batchNumber = etBatchNumber.getText().toString().trim();
            medicine.assignedTo = spinnerAssignedTo.getSelectedItem() != null ? spinnerAssignedTo.getSelectedItem().toString() : "Self";
            medicine.location = spinnerLocation.getSelectedItem() != null ? spinnerLocation.getSelectedItem().toString() : "Kitchen Cabinet";
            medicine.status = "ACTIVE";
            medicine.notifiedSoon = false;
            medicine.notifiedExpired = false;

            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                db.medicineDao().insert(medicine);

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Saved to Digital Cabinet: " + confirmedName + " (" + medicine.assignedTo + ")", Toast.LENGTH_LONG).show();
                    resetForm();
                });
            });
        });

        updateHintText();
    }

    private void setupSpinners() {
        String[] familyOptions = {"Self (Me)", "Mom", "Dad", "Child", "Grandparents", "Other"};
        ArrayAdapter<String> familyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, familyOptions);
        spinnerAssignedTo.setAdapter(familyAdapter);

        String[] locationOptions = {"Kitchen Cabinet", "Fridge", "First-Aid Box", "Bedroom", "Bathroom Mirror", "Other"};
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, locationOptions);
        spinnerLocation.setAdapter(locationAdapter);
    }

    private void updateHintText() {
        if (currentMode == MODE_TEXT) {
            tvHint.setText("Frame the medicine name and expiry text, then tap Scan and hold steady.");
        } else {
            tvHint.setText("Align 2D GS1 DataMatrix barcode pattern, then tap Scan.");
        }
    }

    private void resetForm() {
        nameConfirmed = false;
        expiryConfirmed = false;
        confirmedName = "";
        confirmedExpiry = "";
        etName.setText("");
        etExpiry.setText("");
        etQuantity.setText("");
        etBatchNumber.setText("");
        etName.setEnabled(true);
        etExpiry.setEnabled(true);
        btnConfirmName.setText("Confirm Name");
        btnConfirmExpiry.setText("Confirm Expiry");
        btnConfirmName.setVisibility(View.VISIBLE);
        btnConfirmExpiry.setVisibility(View.VISIBLE);
        tvNameStatus.setVisibility(View.GONE);
        tvExpiryStatus.setVisibility(View.GONE);
        if (btnRescan != null) btnRescan.setVisibility(View.GONE);
        btnSave.setEnabled(false);
        ivPreview.setVisibility(View.GONE);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(requireContext(), "Failed to start the camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder().build();
        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to bind camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void startBurstCapture() {
        if (imageCapture == null) {
            Toast.makeText(requireContext(), "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        burstIndex = 0;
        burstRawTexts.clear();
        burstNameGuesses.clear();
        burstExpiryGuesses.clear();
        btnScan.setEnabled(false);
        tvHint.setText("Hold steady — scanning...");
        captureNextInBurst();
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void captureNextInBurst() {
        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                Image mediaImage = imageProxy.getImage();
                if (mediaImage == null) {
                    imageProxy.close();
                    onBurstFrameDone();
                    return;
                }
                InputImage inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                textRecognizer.process(inputImage)
                        .addOnSuccessListener(text -> {
                            String raw = text.getText();
                            burstRawTexts.add(raw);
                            burstNameGuesses.add(extractName(raw));
                            burstExpiryGuesses.add(extractExpiryDate(raw));
                        })
                        .addOnCompleteListener(task -> {
                            imageProxy.close();
                            onBurstFrameDone();
                        });
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                onBurstFrameDone();
            }
        });
    }

    private void onBurstFrameDone() {
        burstIndex++;

        if (burstIndex < BURST_SIZE) {
            captureNextInBurst();
        } else {
            finishBurstCapture();
        }
    }

    private void finishBurstCapture() {
        requireActivity().runOnUiThread(() -> {
            btnScan.setEnabled(true);
            if (btnRescan != null) btnRescan.setVisibility(View.VISIBLE);
            updateHintText();
            ivPreview.setVisibility(View.VISIBLE);

            tvResult.setText(String.join("\n---\n", burstRawTexts));

            if (!nameConfirmed) {
                String nameGuess = majorityVote(burstNameGuesses);
                if (!nameGuess.isEmpty()) etName.setText(nameGuess);
            }
            if (!expiryConfirmed) {
                String expiryGuess = majorityVote(burstExpiryGuesses);
                if (!expiryGuess.isEmpty()) etExpiry.setText(expiryGuess);
            }

            if (BackendClient.isNetworkAvailable(requireContext()) && AiUsageTracker.canUseAiToday(requireContext())) {
                tvHint.setText("Refining with Gemini AI...");
                Executors.newSingleThreadExecutor().execute(() -> {
                    AiUsageTracker.recordAiCall(requireContext());
                    BackendClient.parseMedicine(burstRawTexts, (success, name, expiry, errorMessage) -> {
                        requireActivity().runOnUiThread(() -> {
                            updateHintText();
                            if (success) {
                                if (!nameConfirmed && name != null && !name.isEmpty()) etName.setText(name);
                                if (!expiryConfirmed && expiry != null && !expiry.isEmpty()) etExpiry.setText(expiry);
                            }
                        });
                    });
                });
            }
        });
    }

    private String majorityVote(List<String> guesses) {
        Map<String, Integer> counts = new HashMap<>();
        Map<String, String> originalCasing = new HashMap<>();
        String best = "";
        int bestCount = 0;
        for (String g : guesses) {
            if (g == null || g.trim().isEmpty()) continue;
            String key = g.trim().toLowerCase();
            int c = counts.getOrDefault(key, 0) + 1;
            counts.put(key, c);
            originalCasing.putIfAbsent(key, g.trim());
            if (c > bestCount) {
                bestCount = c;
                best = originalCasing.get(key);
            }
        }
        return best;
    }

    private void captureBarcodeOnce() {
        if (imageCapture == null) {
            Toast.makeText(requireContext(), "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            @ExperimentalGetImage
            public void onCaptureSuccess(ImageProxy imageProxy) {
                Image mediaImage = imageProxy.getImage();
                if (mediaImage == null) {
                    imageProxy.close();
                    return;
                }
                InputImage inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                barcodeScanner.process(inputImage)
                        .addOnSuccessListener(ScanFragment.this::handleBarcodeResult)
                        .addOnCompleteListener(task -> imageProxy.close());
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Capture failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void handleBarcodeResult(List<Barcode> barcodes) {
        requireActivity().runOnUiThread(() -> {
            ivPreview.setVisibility(View.VISIBLE);
            if (btnRescan != null) btnRescan.setVisibility(View.VISIBLE);

            if (barcodes.isEmpty()) {
                Toast.makeText(requireContext(), "No barcode found — try again", Toast.LENGTH_SHORT).show();
                return;
            }
            Barcode barcode = barcodes.get(0);
            String rawValue = barcode.getRawValue();
            tvResult.setText(rawValue != null ? rawValue : "(empty barcode value)");

            if (rawValue != null && !expiryConfirmed) {
                String expiryGuess = extractExpiryFromGs1(rawValue);
                if (!expiryGuess.isEmpty()) {
                    etExpiry.setText(expiryGuess);
                } else {
                    Toast.makeText(requireContext(), "Couldn't read expiry from barcode — try Text Scan", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateSaveButtonState() {
        btnSave.setEnabled(nameConfirmed && expiryConfirmed);
    }

    private String extractExpiryDate(String rawText) {
        String[] lines = rawText.split("\n");
        Pattern numericDatePattern = Pattern.compile("\\d{1,2}[./-]\\d{2,4}");
        Pattern monthNamePattern = Pattern.compile(
                "(?i)\\b(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)[A-Z]*\\.?\\s*(\\d{2,4})\\b"
        );
        Pattern expKeyword = Pattern.compile("(?i)(EXP|EXPIRY|USE BY|BEST BEFORE|BBE)");
        Pattern mfgKeyword = Pattern.compile("(?i)(MFG|MFD|MANUFACTURE|MANUFACTURED|PKD|PACKED)");

        for (int i = 0; i < lines.length; i++) {
            Matcher keywordMatch = expKeyword.matcher(lines[i]);
            if (keywordMatch.find()) {
                String afterKeyword = lines[i].substring(keywordMatch.end());

                Matcher numericAfter = numericDatePattern.matcher(afterKeyword);
                if (numericAfter.find()) return numericAfter.group();

                Matcher monthAfter = monthNamePattern.matcher(afterKeyword);
                if (monthAfter.find()) return monthNameToSlashFormat(monthAfter.group(1), monthAfter.group(2));

                if (i + 1 < lines.length) {
                    Matcher numericNext = numericDatePattern.matcher(lines[i + 1]);
                    if (numericNext.find()) return numericNext.group();

                    Matcher monthNext = monthNamePattern.matcher(lines[i + 1]);
                    if (monthNext.find()) return monthNameToSlashFormat(monthNext.group(1), monthNext.group(2));
                }
            }
        }

        for (String line : lines) {
            if (mfgKeyword.matcher(line).find()) continue;
            Matcher m = numericDatePattern.matcher(line);
            if (m.find()) return m.group();
            Matcher mn = monthNamePattern.matcher(line);
            if (mn.find()) return monthNameToSlashFormat(mn.group(1), mn.group(2));
        }
        return "";
    }

    private String monthNameToSlashFormat(String monthAbbr, String year) {
        String[] months = {"JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC"};
        int monthIndex = -1;
        for (int i = 0; i < months.length; i++) {
            if (months[i].equalsIgnoreCase(monthAbbr)) {
                monthIndex = i;
                break;
            }
        }
        if (monthIndex == -1) return "";

        String fullYear = year.length() == 2 ? "20" + year : year;
        String monthNum = String.format("%02d", monthIndex + 1);
        return monthNum + "/" + fullYear;
    }

    private String extractName(String rawText) {
        String[] lines = rawText.split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) return line.trim();
        }
        return "";
    }

    private String extractExpiryFromGs1(String rawValue) {
        if (rawValue == null) return "";

        int pos = 0;
        while (pos + 2 <= rawValue.length()) {
            String ai = rawValue.substring(pos, pos + 2);
            pos += 2;

            switch (ai) {
                case "01": // GTIN — fixed 14 digits
                    if (pos + 14 > rawValue.length()) return "";
                    pos += 14;
                    break;

                case "17": // Expiry date — fixed 6 digits, YYMMDD
                    if (pos + 6 > rawValue.length()) return "";
                    String yymmdd = rawValue.substring(pos, pos + 6);
                    String yy = yymmdd.substring(0, 2);
                    String mm = yymmdd.substring(2, 4);
                    return mm + "/20" + yy;

                case "10": // Batch/lot — variable length
                    int gsIndex = rawValue.indexOf('\u001D', pos);
                    pos = (gsIndex == -1) ? rawValue.length() : gsIndex + 1;
                    break;

                default:
                    return "";
            }
        }
        return "";
    }
}
