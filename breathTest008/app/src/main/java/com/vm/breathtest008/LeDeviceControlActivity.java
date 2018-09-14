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
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static LineChart m_Comm_LineChart;

    private static ArrayList<Float> honeywell_data= new ArrayList<>();
    private static ArrayList<Float> MS5525DSO_data= new ArrayList<>();
    //图标数据存储
    private static ArrayList<Entry> m_values=new ArrayList<>();
    private static ArrayList<Entry> m_honeywell_values=new ArrayList<>();
    private static ArrayList<Entry> m_MS5525DSO_values=new ArrayList<>();
    //Y轴上下限
    private static float HONEYWELL_Y_LOW_LIMIT=0f;      //honeywell
    private static float HONEYWELL_Y_UP_LIMIT=10000;  //0-10,000pa,对应1-1.5psi

    private static float MS5525DSO_Y_LOW_LIMIT=0f;     //MS5525DSO
    private static float MS5525DSO_Y_UP_LIMIT=12000f;

    //限制线数值,标签
    private static float HONEYWELL_LIMIT_LINE_VALUE=7000f;      //honeywell 7.5Kpa,对应1psi
    private static String HONEYWELL_LIMIT_LINE_LABLE="目标值：7Kpa";

    private static float MS5525DSO_LIMIT_LINE_VALUE=5000f;       //MS5525DSO
    private static String MS5525DSO_LIMIT_LINT_LABLE="目标值：50L/min";

    //DataSet Lable
    private static String HONEYWELL_DATASET_LABLE="压力(单位：KPa)";     //honeywell
    private static String MS5525DSO_DATASET_LABLE="流量(单位：L/min)";   //MS5525DSO

    private enum TYPE{
        TYPE_HONEYWELL,
        TYPE_MS5525DSO
    }

    private static char[] getChars (byte[] bytes) {
        Charset cs = Charset.forName ("ASCII");
        ByteBuffer bb = ByteBuffer.allocate (bytes.length);
        bb.put (bytes);
        bb.flip ();
        CharBuffer cb = cs.decode (bb);
        return cb.array();
    }

//    private static int fill_cnt=0;
    @SuppressLint("HandlerLeak")
    public static Handler m_Handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case LeService.MSG_DATA_COMMING:
                    byte[] data= (byte[]) msg.obj;
//                    char[] data= getChars((byte[]) msg.obj);
                    //显示数据到图表上
                    m_nCnt+=data.length;
                    m_recvCnt.setText(String.valueOf(m_nCnt));
//                    if(data[0]!=(byte)255){
//                        return;
//                    }

//                    //debug
//                    byte b= (byte) 0xff;
//                    char d= (char) b;
                   if(data.length==19)
                    {
                        for(int i=0;i<4;i++){
                        char tmp_data1 = (char) (((data[1 + 2 * i] & 0xFF) << 8) | (data[1 + 2 * i + 1] & 0xFF));
                        char tmp_data2 = (char) (((data[9 + 2 * i] & 0xFF) << 8) | (data[9 + 2 * i + 1] & 0xFF));
                        //值如果超过最高值，就将HONEYWELL_Y_UP_LIMIT赋给tmp_data1
                        if(tmp_data1>(char)HONEYWELL_Y_UP_LIMIT){
                            tmp_data1= (char) HONEYWELL_Y_UP_LIMIT;
                        }
//                            honeywell_data.add(Float.intBitsToFloat(tmp_data1)/1000);
                        honeywell_data.add((float) tmp_data1);
                        MS5525DSO_data.add((float) tmp_data2);
                        }

                        if(honeywell_data.size()==1500)
                        {
                            for(int i=0;i<4;i++){
                                honeywell_data.remove(i);
                                MS5525DSO_data.remove(i);
                            }
                        }
                        showLineChart(TYPE.TYPE_HONEYWELL,honeywell_data);
                        showLineChart(TYPE.TYPE_MS5525DSO,MS5525DSO_data);
                    }
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

