package com.example.ibob0625.mysignin.droneActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ibob0625.mysignin.FIreBase.CheckFlightActivity;
import com.example.ibob0625.mysignin.FIreBase.DataManager;
import com.example.ibob0625.mysignin.FIreBase.Photo;
import com.example.ibob0625.mysignin.FIreBase.flightData;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.example.ibob0625.mysignin.R;
import com.example.ibob0625.mysignin.drone.BebopDrone;
import com.example.ibob0625.mysignin.view.H264VideoView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect2d;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.tracking.Tracker;
import org.opencv.tracking.TrackerKCF;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING;
import static org.opencv.core.CvType.CV_8U;
import static org.opencv.imgproc.Imgproc.COLOR_RGBA2RGB;
import static org.opencv.imgproc.Imgproc.line;

public class BebopActivity extends AppCompatActivity {

    public double latitude;
    public double longitude;
    /*Firebase*/
    private static Context context;
    private Intent checkIntent;
    private Location location;
    private flightData mFlight;
    private Handler delayHandler;
    private Runnable task;
    private boolean isOnFlight;
    private LocationManager locationMgr;
    private String provider;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;
    private DataManager dataManager;

    private static final String TAG = "BebopActivity";
    private BebopDrone mBebopDrone;

    private ProgressDialog mConnectionProgressDialog;
    private ProgressDialog mDownloadProgressDialog;

    private H264VideoView mVideoView;

    private TextView mBatteryLabel;
    private Button mTakeOffLandBt;
    private FirebaseUser user;
    //追蹤用變數
    private ImageView img;
    private Button followBT;
    private Button setIMGBT;
    private boolean followSWH = false; //控制follow按鈕
    private boolean setIMGSWH = false; //控制setIMG按鈕
    private Bitmap map;
    private Handler handler;

    private View emergencyBT;
    private View takePictureBT;
    private View gazUpBT;
    private View gazDownBT;
    private View yawLeftBT;
    private View yawRightBT;
    private View forwardBT;
    private View backBT;
    private View rollLeftBT;
    private View rollRightBT;
    private boolean isSetIMG = false;
    private boolean isFollow = true;

    private Handler mUI_Handler = new Handler();

    private Handler mThreadHandler;

    private HandlerThread mThread;
    private Message disPlayMode;
    private Tracker tracker;
    private Rect2d roi = new Rect2d(0,0,250,250);
    private Rect2d tempBox;
    private Point[] pot = new Point[4];
    private int videoWidth;
    private int videoHeight;
    private double frameWidth;
    private double frameHeight;
    private byte isNew=1;
    private Mat trackFrame;
    private Bitmap srcBitmap;
    private boolean isRoiSet = false;
    private int maxFrame = 250;
    private Button lookBT;
    private boolean lookSWH = false;
    private boolean isLook = false;

    //控制拍照、傳送狀態
    private Map<String, Integer> downloadList = new HashMap<>();
    private TextView hintText;
    private state downloadState = state.ready;
    private state photoTakingState = state.ready;
    private enum state{
        ready,
        working
    }

