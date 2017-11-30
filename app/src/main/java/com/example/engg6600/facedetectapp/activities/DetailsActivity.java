package com.example.engg6600.facedetectapp.activities;

import android.app.ProgressDialog;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.engg6600.facedetectapp.R;
import com.example.engg6600.facedetectapp.adapters.UsersRecyclerAdapter;
import com.example.engg6600.facedetectapp.model.User;
import com.example.engg6600.facedetectapp.sql.DatabaseHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.FaceRectangle;


public class DetailsActivity extends AppCompatActivity {

    private AppCompatActivity activity = DetailsActivity.this;
    private AppCompatTextView textViewName;
    private RecyclerView recyclerViewUsers;
    private List<User> listUsers;
    private UsersRecyclerAdapter usersRecyclerAdapter;
    private DatabaseHelper databaseHelper;

    private final int RESULT_LOAD_IMG = 1234;
    private final int RESULT_TAKE_IMG = 4321;
    private final int MAX_FACES = 15;
    private ImageView image_view;
    public Bitmap mFaceBitmap;
    ProgressBar mProgressBar ;


    ProgressDialog mProgressDialog;


    // Background task of face detection.
    private class DetectionTask extends AsyncTask<InputStream, String, Face[]> {
        private boolean mSucceed = true;

        @Override
        protected Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.endpoint), getString(R.string.subscription_key));
            try {
                publishProgress("Detecting...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        true,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        new FaceServiceClient.FaceAttributeType[] {
                                FaceServiceClient.FaceAttributeType.Age,
                                FaceServiceClient.FaceAttributeType.Gender,
                                FaceServiceClient.FaceAttributeType.Smile,
                                FaceServiceClient.FaceAttributeType.Glasses,
                                FaceServiceClient.FaceAttributeType.FacialHair,
                                FaceServiceClient.FaceAttributeType.Emotion,
                                FaceServiceClient.FaceAttributeType.HeadPose,
                                FaceServiceClient.FaceAttributeType.Accessories,
                                FaceServiceClient.FaceAttributeType.Blur,
                                FaceServiceClient.FaceAttributeType.Exposure,
                                FaceServiceClient.FaceAttributeType.Hair,
                                FaceServiceClient.FaceAttributeType.Makeup,
                                FaceServiceClient.FaceAttributeType.Noise,
                                FaceServiceClient.FaceAttributeType.Occlusion
                        });
            } catch (Exception e) {
                mSucceed = false;
                publishProgress(e.getMessage());
                //addLog(e.getMessage());
                return null;
            }

        }

        @Override
        protected void onPreExecute() {
            //mProgressDialog.show();
            //addLog("Request: Detecting in image " + mImageUri);

           mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            //mProgressDialog.setMessage(progress[0]);
            //setInfo(progress[0]);
        }

        @Override
        protected void onPostExecute(Face[] result) {
            if (mSucceed) {
               // addLog("Response: Success. Detected " + (result == null ? 0 : result.length)
                //        + " face(s) in " + mImageUri);
                //mProgressDialog.dismiss();
                mProgressBar.setVisibility(View.INVISIBLE);


                // Show the result on screen when detection is done.
                image_view.setImageBitmap(parseBitmap(result,result.length));
            }



        }
    }


    private void setUiAfterDetection(Face[] result, boolean succeed) {

    }



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        getSupportActionBar().setTitle(getString(R.string.app_name));
        initViews();
        initObjects();

       // mProgressDialog = new ProgressDialog(this);
       // mProgressDialog.setTitle(getString(R.string.progress_dialog_title));

         mProgressBar = (ProgressBar)findViewById(R.id.progressbar);
         mProgressBar.setVisibility(View.INVISIBLE);
        //ProgressBar progressBar = new ProgressBar(DetailsActivity.this,null,android.R.attr.progressBarStyleLarge);
        //RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100,100);
        //params.addRule(RelativeLayout.CENTER_IN_PARENT);
        //layout.addView(progressBar,params);

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
        textViewName.setText("Welcome "+userName +" !");

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
        Intent photoTakeIntent = new Intent(this, CameraActivity.class);
        startActivityForResult(photoTakeIntent, RESULT_TAKE_IMG);
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
        if (reqCode == RESULT_LOAD_IMG){
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
        if (reqCode == RESULT_TAKE_IMG){
            if (resultCode == RESULT_OK) {
                try {
                    final String fn = data.getStringExtra("PICNAME");
                    final File file = new File(fn);
                    if(file.exists()){
                        Bitmap selectedImage = BitmapFactory.decodeFile(file.getAbsolutePath());
                        image_view.setImageBitmap(selectedImage);
                        mFaceBitmap = selectedImage;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(DetailsActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
                }
            }else {
                Toast.makeText(DetailsActivity.this, "You haven't picked Image",Toast.LENGTH_LONG).show();
            }
        }
    }

    public  void onTest(View view) {


        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mFaceBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        // Start a background task to detect faces in the image.
        new DetectionTask().execute(inputStream);

/*        Toast.makeText(DetailsActivity.this, "Recognizing", Toast.LENGTH_LONG).show();

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
        }*/
    }
    /**
     * Draw a box on recognized face
     */
    private Bitmap parseBitmap(Face[] faces, int faceCount){
        //Bitmap bitmap = Bitmap.createBitmap(mFaceBitmap.getWidth(), mFaceBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Bitmap bitmap = mFaceBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.YELLOW);
        int stokeWidth = 10;
        paint.setStrokeWidth(stokeWidth);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.RED);
        //textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setStrokeWidth(20);
        textPaint.setStyle(Paint.Style.FILL);


        //canvas.drawBitmap(mFaceBitmap, 0, 0, Paint);
        for (int i = 0; i < faceCount; i++){
            // get middle of eyes
            PointF midPoint = new PointF();
            String age_gender =  faces[i].faceAttributes.gender +"," + faces[i].faceAttributes.age;

            FaceRectangle faceRectangle  = faces[i].faceRectangle;

            textPaint.setTextSize(20*getResources().getDisplayMetrics().density);
            // get distance between eyes
            //float eyeDistance = faces[i].eyesDistance();
            // draw a box
            //canvas.drawRect(midPoint.x - eyeDistance, midPoint.y - eyeDistance, midPoint.x + eyeDistance, midPoint.y + eyeDistance, mPaint);
            canvas.drawRect(faceRectangle.left,
                    faceRectangle.top,
                    faceRectangle.left + faceRectangle.width,
                    faceRectangle.top + faceRectangle.height,
                    paint);
            canvas.drawText(age_gender,
                    faceRectangle.left ,
                    faceRectangle.top-20,
                   textPaint);

        }

        return bitmap;
    }

}
