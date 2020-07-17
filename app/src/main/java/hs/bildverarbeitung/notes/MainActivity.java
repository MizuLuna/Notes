package hs.bildverarbeitung.notes;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    MediaPlayer player;
    TextRecognizer textRecognizer;

    ImageView imageView;
    TextView textView;
    Button btnPlay;

    String pathToFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnCamera = (Button) findViewById(R.id.btnCamera);
        btnPlay = (Button) findViewById(R.id.btnPlay);
        imageView = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textView);

        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }

        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.w("MainActivity", "Detector dependencies are not yet available.");
        }




        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(intent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = createFile();
                    if(photoFile != null) {
                        pathToFile = photoFile.getAbsolutePath();
                        Uri photoURI = FileProvider.getUriForFile(MainActivity.this, "hs.bildverarbeitung.notes.fileprovider", photoFile);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(intent, 1);
                    }
                }
            }
        });

    }

    private File createFile() {
        File image = null;
        String name = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            image = File.createTempFile(name, ".jpg", storageDir);

        } catch (IOException e) {
            Log.d("myLog", "excep: " + e.toString());
        }
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Bitmap bitmap = null;
        final String[] songname = {null};

        if(resultCode == RESULT_OK) {
            if(requestCode == 1) {
                bitmap = BitmapFactory.decodeFile(pathToFile);
                imageView.setImageBitmap(bitmap);
            }
        }

        final Bitmap finalBitmap = bitmap;
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                songname[0] = textRecognition(finalBitmap);
            }
        });

        String[] songs = getSongs(songname[0]);

        if(songs != null) {
            playNote(songs);
        }
    }

    private String[] getSongs(String songname) {
        //String[] result = songname;
        //ToDo Songname aufspliten in song tracks
        return result;
    }

    private String textRecognition(Bitmap bitmap) {
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();

        final SparseArray<TextBlock> items = textRecognizer.detect(frame);
        if (items.size() != 0)
        {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i=0 ; i<items.size(); i++)
            {
                TextBlock item = items.valueAt(i);
                stringBuilder.append(item.getValue());
                stringBuilder.append("\n");
            }
            textView.setText(stringBuilder.toString());
            return stringBuilder.toString();
        }


    }

    private void playNote(String[] name) {
//        if(player == null) {
//            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/raw/" + name);
//            player = MediaPlayer.create(this, uri);
//            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                @Override
//                public void onCompletion(MediaPlayer mp) {
//                    stopPlayer();
//                }
//            });
//        }
//        player.start();
    }

    private void stopPlayer() {
        if(player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPlayer();
    }

//        cameraView = (SurfaceView) findViewById(R.id.surface_view);
//        textBlockContent = (TextView) findViewById(R.id.text_value);
//
//        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
//        if (!textRecognizer.isOperational()) {
//            Log.w("MainActivity", "Detector dependencies are not yet available.");
//        }
//
//        cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
//                .setFacing(CameraSource.CAMERA_FACING_BACK)
//                .setRequestedPreviewSize(1280, 1024)
//                .setRequestedFps(2.0f)
//                .setAutoFocusEnabled(true)
//                .build();
//
//        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(SurfaceHolder holder) {
//                try {
//                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
//                        return;
//                    }
//                    cameraSource.start(cameraView.getHolder());
//                } catch (IOException ex) {
//                    ex.printStackTrace();
//                }
//            }
//
//            @Override
//            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//            }
//
//            @Override
//            public void surfaceDestroyed(SurfaceHolder holder) {
//                cameraSource.stop();
//            }
//        });
//
//        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
//            @Override
//            public void release() {
//            }
//
//            @Override
//            public void receiveDetections(Detector.Detections<TextBlock> detections) {
//                Log.d("Main", "receiveDetections");
//                final SparseArray<TextBlock> items = detections.getDetectedItems();
//                if (items.size() != 0) {
//                    textBlockContent.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            StringBuilder value = new StringBuilder();
//                            for (int i = 0; i < items.size(); ++i) {
//                                TextBlock item = items.valueAt(i);
//                                value.append(item.getValue());
//                                value.append("\n");
//                            }
//                            //update text block content to TextView
//                            textBlockContent.setText(value.toString());
//                        }
//                    });
//                }
//
//            }
//        });


//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        cameraSource.release();
//    }
}