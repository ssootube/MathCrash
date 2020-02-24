package com.example.mathcrash;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
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

public class MainActivity extends AppCompatActivity {
    int port;
    String nickname;
    private Socket socket;
    String serverIP = "192.168.123.101";
    private BufferedInputStream socket_in;
    private BufferedOutputStream socket_out;
    byte[] buf;


    //아래 변수들은 실시간으로 계속 자동으로 서버로 부터 가져오게 구현했으므로 마음대로 사용하시오
    int[] quiz;//현재의 수열추리 문제가 담겨 있는 배열
    int q_size = 0;//수열추리 문제의 길이. quiz[q_size-1]에 담긴 값이 정답이고, quiz[0]~quiz[q_size-2]에는 문제가 담겨 있다.
    int quiz_time = 0;//가장 최근의 수열추리 문제 출제 직후 흐른시간. 초단위. 남은시간이 아니라 흐른 시간이다.

    int[] coin;//현재 접속중인 유저들의 코인 현황. 예를 들어 coin[2]에는 2번 유저의 코인 값이 저장되어 있다. 단, 코인이 0이 저장되어 경우 중도이탈, 접속 끊킨 유저이다.
    //coin배열의 값을 수정해도 서버에서 처리되는 것이기에 코인을 증가시킬 수 없다. 물론 잠깐은 증가된 것처럼 보일 수 있겠지만, 서버에서 데이터를 받아와서 업데이트 시키면 무효.
    int coin_size = 0;//coin 배열의 길이
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
    ///////////////////////

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
            socket_out.write(data.getBytes("UTF-8"));
            socket_out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nickname = getIntent().getExtras().getString("nickname");//LoginActivity에서 받아온 닉네임 정보를 저장한다.
        port = Integer.valueOf(getIntent().getExtras().getString("port"));//LoginActivity에서 받아온 포트번호 정보를 저장한다.

        Thread worker = new Thread() {
            synchronized public void run() {
                try{
                    socket  = new Socket(); // 소켓 생성
                    SocketAddress serverAddress = new InetSocketAddress(serverIP, port);//주소 등록
                    socket.connect(serverAddress, 3000); // 연결시도
                    socket_out = new BufferedOutputStream(socket.getOutputStream());
                    socket_in = new BufferedInputStream(socket.getInputStream());
                    buf= new byte[50];
                    socket_in.read(buf,0,4);
                    int[] temp_buf = getIntArrayFromByteArray(buf,1);
                    user_number = temp_buf[0];

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
                                break;
                            case 1:
                                //서버로 부터 coin정보 데이터가 도착한 경우
                                socket_in.read(buf, 0, 4);
                                temp = getIntArrayFromByteArray(buf, 1);
                                coin_size = temp[0];
                                socket_in.read(buf, 0, 4 * coin_size);
                                coin = getIntArrayFromByteArray(buf, q_size);
                                break;
                            case 2:
                                break;
                        }
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


    }
}
