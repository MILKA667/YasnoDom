package com.yasnodom;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class ButtonScreen extends AppCompatActivity {
    private SharedPreferences pref;
    private SharedPreferences.Editor edit;
    public ImageButton light_butt;
    private static final String FRIDGE_CLICKS_KEY = "fridge_clicks";
    private static final String FRIDGE_TIMES_KEY = "fridge_click_times";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_button_screen);
        light_butt = findViewById(R.id.button_light);
        pref = getSharedPreferences("YasnoDomStorage", MODE_PRIVATE);
        edit = pref.edit();

        // Инициализация данных по кликам, если их нет
        if (!pref.contains(FRIDGE_CLICKS_KEY)) {
            edit.putInt(FRIDGE_CLICKS_KEY, 0);
            edit.putStringSet(FRIDGE_TIMES_KEY, new HashSet<>());
            edit.apply();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLightButton();
        cleanOldClicks(); // Очищаем старые клики при возобновлении активности
    }

    public void light(View view) {
        boolean currentState = pref.getBoolean("light", false);
        edit.putBoolean("light", !currentState);
        edit.apply();
        updateLightButton();
    }

    private void updateLightButton() {
        if(pref.getBoolean("light", false)) {
            light_butt.setImageResource(R.drawable.my_light);
        } else {
            light_butt.setImageResource(R.drawable.my_off_light);
        }
    }

    public void fridge(View view) {
        // Получаем текущее время
        long currentTime = System.currentTimeMillis();

        // Получаем сохраненные времена кликов
        Set<String> clickTimes = new HashSet<>(pref.getStringSet(FRIDGE_TIMES_KEY, new HashSet<>()));

        // Добавляем текущее время
        clickTimes.add(String.valueOf(currentTime));

        // Сохраняем обновленные данные
        edit.putStringSet(FRIDGE_TIMES_KEY, clickTimes);
        edit.putInt(FRIDGE_CLICKS_KEY, clickTimes.size());
        edit.apply();

        // Проверяем количество кликов за последний час
        if (clickTimes.size() >= 3) {
            openYandexFood();
            resetFridgeClicks(); // Сбрасываем счетчик после открытия
        }
    }

    private void cleanOldClicks() {
        long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
        Set<String> clickTimes = new HashSet<>(pref.getStringSet(FRIDGE_TIMES_KEY, new HashSet<>()));
        Set<String> recentClicks = new HashSet<>();

        for (String timeStr : clickTimes) {
            long clickTime = Long.parseLong(timeStr);
            if (clickTime > oneHourAgo) {
                recentClicks.add(timeStr);
            }
        }

        edit.putStringSet(FRIDGE_TIMES_KEY, recentClicks);
        edit.putInt(FRIDGE_CLICKS_KEY, recentClicks.size());
        edit.apply();
    }

    private void resetFridgeClicks() {
        edit.putInt(FRIDGE_CLICKS_KEY, 0);
        edit.putStringSet(FRIDGE_TIMES_KEY, new HashSet<>());
        edit.apply();
    }

    private void openYandexFood() {
        try {
            // Пытаемся открыть Яндекс Еда через официальное приложение
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("yandexfood://"));
            startActivity(intent);
        } catch (Exception e) {
            try {
                // Если приложения нет, открываем в браузере
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://eda.yandex.ru"));
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "Не удалось открыть Яндекс Еда", Toast.LENGTH_SHORT).show();
            }
        }
    }
}