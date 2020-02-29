package com.example.mathcrash;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    int port;
    long mytime;
    int user_online; //온라인 상태인 유저의 수
    String nickname;
    private Socket socket;
    String serverIP = "49.50.162.149";
    private BufferedInputStream socket_in;
    private BufferedOutputStream socket_out;
    byte[] buf;
    TextView question, info, ccu, rank1;
    Button submit, exit;
    EditText answer;
    ProgressBar timer;

    String s = "";

    public static int byteArrayToInt(byte[] b) {
        if (b.length == 4)
            return b[0] << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8
                    | (b[3] & 0xff);
        else if (b.length == 2)
            return 0x00 << 24 | 0x00 << 16 | (b[0] & 0xff) << 8 | (b[1] & 0xff);

        return 0;
    }
    public static byte[] intToByteArray(int value) {
        byte[] bytes=new byte[4];
        bytes[0]=(byte)((value&0xFF000000)>>24);
        bytes[1]=(byte)((value&0x00FF0000)>>16);
        bytes[2]=(byte)((value&0x0000FF00)>>8);
        bytes[3]=(byte) (value&0x000000FF);
        return bytes;
    }

    public static int[] getIntArrayFromByteArray(byte[] array,int array_size){
        int[] result = new int[array_size];
        for(int i = 0 ; i < array_size ; ++i){
            byte[] temp_container = new byte[4];
            for(int j = 0 ; j < 4 ; ++j){
                temp_container[j]=array[4*(i)+(3-j)];
            }
            int temp_int = byteArrayToInt(temp_container);
            result[i] = temp_int;
        }
        return result;
    }

    public void send_signal(int singal){//서버에 시그널을 보낸다.
        write_to_server(singal);
    }
    public void write_to_server(int num){//서버에 정수를 보내는 함수
        try {
            byte[] temp1 = intToByteArray(num);
            byte[] temp2 = new byte[4];
            temp2[0]=temp1[3];temp2[1]=temp1[2];temp2[2]=temp1[1];temp2[3]=temp1[0];
            socket_out.write(temp2,0,4);
            socket_out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void write_to_server(String data){//서버에 문자열을 보내는 함수
        try {
            byte[] temp = data.getBytes("UTF-8");
            send_signal(2);
            write_to_server(temp.length);
            socket_out.write(temp,0,temp.length);
            socket_out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String get_string_from_server(){

        String result ="";
        try {

            byte[] temp_buf = new byte[4];
            int[] temp;
            socket_in.read(temp_buf, 0, 4);
            temp = getIntArrayFromByteArray(temp_buf, 1);
            int size = temp[0];
            if(size != 0){
                temp_buf = new byte[size];
                socket_in.read(temp_buf, 0, size);
               result= temp_buf.toString();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    //아래 변수들은 실시간으로 계속 자동으로 서버로 부터 가져오게 구현했으므로 마음대로 사용하시오

    public synchronized void do_this_when_quiz_arrived(){
        mytime = System.currentTimeMillis();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                answer.setEnabled(true);
                answer.setHint("정답을 입력하세요");

            }
        });
        for(int i=0; i<q_size-1;i++){
            s+= String.valueOf(quiz[i]) +" ";
            question.setText(s);
        }
        s="";
        is_quiz_arrived = false;
    }

    boolean is_quiz_arrived = false; //quiz,q_size,quiz_time은 세트로 동시에 도착함.
    int[] quiz;//현재의 수열추리 문제가 담겨 있는 배열
    int q_size = 0;//수열추리 문제의 길이. quiz[q_size-1]에 담긴 값이 정답이고, quiz[0]~quiz[q_size-2]에는 문제가 담겨 있다.
    int quiz_time = 0;//가장 최근의 수열추리 문제 출제 직후 흐른시간. 초단위. 남은시간이 아니라 흐른 시간이다.
    public synchronized void do_this_when_coin_arrived(){

        info.setText(nickname +"님의 COIN "+ coin[user_number] +"개");

        runOnUiThread(new Runnable() {//랭킹은 코인 수에 따라 바뀌므로, 코인이 도착했을 때 뿌려주는 게 맞다.
            @Override
            public void run() {
                String ranking ="";
                List<Integer> rank = new ArrayList<>();
                for(int i=0; i<coin_size;i++){
                    rank.add(new Integer(coin[i]));
                }

                Collections.reverse(rank);
                if(user_online>=5){
                    for(int i=1; i<=5;i++){
                        ranking += i +"위 " + String.valueOf(rank.get(i-1)) +"개 \n";
                    }
                }else{
                    for(int i=1; i<=user_online;i++){
                        ranking += i +"위 " + String.valueOf(rank.get(i-1)) +"개 \n";
                    }
                }
                rank1.setText(ranking);
            }
        });


        is_coin_arrived = false;

        //네트워크 쪽에서 값을 받아오는 스레드와, 이 함수를 실행시키는 스레드를 분리시켜 놓았으므로, 꼭 is_coin_arrived 변수를 잘 관리해야한다. 제 때 false로 잘 바꿔야 함. is_quiz_arrived도 마찬가지.
    }

    boolean is_coin_arrived = false;//coin, coin_size는 동시에 도착함
    int[] coin;//현재 접속중인 유저들의 코인 현황. 예를 들어 coin[2]에는 2번 유저의 코인 값이 저장되어 있다. 단, 코인이 0이 저장되어 경우 중도이탈, 접속 끊킨 유저이다.
    //coin배열의 값을 수정해도 서버에서 처리되는 것이기에 코인을 증가시킬 수 없다. 물론 잠깐은 증가된 것처럼 보일 수 있겠지만, 서버에서 데이터를 받아와서 업데이트 시키면 무효.
    int coin_size = 0;//coin 배열의 길이

    boolean is_user_number_arrived = false;
    int user_number = -1; // 자신의 유저번호. 즉, 자신의 코인 값은 coin[user_number]로 볼 수 있다.
    public void answer_to_server(boolean correct_or_not){//서버에 문제를 맞추었는지 아닌지 여부를 전송한다. true일 경우 정답. false일 경우 오답.
        if(correct_or_not){
            write_to_server(0);
            write_to_server(0);
        }
        else{
            write_to_server(0);
            write_to_server(1);
        }
    }

    boolean is_nickname_arrived = false;
    String[] other_nicknames;//그냥 nicknames로 하려니까, nickname변수랑 헷갈릴 거 같아서 이렇게 명명함.
    int other_nicknames_size = 0;
    public synchronized void do_this_when_nicknames_arrived(){

        runOnUiThread(new Runnable() {//닉네임은 접속하거나 종료할 때만 변경되므로, 이 부분에 접속자 체크를 하는 게 더 효율적이다.
            @Override
            public void run() {
                int temp = 0;
                for(int i=0; i<coin_size; i++){
                    if(coin[i]!=0) temp++;
                }
                ccu.setText(String.valueOf(temp) +"명 접속 중입니다");
                user_online = temp; //스레드를 사용할 때에는 전역 변수에 완성된 결과만을 담자. 중간에 끊켜서 들어갈 수도 있다. 전역변수 채로 카운트 하지 말자.
            }
        });

        is_nickname_arrived = false;
    }

    int my_attack_success = 0; // 공격을 시도하기 전, 항상 이 변수를 0으로 맞춰놓는다. 이 변수가 1로 바뀌는 순간, 내 공격이 성공한 것이고, 2로 바뀌면 실패한 것이다. 그대로 0이면, 아직 내 공격에 대한 성공 여부가 서버로 부터 도착하지 않은 것이다.
    public synchronized void do_this_when_attacked(int attacked_by, boolean attack_success){
        //attacked_by에는 자신을 공격한 유저의 유저번호가 담겨있다.
        if(attack_success == true){
            //상대의 공격이 성공한 경우
        }
        else if(attack_success == false){
            //상대의 공격이 실패한 경우 (실드에 막힘). 실드의 소모는 서버에서 자동으로 이미 처리 됨.
        }

        //공격의 경우에는 false로 만들어줄 arrived변수는 없음.
    }

    int my_shield = 0; //내 실드 아이템의 개수. 3개가 최대.
    boolean is_my_shield_arrived = false;
    public synchronized void do_this_when_my_shield_arrived(){
        is_my_shield_arrived = false;
    }

    public synchronized void buy_item(int item_code, int user_number){
        //이 함수 내부는 수정하지 말고, 함수 자체를 이용만 해주세요.
        //당연히 서버로 구매 신호를 보내는 거기 때문에, 이 함수는 메인 스레드에서 실행하면 안된다.
        //코인이 부족하거나, 이미 접속이 끊어진 유저에게 공격을 보낸다던가 하는 경우에는 아무런 동작도 하지 않는다. 오류는 안나니 걱정 하지 마세요. 그냥 아무런 동작도 안함.

        /*
        가격은 추후 변동될 예정.
        0: 공격하기 20코인 :usernumber를 공격한다.
        1: 실드 구매 30코인
         */
        switch (item_code){
            case 0://공격하기
                send_signal(4);
                write_to_server(user_number);
                break;
            case 1://실드 구매
                send_signal(5);
                break;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nickname = getIntent().getExtras().getString("nickname");//LoginActivity에서 받아온 닉네임 정보를 저장한다.
        port = Integer.valueOf(getIntent().getExtras().getString("port"));//LoginActivity에서 받아온 포트번호 정보를 저장한다.

        question = (TextView)findViewById(R.id.question);
        submit = (Button)findViewById(R.id.submit);
        answer = (EditText)findViewById(R.id.answer);
        info = (TextView)findViewById(R.id.info);
        exit = (Button)findViewById(R.id.exit);
        ccu = (TextView)findViewById(R.id.ccu);
        rank1 = (TextView)findViewById(R.id.rank1);
        timer = (ProgressBar)findViewById(R.id.timer);

        Thread worker = new Thread() {
             public void run() {
                try{
                    socket  = new Socket(); // 소켓 생성
                    SocketAddress serverAddress = new InetSocketAddress(serverIP, port);//주소 등록
                    socket.connect(serverAddress, 3000); // 연결시도
                    socket_out = new BufferedOutputStream(socket.getOutputStream());
                    socket_in = new BufferedInputStream(socket.getInputStream());
                    buf= new byte[50];

                    //서버에 닉네임을 전송합니다.
                    write_to_server(nickname);

                    //유저번호를 받아옵니다.
                    socket_in.read(buf,0,4);
                    int[] temp_buf = getIntArrayFromByteArray(buf,1);
                    user_number = temp_buf[0];
                    is_user_number_arrived = true;

                    while(socket.isConnected()) {
                        socket_in.read(buf, 0, 4);
                        temp_buf = getIntArrayFromByteArray(buf, 1);
                        int[] temp;
                        switch (temp_buf[0]) {
                            case 0:
                                //서버로 부터 수열 추리 문제가 도착한 경우
                                socket_in.read(buf, 0, 4);
                                temp = getIntArrayFromByteArray(buf, 1);
                                q_size = temp[0];
                                socket_in.read(buf, 0, 4 * q_size);
                                quiz = getIntArrayFromByteArray(buf, q_size);
                                socket_in.read(buf, 0, 4);
                                temp = getIntArrayFromByteArray(buf, 1);
                                quiz_time = temp[0];
                                is_quiz_arrived = true;
                                break;
                            case 1:
                                //서버로 부터 coin정보 데이터가 도착한 경우
                                socket_in.read(buf, 0, 4);
                                temp = getIntArrayFromByteArray(buf, 1);
                                coin_size = temp[0];
                                socket_in.read(buf, 0, 4 * coin_size);
                                coin = getIntArrayFromByteArray(buf, coin_size);
                                is_coin_arrived = true;
                                break;
                            case 2:
                                break;
                            case 3:
                                //서버로 부터 닉네임 정보 데이터가 도착한 경우
                                socket_in.read(buf, 0, 4);
                                temp = getIntArrayFromByteArray(buf, 1);
                                other_nicknames_size = temp[0]+4;
                                other_nicknames = new String[other_nicknames_size];
                                for(int i = 4 ; i <other_nicknames_size;++i){
                                    other_nicknames[i] = get_string_from_server();
                                }
                                is_nickname_arrived = true;
                                break;
                            case 4:
                                //서버로 부터 공격 신호가 도착한 경우
                                socket_in.read(buf, 0, 4);
                                temp = getIntArrayFromByteArray(buf, 1);
                                switch(temp[0]){
                                    case 0:
                                        //내 공격이 성공한 경우
                                        my_attack_success = 1;
                                        break;
                                    case 1:
                                        //내 공격이 실패한 경우
                                        my_attack_success = 2;
                                        break;
                                    case 2:
                                        //상대의 공격시도가 성공한 경우
                                    {
                                        socket_in.read(buf, 0, 4);
                                        int[] temp2;
                                        temp2 = getIntArrayFromByteArray(buf, 1);
                                        do_this_when_attacked(temp2[0],true);
                                    }
                                        break;
                                    case 3:
                                    {
                                        socket_in.read(buf, 0, 4);
                                        int[] temp2;
                                        temp2 = getIntArrayFromByteArray(buf, 1);
                                        do_this_when_attacked(temp2[0],false);
                                    }
                                        //상대의 공격시도가 실패한 경우
                                        break;
                                }

                                break;

                            case 5:
                                //서버로 부터 자신의 실드 아이템 개수 정보가 도착한 경우
                                socket_in.read(buf, 0, 4);
                                temp = getIntArrayFromByteArray(buf, 1);
                                my_shield = temp[0];
                                is_my_shield_arrived = true;
                                break;
                        }

                        exit.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                try {
                                    socket.close();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    });

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                    }
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "서버와의 연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                    finish();
                }catch (SocketTimeoutException e){
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "서버에 연결할 수 없습니다. 잠시 후에 다시 시도하거나 포트번호를 확인해주세요", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                    finish();
                } catch (UnsupportedEncodingException e) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "서버와의 연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                    finish();
                } catch (IOException e) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "서버와의 연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });

                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };
        worker.start();



        Thread gamePlay = new Thread() {
            public void run() {
                while(true) {
                    if(System.currentTimeMillis()-mytime > 1000){
                        quiz_time+=(int)Math.round((System.currentTimeMillis()-mytime)/1000);
                        mytime=System.currentTimeMillis();

                        runOnUiThread(new Runnable() {//quiz_time변수에 변동이 있을 경우만 프로그레스 바를 set 해준다. 이 조건문 안에서 실행하면 더 효율적이다.
                            @Override
                            public void run() {
                                timer.setProgress(quiz_time);
                            }
                        });
                    }

                    //아래 if문 순서도 동작에 영향을 미치니 바꾸지 말것.
                    if (is_nickname_arrived) do_this_when_nicknames_arrived();
                    if (is_coin_arrived) do_this_when_coin_arrived();
                    if (is_quiz_arrived) do_this_when_quiz_arrived();
                    if (is_my_shield_arrived) do_this_when_my_shield_arrived();


                }
            }
            };
        gamePlay.start();


        submit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                String ans = String.valueOf(answer.getText());
                boolean checkDigit = true;

                if(ans.charAt(0) == '-'){
                    for(int i=1; i<ans.length();i++){
                        if(!Character.isDigit(ans.charAt(i))){
                            checkDigit = false;
                            break;
                        }
                    }
                }else{
                    for(int i=0; i<ans.length();i++){
                        if(!Character.isDigit(ans.charAt(i))){
                            checkDigit = false;
                            break;
                        }
                    }
                }

                if(checkDigit){

                    if(Integer.parseInt(ans) == quiz[q_size-1]){
                        Toast.makeText(getApplicationContext(),"정답입니다", Toast.LENGTH_LONG).show();
                        Thread correct = new Thread() {
                            public void run() {
                                answer_to_server(true);
                              }
                            };
                        correct.start();
                        answer.setEnabled(false);
                        answer.setHint("");
                    }else{
                        Toast.makeText(getApplicationContext(),"틀렸습니다", Toast.LENGTH_LONG).show();
                        Thread fail = new Thread() {
                            public void run() {
                                answer_to_server(false);
                            }
                        };
                        fail.start();
                    }
                }else{
                    Toast.makeText(getApplicationContext(),"숫자만 입력해주세요", Toast.LENGTH_LONG).show();
                }
                answer.setText("");

            }
        });


    }
}
