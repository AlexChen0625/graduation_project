package com.example.ibob0625.mysignin.history;


import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ibob0625.mysignin.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity{
    private FirebaseUser user;
    private TextView userNameText;
    private TextView listNumberText;
    private TextView hintText;
    private ListView flightList;
    private ArrayList<String> flightListKeys = new ArrayList<>();

    private int downloadPhotoCount = 0;
    private int avaliblePhotoCount = 0;
    private ArrayList<Bitmap> photoBitmapList = new ArrayList<>();
    private int photoDisplayIndex = 0;

    private int dataCount = 0;
    private List<Map<String, Object>> items = new ArrayList<Map<String,Object>>();

    HistoryInfoDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        userNameText = (TextView) findViewById(R.id.user_text);
        listNumberText = (TextView) findViewById(R.id.list_number_text);
        hintText = (TextView) findViewById(R.id.hint_text);
        flightList = (ListView) findViewById(R.id.flight_list);
        user = FirebaseAuth.getInstance().getCurrentUser();

        userNameText.setText("USER: " + user.getDisplayName());

        final DatabaseReference dataRef = FirebaseDatabase.getInstance().getReference("userFlightList").child(user.getUid());
        dataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listNumberText.setText("你的旅行次數: " + dataSnapshot.getChildrenCount() + "次");
                for (DataSnapshot ds : dataSnapshot.getChildren() ){
                    Map<String, Object> item = new HashMap<String, Object>();

                    String flightName = ds.getValue().toString();
                    String startTimeInMillisecond = ds.getKey();

                    Calendar date = Calendar.getInstance();
                    date.setTimeInMillis(Long.parseLong(startTimeInMillisecond));
                    String time = new SimpleDateFormat("EEE yyyy/MM/dd\na HH:mm:ss").format(date.getTime()); //turn millsec to format time

                    item.put("flightName", flightName);
                    item.put("startTime", time);
                    items.add(item);
                    flightListKeys.add(startTimeInMillisecond);

                    dataCount++;
                    if(dataCount == dataSnapshot.getChildrenCount()){ //if all data download is finished, set adapter
                        Log.d("download finished", "set adapter");
                        final SimpleAdapter adapter = new SimpleAdapter(
                                getApplicationContext(),
                                items,
                                R.layout.list_style,
                                new String[]{"flightName", "startTime"},
                                new int[]{R.id.first_text, R.id.second_text}
                        );
                        flightList.setAdapter(adapter);
                        hintText.setText("OK");
                    }

                }
            }
            @Override
            public void onCancelled(DatabaseError error){
                Log.e("get data error: ", error.getMessage());
            }
        });

        AdapterView.OnItemClickListener listClick = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                hintText.setText("照片下載中, 請耐心等待. . . ");
                flightList.setEnabled(false);
                flightList.setClickable(false);

                String s = flightListKeys.get(position);
                initPhotoBitmap(s);
            }

        };
        flightList.setOnItemClickListener(listClick);
    }

    void initPhotoBitmap(final String key){
        final DatabaseReference flightDataRef = FirebaseDatabase.getInstance().getReference("flightData")
                .child(user.getUid() + "_" + key + "_flight");
        flightDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {
                if(dataSnapshot.child("endTime").getValue() == null) {
                    hintText.setText("檔案讀取失敗，資料可能已經遺失！");
                    flightList.setEnabled(true);
                    flightList.setClickable(true);
                    return;
                }
                DataSnapshot photoFileRef = dataSnapshot.child("photoFileNameList");
                final String photoNumber = dataSnapshot.child("photoNumber").getValue().toString();
                if(photoNumber.equals("0"))
                    showHistoryInfoDialog(key);
                else {
                    hintText.setText("照片下載中, 請耐心等待. . . (1/" + photoNumber + ")");
                    for (DataSnapshot ds : photoFileRef.getChildren()) {
                        final String photoName = ds.getValue().toString();
                        final StorageReference imgRef = FirebaseStorage.getInstance().getReference().child(photoName);
                        imgRef.getBytes(1024 * 1024 * 4).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                Bitmap photoBm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                photoBitmapList.add(photoBm);
                                Log.d("download event", "success, add bm to list(with file name " + photoName + ")");
                                downloadPhotoCount++;
                                avaliblePhotoCount++;
                                if (String.valueOf(downloadPhotoCount).equals(photoNumber)) {
                                    Log.d("download event", "all download success");
                                    showHistoryInfoDialog(key);
                                }
                                else
                                    hintText.setText("照片下載中, 請耐心等待. . . (" + (downloadPhotoCount+1) + "/" + photoNumber + ")");

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                downloadPhotoCount++;
                                if (String.valueOf(downloadPhotoCount).equals(photoNumber)) {
                                    Log.d("download event", "all download finished");
                                    showHistoryInfoDialog(key);
                                }
                                else
                                    hintText.setText("照片下載中, 請耐心等待. . . (" + (downloadPhotoCount+1) + "/" + photoNumber + ")");
                                Log.e("Download failure", exception.toString());
                            }
                        });
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError error){
                Log.e("get data error: ", error.getMessage());
            }
        });
    }

    void showHistoryInfoDialog(final String key){
        dialog = new HistoryInfoDialog(HistoryActivity.this);

        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("flightData").child(user.getUid() + "_" + key + "_flight");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String flightName = dataSnapshot.child("flightName").getValue().toString();
                String startTime = dataSnapshot.child("startTime").getValue().toString();
                String endTime = dataSnapshot.child("endTime").getValue().toString();
                String spendTime = dataSnapshot.child("spendTime").getValue().toString();
                String photoNumber = dataSnapshot.child("photoNumber").getValue().toString();

                String displayPhotoNumber = "照了 " + photoNumber + " 張照片\n" +
                        "遺失照片: " + (downloadPhotoCount-avaliblePhotoCount) + "張";

                if(!isFinishing()) {
                    dialog.show(flightName, startTime, endTime, spendTime, displayPhotoNumber);
                    dialog.getMapButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            closeDialog();
                            Intent intent = new Intent();
                            intent.setClass(HistoryActivity.this,MapsActivity.class);
                            intent.putExtra("key",key);
                            startActivity(intent);
                        }
                    });
                    dialog.getCloseButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            closeDialog();
                        }
                    });

                    dialog.getPhotoListIndex().setText("0/0");

                    if(!photoNumber.equals("0") && photoBitmapList.size() != 0) {
                        dialog.getPagedownButton().setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                photoDisplayIndex++;
                                if (photoDisplayIndex == avaliblePhotoCount)
                                    photoDisplayIndex = 0;
                                dialog.getImageView().setImageBitmap(photoBitmapList.get(photoDisplayIndex));
                                dialog.getPhotoListIndex().setText((photoDisplayIndex + 1) + "/" + avaliblePhotoCount);
                            }
                        });
                        dialog.getPageupButton().setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                photoDisplayIndex--;
                                if (photoDisplayIndex == -1)
                                    photoDisplayIndex = avaliblePhotoCount - 1;
                                dialog.getImageView().setImageBitmap(photoBitmapList.get(photoDisplayIndex));
                                dialog.getPhotoListIndex().setText((photoDisplayIndex + 1) + "/" + avaliblePhotoCount);
                            }
                        });
                        dialog.getImageView().setImageBitmap(photoBitmapList.get(photoDisplayIndex));
                        dialog.getPhotoListIndex().setText((photoDisplayIndex + 1) + "/" + avaliblePhotoCount);
                    }
                }
                hintText.setText("OK");
                flightList.setEnabled(true);
                flightList.setClickable(true);
            }
            @Override
            public void onCancelled(DatabaseError error){
                Log.e("get data error: ", error.getMessage());
            }
        });
    }

    void closeDialog(){
        dialog.dismiss();
        downloadPhotoCount = 0;
        avaliblePhotoCount = 0;
        photoDisplayIndex = 0;
        for(Bitmap bm : photoBitmapList)
            bm.recycle();
        photoBitmapList.clear();
    }
}


