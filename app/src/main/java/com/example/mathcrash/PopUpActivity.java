package com.example.mathcrash;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

public class PopUpActivity extends AppCompatActivity {

    TextView tv_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_pop_up);
        tv_text = (TextView)findViewById(R.id.str);
        Intent intent = getIntent();
        String str = intent.getStringExtra("str");
        tv_text.setText(str);
    }


    public void YES(View v){
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }
    public void NO(View v){
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }
    @Override
    public void onBackPressed() {
        return;
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {//다른 곳 터치 시 닫히는 것을 막는다.
        if(event.getAction()== MotionEvent.ACTION_OUTSIDE){
            return false;
        }
        return true;
    }

}
