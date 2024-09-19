package com.rahuldubey.ImageAnalysis.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.chrisbanes.photoview.PhotoView;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.rahuldubey.ImageAnalysis.R;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_SELECT = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private PhotoView imageView;
    private TextView textViewKm;
    private Button buttonSelectImage, buttonCaptureImage,btnNextActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        textViewKm = findViewById(R.id.textViewKm);
        buttonSelectImage = findViewById(R.id.buttonSelectImage);
        buttonCaptureImage = findViewById(R.id.buttonCaptureImage);
        btnNextActivity = findViewById(R.id.btnNextActivity);

        buttonSelectImage.setOnClickListener(v -> selectImageFromGallery());
        buttonCaptureImage.setOnClickListener(v -> requestCameraPermission());
        btnNextActivity.setOnClickListener( v ->{
            Intent intent = new Intent(getApplicationContext(), MLActivity.class);
            startActivity(intent);
        });
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            captureImage();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureImage();
            } else {
                Log.e("Permission", "Camera permission denied");
            }
        }
    }

    private void captureImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void selectImageFromGallery() {
        Intent selectImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(selectImageIntent, REQUEST_IMAGE_SELECT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                imageView.setImageBitmap(imageBitmap);
                processImage(FirebaseVisionImage.fromBitmap(imageBitmap));
            } else if (requestCode == REQUEST_IMAGE_SELECT) {
                Uri selectedImageUri = data.getData();

                try {
                    Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    imageView.setImageBitmap(imageBitmap);
                    Log.d("RahulD",imageBitmap.toString());
                    processImage(FirebaseVisionImage.fromBitmap(imageBitmap));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processImage(FirebaseVisionImage image) {
        FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        textRecognizer.processImage(image)
                .addOnSuccessListener(this::extractKilometersFromText)
                .addOnFailureListener(e -> Log.e("TextRecognition", "Failed to recognize text", e));
    }

    private void extractKilometersFromText(FirebaseVisionText firebaseVisionText) {
        String recognizedText = firebaseVisionText.getText().trim();
//
////        // Regular expressions for matching odometer readings
//        String kmPattern = "\\b\\d{5}\\b";   // Matches exactly 5 digits (e.g., 12345)
//        String kmAltPattern1 = "\\b\\d{6}\\b"; // Matches exactly 6 digits (e.g., 123456)
//        String kmAltPattern2 = "\\b\\d{3}\\b"; // Matches exactly 3 digits (e.g., 123)
//        String kmAltPattern3 = "\\b\\d{2}\\b"; // Matches exactly 2 digits (e.g., 12)
////        String regex = "\\b\\d+(?:,\\d+)?(?:\\.\\d+)?\\s?(?:km|kilometers|kilometer|km/h|kmph)\\b";
////        // Combining all patterns into one
//        String combinedPattern = kmPattern + "|" + kmAltPattern1 + "|" + kmAltPattern2 + "|" + kmAltPattern3;
////
////        // Compile the combined pattern
//        Pattern pattern = Pattern.compile(combinedPattern);
//        Matcher matcher = pattern.matcher(recognizedText);
//
//        // Check if any of the patterns match
//        if (matcher.find()) {
//            String kmValue = matcher.group();
//            textViewKm.setText("Kilometers: " + kmValue + " km");
//        } else {
//            textViewKm.setText("Kilometers: Not found");
//        }

        String regex = "\\b\\d+(?:,\\d+)?(?:\\.\\d+)?\\s?(?:km|kilometers|kilometer|km/h|kmph)\\b";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(recognizedText);

        if (matcher.find()) {
            String kmValue = matcher.group();
            textViewKm.setText("Kilometers: " + kmValue );
        } else {
            textViewKm.setText("Kilometers: Not found");
        }
    }
}
