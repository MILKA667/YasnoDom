package com.yasnodom;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences pref;
    private SharedPreferences.Editor edit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = getSharedPreferences("YasnoDomStorage", MODE_PRIVATE);
        edit = pref.edit();
        if(pref.getBoolean("auth", false)){
            Intent intent = new Intent(this, HomeScreen.class);
            startActivity(intent);
            finish();
        }

        setContentView(R.layout.activity_main);
    }

    public void authorization(View view){
        EditText UserName = findViewById(R.id.EditTextUserName);
        String userName = UserName.getText().toString().trim();

        if(!userName.isEmpty()){
            edit.putBoolean("auth", true);
            edit.putString("userName", userName);
            edit.apply();

            Intent intent = new Intent(this, HomeScreen.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Введите ваше имя!", Toast.LENGTH_LONG).show();
        }
    }
}