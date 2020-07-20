package hs.bildverarbeitung.notes;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    MediaPlayer player;
    TextRecognizer textRecognizer;

    ImageView imageView;
    TextView textView;
    Button btnPlay;

    String pathToFile;
    int currentPosition;

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

                if(songname[0] != null) {
                    List<String> songs = getSongs(songname[0]);

                    StringBuilder notes = new StringBuilder();
                    for (int i = 0; i < songs.size(); i++) {
                        notes.append(songs.get(i)).append(" ");
                    }
                    textView.setText(notes);

                    playNote(songs);
                }
                else {
                    textView.setText("Es konnte keine Note erkannt werden.");
                }
            }
        });
    }

    private List<String> getSongs(String songname) {
        List<String> songs = new ArrayList<>();
        System.out.println(songname);
        for (int i = 0; i < songname.length() ; i++) {
            char song = songname.charAt(i);
            if(song == 'b') {
                if(songname.charAt(i+1) == 'H') {
                    songs.add("B");
                } else {
                    songs.add(String.valueOf(song) + songname.charAt(i + 1));
                }
                i++;
            } else if (song == 'h') {
                songs.add(String.valueOf(song) + songname.charAt(i + 1));
                i++;
            } else if (song == 'C' || song == 'D' || song == 'F' || song == 'G' || song == 'A') {
                char halbtonCheck = songname.charAt(i+1);
                if(halbtonCheck == 'i') {
                    songs.add('h' + String.valueOf(song));
                    i = i+2;
                } else if(halbtonCheck == 'e') {
                    songs.add('b' + String.valueOf(song));
                    i = i+2;
                } else {
                    songs.add(String.valueOf(song));
                }
            } else if (song == 'H') {
                char halbtonCheck = songname.charAt(i+1);
                if(halbtonCheck == 'i') {
                    songs.add('h' + String.valueOf(song));
                    i = i+2;
                } else if(halbtonCheck == 'e') {
                    songs.add("B");
                    i = i+2;
                } else {
                    songs.add(String.valueOf(song));
                }
            } else if (song == 'E') {
                char halbtonCheck = songname.charAt(i+1);
                if(halbtonCheck == 's') {
                    songs.add('b' + String.valueOf(song));
                    i++;
                } else {
                    songs.add(String.valueOf(song));
                }
            } else if (song == 'B') {
                songs.add("B");
            }
        }
        return songs;
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
            return stringBuilder.toString();
        }

        return null;
    }

    private void playNote(List<String> name) {
        currentPosition = 0;
        final List<Uri> songlist = new ArrayList<>();
        for (int i = 0; i < name.size(); i++) {
            songlist.add(Uri.parse("android.resource://" + getPackageName() + "/raw/" + name.get(i).toLowerCase()));
        }


        if(player == null) {
            //Uri uri = Uri.parse("android.resource://" + getPackageName() + "/raw/" + name.get(0));
            player = MediaPlayer.create(this, songlist.get(currentPosition));
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    currentPosition = currentPosition+1;
                    if(currentPosition<songlist.size())
                    {
                        mp.reset();
                        try {
                            /* load the new source */
                            mp.setDataSource(MainActivity.this ,songlist.get(currentPosition));
                            /* Prepare the mediaplayer */
                            mp.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        /* start */
                        mp.start();
                    }
                    else
                    {
                        /* release mediaplayer */
                        stopPlayer();
                    }
                }
            });
        }
        player.start();
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
}