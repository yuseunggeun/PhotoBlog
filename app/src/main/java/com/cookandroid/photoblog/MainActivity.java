package com.cookandroid.photoblog;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {


    private static final int READ_MEDIA_IMAGES_PERMISSION_CODE = 1001;
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_CODE = 1002;

    //private static final String UPLOAD_URL = "http://127.0.0.1:8000/api_root/Post/";
    //private static final String UPLOAD_URL = "http://10.0.2.2:8000/api_root/Post/";
    private static final String UPLOAD_URL = "http://yuu3730.pythonanywhere.com/api_root/Post/";
    Uri imageUri = null;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    String filePath = getRealPathFromURI(imageUri);
                    executorService.execute(() -> {
                        String uploadResult;
                        try {
                            uploadResult = uploadImage(filePath);
                        } catch (IOException e) {
                            uploadResult = "Upload failed: " + e.getMessage();
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        String finalUploadResult = uploadResult;
                        handler.post(() -> Toast.makeText(MainActivity.this, finalUploadResult, Toast.LENGTH_LONG).show());
                    });
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button uploadButton = findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                                READ_MEDIA_IMAGES_PERMISSION_CODE);
                    } else {
                        openImagePicker();
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                READ_EXTERNAL_STORAGE_PERMISSION_CODE);
                    } else {
                        openImagePicker();
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_MEDIA_IMAGES_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }
    private String getRealPathFromURI(Uri contentUri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            String path = cursor.getString(columnIndex);
            cursor.close();
            return path;
        }
    }

    private String uploadImage(String imageUrl) throws IOException, JSONException {
        OutputStreamWriter outputStreamWriter = null;
        try {
            try {
                URL url = new URL(UPLOAD_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "JWT 4e977571933da926bae4a05b07a6edc929ea87b5");
                connection.setRequestProperty("Content-Type", "application/json");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("author", 1);
                jsonObject.put("title", "안드로이드-REST API 테스트");
                jsonObject.put("text", "안드로이드로 작성된 REST API 테스트 입력 입니다.");
                jsonObject.put("created_date", "2024-06-03T18:34:00+09:00");
                jsonObject.put("published_date", "2024-06-03T18:34:00+09:00");
                //jsonObject.put("image", imageUrl);
                outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
                outputStreamWriter.write(jsonObject.toString());
                outputStreamWriter.flush();
                connection.connect();
                if (connection.getResponseCode() == 200) {
                    Log.e("uploadImage", "Success");
                }
                connection.disconnect();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            Log.e("uploadImage", "Exception in uploadImage: " + e.getMessage());
        }
        Log.e("LogInTask", "Failed to login");
        throw new Error("failed to login");
    }
}