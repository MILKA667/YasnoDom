package com.yasnodom;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class HomeScreen extends AppCompatActivity {
    private TextToSpeech textToSpeech;
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        pref = getSharedPreferences("YasnoDomStorage", MODE_PRIVATE);

        // Проверка разрешений
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1001);
        }

        speak("Здравствуйте, " + pref.getString("userName", "пользователь"));

        // Запуск сервиса
        Intent intent = new Intent(this, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView user_home_text = findViewById(R.id.textView);
        TextView distance_text = findViewById(R.id.textView3);
        if(pref.getBoolean("user_is_home", true)) {
            user_home_text.setText("Вы дома");
        } else {
            user_home_text.setText("Вы не дома");
        }
        distance_text.setText(String.valueOf(pref.getFloat("distance",0.0F)));

    }

    private void speak(String text) {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("ru", "RU"));
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Язык не поддерживается", Toast.LENGTH_SHORT).show();
                } else {
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            } else {
                Toast.makeText(this, "Ошибка инициализации TTS", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    public void GoToButtonScreen(View view) {
        Intent intent = new Intent(this, ButtonScreen.class);
        startActivity(intent);
    }
}