package org.fieldsight.naxa.educational;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.bcss.collect.android.R;;
import org.fieldsight.naxa.common.GlideApp;
import org.odk.collect.android.activities.CollectAbstractActivity;

import java.io.File;

/**
 * Created by susan on 7/18/2017.
 */

public class EduMatImageDetailsActivity extends CollectAbstractActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edu_mat_view_image_detail_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);


        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        ImageView imageView = findViewById(R.id.image_url);
        TextView imageTitle = findViewById(R.id.image_title);
        TextView imageDesc = findViewById(R.id.image_desc);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String imageUrlOn = bundle.getString("IMAGE_URL_ON");
            String imageUrlOff = bundle.getString("IMAGE_URL_OFF");
            String imageTitle1 = bundle.getString("IMAGE_TITLE");
            String imageDesc1 = bundle.getString("IMAGE_DESC");

            getSupportActionBar().setTitle(imageTitle1);


            if (imageUrlOff != null) {
                File f = new File(imageUrlOff);
                Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                imageView.setImageBitmap(bmp);
            } else {
                GlideApp.with(this)
                        .load(imageUrlOn)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageView);
            }

//            usingSimpleImage(imageView);

            imageTitle.setText(imageTitle1);
            imageDesc.setText(imageDesc1);
        }
    }


//    public void usingSimpleImage(ImageView imageView) {
//        ImageAttacher mAttacher = new ImageAttacher(imageView);
//        ImageAttacher.MAX_ZOOM = 3.0f; // Triple the current Size
//        ImageAttacher.MIN_ZOOM = 0.5f; // Half the current Size
//        MatrixChangeListener mMaListener = new MatrixChangeListener();
//        mAttacher.setOnMatrixChangeListener(mMaListener);
//        PhotoTapListener mPhotoTap = new PhotoTapListener();
//        mAttacher.setOnPhotoTapListener(mPhotoTap);
//    }
//
//    private class PhotoTapListener implements ImageAttacher.OnPhotoTapListener {
//
//        @Override
//        public void onPhotoTap(View view, float x, float y) {
//
//        }
//    }
//
//    private class MatrixChangeListener implements ImageAttacher.OnMatrixChangedListener {
//
//        @Override
//        public void onMatrixChanged(RectF rect) {
//
//        }
//    }
}