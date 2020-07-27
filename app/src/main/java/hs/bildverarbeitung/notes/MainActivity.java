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

    /*
     * Initialisierung aller Felder der View
     * Abfrage Kamera und Speicherzugriff
     * Starten der Kamera auf Knopfdruck
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Initial anlegen der Viewelemente */
        Button btnCamera = (Button) findViewById(R.id.btnCamera);
        btnPlay = (Button) findViewById(R.id.btnPlay);
        imageView = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textView);

        /* Abfrage Berechtigung auf Kamera */
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }

        /* Abfrage ob Bibliothek für die Texterkennung funktionsfähig ist */
        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!textRecognizer.isOperational()) {
            Log.w("MainActivity", "Detector dependencies are not yet available.");
        }




        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textView.setText("Noticed Notes");
                /* Kamera starten */
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(intent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = createFile();
                    if(photoFile != null) {
                        /* aufgenommenes Foto da der Stelle des temporären Fotos speichern */
                        pathToFile = photoFile.getAbsolutePath();
                        Uri photoURI = FileProvider.getUriForFile(MainActivity.this, "hs.bildverarbeitung.notes.fileprovider", photoFile);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(intent, 1);
                    }
                }
            }
        });

    }

    /*
     * @return: Temporäres File für das aufgenommenen Bild
     * Anlegen einer temporären Datei, unter welchem später das aufgenommene Bild gespeichert wird.
     */
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

    /*
     * Erstellen der Bitmap und übergeben zur Anzeige und Texterkennung
     * Anzeige alle erkannter Noten in lateinischer Umschrift
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Bitmap bitmap = null;
        final String[] songname = {null};
        List<String> songs = null;

        /* Auslesen des Bildes zur Bitmap und Anzeigen in der Imageview der App */
        if(resultCode == RESULT_OK) {
            if(requestCode == 1) {
                bitmap = BitmapFactory.decodeFile(pathToFile);
                imageView.setImageBitmap(bitmap);
            }
        }

        /* Texterkennung aufruf */
        songname[0] = textRecognition(bitmap);
        if(songname[0] != null) {
            /* Notennamen aus erkanntem Text filtern */
            songs = getSongs(songname[0]);

            /* Notenliste in String zur Anzeige umformatieren*/
            StringBuilder notes = new StringBuilder();
            for (int i = 0; i < songs.size(); i++) {
                notes.append(songs.get(i)).append(" ");
            }
            textView.setText(notes);
        }
        else {
            /* 'Fehlerausgabe' falls keine Note erkannt wurde*/
            textView.setText("Es konnte keine Note erkannt werden.");
        }

        final List<String> finalSongs = songs;
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* Aufrufen des Players */
                playNote(finalSongs);
            }
        });
    }

    /*
    * @Params: String des erkannten Textes
    * @return: Liste der erkannten einzel Noten
    * Erkennen der Noten, ihre Halbtöne und Oktavenlage aus dem Text des Bildes
    * Untersuchen String auf halbton Markierungen (b, h) oder Noten (Großbuchstaben)
    */
    private List<String> getSongs(String songname) {
        List<String> songs = new ArrayList<>();
        System.out.println(songname);


        for (int i = 0; i < songname.length() ; i++) {
            char song = songname.charAt(i);
            int x;

            if((song == 'b' && songname.charAt(i+1) == 'H') || song == 'B') {
                int y = (song == 'B'? i+1 : i+2);
                x = checkHeight(songname, y);
                songs.add(buildNoteName(null, String.valueOf('B'), x));
                i = i + 1 + x;
            } else if(song == 'b' || song == 'h') {
                char note = songname.charAt(i+1);
                if(note == 'C' || note == 'D' || note == 'E' || note == 'F' || note == 'G'|| note == 'A'|| note == 'H') {
                    x = checkHeight(songname, i+2);
                    songs.add(buildNoteName(String.valueOf(song), String.valueOf(note), x));
                    i = i + 1 + x;
                }
            } else if(song == 'C' || song == 'D' || song == 'F' || song == 'G') {
                char halbtonCheck = songname.charAt(i+1);
                if(halbtonCheck == 'i') {
                    x = checkHeight(songname, i+3);
                    songs.add(buildNoteName(String.valueOf('h'), String.valueOf(song), x));
                    i = i+2+x;
                } else if(halbtonCheck == 'e') {
                    x = checkHeight(songname, i+3);
                    songs.add(buildNoteName(String.valueOf('b'), String.valueOf(song), x));
                    i = i+2+x;
                } else {
                    x = checkHeight(songname, i+1);
                    songs.add(buildNoteName(null, String.valueOf(song), x));
                    i = i+x;
                }
            } else if (song == 'H') {
                char halbtonCheck = songname.charAt(i+1);
                if(halbtonCheck == 'i') {
                    x = checkHeight(songname, i+3);
                    songs.add(buildNoteName(String.valueOf('h'), String.valueOf(song), x));
                    i = i+2+x;
                } else if(halbtonCheck == 'e') {
                    x = checkHeight(songname, i+3);
                    songs.add(buildNoteName(null, String.valueOf('B'), x));
                    i = i+2+x;
                } else {
                    x = checkHeight(songname, i+1);
                    songs.add(buildNoteName(null, String.valueOf(song), x));
                    i = i+x;
                }
            } else if (song == 'E' || song == 'A') {
                char halbtonCheck = songname.charAt(i + 1);
                if (halbtonCheck == 's' || halbtonCheck == 'S') {
                    x = checkHeight(songname, i+2);
                    songs.add(buildNoteName(String.valueOf('b'), String.valueOf(song), x));
                    i = i+1+x;
                } else if(halbtonCheck == 'i') {
                    x = checkHeight(songname, i + 3);
                    songs.add(buildNoteName(String.valueOf('h'), String.valueOf(song), x));
                    i = i + 2 + x;
                } else {
                    x = checkHeight(songname, i+1);
                    songs.add(buildNoteName(null, String.valueOf(song), x));
                    i = i+x;
                }
            }
        }

        return songs;
    }

    /*
     * @Params: tone String um welchen Halbton handelt es sich (b,h),
     *          note String welche Note (C,D,E,F,G,A,H),
     *          pitch welche Oktave
     * @return: String des Notennamens -> entspricht Audiodateinamen
     * Umschreiben der Höhe für die Oktave (vorerst low-high / 3 Oktaven)
     * und zusammenbauen des Strings für den Notennamen
     */
    private String buildNoteName(String tone, String note, int pitch) {
        String height = null;
        if (pitch == 0) {
            height = "low";
        } else if (pitch == 1) {
            height = "middle";
        } else if (pitch == 2) {
            height = "high";
        }

        if (tone == null) {
            return note + height;
        } else {
            return tone + note + height;
        }
    }

    /*
     * @Params: String aller erkannter Noten,
     *          i ab gegebener Stelle ist zu suchen
     * @return: Anzahl gefundener Ausrufezeichen für Tonhöhe
     * Erkennen der Anzahl von Ausrufezeichen hinter einer Note
     */
    private int checkHeight(String songname, int i) {
        if(songname.length() >= i) {
            int height = (songname.charAt(i) == '!'? 1:0);
            if (height==1 && (songname.length() >= i+1)) {
                height = (songname.charAt(i+1) == '!'? 2:1);
            }
            return height;
        }
        return 0;
    }

    /*
     * @Params: Bitmap des Bildes
     * @return: String des erkannten Textes
     * Erkennen des Textes im Bild, umschreiben in einen String
     */
    private String textRecognition(Bitmap bitmap) {
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        /* Texterkennung durch die Bibliotheksfunktion */
        final SparseArray<TextBlock> items = textRecognizer.detect(frame);
        if (items.size() != 0)
        {
            StringBuilder stringBuilder = new StringBuilder();
            /* Umschreiben der Liste in einen String */
            for (int i=0 ; i<items.size(); i++)
            {
                TextBlock item = items.valueAt(i);
                stringBuilder.append(item.getValue());
                stringBuilder.append("\n");
            }
            return stringBuilder.toString();
        }
        /* Sollte kein Text erkannt werden, Rückgabe null */
        return null;
    }

    /*
     * @Params: Liste von Strings der Notennamen (gleichen den Audiodateinamen)
     * Erstellen der Playlist, anhand der Audiodateinamen und nacheinander
     * abspielen der Noten
     */
    private void playNote(List<String> name) {
        currentPosition = 0;
        final List<Uri> songlist = new ArrayList<>();
        /* Hinzufügen der Audiodateien zur Playlist */
        for (int i = 0; i < name.size(); i++) {
            songlist.add(Uri.parse("android.resource://" + getPackageName() + "/raw/" + name.get(i).toLowerCase()));
        }


        if(player == null) {
            /* Mediaplayer mit erster Audiodatei erstellen */
            player = MediaPlayer.create(this, songlist.get(currentPosition));
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                /* Nach Ende der Audiodatei auszuführender Code */
                @Override
                public void onCompletion(MediaPlayer mp) {
                    currentPosition = currentPosition+1;
                    if(currentPosition<songlist.size())
                    {
                        mp.reset();
                        try {
                            /* neue Audiodatei laden */
                            mp.setDataSource(MainActivity.this ,songlist.get(currentPosition));
                            /* Mediaplayer vorbereiten */
                            mp.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        /* Mediaplayer starten */
                        mp.start();
                    }
                    else
                    {
                        /* Mediaplayer stoppen */
                        stopPlayer();
                    }
                }
            });
        }
        player.start();
    }

    /*
    * Freigeben des Mediaplayers
    */
    private void stopPlayer() {
        if(player != null) {
            /* release mediaplayer */
            player.release();
            player = null;
        }
    }

    /*
    * Wird die App geschlossen oder läuft nur noch im Hintergrund,
    * stoppen der Ton ausgabe
     */
    @Override
    protected void onStop() {
        super.onStop();
        stopPlayer();
    }
}