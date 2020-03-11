package com.example.mathcrash;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.util.Linkify;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfoActivity extends AppCompatActivity {

    TextView information;
    Vibrator  vibrator;
    Linkify.TransformFilter my_filter = new Linkify.TransformFilter() { @Override public String transformUrl(Matcher match, String url) { return ""; } };
    List<Pair<String,String>> list;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        vibrator  = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        information = (TextView)findViewById(R.id.information);
        list = new ArrayList<Pair<String, String>>();

        ///////////
        add("google","https://www.flaticon.com/authors/google",true);
        add("Pixel perfect","https://www.flaticon.com/authors/pixel-perfect",false);
        ///////////


        for(int i = 0 ; i < list.size() ; ++i){
            Pattern pattern = Pattern.compile(list.get(i).first);
            Linkify.addLinks(information, pattern,list.get(i).second,null,my_filter);
        }
        Pattern pattern1 = Pattern.compile("www.flaticon.com");
        Linkify.addLinks(information, pattern1,"www.flaticon.com",null,my_filter);
        Pattern pattern2 = Pattern.compile("CC BY 3.0");
        Linkify.addLinks(information, pattern2,"https://creativecommons.org/licenses/by/3.0/",null,my_filter);
    }
    public void back(View v) {
        vibrator.vibrate(5);
        finish();
    }
    public void add(String name,String url,boolean CC_BY){
        information.setText(information.getText()+"\n● Icon made by "+name+" from www.flaticon.com");
        if(CC_BY)   information.setText(information.getText()+" (CC BY 3.0)");
        list.add(new Pair<String,String>(name,url));
    }
}
