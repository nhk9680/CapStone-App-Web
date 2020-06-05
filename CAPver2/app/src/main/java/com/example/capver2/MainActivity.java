package com.example.capver2;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.icu.text.AlphabeticIndex;
import android.os.Bundle;



import android.bluetooth.BluetoothAdapter;

import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothSocket;

import android.content.DialogInterface;

import android.content.Intent;


import android.os.Handler;

import android.os.SystemClock;

//import android.support.v7.app.AlertDialog;

//import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import android.webkit.WebView;
import android.widget.Button;

import android.widget.EditText;

import android.widget.TextView;

import android.widget.Toast;


import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;

import java.io.InputStream;

import java.io.OutputStream;

import java.io.UnsupportedEncodingException;

import java.net.URISyntaxException;
import java.util.ArrayList;

import java.util.Collection;
import java.util.List;

import java.util.Set;

import java.util.UUID;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import io.socket.client.IO;
import io.socket.client.Socket;

public class MainActivity extends AppCompatActivity {
    private LineChart chart;
    private ArrayList<String> xVals;
    private ArrayList<ILineDataSet> iLineDataSets;
    private  LineData lineData;

    private int X_RANGE=500;
    private int DATA_RANGE=100;

    private BluetoothSPP bt;

    private String str;
    private String v1, v2, v3;
    private Socket mSocket;

    //블루투스가 켜졌는지 아닌지
    TextView mTvBluetoothStatus;
    //ReceiveData=받는데이터
    TextView mTvReceiveData;
    //SendDat=보낼데이터
    TextView mTvSendData;
    TextView extra;

    Button mBtnBluetoothOn;
    Button mBtnBluetoothOff;
    //연결하기
    Button mBtnConnect;
    //전송버튼
    Button mBtnSendData;


    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevices;
    List<String> mListPairedDevices;


    Handler mBluetoothHandler;
    ConnectedBluetoothThread mThreadConnectedBluetooth;
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;



    final static int BT_REQUEST_ENABLE = 1;

    final static int BT_MESSAGE_READ = 2;

