package com.example.engg6600.facedetectapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.engg6600.facedetectapp.R;
import com.example.engg6600.facedetectapp.adapters.UsersRecyclerAdapter;
import com.example.engg6600.facedetectapp.model.User;
import com.example.engg6600.facedetectapp.sql.DatabaseHelper;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class DetailsActivity extends AppCompatActivity {

    private AppCompatActivity activity = DetailsActivity.this;
    private AppCompatTextView textViewName;
    private RecyclerView recyclerViewUsers;
    private List<User> listUsers;
    private UsersRecyclerAdapter usersRecyclerAdapter;
    private DatabaseHelper databaseHelper;

    private final int RESULT_LOAD_IMG = 1234;
    private final int MAX_FACES = 5;
    private ImageView image_view;
    public Bitmap mFaceBitmap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        getSupportActionBar().setTitle("");
        initViews();
        initObjects();

    }

    /**
     * This method is to initialize views
     */
    private void initViews() {
        textViewName = (AppCompatTextView) findViewById(R.id.textViewName);
        recyclerViewUsers = (RecyclerView) findViewById(R.id.recyclerViewUsers);
        image_view = (ImageView) findViewById(R.id.imgViewerPicture);
    }

    /**
     * This method is to initialize objects to be used
     */
    private void initObjects() {
        listUsers = new ArrayList<>();
        usersRecyclerAdapter = new UsersRecyclerAdapter(listUsers);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerViewUsers.setLayoutManager(mLayoutManager);
        recyclerViewUsers.setItemAnimator(new DefaultItemAnimator());
        recyclerViewUsers.setHasFixedSize(true);
        recyclerViewUsers.setAdapter(usersRecyclerAdapter);
        databaseHelper = new DatabaseHelper(activity);

        String emailFromIntent = getIntent().getStringExtra("EMAIL");

        // query database to get name from email
        String userName = databaseHelper.getUserName(emailFromIntent);
        textViewName.setText(userName);

        //getDataFromSQLite();
        getUserFromSQLite(emailFromIntent);
        mFaceBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.family);
        image_view.setImageBitmap(mFaceBitmap);
    }

    /**
     * This method is to fetch all user records from SQLite
     */
    private void getDataFromSQLite() {
        // AsyncTask is used that SQLite operation not blocks the UI Thread.
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                listUsers.clear();
                listUsers.addAll(databaseHelper.getAllUser());

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                usersRecyclerAdapter.notifyDataSetChanged();
            }
        }.execute();
    }

    /**
     * This method is to fetch all user records from SQLite
     */
    private void getUserFromSQLite(final String email) {
        // AsyncTask is used that SQLite operation not blocks the UI Thread.
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                listUsers.clear();
                listUsers.addAll(databaseHelper.getCurrentUser(email));

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                usersRecyclerAdapter.notifyDataSetChanged();
            }
        }.execute();
    }

    public void onTake(View view)
    {
        Intent takePhotoIntent = new Intent(this, CameraActivity.class);
        startActivity(takePhotoIntent);
    }

    public void onLoad(View view)
    {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                image_view.setImageBitmap(selectedImage);
                mFaceBitmap = selectedImage;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(DetailsActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
            }

        }else {
            Toast.makeText(DetailsActivity.this, "You haven't picked Image",Toast.LENGTH_LONG).show();
        }
    }

    public  void onTest(View view) {
        Toast.makeText(DetailsActivity.this, "Recognizing", Toast.LENGTH_LONG).show();

        FaceDetector.Face[] faces = new FaceDetector.Face[MAX_FACES];
        // Setup format as RGB_565
        Bitmap bmp = mFaceBitmap.copy(Bitmap.Config.RGB_565, true);
        // get number of the faces recognized
        int faceCount = new FaceDetector(bmp.getWidth(), bmp.getHeight(), MAX_FACES).findFaces(bmp, faces);
        bmp.recycle();
        Log.e("tag", "Recognized faces:" + faceCount);

        if (faceCount > 0) {
            final Bitmap bitmap = parseBitmap(faces, faceCount);
            // display recognized faces
            image_view.post(new Runnable() {
                @Override
                public void run() {
                    image_view.setImageBitmap(bitmap);
                }
            });
        }
        else{
            Toast.makeText(DetailsActivity.this, "No face recognized", Toast.LENGTH_LONG).show();
        }
    }
    /**
     * Draw a box on recognized face
     */
    private Bitmap parseBitmap(FaceDetector.Face[] faces, int faceCount){
        Bitmap bitmap = Bitmap.createBitmap(mFaceBitmap.getWidth(), mFaceBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.YELLOW);
        mPaint.setStrokeWidth(10);
        mPaint.setStyle(Paint.Style.STROKE);

        canvas.drawBitmap(mFaceBitmap, 0, 0, mPaint);
        for (int i = 0; i < faceCount; i++){
            // get middle of eyes
            PointF midPoint = new PointF();
            faces[i].getMidPoint(midPoint);
            // get distance between eyes
            float eyeDistance = faces[i].eyesDistance();
            // draw a box
            canvas.drawRect(midPoint.x - eyeDistance, midPoint.y - eyeDistance, midPoint.x + eyeDistance, midPoint.y + eyeDistance, mPaint);
        }

        return bitmap;
    }

}
