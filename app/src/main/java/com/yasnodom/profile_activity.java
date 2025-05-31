package com.yasnodom;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class profile_activity extends AppCompatActivity {

    private SharedPreferences pref;
    private SharedPreferences.Editor edit;
    public TextView username;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        username = findViewById(R.id.text_user_name);
        pref = getSharedPreferences("YasnoDomStorage", MODE_PRIVATE);
        edit = pref.edit();
        username.setText(pref.getString("userName",""));

    }
}