    final static int BT_CONNECTING_STATUS = 3;

    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    ArrayList<Entry> xVal1;
    ArrayList<Entry> xVal2;
    ArrayList<Entry> xVal3;
    LineDataSet setXcomp1;
    LineDataSet setXcomp2;
    LineDataSet setXcomp3;

    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //////////////////////////////////////
        str = "http://74c232c13e2a.ngrok.io ";
        try{
            mSocket = IO.socket(str);
            mSocket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        //////////////////////////////////////

        bt = new BluetoothSPP(this);

        mTvBluetoothStatus = (TextView) findViewById(R.id.tvBluetoothStatus);
        mTvReceiveData = (TextView) findViewById(R.id.tvReceiveData);
        mTvSendData = (EditText) findViewById(R.id.tvSendData);
        mBtnBluetoothOn = (Button) findViewById(R.id.btnBluetoothOn);
        mBtnBluetoothOff = (Button) findViewById(R.id.btnBluetoothOff);
        mBtnConnect = (Button) findViewById(R.id.btnConnect);
        mBtnSendData = (Button) findViewById(R.id.btnSendData);


        //해당 장치가 블루투스 기능을 지원하는지 알아보는 method
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBtnBluetoothOn.setOnClickListener(new Button.OnClickListener() {
            @Override

            public void onClick(View view) {
                bluetoothOn();
            }
        });
        mBtnBluetoothOff.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothOff();
            }
        });

        mBtnConnect.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                listPairedDevices();
            }
        });

        mBtnSendData.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mThreadConnectedBluetooth != null) {
                    mThreadConnectedBluetooth.write(mTvSendData.getText().toString());
                    mTvSendData.setText("");
                }
            }
        });


        chart = (LineChart) findViewById(R.id.chart);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartinit();


     /*   chart.setAutoScaleMinMaxEnabled(true);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setDrawAxisLine(false);
        chart.getXAxis().setEnabled(true);
        chart.setDrawMarkers(false);


        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMaximum(50f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(false);

        YAxis rightAxis = chart.getAxisRight();
        // rightAxis.setEnabled(true);
        rightAxis.setAxisMaximum(50f);
        rightAxis.setAxisMinimum(0f);
        rightAxis.setDrawGridLines(false);*/


        //BluetoothHandler로 블루투스 연결
        mBluetoothHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                //수신된 데이터를 읽어옴
                if (msg.what == BT_MESSAGE_READ) {
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    //수신된 데이터를 ReceiveData에 표시해줌
                    mTvReceiveData.setText(readMessage); //readMessage로 받아와서
                    String test = readMessage.substring(0, 16); //String형태로 변환하고 substring으로 잘라줌

                    String[] arr = test.split(":");


                    float k = Float.parseFloat(arr[0]);
                    float k2 = Float.parseFloat(arr[1]);
                    float k3=Float.parseFloat(arr[2]);
                    //System.out.println(k);
                    //addEntry(k);
                    //addEntry2(k2);
                    chartUpdate(k,k2,k3);

                    /////////////////////////////////////////
                    // send data to server

                    response(k,k2,k3);

                    /////////////////////////////////////////
                }
            }
        };

    }
    private  void chartinit(){
        chart.setAutoScaleMinMaxEnabled(true);
        iLineDataSets=new ArrayList<ILineDataSet>();

        xVal1=new ArrayList<Entry>();
        xVal2=new ArrayList<Entry>();
        xVal3=new ArrayList<Entry>();

        setXcomp1=initLineDataSet(xVal1,"Gyro",Color.RED);
        setXcomp2=initLineDataSet(xVal2,"Camera",Color.BLUE);
        setXcomp3=initLineDataSet(xVal3,"Fusion",Color.BLACK);

        xVals=new ArrayList<String>();
        for(int i=0;i<X_RANGE;i++){
            xVals.add("");
        }
        //lineData=new LineData(xVals,iLineDataSets);
        lineData=new LineData(iLineDataSets);
        chart.setData(lineData);
        chart.invalidate();
    }

    private LineDataSet initLineDataSet(ArrayList<Entry> xVal, String label, int color){
        LineDataSet lineDataSet = new LineDataSet(xVal, label);
        lineDataSet.setColor(color);
        lineDataSet.setDrawValues(false);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);

        iLineDataSets.add(lineDataSet);
        return lineDataSet;
    }


    public static int index = 0;
    public void chartUpdate(float x,float x2,float x3){
        if (xVal1.size() > DATA_RANGE){ // 출력범위 초과하면
            xVal1.remove(0);    //앞에꺼 하나 지우고
            xVal2.remove(0);
            xVal3.remove(0);
            for (int i=0; i<DATA_RANGE; i++){   // 한칸씩 앞으로 당기는?
//               xVal1.get(i).setXIndex(i); // 원본 코드인데 setXIndex 메소드가 없음
                xVal1.get(i).setX(i);
                xVal2.get(i).setX(i);
                xVal3.get(i).setX(i);
            }
        }
        xVal1.add(new Entry(index, x)); //엔트리 추가
        xVal2.add(new Entry(index, x2)); //이런식으로
        xVal3.add(new Entry(index,x3));


        setXcomp1.notifyDataSetChanged();
        setXcomp2.notifyDataSetChanged();
        setXcomp3.notifyDataSetChanged();

        lineData.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();

        if(index < DATA_RANGE) index++;
    }


    private void addEntry(double num ){
        LineData data= chart.getData();


        if(data==null){
            data=new LineData();
            chart.setData(data);
        }

        ILineDataSet set=data.getDataSetByIndex(0);


        if(set==null){
            set=createSet();
            data.addDataSet(set);
        }


        data.addEntry(new Entry((float)set.getEntryCount(),(float)num),0);



        data.notifyDataChanged();

        //notify chart data have changed
        chart.notifyDataSetChanged();

        //limit number of visible entries
        chart.setVisibleXRangeMaximum(150);

        //scroll to the last entry
        chart.moveViewToX(data.getEntryCount());
        //chart.moveViewToX(data2.getEntryCount());


    }


    private LineDataSet createSet(){

        LineDataSet set=new LineDataSet(null,"Real time Line data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(ColorTemplate.getHoloBlue());
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244,117,117));
        set.setValueTextColor(Color.WHITE);


        return set;
    }



    void bluetoothOn() {
        //블루투스를 지원하는 기기인지 아닌지 판별
        if(mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show();

        }

        else {
            //블루투스가 활성화 되어있는지
            if (mBluetoothAdapter.isEnabled()) {

                Toast.makeText(getApplicationContext(), "블루투스가 이미 활성화 되어 있습니다.", Toast.LENGTH_LONG).show();

                mTvBluetoothStatus.setText("활성화");

            }

            else {

                Toast.makeText(getApplicationContext(), "블루투스가 활성화 되어 있지 않습니다.", Toast.LENGTH_LONG).show();
                //비활성화 되어 있다면 Intent를 이용하여 활성화 창 띄움
                Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //OnActivityResult에서 결과 처리
                startActivityForResult(intentBluetoothEnable, BT_REQUEST_ENABLE);
            }
        }
    }
    //블루투스 비활성화 method
    void bluetoothOff() {

        if (mBluetoothAdapter.isEnabled()) {
            //disable이용하여 비활성화
            mBluetoothAdapter.disable();
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되었습니다.", Toast.LENGTH_SHORT).show();
            mTvBluetoothStatus.setText("비활성화");
        }

        else {
            Toast.makeText(getApplicationContext(), "블루투스가 이미 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case BT_REQUEST_ENABLE:
                if (resultCode == RESULT_OK) { // 블루투스 활성화를 확인을 클릭하였다면

                    Toast.makeText(getApplicationContext(), "블루투스 활성화", Toast.LENGTH_LONG).show();

                    mTvBluetoothStatus.setText("활성화");

                } else if (resultCode == RESULT_CANCELED) { // 블루투스 활성화를 취소를 클릭하였다면

                    Toast.makeText(getApplicationContext(), "취소", Toast.LENGTH_LONG).show();

                    mTvBluetoothStatus.setText("비활성화");

                }
                break;

        }
        super.onActivityResult(requestCode, resultCode, data);

    }
    //블루투스 페어랑 징치 목록 가져오는 method
    void listPairedDevices() {
        //블루투스가 활성화 상태인지 확인
        if (mBluetoothAdapter.isEnabled()) {
            mPairedDevices = mBluetoothAdapter.getBondedDevices();

            //페어링된장치가 있으면
            if (mPairedDevices.size() > 0) {
                //새로운 알람창 객체 생성
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle("장치 선택");


                //페어링된 장치 리스트
                mListPairedDevices = new ArrayList<String>();

                for (BluetoothDevice device : mPairedDevices) {
                    mListPairedDevices.add(device.getName());

                }
                //페어링된 장치 수를 얻어옴
                final    CharSequence[] items = mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);
                //장치명을 매개변수로 사용하여
                //connectSelectedDevice method로 전달
                mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        connectSelectedDevice(items[item].toString());
                    }
                });

                AlertDialog alert = builder.create();

                alert.show();

            } else {

                Toast.makeText(getApplicationContext(), "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show();

            }

        }

        else {

            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show();

        }

    }
    //실제 블루투스 장치와 연결하는 부분
    //listPairedDevices 메서드를 통해 전달받은 매개변수값은 장치 이름임 (selectedDeviceName)
    //그러나 연결에 필요한 값은 장치의 주소
    void connectSelectedDevice(String selectedDeviceName) {
        //for문을 이용해서 페어링된 모든 장치를 검색하며 매개변수와 같은 이름의 장치 주소값 얻음
        for(BluetoothDevice tempDevice : mPairedDevices) {
            if (selectedDeviceName.equals(tempDevice.getName())) {
                mBluetoothDevice = tempDevice;
                break;
            }
        }
        try {
            //mBluetoothDevice를 통해 createRfcommSocketToServiceRecord(UUID) 호출
            //mBluetoothSocker을 가져옴( UUID값 ->시리얼 통신용)
            //mBluetoothDevice에 연결될 mBluetoothSocket이 초기화-> connect로 호출하여 연결 시작
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            mBluetoothSocket.connect();

            //데이터는 언제 수신받을지 모르니 데이터 수신을위한 스레드를 따로 만들어 처리
            mThreadConnectedBluetooth = new ConnectedBluetoothThread(mBluetoothSocket);
            mThreadConnectedBluetooth.start();
            mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();

        } catch (IOException e) {

            Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();

        }

    }



    private class ConnectedBluetoothThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;


        //스레드 초기화
        public ConnectedBluetoothThread(BluetoothSocket socket) {

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //데이터 전송, 수신을 위한 길을 만들어줌
            try {

                tmpIn = socket.getInputStream();

                tmpOut = socket.getOutputStream();

            } catch (IOException e) {

                Toast.makeText(getApplicationContext(), "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();

            }


            mmInStream = tmpIn;
            mmOutStream = tmpOut;

        }
        //수신받은 데이터는 언제 들어올지 모르니 항상 확인해야함
        public void run() {

            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {

                try {

                    bytes = mmInStream.available();

                    if (bytes != 0) {
                        SystemClock.sleep(100);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes);
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget();

                    }

                } catch (IOException e) {

                    break;

                }

            }

        }
        //데이터 전송을 위한 ConnectedBluetoothThread 의 메서드
        public void write(String str) {

            byte[] bytes = str.getBytes();

            try {

                mmOutStream.write(bytes);

            } catch (IOException e) {

                Toast.makeText(getApplicationContext(), "데이터 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();

            }

        }
        //블루투스 소켓을 닫음->어플을 닫으면 자동으로 닫아짐
        public void cacel() {

            try {

                mmSocket.close();

            } catch (IOException e) {

                Toast.makeText(getApplicationContext(), "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();

            }

        }

    }


    //////////////////////////////////////////////
    // socket clinet /////////////////////////////

    public void response(float k, float k2, float k3){
        v1 = String.valueOf(k);
        v2 = String.valueOf(k2);
        v3 = String.valueOf(k3);

        JSONObject data = new JSONObject();
        try{
            data.put("v1", v1);
            data.put("v2", v2);
            data.put("v3", v3);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSocket.emit("chat_message", data);
    }


}