    /***抓GPS***/
    private void getLocation(){
        if(ContextCompat.checkSelfPermission(context,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            location = locationMgr.getLastKnownLocation(provider);
        }
        else
            Log.d(TAG, "fail to get Gps location");
        if(location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            Log.d("location","latitude:" + location.getLatitude() + " longitude:" + location.getLongitude());
            Toast.makeText(BebopActivity.this, "latitude:" + location.getLatitude() + " longitude:" + location.getLongitude(), Toast.LENGTH_SHORT).show();
        }
        else
            Log.d("Exception","location null");
    }

    private void initDelayRunner(){ //auto photo taking
        delayHandler = new Handler();
        task = new Runnable() {
            @Override
            public void run() {
                if(isOnFlight) {
                    Log.d(TAG, "auto photo taking");
                    hintText.setText("自動拍照中");
                    mBebopDrone.takePicture();
                    photoTakingState = state.working;
                }
                delayHandler.postDelayed(task, 1000L * 15); //every 15 second
            }
        };
        task.run();
    }



    private boolean initLocationProvider() {

        locationMgr = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
            Log.d("provider","gps");
            return true;
        }else if(locationMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            provider = LocationManager.NETWORK_PROVIDER;
            Log.d("provider","network");
            return true;
        }
        Log.d("provider","f");
        return false;

    }
    private boolean initLocate(){
        if (ActivityCompat.checkSelfPermission(BebopActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(BebopActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        //Location Listener
        long minTime = 0;//ms
        float minDist = 0;//meter
        if(provider == null)
            return false;
        locationMgr.requestLocationUpdates(provider, minTime, minDist, locationListen);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
    }

    private Runnable r1=new Runnable () {

        public void run() {
            while (isFollow && !isSetIMG && !isLook) {
                Bitmap myMap = mVideoView.getBitmap();
                Mat frame = new Mat();
                Utils.bitmapToMat(myMap, frame);
                videoWidth = frame.width();
                videoHeight = frame.height();
                Imgproc.cvtColor(frame,frame,COLOR_RGBA2RGB);


                trackFrame = frame.clone();
                if(isRoiSet) {
                    pot[0] = new Point(roi.x, roi.y);
                    pot[1] = new Point((roi.x + roi.width), roi.y);
                    pot[2] = new Point((roi.x + roi.width), (roi.y + roi.height));
                    pot[3] = new Point(roi.x, (roi.y + roi.height));

                    for (int i = 0; i < 4; i++)
                        line(frame, pot[i], pot[(i + 1) % 4], new Scalar(255, 0, 0), 5);
                }


                Utils.matToBitmap(frame, myMap);
                Message disF = new Message();
                disF.what=0;
                srcBitmap = myMap;
                handler.sendMessage(disF);
            }
        }

    };

    private Runnable r2=new Runnable () {

        public void run() {
            while (isSetIMG || isLook) {
                Bitmap myMap = mVideoView.getBitmap();
                Utils.bitmapToMat(myMap, trackFrame);
                Imgproc.cvtColor(trackFrame,trackFrame,COLOR_RGBA2RGB);
                Mat frame = trackFrame.clone();
                boolean ok = tracker.update(frame, tempBox);

                if(ok) {
                    frameWidth = tempBox.width;
                    frameHeight = tempBox.height;

                    pot[0] = new Point(tempBox.x, tempBox.y);
                    pot[1] = new Point((tempBox.x + tempBox.width), tempBox.y);
                    pot[2] = new Point((tempBox.x + tempBox.width), (tempBox.y + tempBox.height));
                    pot[3] = new Point(tempBox.x, (tempBox.y + tempBox.height));
                    for (int i = 0; i < 4; i++)
                        line(frame, pot[i], pot[(i + 1) % 4], new Scalar(255, 0, 0), 5);


                    if (isLook) {
                        final float horizAngle = (float) Math.toRadians(Math.toDegrees(mBebopDrone.droneYaw) + getPan());
                        final float vertAngle = (float) Math.toRadians(mBebopDrone.cameraTilt);
                        mBebopDrone.setDetect(horizAngle, vertAngle, isNew);
                        isNew = 0;
                    }
                }
                Utils.matToBitmap(frame, myMap);
                Message disF = new Message();
                disF.what=0;
                srcBitmap = myMap;
                handler.sendMessage(disF);
            }
        }

    };

    public float getPan(){
        final float zero = videoWidth / 2;
        final float minMultiplier = mBebopDrone.cameraMinPan / zero;
        final float maxMultiplier = mBebopDrone.cameraMaxPan / zero;
        final float SIZE_MULTIPLIER = maxMultiplier-minMultiplier;
        final float x = (float) (frameWidth / 2 + tempBox.x );

        if (x > zero) {
            return mBebopDrone.cameraPan + (x - zero) * maxMultiplier;
        } else if (x < zero) {
            return mBebopDrone.cameraPan + (zero - x) * minMultiplier;
        }

        return mBebopDrone.cameraPan;
    }


    public float getTilt() {
        final float zero = videoHeight / 2;
        final float minMultiplier = mBebopDrone.cameraMinTilt / zero;  // downward as negative
        final float maxMultiplier = mBebopDrone.cameraMaxTilt / zero;  // upward as positive
        final float SIZE_MULTIPLIER = maxMultiplier-minMultiplier;
        final float y = (float) (frameHeight / 2 + tempBox.y );

        if (y > zero) {
            return mBebopDrone.cameraTilt + (y - zero) * minMultiplier;
        } else if (y < zero) {
            return mBebopDrone.cameraTilt + (zero - y) * maxMultiplier;
        }

        return mBebopDrone.cameraTilt;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bebop);

        initIHM();
        initLocationProvider();


        ARDiscoveryDeviceService service = DeviceListActivity.service; //intent.getParcelableExtra(DeviceListActivity.EXTRA_DEVICE_SERVICE);
        mBebopDrone = new BebopDrone(this, service);
        mBebopDrone.addListener(mBebopListener);

        /***FireBasa***/
        user = FirebaseAuth.getInstance().getCurrentUser();
        context = getApplicationContext();
        checkIntent = new Intent(this, CheckFlightActivity.class);
        isOnFlight = false;
        dataManager = new DataManager(context);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // show a loading view while the bebop drone is connecting
        if ((mBebopDrone != null) && !(ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING.equals(mBebopDrone.getConnectionState())))
        {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Connecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();

            // if the connection to the Bebop fails, finish the activity
            if (!mBebopDrone.connect()) {
                finish();
            }
        }
    }

    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onBackPressed() {
        if (mBebopDrone != null)
        {
            mConnectionProgressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
            mConnectionProgressDialog.setIndeterminate(true);
            mConnectionProgressDialog.setMessage("Disconnecting ...");
            mConnectionProgressDialog.setCancelable(false);
            mConnectionProgressDialog.show();

            if (!mBebopDrone.disconnect()) {
                finish();
            }
        }
    }

    @Override
    public void onPause(){
        if (mThreadHandler != null) {
            if (isSetIMG){
                isSetIMG = false;
                mThreadHandler.removeCallbacks(r2);

            }else {
                isFollow = false;
                mThreadHandler.removeCallbacks(r1);
            }
        }
        super.onPause();
    }

    @Override
    public void onDestroy()
    {
        if (mThreadHandler != null) {
            if (isSetIMG){
                isSetIMG = false;
                mThreadHandler.removeCallbacks(r2);

            }else {
                isFollow = false;
                mThreadHandler.removeCallbacks(r1);
            }
        }
        mBebopDrone.dispose();
        super.onDestroy();
    }

    View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if(isFollow && !isSetIMG && !isLook) {
                int x = (int)event.getX();
                int y = (int)event.getY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        roi.x = x;
                        roi.y = y;
                        roi.width = 1;
                        roi.height = 1;
                        break;
                    case MotionEvent.ACTION_UP:
                        if(x<roi.x && y<roi.y){
                            roi.width = roi.x - x;
                            roi.width = roi.width<(maxFrame+1)?roi.width:maxFrame;
                            roi.height = roi.width;
                            roi.x = x;
                            roi.y = y;
                        }else if(x<roi.x){
                            roi.width = roi.x - x;
                            roi.width = roi.width<(maxFrame+1)?roi.width:maxFrame;
                            roi.x=x;
                        }else if(y<roi.y){
                            roi.y = y;
                            roi.height = roi.height*-1;
                        }
                        return false;

                    case MotionEvent.ACTION_MOVE:
                        roi.width = x - roi.x;
                        roi.width = roi.width<(maxFrame+1)?roi.width:maxFrame;
                        if(y<roi.y){
                            roi.height = Math.abs(roi.width)*-1;
                        }else{
                            roi.height = Math.abs(roi.width);
                        }
                        isRoiSet = true;
                        break;
                }
                return true;
            }
            return false;
        }
    };

    /***航程***/
    private void startFlight(){
        initDelayRunner();
        mFlight = new flightData(user.getUid());
        isOnFlight = true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void endFlight(){
        delayHandler.removeCallbacks(task); //remove delay loop
        mFlight.endSetting(dataManager.getFilesDir().getPath());

        Bundle mbundle = new Bundle();
        mbundle.putLong("startTime", mFlight.getStartTimeWithMillSec());
        mbundle.putString("uid", mFlight.getUserID());
        checkIntent.putExtras(mbundle);

        isOnFlight = false;
    }

    private void addPhotoToList(){
        downloadState = state.working;
        String mediaName = mBebopDrone.getOneLastFlightMedias();

        getLocation();
        String time = new SimpleDateFormat("EEE yyyy/MM/dd a HH:mm:ss").format(new Date());

        Log.d(TAG, "TAKED PICTURE, time = " + time);
        Log.d(TAG, "TAKED PICTURE, mediaName = " + mediaName);

        Photo mPhoto = new Photo(mediaName, latitude, longitude, time);
        if(mPhoto != null)
            mFlight.setPhotoList(mPhoto);
    }

    private void showHint(){
        String display = "照片傳送中:\n";
        for(String mediaName : downloadList.keySet())
            display += "第" + mFlight.getPhotoNumber() + "張照片: " + downloadList.get(mediaName) + "%\n";

        hintText.setText(display);
    }

    /***---***/
    private void initIHM() {
        mVideoView = (H264VideoView) findViewById(R.id.videoView);
        img = (ImageView) findViewById(R.id.imageView);
        img.setOnTouchListener(touchListener);
        //聘請一個特約工人，有其經紀人派遣其工人做事 (另起一個有Handler的Thread)
        mThread = new HandlerThread("name");
        //讓Worker待命，等待其工作 (開啟Thread)
        mThread.start();
        //找到特約工人的經紀人，這樣才能派遣工作 (找到Thread上的Handler)
        mThreadHandler=new Handler(mThread.getLooper());
        handler= new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case 0:
                        img.setImageBitmap(srcBitmap);
                        break;
                    case 1:

                        break;
                }
            }
        };

        followBT = (Button) findViewById(R.id.followBT);
        followBT.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Button fo = findViewById(R.id.followBT);
                if(!followSWH) {
                    isFollow = true;
                    followSWH = true;
                    fo.setBackgroundResource(R.drawable.follow);
                    setIMGBT.setVisibility(View.VISIBLE);
                    lookBT.setVisibility(View.VISIBLE);
                    img.setVisibility(View.VISIBLE);
                    srcBitmap = mVideoView.getBitmap();
                    roi.x = srcBitmap.getWidth() / 2 - roi.width / 2;
                    roi.y = srcBitmap.getHeight() / 2 - roi.height / 2;
                    mThreadHandler.post(r1);
                }else{
                    fo.setBackgroundResource(R.drawable.unfollow);
                    isRoiSet=false;
                    isFollow = false;
                    img.setVisibility(View.INVISIBLE);
                    setIMGBT.setVisibility(View.INVISIBLE);
                    lookBT.setVisibility(View.INVISIBLE);
                    followSWH = false;
                    if (mThreadHandler != null) {
                        mThreadHandler.removeCallbacks(r1);
                    }
                }
            }
        });

        lookBT = (Button) findViewById(R.id.look);
        lookBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!lookSWH){

                    if (mThreadHandler != null) {
                        mThreadHandler.removeCallbacks(r1);
                    }
                    lookBT.setBackgroundResource(R.drawable.focusing);
                    yawRightBT.setVisibility(View.INVISIBLE);
                    rollLeftBT.setVisibility(View.INVISIBLE);
                    yawLeftBT.setVisibility(View.INVISIBLE);
                    rollRightBT.setVisibility(View.INVISIBLE);
                    gazUpBT.setVisibility(View.INVISIBLE);
                    gazDownBT.setVisibility(View.INVISIBLE);
                    forwardBT.setVisibility(View.INVISIBLE);
                    backBT.setVisibility(View.INVISIBLE);

                    isNew = 1;
                    isLook = true;
                    lookSWH = true;
                    tracker = TrackerKCF.create();
                    tracker.init(trackFrame, roi);
                    tempBox = roi.clone();
                    followBT.setVisibility(View.INVISIBLE);
                    setIMGBT.setVisibility(View.INVISIBLE);
                    mBebopDrone.setFrame();
                    mThreadHandler.post(r2);
                    mBebopDrone.startFollow();
                }else{
                    if (mThreadHandler != null) {
                        mThreadHandler.removeCallbacks(r2);
                    }
                    lookBT.setBackgroundResource(R.drawable.focus);
                    yawRightBT.setVisibility(View.VISIBLE);
                    rollLeftBT.setVisibility(View.VISIBLE);
                    yawLeftBT.setVisibility(View.VISIBLE);
                    rollRightBT.setVisibility(View.VISIBLE);
                    gazUpBT.setVisibility(View.VISIBLE);
                    gazDownBT.setVisibility(View.VISIBLE);
                    forwardBT.setVisibility(View.VISIBLE);
                    backBT.setVisibility(View.VISIBLE);
                    isLook = false;
                    isRoiSet=false;
                    lookSWH = false;
                    mBebopDrone.stopFollow();
                    mThreadHandler.post(r1);
                    followBT.setVisibility(View.VISIBLE);
                    setIMGBT.setVisibility(View.VISIBLE);
                }
            }
        });

        setIMGBT = (Button) findViewById(R.id.setIMG);
        setIMGBT.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Button si = findViewById(R.id.setIMG);
                if(!setIMGSWH) {
                    si.setBackgroundResource(R.drawable.tracking);
                    isSetIMG = true;
                    if (mThreadHandler != null) {
                        mThreadHandler.removeCallbacks(r1);
                    }
                    yawRightBT.setVisibility(View.INVISIBLE);
                    rollLeftBT.setVisibility(View.INVISIBLE);
                    yawLeftBT.setVisibility(View.INVISIBLE);
                    rollRightBT.setVisibility(View.INVISIBLE);
                    gazUpBT.setVisibility(View.INVISIBLE);
                    gazDownBT.setVisibility(View.INVISIBLE);
                    forwardBT.setVisibility(View.INVISIBLE);
                    backBT.setVisibility(View.INVISIBLE);


                    setIMGSWH = true;
                    tracker = TrackerKCF.create();
                    tracker.init(trackFrame, roi);
                    tempBox = roi.clone();
                    followBT.setVisibility(View.INVISIBLE);
                    lookBT.setVisibility(View.INVISIBLE);
                    mBebopDrone.setFrame();
                    mThreadHandler.post(r2);
                }else{
                    si.setBackgroundResource(R.drawable.setimg);
                    if (mThreadHandler != null) {
                        mThreadHandler.removeCallbacks(r2);
                    }
                    yawRightBT.setVisibility(View.VISIBLE);
                    rollLeftBT.setVisibility(View.VISIBLE);
                    yawLeftBT.setVisibility(View.VISIBLE);
                    rollRightBT.setVisibility(View.VISIBLE);
                    gazUpBT.setVisibility(View.VISIBLE);
                    gazDownBT.setVisibility(View.VISIBLE);
                    forwardBT.setVisibility(View.VISIBLE);
                    backBT.setVisibility(View.VISIBLE);
                    isSetIMG = false;
                    isRoiSet=false;
                    setIMGSWH = false;
                    mBebopDrone.stopMoveTo();
                    mThreadHandler.post(r1);
                    followBT.setVisibility(View.VISIBLE);
                    lookBT.setVisibility(View.VISIBLE);

                }
            }
        });
        emergencyBT = findViewById(R.id.emergencyBt);
        emergencyBT.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.emergency();
            }
        });

        mTakeOffLandBt = (Button) findViewById(R.id.takeOffOrLandBt);
        mTakeOffLandBt.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            public void onClick(View v) {
                switch (mBebopDrone.getFlyingState()) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                        if(!initLocate())
                            new AlertDialog.Builder(BebopActivity.this)
                                    .setTitle("無法偵測GPS")
                                    .setMessage("請檢查GPS訊號是否開啟且良好")
                                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                        else {
                            mBebopDrone.takeOff();
                            mTakeOffLandBt.setBackgroundResource(R.drawable.land);
                            startFlight();
                            hintText.setText("目前照片: " + mFlight.getPhotoNumber() + "張");
                        }
                        break;
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        mBebopDrone.land();
                        mTakeOffLandBt.setBackgroundResource(R.drawable.takeoff);
                        locationMgr.removeUpdates(locationListen);
                        hintText.setText("飛行結束");

                        if(photoTakingState == state.working || downloadState == state.working){
                            AlertDialog dialog = new AlertDialog.Builder(BebopActivity.this)
                                    .setTitle("拍照或傳送工作尚未完成，請稍後")
                                    .setMessage("確認右上角工作完成後，再按下結束鈕")
                                    .setPositiveButton("結束", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int i) {
                                            if(photoTakingState == state.ready && downloadState == state.ready) {
                                                dialog.dismiss();
                                                endFlight();
                                                startActivity(checkIntent);
                                            }
                                        }
                                    })
                                    .show();
                        }
                        else{
                            endFlight();
                            startActivity(checkIntent);
                        }
                        break;
                    default:
                }
            }
        });

        takePictureBT = findViewById(R.id.takePictureBt);
        takePictureBT.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBebopDrone.takePicture();
                hintText.setText("手動拍照中");
                photoTakingState = state.working;
            }
        });
        gazUpBT = findViewById(R.id.gazUpBt);
        gazUpBT.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setGaz((byte) 50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setGaz((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        gazDownBT = findViewById(R.id.gazDownBt);
        gazDownBT.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setGaz((byte) -50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setGaz((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        yawLeftBT = findViewById(R.id.yawLeftBt);
        yawLeftBT.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setYaw((byte) -50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setYaw((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        yawRightBT = findViewById(R.id.yawRightBt);
        yawRightBT.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setYaw((byte) 50);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setYaw((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        forwardBT = findViewById(R.id.forwardBt);
        forwardBT.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setPitch((byte) 50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setPitch((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        backBT = findViewById(R.id.backBt);
        backBT.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setPitch((byte) -50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setPitch((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        rollLeftBT = findViewById(R.id.rollLeftBt);
        rollLeftBT.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setRoll((byte) -50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setRoll((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        rollRightBT = findViewById(R.id.rollRightBt);
        rollRightBT.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        mBebopDrone.setRoll((byte) 50);
                        mBebopDrone.setFlag((byte) 1);
                        break;

                    case MotionEvent.ACTION_UP:
                        v.setPressed(false);
                        mBebopDrone.setRoll((byte) 0);
                        mBebopDrone.setFlag((byte) 0);
                        break;

                    default:

                        break;
                }

                return true;
            }
        });

        mBatteryLabel = (TextView) findViewById(R.id.batteryLabel);
        hintText = (TextView) findViewById(R.id.hint_text);
        hintText.setText("待命中");
    }

    private final BebopDrone.Listener mBebopListener = new BebopDrone.Listener() {
        @Override
        public void onDroneConnectionChanged(ARCONTROLLER_DEVICE_STATE_ENUM state) {
            switch (state)
            {
                case ARCONTROLLER_DEVICE_STATE_RUNNING:
                    mConnectionProgressDialog.dismiss();
                    break;

                case ARCONTROLLER_DEVICE_STATE_STOPPED:
                    // if the deviceController is stopped, go back to the previous activity
                    mConnectionProgressDialog.dismiss();
                    finish();
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onBatteryChargeChanged(int batteryPercentage) {
            mBatteryLabel.setText(String.format("%d%%", batteryPercentage));
        }

        @Override
        public void onPilotingStateChanged(ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state) {
            switch (state) {
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                    mTakeOffLandBt.setEnabled(true);
                    break;
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                    mTakeOffLandBt.setEnabled(true);
                    break;
                default:
                    mTakeOffLandBt.setEnabled(false);
            }
        }

        @Override
        public void onPictureTaken(ARCOMMANDS_ARDRONE3_MEDIARECORDEVENT_PICTUREEVENTCHANGED_ERROR_ENUM error) {
            hintText.setText("拍照完成");
            photoTakingState = state.ready;

            addPhotoToList();
            Log.i(TAG, "Picture has been taken");
        }


        @Override
        public void configureDecoder(ARControllerCodec codec) {
            mVideoView.configureDecoder(codec);
        }

        @Override
        public void onFrameReceived(ARFrame frame) {
            mVideoView.displayFrame(frame);

        }

        @Override
        public void onMatchingMediasFound(int nbMedias) {

        }

        @Override
        public void onDownloadProgressed(String mediaName, int progress) {
            downloadState = state.working;
            downloadList.put(mediaName, progress);
            showHint();
        }
        @Override
        public void onDownloadComplete(String mediaName) {
            downloadList.remove(mediaName);
            if(downloadList.isEmpty()) {
                downloadState = state.ready;
                hintText.setText("目前照片: " + mFlight.getPhotoNumber() + "張\n照片傳送完成");
            }
            else
                showHint();
        }

    };


    LocationListener locationListen = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            gpsUpdate(location);
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    };

    private Location lastLoc = null;


    public void gpsUpdate(Location location){
        final double lat = location.getLatitude();
        final double lng = location.getLongitude();
        final double alt = location.getAltitude();
        latitude = lat;
        longitude = lng;
        mFlight.recordPath(String.valueOf(latitude), String.valueOf(longitude));
        //Log.d("why",String.valueOf(latitude)+String.valueOf(longitude));
        //Toast.makeText(BebopActivity.this, "latitude:" + latitude + " longitude:" + longitude, Toast.LENGTH_SHORT).show();
        final float accuracy = location.getAccuracy();

        if (lastLoc != null&& isSetIMG) {
            // calculate ns, es, and ds
            float head = (float)(Math.toDegrees(mBebopDrone.droneYaw) + getPan());

            mBebopDrone.setMoveTo(lastLoc.getLatitude(),lastLoc.getLongitude(),mBebopDrone.altValue,head);
        }
        lastLoc = location;
    }


}