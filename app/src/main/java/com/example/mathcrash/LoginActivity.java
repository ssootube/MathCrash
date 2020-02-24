package com.example.mathcrash;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    TextView edt_port;
    TextView edt_nickname;
    String port;
    String nickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        edt_port = (TextView) findViewById(R.id.edt_port);
        edt_nickname = (TextView) findViewById(R.id.edt_nickname);
    }
    public void login(View v){
        if(edt_nickname.getText().toString().equals("") || edt_port.getText().toString().equals("")){
            Toast.makeText(getApplicationContext(),  "포트번호, 닉네임을 모두 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        else{
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            nickname = String.valueOf(edt_nickname.getText());
            port = String.valueOf(edt_port.getText());
            intent.putExtra("nickname",nickname);//닉네임 정보를 MainActivity에 전달합니다.
            intent.putExtra("port",port);//닉네임 정보를 MainActivity에 전달합니다.
            startActivity(intent);
            finish();
        }
    }
}
