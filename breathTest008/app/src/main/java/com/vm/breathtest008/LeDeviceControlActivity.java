package com.vm.breathtest008;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.LongDef;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class LeDeviceControlActivity extends Activity{
    private final String UUID="00001002-0000-1000-8000-00805f9b34fb";
    public final static String DEVICE_NAME="BLE#";
    private String TAG="hancy";
    private LeService m_LeService;
    private String m_deviceName;
    private String m_deviceAddress;
    private boolean m_connected;
    private TextView m_connectState;
    private static TextView m_recvCnt;
    private static int m_nCnt;
    private BluetoothGattCharacteristic m_BluetoothGattCharacteristic;

    private static LineChart m_LineChart_flow;
    private static LineChart m_LineChart_pressure;

    @SuppressLint("HandlerLeak")
    public static Handler m_Handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case LeService.MSG_DATA_COMMING:
                    byte[] data= (byte[]) msg.obj;
                    //显示数据到图表上
                    m_nCnt+=data.length;
                    m_recvCnt.setText(String.valueOf(m_nCnt));
                    showLineChart(data);
                    break;
                default:
                        break;
            }
        }
    };

//    @SuppressLint("HandlerLeak")
//    private Handler m_Handler=new Handler(){
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            if(msg.what==0x01){
////                setData(1000,200);
//                showLineChart();
////                Log.d("hancy",++i+"");
//            }
//        }
//    };
//    private Thread m_Thread_drawing_linechart=new Thread(new Runnable() {
//        @Override
//        public void run() {
//            while (true){
//                try {
//                    Thread.sleep(350);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                Message message=obtainMessageage();
//                message.what=0x01;
//                m_Handler.sendMessage(message);
//            }
//        }
//    });

    public ServiceConnection m_ServiceConnection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            m_LeService= ((LeService.LocalBinder)service).getService(); //获取service
            //初始化
            if(!m_LeService.init()){
                Log.d(TAG,"m_LeService.init()初始化失败");
                return;
            }
            //连接设备
            if(!m_LeService.connect(m_deviceAddress)){
                Log.d(TAG,"m_LeService.connect()连接失败");
                return;
            }else {
                Log.d(TAG,"m_LeService.connect()连接成功");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG,"调用onServiceDisconnected");
        }
    };

    private BroadcastReceiver m_BroadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if(action.equals(LeService.ACTION_GATT_CONNECTED)){
                m_connectState.setText("已连接");
                m_connected=true;
                invalidateOptionsMenu();
            }else if(action.equals(LeService.ACTION_GATT_DISCONNECTED)){
                m_connectState.setText("未连接");
                m_connected=false;
                invalidateOptionsMenu();
            }else if(action.equals(LeService.ACTION_GATT_SERVICE_DISCOVERED)){
                m_BluetoothGattCharacteristic=getCharacteristicFrom(UUID);
                if(m_BluetoothGattCharacteristic==null){
                    Log.d(TAG,"获取m_BluetoothGattCharacteristic失败");
                }else {
                    Log.d(TAG,"获取m_BluetoothGattCharacteristic成功");
                }
                if(m_LeService.setNotification(m_BluetoothGattCharacteristic,true)){
                    Log.d(TAG,"m_LeService.setNofitication设置成功");
                }
            }else if(action.equals(LeService.ACTION_GATT_DATA_AVAILABLE)){
//                String data=intent.getStringExtra(LeService.ACTION_GATT_EXTRA_DATA);
                byte[] data=intent.getByteArrayExtra(LeService.ACTION_GATT_EXTRA_DATA);
                //debug
//                StringBuilder builder=new StringBuilder();
//                if(data!=null&&data.length>0){
//                    for(byte aByte:data){
//                        builder.append(String.format("%02X",aByte));
//                    }
//                    Log.d(TAG,"收到数据:"+builder.toString());
//                }
//                Log.d(TAG,"数据来了");

//                m_nCnt=LeService.m_nCnt;

                //显示数据到图表上
               m_nCnt+=data.length;
                m_recvCnt.setText(String.valueOf(m_nCnt));
                showLineChart(data);
            }
        }
    };

    public static void showLineChart(byte[] bytes){
        m_LineChart_flow.getDescription().setEnabled(false);
        m_LineChart_flow.setTouchEnabled(true);
        m_LineChart_flow.setDragEnabled(true);
        m_LineChart_flow.setScaleEnabled(true);
        m_LineChart_flow.setPinchZoom(true);

        m_LineChart_flow.clear();

        YAxis leftAxis = m_LineChart_flow.getAxisLeft();
        leftAxis.setTextColor(ColorTemplate.getHoloBlue());
        leftAxis.setAxisMaximum(58f);
        leftAxis.setAxisMinimum(48f);

//        YAxis rightAxis = m_LineChart_flow.getAxisRight();
//        rightAxis.setAxisMaximum(58f);
//        rightAxis.setAxisMinimum(48f);
        m_LineChart_flow.getAxisRight().setEnabled(false); //去掉右边的y轴

        LimitLine ll1 = new LimitLine(50f, "50");
        ll1.setLineWidth(4f);
        ll1.enableDashedLine(5f, 5f, 0f);
        ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll1.setTextSize(10f);
        leftAxis.removeAllLimitLines();  // reset all limit lines to avoid overlapping lines
        leftAxis.addLimitLine(ll1);

        ArrayList<Entry> values=new ArrayList<>();
        for(int i=0;i<bytes.length;i++){
            values.add(new Entry(i,bytes[i]));
        }

        LineDataSet set1;

        if(m_LineChart_flow.getData()!=null&&m_LineChart_flow.getData().getDataSetCount()>0){
            set1=(LineDataSet) m_LineChart_flow.getData().getDataSetByIndex(0);
            set1.setValues(values);
            m_LineChart_flow.getData().notifyDataChanged();
            m_LineChart_flow.notifyDataSetChanged();
        }else {
            set1=new LineDataSet(values,"DataSet1");

            ArrayList<ILineDataSet> dataSets=new ArrayList<>();
            dataSets.add(set1);

            LineData data=new LineData(dataSets);
            m_LineChart_flow.setData(data);
        }
    }

    public BluetoothGattCharacteristic getCharacteristicFrom(String uuid){
        if(uuid==null||uuid.equals("")){
            Log.d(TAG,"传入的uuid不能为null不能为“”");
            return null;
        }
        List<BluetoothGattService> services=m_LeService.getSupportServices();
        if(services==null){
            Log.d(TAG,"dealwithUUID中获取服务为空");
            return null;
        }
        BluetoothGattCharacteristic tmp_characteristic=null;
        for(BluetoothGattService service:services){
            List<BluetoothGattCharacteristic> characteristics=service.getCharacteristics();
            for(BluetoothGattCharacteristic characteristic:characteristics){
                if(characteristic.getUuid().toString().equals(uuid)){
                    tmp_characteristic=characteristic;
                }
            }
        }
        return tmp_characteristic;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.breath_monitor);

        m_connectState=findViewById(R.id.tv_device_connect_state);
        m_recvCnt=findViewById(R.id.tv_recv_cnt);
        m_LineChart_flow=findViewById(R.id.lineChart_flow);
        m_LineChart_pressure=findViewById(R.id.lineChart_pressure);

        //获取MainActivity页面传来的设备名字和设备信息
        Intent intent=getIntent();
        String name,address;
        name=intent.getStringExtra("NAME");
        address=intent.getStringExtra("ADDRESS");
        if(name.contains(DEVICE_NAME)){
            m_deviceName=name;
            m_deviceAddress=address;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Objects.requireNonNull(getActionBar()).setTitle(m_deviceName);
                Objects.requireNonNull(getActionBar()).setDisplayHomeAsUpEnabled(true);
            }
        }
