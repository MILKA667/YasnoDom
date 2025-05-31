package com.yasnodom;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ForegroundService extends Service implements LocationListener {
    private static final String TAG = "LocationForegroundService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final String KEY_LATITUDE = "current_latitude";
    private static final String KEY_LONGITUDE = "current_longitude";
    private static final long UPDATE_INTERVAL_MS = 10000;
    private static final float MIN_DISTANCE_M = 10;

    private LocationManager locationManager;
    private SharedPreferences sharedPreferences;
    private ScheduledExecutorService scheduler;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("YasnoDomStorage", MODE_PRIVATE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        notificationManager = getSystemService(NotificationManager.class);

        createNotificationChannel();
        startLocationTracking();
    }



    private void startLocationTracking() {
        if (checkLocationPermission()) {
            try {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        UPDATE_INTERVAL_MS,
                        MIN_DISTANCE_M,
                        this
                );

                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        UPDATE_INTERVAL_MS,
                        MIN_DISTANCE_M,
                        this
                );

                getLastKnownLocation();

                scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleAtFixedRate(this::checkProviders,
                        0, 30, TimeUnit.SECONDS);

            } catch (SecurityException e) {
                Log.e(TAG, "Ошибка безопасности: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при запуске отслеживания: " + e.getMessage());
            }
        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void getLastKnownLocation() {
        try {
            Location lastGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location lastNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            Location lastLocation = getMostRecentLocation(lastGpsLocation, lastNetworkLocation);
            if (lastLocation != null) {
                saveLocation(lastLocation);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Нет разрешения для получения последнего местоположения");
        }
    }

    private Location getMostRecentLocation(Location... locations) {
        Location result = null;
        long maxTime = Long.MIN_VALUE;

        for (Location location : locations) {
            if (location != null && location.getTime() > maxTime) {
                result = location;
                maxTime = location.getTime();
            }
        }
        return result;
    }

    private void sendNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_yasnodom_background)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(2, builder.build()); // Используем другой ID для этого уведомления
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        saveLocation(location);

        float distance = calculateDistance();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("distance", distance);

        boolean wasHome = sharedPreferences.getBoolean("user_is_home", true);
        boolean isHomeNow = distance < 500.0F;

        if (!isHomeNow && wasHome) {
            // Пользователь только что вышел из дома
            sendNotification("Вы вышли из дома", "Вы находитесь на расстоянии " + (int) distance + " метров от дома");
            Log.d(TAG, "Отправлено уведомление: пользователь вышел из дома");
        }

        editor.putBoolean("user_is_home", isHomeNow);
        editor.apply();

        if (isHomeNow) {
            Log.d(TAG, "Пользователь в пределах 500 м от дома");
        } else {
            Log.d(TAG, "Пользователь вышел за 500 м от дома");
        }
    }

    private void saveLocation(Location location) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(KEY_LATITUDE, (float) location.getLatitude());
        editor.putFloat(KEY_LONGITUDE, (float) location.getLongitude());
        editor.apply();

        Log.d(TAG, String.format("Сохранено местоположение: %.6f, %.6f",
                location.getLatitude(), location.getLongitude()));
    }

    private float calculateDistance() {
        try {
            float currentLat = sharedPreferences.getFloat(KEY_LATITUDE, Float.NaN);
            float currentLon = sharedPreferences.getFloat(KEY_LONGITUDE, Float.NaN);
            float homeLat = sharedPreferences.getFloat("home_latitude", Float.NaN);
            float homeLon = sharedPreferences.getFloat("home_longitude", Float.NaN);
            Log.d(TAG,"СЧИТАЕМ");
            Location currentLoc = new Location("current");
            currentLoc.setLatitude(currentLat);
            currentLoc.setLongitude(currentLon);

            Location homeLoc = new Location("home");
            homeLoc.setLatitude(homeLat);
            homeLoc.setLongitude(homeLon);

            float distance = currentLoc.distanceTo(homeLoc);
            Log.d(TAG, String.format("Distance calculation: current(%.6f,%.6f) to home(%.6f,%.6f) = %.2fm",
                    currentLat, currentLon, homeLat, homeLon, distance));
            return distance;
        } catch (Exception e) {
            Log.e(TAG, "Distance calculation error: " + e.getMessage());
            return Float.POSITIVE_INFINITY;
        }
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Сервис геолокации YasnoDom")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_yasnodom_background)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void updateNotification(String text) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Сервис геолокации",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Канал для сервиса отслеживания местоположения");
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void checkProviders() {
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsEnabled && !networkEnabled) {
            updateNotification("Поиск сигнала...");
            Log.w(TAG, "Нет доступных провайдеров местоположения");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationTracking();
        if (scheduler != null) {
            scheduler.shutdown();
        }
        Log.d(TAG, "Сервис остановлен");
    }

    private void stopLocationTracking() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(@NonNull String provider) {}
    @Override public void onProviderDisabled(@NonNull String provider) {}
}