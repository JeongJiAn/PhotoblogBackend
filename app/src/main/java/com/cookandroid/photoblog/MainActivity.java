package com.cookandroid.photoblog;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int READ_MEDIA_IMAGES_PERMISSION_CODE=1001;
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_CODE=1002;

    private static final String UPLOAD_URL="http://10.0.2.2:8000/api_root/Post/";
    Uri imageUri = null;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result-> {
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

    private String uploadImage(String filePath) throws IOException, JSONException {
        OutputStreamWriter outputStreamWriter = null;
        try {
            try {
                String boundary = "*****";
                URL url = new URL(UPLOAD_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "JWT c5912f357b287f248043c1e8e6bc5b3bb4939a01");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=\""+boundary+"\"");

                DataOutputStream request = new DataOutputStream(connection.getOutputStream());

                request.writeBytes("--"+boundary+"\r\n");
                request.writeBytes("Content-Disposition: form-data;name=\"author\"\r\n");
                request.writeBytes("Content-Type: text/plain; charset=UTF-8\r\n");
                request.writeBytes("\r\n");
                request.writeBytes("1"+"\r\n");

                request.writeBytes("--"+boundary+"\r\n");
                request.writeBytes("Content-Disposition: form-data;name=\"title\"\r\n");
                request.writeBytes("Content-Type: text/plain; charset=UTF-8\r\n");
                request.writeBytes("\r\n");
                request.writeBytes("안드로이드-REST API 테스트"+"\r\n");

                request.writeBytes("--"+boundary+"\r\n");
                request.writeBytes("Content-Disposition: form-data;name=\"text\"\r\n");
                request.writeBytes("Content-Type: text/plain; charset=UTF-8\r\n");
                request.writeBytes("\r\n");
                request.writeBytes("안드로이드로 작성된 REST API 테스트 입력 입니다."+"\r\n");

                request.writeBytes("--"+boundary+"\r\n");
                request.writeBytes("Content-Disposition: form-data;name=\"created_date\"\r\n");
                request.writeBytes("Content-Type: text/plain; charset=UTF-8\r\n");
                request.writeBytes("\r\n");
                request.writeBytes("2024-06-03T18:34:00+09:00"+"\r\n");

                request.writeBytes("--"+boundary+"\r\n");
                request.writeBytes("Content-Disposition: form-data;name=\"published_date\"\r\n");
                request.writeBytes("Content-Type: text/plain; charset=UTF-8\r\n");
                request.writeBytes("\r\n");
                request.writeBytes("2024-06-03T18:34:00+09:00"+"\r\n");

                request.writeBytes("--" + boundary + "\r\n");
                request.writeBytes("Content-Disposition: form-data; name=\"image\";filename=\""+filePath.split("/")[filePath.split("/").length - 1]+"\"\r\n");
                request.writeBytes("\r\n");
                File file = new File(filePath);
                byte[] bytes = Files.readAllBytes(file.toPath());
                request.write(bytes);

                request.writeBytes("\r\n");
                request.writeBytes("--"+boundary+"--\r\n");
                request.flush();

//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("author", 1);
//                jsonObject.put("title", "안드로이드-REST API 테스트");
//                jsonObject.put("text", "안드로이드로 작성된 REST API 테스트 입력 입니다.");
//                jsonObject.put("created_date", "2024-06-03T18:34:00+09:00");
//                jsonObject.put("published_date", "2024-06-03T18:34:00+09:00");
//                jsonObject.put("image", imageUri);

                outputStreamWriter = new OutputStreamWriter(connection.getOutputStream());
                outputStreamWriter.write(request.toString());
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
            Log.e("uploadImage", "Exception in uploadImage" + e.getMessage());
            return "uploadImage: Exception in uploadImage " + e.getMessage();
        }

        return "uploadImage: " + "Success";
//        Log.e("LoginTask", "Failed to login");
//        throw new Error("failed to login");
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button uploadButton = findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, READ_MEDIA_IMAGES_PERMISSION_CODE);
                    } else {
                        openImagePicker();
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION_CODE);
                    } else {
                        openImagePicker();
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResult) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResult);
        if (requestCode == READ_MEDIA_IMAGES_PERMISSION_CODE) {
            if (grantResult.length > 0 && grantResult[0] == PackageManager.PERMISSION_GRANTED) {
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


}