//        Log.d(TAG,m_deviceName+" "+m_deviceAddress);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.conncet_disconnect,menu);
        if(m_connected){
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        }else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_connect:
                m_connected=true;
//                m_LeService.init();
                m_LeService.connect(m_deviceAddress);
                break;
            case R.id.menu_disconnect:
                m_connected=false;
                m_recvCnt.setText("");
                m_nCnt=0;
//                LeService.m_nCnt=0;
                if(!m_LeService.disconnect()){
                    Log.d(TAG,"case R.id.menu_disconnect:中m_LeService.disconnect()无法断开连接");
                    return false;
                }
                break;
            case android.R.id.home:
                if(!m_LeService.disconnect()){
                    Log.d(TAG,"case android.R.id.home:中m_LeService.disconnect()无法断开连接");
                    return false;
                }
                onBackPressed();
        }
        invalidateOptionsMenu();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent service=new Intent(this,LeService.class);
//        service.putExtra("THIS", (Parcelable) this);
        bindService(service,m_ServiceConnection,BIND_AUTO_CREATE);

        registerReceiver(m_BroadcastReceiver,makeInterFilter());
//        //连接设备
//        m_LeService=new LeService();
//        m_LeService.connect(m_deviceAddress);
    }

    @Override
    protected void onPause() {
        super.onPause();
        m_nCnt=0;
        unregisterReceiver(m_BroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(m_ServiceConnection);
//        m_ServiceConnection=null;
    }

    private IntentFilter makeInterFilter(){
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(LeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(LeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(LeService.ACTION_GATT_SERVICE_DISCOVERED);
        intentFilter.addAction(LeService.ACTION_GATT_DATA_AVAILABLE);
        return intentFilter;
    }
}
