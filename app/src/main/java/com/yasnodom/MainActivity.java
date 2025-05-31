package com.yasnodom;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences pref;
    private SharedPreferences.Editor edit;
    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = getSharedPreferences("YasnoDomStorage", MODE_PRIVATE);
        edit = pref.edit();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if(pref.getBoolean("auth", false)){
            Intent intent = new Intent(this, HomeScreen.class);
            startActivity(intent);
            finish();
        }

        setContentView(R.layout.activity_main);
    }

    public void authorization(View view) {
        EditText UserName = findViewById(R.id.EditTextUserName);
        String userName = UserName.getText().toString().trim();

        if(!userName.isEmpty()) {
            if (checkLocationPermission()) {
                getLocationAndProceed(userName);
            }
        } else {
            Toast.makeText(this, "Введите ваше имя!", Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
    }

    private void getLocationAndProceed(String userName) {
        try {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    saveLocationAndUser(userName, lastKnownLocation);
                }

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);

                proceedToHomeScreen(userName);
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Ошибка доступа к геолокации", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveLocationAndUser(String userName, Location location) {
        edit.putBoolean("auth", true);
        edit.putString("userName", userName);
        edit.putFloat("home_latitude", (float) location.getLatitude());
        edit.putFloat("home_longitude", (float) location.getLongitude());
        edit.apply();

        Toast.makeText(this,
                "Широта: " + location.getLatitude() +
                        "\nДолгота: " + location.getLongitude(),
                Toast.LENGTH_LONG).show();
    }

    private void proceedToHomeScreen(String userName) {
        edit.putBoolean("auth", true);
        edit.putString("userName", userName);
        edit.apply();

        Intent intent = new Intent(this, HomeScreen.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                EditText UserName = findViewById(R.id.EditTextUserName);
                String userName = UserName.getText().toString().trim();
                getLocationAndProceed(userName);
            } else {
                Toast.makeText(this,
                        "Для работы приложения необходимо разрешение на доступ к геолокации",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                saveLocationAndUser(pref.getString("userName", ""), location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };

    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }
}