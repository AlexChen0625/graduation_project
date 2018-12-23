package com.example.ibob0625.mysignin;
import android.content.Intent;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ibob0625.mysignin.droneActivity.BebopActivity;
import com.example.ibob0625.mysignin.droneActivity.DeviceListActivity;
import com.example.ibob0625.mysignin.history.HistoryActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;


public class GateUI extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener ,View.OnClickListener
{
    private int nowPicPos = 0;
    private int[] imgRes = {R.drawable.p2,R.drawable.p1};
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mToggle;
    private Toolbar mToolboar;
    /*改header*/
    private DrawerLayout DL;
    protected NavigationView NV;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        DL = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_gate_ui, null);
        setContentView(DL);

        Bundle check = getIntent().getExtras();
        if(check!=null){
            TextView droneName=findViewById(R.id.droneName);
            droneName.setText("已連接:Bebop2");
        }

        /***抓navigation_header修改成使用者名稱**/
        NV = (NavigationView)DL.findViewById(R.id.nav_view);
        View headerLayout = NV.getHeaderView(0);
        TextView headerText = headerLayout.findViewById(R.id.userName);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        headerText.setText(account.getDisplayName());

        /*** 改無人機名稱 ***/
        /*String droneNam = new DeviceListActivity().droneName;
        Log.d("droneName",droneNam);
        TextView droneNameText = findViewById(R.id.droneName);
        droneNameText.setText(droneNam);*/

        /**Toolbar取代原本的ActionBar**/
        mToolboar=(Toolbar)findViewById(R.id.nav_action);
        mToolboar.setTitle("Sky Tripper");
        setSupportActionBar(mToolboar);
        /**工具欄監聽事件**/
        mDrawerLayout=(DrawerLayout)findViewById(R.id.drawerLayout);
        mToggle=new ActionBarDrawerToggle(this,mDrawerLayout,R.string.open,R.string.close);//必須用字串資源檔
        mDrawerLayout.addDrawerListener(mToggle);
        /**隱藏顯示箭頭返回**/
        mToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        /****/
        ImageView splashImageView = (ImageView)findViewById(R.id.SplashImageView);
        fadeOutAndHideImage(splashImageView);
        /**清單觸發監聽事件**/
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        findViewById(R.id.startRecoding).setOnClickListener(this);
        findViewById(R.id.historyview).setOnClickListener(this);

    }
    @Override
    /**實作清單觸發**/
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_wifi_connect) {
            Log.d("Tag", "QQ");
            Intent intent = new Intent();
            intent.setClass(GateUI.this,DeviceListActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.nav_tutorial) {
            Intent intent = new Intent();
            intent.setClass(GateUI.this,Tutorial.class);
            startActivity(intent);
        }
        else if (id == R.id.nav_logout) {
            Intent intent = new Intent();
            intent.setClass(GateUI.this,MainActivity.class);
            startActivity(intent);

        }
        return true;
    }

    @Override
    /**當按下左上三條線或顯示工具列**/
    public boolean onOptionsItemSelected(MenuItem item) {
        if(mToggle.onOptionsItemSelected(item)){

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onClick(View v) {
        Log.d("button",String.valueOf(v.getId()));
        switch (v.getId()) {
            case R.id.historyview:
                Intent intent = new Intent();
                intent.setClass(GateUI.this, HistoryActivity.class);
                startActivity(intent);
                //finish();
                break;

            case R.id.startRecoding:
                Bundle check = getIntent().getExtras();
                if(check!=null){
                    intent = new Intent();
                    intent.setClass(GateUI.this,BebopActivity.class);
                    startActivity(intent);
                    //finish();
                    break;
                }
                else{
                    Toast.makeText(this,"請連接空拍機", Toast.LENGTH_LONG).show();
                }


        }
    }
    private void fadeOutAndHideImage(final ImageView img){
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(2000);

        fadeOut.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationEnd(Animation animation)
            {
                nowPicPos %= 2;
                img.setImageResource(imgRes[nowPicPos]);
                nowPicPos++;
                fadeInAndShowImage(img);
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationStart(Animation animation) {}
        });

        img.startAnimation(fadeOut);
    }
    private void fadeInAndShowImage(final ImageView img){
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new AccelerateInterpolator());
        fadeIn.setDuration(2000);

        fadeIn.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationEnd(Animation animation)
            {
                fadeOutAndHideImage(img);
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationStart(Animation animation) {}
        });

        img.startAnimation(fadeIn);
    }
}
