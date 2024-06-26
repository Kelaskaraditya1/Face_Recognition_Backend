package com.starkindustries.face_recognition_backend.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.databinding.DataBindingUtil;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.starkindustries.face_recognition_backend.FaceDetection.FaceClassifier;
import com.starkindustries.face_recognition_backend.FaceDetection.TFLiteFaceRecognition;
import com.starkindustries.face_recognition_backend.R;
import com.starkindustries.face_recognition_backend.databinding.ActivityRecognizationBinding;

import java.util.HashMap;
import java.util.List;

public class RecognizationActivity extends AppCompatActivity {
    public ActivityRecognizationBinding binding;
    public static FaceDetector detector;
    public FaceClassifier classifier;
    public AppCompatImageView register_image_view;
    public AppCompatButton register;
    public AppCompatEditText register_id;
    public static HashMap<String, FaceClassifier.Recognition> registered = new HashMap<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recognization);
        binding= DataBindingUtil.setContentView(RecognizationActivity.this,R.layout.activity_recognization);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            binding.recognizeCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(camera,100);
                }
            });
            binding.recognizeGallery.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent gallery = new Intent(Intent.ACTION_PICK);
                    gallery.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(gallery,101);
                }
            });
            return insets;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK)
        {
            if(requestCode==100)
            {

                Bitmap image =(Bitmap) data.getExtras().get("data");
                com.starkindustries.face_recognition_backend.FaceDetection.FaceDetection.perform_face_detection((Bitmap) data.getExtras().get("data"));
                binding.recognizeImageview.setImageBitmap(image);
            }
            if(requestCode==101)
            {
                Bitmap bitmap = null;
                try
                {
                    if(Build.VERSION.SDK_INT>Build.VERSION_CODES.O_MR1)
                    {
                        ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(),data.getData());
                        bitmap=ImageDecoder.decodeBitmap(source);
                        perform_face_detection(bitmap);
                    }
                    else {
                        bitmap=MediaStore.Images.Media.getBitmap(getContentResolver(),data.getData());
                        perform_face_detection(bitmap);
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                binding.recognizeImageview.setImageBitmap(bitmap);

            }
        }
    }
    Canvas canvas;
    void perform_face_detection(Bitmap bitmap)
    {
        AppCompatImageView imageView = findViewById(R.id.recognize_imageview);
        Bitmap mutable_bitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
        canvas = new Canvas(mutable_bitmap);
        FaceDetectorOptions options=
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();
        detector = com.google.mlkit.vision.face.FaceDetection.getClient(options);
        try
        {
            classifier= TFLiteFaceRecognition.create(getAssets(),"facenet.tflite",160,false,RecognizationActivity.this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        Task<List<Face>> result =
                detector.process(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        for (Face face : faces) {

                                            Rect bounds = face.getBoundingBox();
                                            Log.d("tryfaces","Length="+faces.size());
                                            Paint p1 = new Paint();
                                            p1.setColor(Color.RED);
                                            p1.setStyle(Paint.Style.STROKE);
                                            p1.setStrokeWidth(10);
                                            cropped_bitmap_image(bounds,bitmap);
                                            canvas.drawRect(bounds,p1);
//                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
//                                            float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees
//
//                                            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
//                                            // nose available):
//                                            FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
//                                            if (leftEar != null) {
//                                                PointF leftEarPos = leftEar.getPosition();
//                                            }
//
//                                            // If contour detection was enabled:
//                                            List<PointF> leftEyeContour =
//                                                    face.getContour(FaceContour.LEFT_EYE).getPoints();
//                                            List<PointF> upperLipBottomContour =
//                                                    face.getContour(FaceContour.UPPER_LIP_BOTTOM).getPoints();
//
//                                            // If classification was enabled:
//                                            if (face.getSmilingProbability() != null) {
//                                                float smileProb = face.getSmilingProbability();
//                                            }
//                                            if (face.getRightEyeOpenProbability() != null) {
//                                                float rightEyeOpenProb = face.getRightEyeOpenProbability();
//                                            }
//
//                                            // If face tracking was enabled:
//                                            if (face.getTrackingId() != null) {
//                                                int id = face.getTrackingId();
//                                            }
                                        }
                                        imageView.setImageBitmap(mutable_bitmap);
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
    }
    public void cropped_bitmap_image(Rect bound,Bitmap input)
    {
        AppCompatImageView imageView = findViewById(R.id.recognize_imageview);
        if(bound.top<0)
            bound.top=0;
        else if(bound.left<0)
            bound.left=0;
        else if(bound.right>input.getWidth())
            bound.right=input.getWidth()-1;
        else if(bound.bottom>input.getHeight())
            bound.bottom=input.getHeight()-1;
        Bitmap cropped_face = Bitmap.createBitmap(input,bound.left,bound.top,bound.width(),bound.height());
        imageView.setImageBitmap(cropped_face);
        cropped_face=Bitmap.createScaledBitmap(cropped_face,160,160,false);
        cropped_face=convertToNonHardwareConfig(cropped_face);
        FaceClassifier.Recognition recognition = classifier.recognizeImage(cropped_face,false);
//        recognition.getEmbeeding();
//        showRegisterDialoog(cropped_face,recognition);
        if(recognition!=null)
        {
            Log.d("face-recognition",recognition.getTitle()+" "+recognition.getDistance());
            if(recognition.getDistance()<1)
            {
                Paint p1 = new Paint();
                p1.setColor(Color.GREEN);
                p1.setTextSize(150);
                canvas.drawText(recognition.getTitle(),bound.left,bound.top,p1);
            }
        }

    }
    public void showRegisterDialoog(Bitmap face,FaceClassifier.Recognition recognition)
    {
        Dialog dialog = new Dialog(RecognizationActivity.this);
        dialog.setContentView(R.layout.register_dialog);
        register_image_view=dialog.findViewById(R.id.register_image_view);
        register=dialog.findViewById(R.id.register_button);
        register_id=dialog.findViewById(R.id.register_id);
        register_image_view.setImageBitmap(face);
        dialog.show();
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(TextUtils.isEmpty(register_id.getText().toString().trim()))
                    register_id.setError("Enter Proper Register-ID");
                else
                {
                    classifier.register(register_id.getText().toString().trim(),recognition);
                    Toast.makeText(RecognizationActivity.this, "Face Registered Successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            }
        });

    }
    public Bitmap convertToNonHardwareConfig(Bitmap bitmap) {
        if (bitmap.getConfig() == Bitmap.Config.HARDWARE) {
            // Create a new bitmap with a different config
            Bitmap nonHardwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
            return nonHardwareBitmap;
        }
        return bitmap;
    }



}