//                //显示数据到图表上
//               m_nCnt+=data.length;
//                m_recvCnt.setText(String.valueOf(m_nCnt));
//                showLineChart(data);
            }
        }
    };

    private static class Honeywell_YValueFormatter implements IAxisValueFormatter{
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
//            return String.valueOf(new DecimalFormat().format(value/1000))+"KPa";
            return String.valueOf(new DecimalFormat().format(value/1000));
        }
    }

    private static class MS5525DSO_YValueFormatter implements IAxisValueFormatter{
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
//            return String.valueOf(new DecimalFormat().format(value/100))+"L/min";
            return String.valueOf(new DecimalFormat().format(value/100));
        }
    }

    private static Honeywell_YValueFormatter honeywell_yValueFormatter=new Honeywell_YValueFormatter();
    private static MS5525DSO_YValueFormatter ms5525DSO_yValueFormatter=new MS5525DSO_YValueFormatter();

    public static void showLineChart(TYPE type,ArrayList<Float> datas){
        if(datas==null||datas.size()==0){
            return;
        }
        if(type==TYPE.TYPE_HONEYWELL){
            m_Comm_LineChart=m_LineChart_pressure;
        }else if (type==TYPE.TYPE_MS5525DSO){
            m_Comm_LineChart=m_LineChart_flow;
        }
        m_Comm_LineChart.getDescription().setEnabled(false);
        m_Comm_LineChart.setTouchEnabled(true);
        m_Comm_LineChart.setDragEnabled(true);
        m_Comm_LineChart.setScaleEnabled(true);
        m_Comm_LineChart.setPinchZoom(true);

        m_Comm_LineChart.clear();

        XAxis xAxis= m_Comm_LineChart.getXAxis();
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(1500f);
        xAxis.setDrawGridLines(true);
//        xAxis.enableGridDashedLine(1f,0,1f);
        xAxis.setEnabled(false);

        YAxis leftAxis = m_Comm_LineChart.getAxisLeft();
        leftAxis.setTextColor(ColorTemplate.getHoloBlue());
        if(type==TYPE.TYPE_HONEYWELL){
            leftAxis.setAxisMaximum(HONEYWELL_Y_UP_LIMIT);
            leftAxis.setAxisMinimum(HONEYWELL_Y_LOW_LIMIT);
            leftAxis.setValueFormatter(honeywell_yValueFormatter);

//            leftAxis.setAxisMaximum(15f);
//            leftAxis.setAxisMinimum(0f);

        }else if (type==TYPE.TYPE_MS5525DSO){
            leftAxis.setAxisMaximum(MS5525DSO_Y_UP_LIMIT);
            leftAxis.setAxisMinimum(MS5525DSO_Y_LOW_LIMIT);
            leftAxis.setValueFormatter(ms5525DSO_yValueFormatter);
        }


//        YAxis rightAxis = m_LineChart_flow.getAxisRight();
//        rightAxis.setAxisMaximum(58f);
//        rightAxis.setAxisMinimum(48f);
        m_Comm_LineChart.getAxisRight().setEnabled(false); //去掉右边的y轴

        LimitLine lll=null;
        if(type==TYPE.TYPE_HONEYWELL){
            lll = new LimitLine(HONEYWELL_LIMIT_LINE_VALUE, HONEYWELL_LIMIT_LINE_LABLE);
//            lll.setLineColor(Color.BLACK);
        }else if (type==TYPE.TYPE_MS5525DSO){
            lll = new LimitLine(MS5525DSO_LIMIT_LINE_VALUE, MS5525DSO_LIMIT_LINT_LABLE);
        }
        lll.setLineWidth(1.5f);
        lll.enableDashedLine(5f, 5f, 0f);
        lll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        lll.setTextSize(10f);
        leftAxis.removeAllLimitLines();  // reset all limit lines to avoid overlapping lines
        leftAxis.addLimitLine(lll);
        leftAxis.addLimitLine(lll);

//        ArrayList<Entry> values=new ArrayList<>();
//        for(int i=0;i<datas.size();i++){
//            values.add(new Entry(i,datas.get(i)));
//        }
        if(type==TYPE.TYPE_HONEYWELL){
            m_values=m_honeywell_values;
        }else if (type==TYPE.TYPE_MS5525DSO){
            m_values=m_MS5525DSO_values;
        }
        m_values.clear();
        for(int i=0;i<datas.size();i++){
            m_values.add(new Entry(i,datas.get(i)));
        }

        LineDataSet set1;

        if(m_Comm_LineChart.getData()!=null&&m_Comm_LineChart.getData().getDataSetCount()>0){
            set1=(LineDataSet) m_Comm_LineChart.getData().getDataSetByIndex(0);
            set1.setValues(m_values);
            m_Comm_LineChart.getData().notifyDataChanged();
            m_Comm_LineChart.notifyDataSetChanged();
        }else {
            String label=null;
            if(type==TYPE.TYPE_HONEYWELL){
                label=HONEYWELL_DATASET_LABLE;
            }else if (type==TYPE.TYPE_MS5525DSO){
                label=MS5525DSO_DATASET_LABLE;
            }

            set1=new LineDataSet(m_values,label);
            set1.setCircleRadius(1f);

            ArrayList<ILineDataSet> dataSets=new ArrayList<>();
            dataSets.add(set1);

            LineData data=new LineData(dataSets);
            m_Comm_LineChart.setData(data);
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
            Objects.requireNonNull(getActionBar()).setTitle(m_deviceName);
            Objects.requireNonNull(getActionBar()).setDisplayHomeAsUpEnabled(true);
        }
//        Log.d(TAG,m_deviceName+" "+m_deviceAddress);
    }

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
