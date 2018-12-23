package com.example.ibob0625.mysignin.FIreBase;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.example.ibob0625.mysignin.R;

public class ImageDialog implements DialogInterface.OnCancelListener, View.OnClickListener {
    private Context mContext;
    private Dialog mDialog;

    private Button closeButton;
    private ImageView image;

    ImageDialog(Context context){
        mContext = context;
        mDialog = new Dialog(mContext, R.style.DialogTheme);
        mDialog.setContentView(R.layout.img_dialog_layout);
        mDialog.getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);

        closeButton = (Button) mDialog.findViewById(R.id.close_btn);
        image = (ImageView) mDialog.findViewById(R.id.imageView);
    }

    public ImageDialog show(){
        // 點邊取消 false
        mDialog.setCancelable(true);
        mDialog.setCanceledOnTouchOutside(false);

        closeButton.setOnClickListener(this);

        mDialog.setOnCancelListener(this);
        mDialog.show();

        return this;
    }

    public ImageView getImageView(){
        return image;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mDialog.dismiss();
    }

    @Override
    public void onClick(View v) {
        mDialog.dismiss();
    }
}
