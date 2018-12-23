package com.example.ibob0625.mysignin.history;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.ibob0625.mysignin.R;

import org.w3c.dom.Text;

public class HistoryInfoDialog implements DialogInterface.OnCancelListener, View.OnClickListener{

    private Context mContext;
    private Dialog mDialog;

    private Button closeButton;
    private Button gotoMapButton;

    private Button pageupButton;
    private Button pagedownButton;
    private ImageView photoImage;
    private TextView photoListIndex;

    private TextView titleText;
    private TextView startTimeText;
    private TextView endTimeText;
    private TextView spendTimeText;
    private TextView photoNumberText;

    HistoryInfoDialog(Context context){
        this.mContext = context;
        mDialog = new Dialog(mContext, R.style.DialogTheme);
        mDialog.setContentView(R.layout.history_dialog_layout);
        mDialog.getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);

        closeButton = (Button) mDialog.findViewById(R.id.close_btn);
        gotoMapButton = (Button) mDialog.findViewById(R.id.goto_map_btn);
        pageupButton = (Button) mDialog.findViewById(R.id.img_page_up_btn);
        pagedownButton = (Button) mDialog.findViewById(R.id.img_page_down_btn);
        titleText = (TextView) mDialog.findViewById(R.id.title_text);
        startTimeText = (TextView) mDialog.findViewById(R.id.startTime_text);
        endTimeText = (TextView) mDialog.findViewById(R.id.endTime_text);
        spendTimeText = (TextView) mDialog.findViewById(R.id.spendTime_text);
        photoNumberText = (TextView) mDialog.findViewById(R.id.photoNumber_text);
        photoListIndex = (TextView) mDialog.findViewById(R.id.photo_list_index);
        photoImage = (ImageView) mDialog.findViewById(R.id.photo_img);
    }

    public HistoryInfoDialog show(String t, String st, String et, String spt, String pn){
        String title = t;
        String start_time = "開始時間: " + st;
        String end_time = "結束時間: " + et;
        String spend_time = "全程: " + spt;
        String photo_number = pn;
        titleText.setText(title);
        startTimeText.setText(start_time);
        endTimeText.setText(end_time);
        spendTimeText.setText(spend_time);
        photoNumberText.setText(photo_number);

        // 點邊取消
        mDialog.setCancelable(true);
        mDialog.setCanceledOnTouchOutside(false);

        mDialog.setOnCancelListener(this);
        mDialog.show();

        return this;
    }

    public TextView getPhotoListIndex(){ return photoListIndex;   }
    public Button getMapButton(){ return gotoMapButton;    }
    public Button getPageupButton(){ return pageupButton;    }
    public Button getPagedownButton(){ return pagedownButton;    }
    public Button getCloseButton(){ return  closeButton;    }
    public ImageView getImageView(){ return photoImage;     }

    public void dismiss(){ mDialog.dismiss();   }

    @Override
    public void onCancel(DialogInterface dialog) {
        mDialog.dismiss();
    }

    @Override
    public void onClick(View v) {
        mDialog.dismiss();
    }
}
