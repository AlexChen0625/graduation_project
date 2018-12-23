package com.example.ibob0625.mysignin.FIreBase;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ibob0625.mysignin.GateUI;
import com.example.ibob0625.mysignin.R;
import com.example.ibob0625.mysignin.droneActivity.DeviceListActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.w3c.dom.Text;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckFlightActivity extends AppCompatActivity {
    private FirebaseUser user;

    private static Context context;

    private TextView dateText;
    private TextView progressText;
    private ListView list;
    private Button uploadButton;
    private EditText flightNameText;

    private flightData mFlightData;
    private DataManager dataManager;

    List<Map<String, Object>> items = new ArrayList<Map<String,Object>>(); //for list adater mapping

    private ImageDialog dialog;
    private String ImgFilesDir = Environment.getExternalStorageDirectory().toString().concat("/ARSDKMedias/");

    private Button returnFromCheck;

    private state uploadState = state.waiting_upload;
    private enum state {
        waiting_upload,
        uploading
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_flight);

        user = FirebaseAuth.getInstance().getCurrentUser();
        context = getApplicationContext();
        dataManager = new DataManager(context);

        dateText = (TextView) findViewById(R.id.date_text);
        progressText = (TextView) findViewById(R.id.progress_text);
        uploadButton = (Button) findViewById(R.id.upload_button);
        list = (ListView) findViewById(R.id.list);
        flightNameText = (EditText) findViewById(R.id.editText);

        returnFromCheck = (Button) findViewById(R.id.returnFromCheck);

        Bundle mbundle = getIntent().getExtras();
        final String uid = mbundle.getString("uid");
        final long startTime = mbundle.getLong("startTime");
        final String fileName;

        fileName = uid + "_" + startTime + "_flight.json";
        File f = new File(dataManager.getFilesDir().getPath() + "/" + fileName);
        mFlightData = dataManager.jsonToObject(f);
        Log.d("flight data ", mFlightData.toString());

        flightNameText.setText(user.getDisplayName() + " 's travel");

        for(Photo p : mFlightData.getPhotoList()) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("fileName", p.getFileName());
            item.put("takedTime", p.getTakedTime());
            items.add(item);
        }
        final SimpleAdapter adapter = new SimpleAdapter(
                getApplicationContext(),
                items,
                R.layout.list_style,
                new String[]{"fileName", "takedTime"},
                new int[]{R.id.first_text, R.id.second_text}
        );
        list.setAdapter(adapter);

        AdapterView.OnItemClickListener listClick = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                list.setEnabled(false);

                //show photo with dialog
                String imgPath = ImgFilesDir + mFlightData.getPhotoFileNameList().get(position);
                File fileName = new File(imgPath);
                Bitmap bm = BitmapFactory.decodeFile(imgPath);

                if(!isFinishing()) {
                    dialog = new ImageDialog(CheckFlightActivity.this);
                    dialog.show();
                    dialog.getImageView().setImageBitmap(bm);
                }

                list.setEnabled(true);
            }

        };
        list.setOnItemClickListener(listClick);

        dateText.setText("起飛時間: " + mFlightData.getStartTime());

        View.OnClickListener havent_upload = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(dataManager.getUploadIsFinished()){
                    new AlertDialog.Builder(CheckFlightActivity.this)
                            .setMessage("你已經上傳過囉")
                            .setPositiveButton("好", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
                else if(uploadState == state.uploading){
                    new AlertDialog.Builder(CheckFlightActivity.this)
                            .setMessage("上傳中，請耐心等候")
                            .setPositiveButton("好", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
                else if(uploadState == state.waiting_upload) {
                    if(checkInternet()) {
                        mFlightData.setFlightName(flightNameText.getText().toString());
                        dataManager.uploadAllData(mFlightData, progressText);
                        uploadState = state.uploading;
                    }
                    else {
                        new AlertDialog.Builder(CheckFlightActivity.this)
                                .setTitle("沒有網路連線")
                                .setMessage("請檢查網路狀態")
                                .setPositiveButton("好", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                    }
                }

            }
        };

        uploadButton.setOnClickListener(havent_upload);

        returnFromCheck.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final Intent intent = new Intent();
                intent.setClass(CheckFlightActivity.this, GateUI.class);
                if(dataManager.getUploadIsFinished()) {
                    startActivity(intent);
                    finish();
                }
                else if(uploadState == state.uploading){
                    new AlertDialog.Builder(CheckFlightActivity.this)
                            .setMessage("檔案上傳中~")
                            .setPositiveButton("我等", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                }
                else if(uploadState == state.waiting_upload){
                    new AlertDialog.Builder(CheckFlightActivity.this)
                            .setMessage("檔案還沒上傳，請點擊UPLOAD按鈕進行上傳~")
                            .setPositiveButton("好", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("不上傳了，直接退出", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(intent);
                                    finish();
                                }
                            })
                            .show();
                }
            }
        });
    }

    public boolean checkInternet(){
        ConnectivityManager mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        NetworkInfo mWiFiNetworkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mMobileNetworkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        //如果未連線的話，mNetworkInfo會等於null
        if(mNetworkInfo != null) {
            if(!mNetworkInfo.isConnected())
                return false;
            if(!mNetworkInfo.isAvailable())
                return false;
            if(mNetworkInfo.isFailover())
                return false;
            if (mWiFiNetworkInfo != null && !mWiFiNetworkInfo.isAvailable())
                return false;
            if (mMobileNetworkInfo != null && !mMobileNetworkInfo.isAvailable())
                return false;
        }
        return true;
    }
}


