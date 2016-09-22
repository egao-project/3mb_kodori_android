package com.egao_inc.kodori.kodori;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    // camera ok
    static final int REQUEST_CAPTURE_IMAGE = 100;
    // camera button
    Button button1;
    // camera image
    ImageView imageView1;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        setListeners();
    }

    /**
     * create views
     */
    protected void findViews()
    {
        button1 = (Button)findViewById(R.id.button1);
        imageView1 = (ImageView)findViewById(R.id.imageView1);
    }

    /**
     * set camera listener
     */
    protected void setListeners()
    {
        button1.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent( MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult( intent, REQUEST_CAPTURE_IMAGE);
            }
        });
    }

    /**
     * camera capture result
     */
    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data)
    {
        if(REQUEST_CAPTURE_IMAGE == requestCode && resultCode == Activity.RESULT_OK )
        {
            Bitmap capturedImage = (Bitmap) data.getExtras().get("data");
            imageView1.setImageBitmap(capturedImage);
        }
    }
}

