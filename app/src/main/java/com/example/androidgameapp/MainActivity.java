package com.example.androidgameapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {

    private TextToSpeech textToSpeech;

    protected static final int RESULT_SPEECH = 1;

    private static String myURL = "https://kezikov-spring-test.herokuapp.com/android?letter=";

    private static String lastLetter = null;


    private Button btnSpeak;
    private TextView txtText;

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtText = findViewById(R.id.txtText);
        btnSpeak = findViewById(R.id.mainButton);
        btnSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
                startActivityForResult(intent, RESULT_SPEECH);
            }
        });

    }

    @Override
    public void onPause() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> text = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    assert text != null;
                    String userCity = text.get(0);
                    lastLetter = userCity.substring(userCity.length() - 1).toUpperCase();
                    callServer();

                    //txtText.setText(newCity);
                }
                break;
            }
        }
    }

    public static String doGet(String url) throws IOException {

        URL obj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        connection.setRequestProperty("Content-Type", "application/json");

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = bufferedReader.readLine()) != null) {
            response.append(inputLine);
        }
        bufferedReader.close();

        System.out.println("Response string: " + response.toString());


        return response.toString();
    }

    @SuppressLint("StaticFieldLeak")
    private void callServer() {
        new AsyncTask<Void, String, String>() {
            @Override
            protected String doInBackground(Void[] voids) {
                String s = "";
                try {
                    s = doGet(myURL + lastLetter);
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Cannot load cities list", Toast.LENGTH_LONG).show();
                }
                return s;
            }

            @Override
            protected void onPostExecute(final String result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtText.setText(result);
                        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int status) {
                                Log.e("TTS", "TextToSpeech.OnInitListener.onInit...");
                                speakOut(result);


                            }
                        });
                    }
                });
            }
        }.execute();
    }

    private void speakOut(String text) {
        // Text to Speak
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        // A random String (Unique ID).
        String utteranceId = UUID.randomUUID().toString();
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }


}








