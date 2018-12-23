package com.example.ibob0625.mysignin;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;

import com.example.ibob0625.mysignin.droneActivity.BebopActivity;

public class Tutorial extends AppCompatActivity {

    private int nowPicPos = 0;
    private int[] imgRes = {R.drawable.tutorial1,R.drawable.tutorial2,R.drawable.tutorial3};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        ImageView tutorialImageView = (ImageView)findViewById(R.id.tutorialImage);
        fadeOutAndHideImage(tutorialImageView);

        Button back = findViewById(R.id.backFromTutorial);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(Tutorial.this,GateUI.class);
                startActivity(intent);
            }
        }

        );
    }


    private void fadeOutAndHideImage(final ImageView img){
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(5000);

        fadeOut.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationEnd(Animation animation)
            {
                nowPicPos %= 3;
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
