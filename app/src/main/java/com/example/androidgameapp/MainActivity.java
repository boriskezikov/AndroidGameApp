package com.example.androidgameapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MainActivity extends Activity {

    private TextToSpeech textToSpeech;
    protected static final int RESULT_SPEECH = 1;
    private static String lastUserLetter = null;
    private static String lastMachineLetter = null;
    private TextView currentLives;
    private static int initLivesNum = 1;
    private Button btnSpeak;
    private static String currentUserCity = null;
    private List<String> usedCities = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isOnline();
        currentLives = findViewById(R.id.lives);
        btnSpeak = findViewById(R.id.mainButton);
        setLives(initLivesNum);
        requestAudioPermissions();
        btnSpeak.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
            startActivityForResult(intent, RESULT_SPEECH);
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    currentUserCity = text != null ? text.get(0) : null;

                    if (checkUserCity(currentUserCity)) {
                        usedCities.add(currentUserCity);
                        getCity();
                    }
                }
                break;
            }
            default:
                throw new IllegalStateException("Unexpected value: " + requestCode);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void getCity() {
        new AsyncTask<Void, String, String>() {
            @Override
            protected String doInBackground(Void[] voids) {
                String status  = doGet(getResources().getString(R.string.HEALTH));
                if (status.equals("UP")){
                    Boolean valid = Boolean.valueOf(doGet(getResources().getString(R.string.VALIDATE_CITY) + currentUserCity));
                    if (valid) {
                        lastUserLetter = currentUserCity.substring(currentUserCity.length() - 1).toUpperCase();
                        return doGet(getResources().getString(R.string.GET_CITY) + lastUserLetter);
                    } else {
                        return String.valueOf(valid);
                    }

                }
                else{
                    Intent myIntent = new Intent(MainActivity.this,NetworkFailsActivity.class);
                    MainActivity.this.startActivity(myIntent);
                    throw new AndroidBusinessApiException("Business API currently unavailable ");

                }

            }

            @Override
            protected void onPostExecute(final String result) {
                runOnUiThread(() -> textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
                    Log.e("TTS", "OnPostExecute with result: " + result);
                    if (result.equals("false")) {
                        speakOut(getResources().getString(R.string.INCORRECT_CITY_PHRASE));
                    } else {
                        speakOut(result);
                        lastMachineLetter = result.substring(result.length() - 1);
                        usedCities.add(result);
                    }
                }));
            }
        }.execute();
    }

    private void speakOut(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        String utteranceId = UUID.randomUUID().toString();
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    private static String doGet(String url) {
        StringBuilder response = new StringBuilder();
        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            connection.setRequestProperty("Content-Type", "application/json");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = bufferedReader.readLine()) != null) {
                response.append(inputLine);
            }
            bufferedReader.close();

            System.out.println("Response string: " + response.toString());

        }
        catch (IOException io){
            Log.e("WEB-SOCKET","Connection disabled! Check logs!");
        }
        return response.toString();
    }

    private void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        1);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        1);
            }
        }
        //If permission is granted, then go ahead recording audio
        else {
            ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Permissions Denied to record audio", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void setLives(int lives) {
        currentLives.setText(getResources().getString(R.string.LIVES_TEXT) + lives);
    }

    private boolean checkUserCity(String city) {
        if (Objects.isNull(city)) {
            return false;
        }
        if (initLivesNum < 1) {
            Toast.makeText(getApplicationContext(), "You loose!", Toast.LENGTH_SHORT).show();
            initLivesNum = 5;
            usedCities.clear();
            textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
                Log.e("TTS", "TextToSpeech.LIVES_OVER.");
                speakOut(getResources().getString(R.string.LOOSE_TEXT));
            });
            this.onRestart();
        }

        if (Objects.nonNull(lastMachineLetter)) {
            if (!currentUserCity.substring(0, 1).toLowerCase().equals(lastMachineLetter)) {
                textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
                    Log.e("TTS", "TextToSpeech.IncorrectLastLetter");
                    speakOut(String.format(getResources().getString(R.string.INCORRECT_LAST_LATTER_PHRASE), lastMachineLetter));
                });
                return false;
            }
        }

        if (usedCities.contains(city)) {
            textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
                Log.e("TTS", "TextToSpeech.USER_CITY_REPEATS");
                speakOut(getResources().getString(R.string.ON_REPEAT_PHRASE));
            });
            initLivesNum -= 1;
            setLives(initLivesNum);
            return false;
        }

        return true;
    }

    @Override
    public void onPause() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        System.out.println("OnPause");
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
        System.out.println("ONDestroy");

        super.onDestroy();
    }

    @SuppressLint("StaticFieldLeak")
    public void isOnline() {

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {
                ConnectivityManager cm =
                        (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                while (true) {
                    NetworkInfo activeNetwork = Objects.requireNonNull(cm).getActiveNetworkInfo();
                    boolean isConnected = Objects.requireNonNull(activeNetwork).isConnected();
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (isConnected) {
                        break;
                    }
                    Toast.makeText(getApplicationContext(), "No internet connection.. Check it and try again!", Toast.LENGTH_LONG).show();

                }
                return true;
            }


        };
    }


}
















