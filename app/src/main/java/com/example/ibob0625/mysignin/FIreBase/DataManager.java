package com.example.ibob0625.mysignin.FIreBase;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class DataManager {
    private File filesDir;
    private String ImgFilesDir;

    private Gson gson;
    private static Context context;
    private StorageReference fbStorageRef;
    private DatabaseReference photo_ref;
    private DatabaseReference flight_ref;
    private DatabaseReference user_flight_list_ref;

    private int photo_counter = 0;
    private int upload_fail_counter = 0;
    private int totalPhoto;
    private boolean uploadIsFinished = false;

    public DataManager() {
    }

    public DataManager(Context context) {
        this.context = context;

        gson = new Gson();
        filesDir = context.getFilesDir();
        ImgFilesDir = Environment.getExternalStorageDirectory().toString().concat("/ARSDKMedias/");
    }

    public File getFilesDir() {
        return filesDir;
    }

    public void uploadImgFile(String path, final TextView progressText) {
        final Uri file = Uri.fromFile(new File(path));
        final File f = new File(path);

        fbStorageRef = FirebaseStorage.getInstance().getReference().child(file.getLastPathSegment());
        UploadTask uploadTask = fbStorageRef.putFile(file);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e("upload failure", exception.toString());
                photo_counter++;
                upload_fail_counter++;
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d("upload listener ", "success");
                if (f.delete())
                    Log.d("delete file with path ", f.getPath());
                photo_counter++;
                if (photo_counter == totalPhoto) {
                    progressText.setText("檔案上傳完成\n照片上傳完成\n失敗: " + upload_fail_counter + "張");
                    uploadIsFinished = true;
                } else
                    progressText.setText("檔案上傳完成\n" +
                            "照片上傳中. . . (" + (int) (photo_counter + 1) + "/" + totalPhoto + ")");
            }
        });
    }

    public flightData jsonToObject(File f) {
        flightData d = gson.fromJson(readFromFile(f), flightData.class);
        return d;
    }

    public void uploadAllData(final flightData mData, final TextView progressText) {
        totalPhoto = mData.getPhotoNumber();
        progressText.setText("檔案上傳中. . . \n");
        flight_ref = FirebaseDatabase.getInstance().getReference("flightData");
        flight_ref.child(mData.getJsonFileName().split("\\.")[0]).setValue(mData, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                final String childName = mData.getStartTimeWithMillSec() + "_flight";
                user_flight_list_ref = FirebaseDatabase.getInstance().getReference("userFlightList").child(mData.getUserID());
                user_flight_list_ref.child(String.valueOf(mData.getStartTimeWithMillSec())).setValue(mData.getFlightName(), new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        progressText.setText("檔案上傳完成\n" +
                                "照片上傳中. . . ");
                        for (int c = 0; c < mData.getPhotoList().size(); c++) {
                            Photo p = mData.getPhotoList().get(c);
                            final String path = ImgFilesDir + p.getFileName();

                            photo_ref = FirebaseDatabase.getInstance().getReference("photo");
                            photo_ref.child(p.getFileName().split("\\.")[0]).setValue(p);
                            uploadImgFile(path, progressText);

                        }

                    }
                });
            }
        });

        context.deleteFile(mData.getJsonFileName());
        Log.d("delete file with name: ", mData.getJsonFileName());
    }

    public boolean getUploadIsFinished() {
        return uploadIsFinished;
    }

    public String readFromFile(File fin) {
        StringBuilder data = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(fin), "utf-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                data.append(line);
            }
        } catch (Exception e) {
            ;
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
                ;
            }
        }
        return data.toString();
    }

    private void writeToFile(File fout, String data) {
        FileOutputStream osw = null;
        try {
            osw = new FileOutputStream(fout);
            osw.write(data.getBytes());
            osw.flush();
        } catch (Exception e) {
            ;
        } finally {
            try {
                osw.close();
            } catch (Exception e) {
                ;
            }
        }
    }
}
