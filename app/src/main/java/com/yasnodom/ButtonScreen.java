package com.yasnodom;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;


public class ButtonScreen extends AppCompatActivity {
    private SharedPreferences pref;
    private SharedPreferences.Editor edit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_button_screen);
    }

    public void light(View view){
        pref = getSharedPreferences("YasnoDomStorage", MODE_PRIVATE);
        edit = pref.edit();
        if(pref.getBoolean("light", false)){
            edit.putBoolean("light",false);
            edit.apply();
        }else{
            edit.putBoolean("light",true);
            edit.apply();
            Button light_butt = findViewById(R.id.button_light);
            light_butt.setText("Свет включен");
        }
    }
}