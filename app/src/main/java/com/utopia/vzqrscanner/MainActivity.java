package com.utopia.vzqrscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.io.IOException;
import java.util.List;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

    CameraView cameraView;
    Button btn, btn_2;
    AlertDialog wait_dialog;
    static final int GALLERY_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraView = findViewById(R.id.cameraView);
        btn = findViewById(R.id.btn);
        btn_2 = findViewById(R.id.btn_2);

        wait_dialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Ожидайте")
                .setCancelable(false)
                .build();

        btn.setOnClickListener(v -> {
            cameraView.start();
            cameraView.captureImage();
        });

        btn_2.setOnClickListener(v -> {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, GALLERY_REQUEST);
        });

        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                wait_dialog.show();
                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap, cameraView.getWidth(), cameraView.getHeight(), false);
                cameraView.stop();
                runDetector(bitmap);
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });
    }


    private void runDetector(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_PDF417
                ).build();

        BarcodeScanner scanner = BarcodeScanning.getClient(options);

        Task<List<Barcode>> result = scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for(Barcode item : barcodes){
                        int value_type = item.getValueType();
                        switch (value_type){
                            case Barcode.TYPE_TEXT:
                            {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setMessage(item.getRawValue());
                                builder.setPositiveButton("ОК", (dialog, which) -> dialog.dismiss());
                                AlertDialog dialog = builder.create();
                                    dialog.show();
                            }
                            break;

                            case Barcode.TYPE_URL:
                            {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getRawValue()));
                                startActivity(intent);
                            }
                            break;

                            case Barcode.TYPE_CONTACT_INFO:
                            {
                                String info = "Name: " +
                                        item.getContactInfo().getName().getFormattedName() +
                                        "\n" +
                                        "Adress: " +
                                        item.getContactInfo().getAddresses().get(0).getAddressLines() +
                                        "\n" +
                                        "Email: " +
                                        item.getContactInfo().getEmails().get(0).getAddress();
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setMessage(info);
                                builder.setPositiveButton("ОК", (dialog, which) -> dialog.dismiss());
                                AlertDialog dialog = builder.create();
                                dialog.show();
                            }
                            break;

                            default:
                                break;
                        }
                    }
                    wait_dialog.dismiss();
                })
                .addOnFailureListener(e -> {

                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        Bitmap getted_bitmap = null;

        if (requestCode == GALLERY_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri selectedImage = imageReturnedIntent.getData();
                try {
                    getted_bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                    Bitmap bitmap = Bitmap.createScaledBitmap(getted_bitmap, cameraView.getWidth(), cameraView.getHeight(), false);
                    runDetector(bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }
}
