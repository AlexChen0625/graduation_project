package com.example.ibob0625.mysignin.history;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.ibob0625.mysignin.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
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


import java.util.ArrayList;
import java.util.HashMap;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{
    private String tests;

    //建立hashmap
    private HashMap<Marker, Bitmap> hash = new HashMap<Marker, Bitmap>();
    private GoogleMap mMap;
    private ArrayList<LatLng> traceOfMe;
    private FirebaseUser user;
    private String key;

    private StorageReference rootStorageRef;
    private DatabaseReference rootDatabaseRef;

    public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private Activity context;

        public CustomInfoWindowAdapter(Activity context){
            this.context = context;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            View view = context.getLayoutInflater().inflate(R.layout.infowindow, null);
            //說定圖示、標題和說明
            ImageView badge = (ImageView) view.findViewById(R.id.badge);
            //根據hash設定照片
            badge.setImageBitmap(hash.get(marker));
            ///傳回自訂的訊息視窗
            return view;
        }

        @Override
        public View getInfoContents(Marker marker) {
            View view = context.getLayoutInflater().inflate(R.layout.infocontent, null);

            TextView tvTitle = (TextView) view.findViewById(R.id.tv_title);
            TextView tvSubTitle = (TextView) view.findViewById(R.id.tv_subtitle);
            tvTitle.setText(marker.getTitle());
            tvSubTitle.setText(marker.getSnippet());

            return view;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        key = getIntent().getStringExtra("key");
        user = FirebaseAuth.getInstance().getCurrentUser();

        rootStorageRef = FirebaseStorage.getInstance().getReference();
        rootDatabaseRef = FirebaseDatabase.getInstance().getReference();

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        //新建Marker 抓手機照片 push到hash
        CustomInfoWindowAdapter adapter = new CustomInfoWindowAdapter(MapsActivity.this);
        googleMap.setInfoWindowAdapter(adapter);

        setMarker();
        setTrack();
        mMap.animateCamera(CameraUpdateFactory.zoomBy(13));
    }
    private void insertMarker(Bitmap bMap, double lat, double lng){
        Marker marker = mMap.addMarker((new MarkerOptions().position(new LatLng(lat,lng)).title(String.valueOf(lat)).snippet(String.valueOf(lng))));
        hash.put(marker, bMap);
        marker.hideInfoWindow();
    }

    private void trackToMe(double lat, double lng){
        if (traceOfMe == null) {
            traceOfMe = new ArrayList<LatLng>();
        }
        traceOfMe.add(new LatLng(lat, lng));

        PolylineOptions polylineOpt = new PolylineOptions();
        for (LatLng latlng : traceOfMe) {
            polylineOpt.add(latlng);
        }
        polylineOpt.color(Color.RED);
        Polyline line = mMap.addPolyline(polylineOpt);
        line.setWidth(20);
    }

    private void setMarker(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("flightData").child(user.getUid() + "_" + key + "_flight").child("photoFileNameList");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren() ){
                    String key = ds.getValue().toString().split("\\.")[0];
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("photo").child(key);
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String fileName = dataSnapshot.child("fileName").getValue().toString();
                            final String latitude = dataSnapshot.child("latitude").getValue().toString();
                            final String longitude = dataSnapshot.child("longitude").getValue().toString();
                            StorageReference imgRef = rootStorageRef.child(fileName); //info[0] = name
                            imgRef.getBytes(1024 * 1024 * 4).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] bytes) {
                                    insertMarker(BitmapFactory.decodeByteArray(bytes, 0, bytes.length), Double.parseDouble(latitude), Double.parseDouble(longitude));
                                    mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude))));
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    Log.e("STORAGE fail Download", exception.toString());
                                }
                            });
                        }
                        @Override
                        public void onCancelled(DatabaseError error){
                            Log.e("get data error: ", error.getMessage());
                        }
                    });
                }
            }
            @Override
            public void onCancelled(DatabaseError error){
                Log.e("get data error: ", error.getMessage());
            }
        });
    }

    public void setTrack(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("flightData").child(user.getUid() + "_" + key + "_flight").child("flyingPath");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren() ){
                    String latitude = ds.getValue().toString().split(",")[0];
                    String longitude = ds.getValue().toString().split(",")[1];

                    trackToMe(Double.parseDouble(latitude), Double.parseDouble(longitude));
                }
            }
            @Override
            public void onCancelled(DatabaseError error){
                Log.e("get data error: ", error.getMessage());
            }
        });
    }
}
