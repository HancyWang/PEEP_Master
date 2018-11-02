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

    private static ArrayList<Float> pressure_data= new ArrayList<>();
    private static ArrayList<Float> flow_data= new ArrayList<>();
    //图标数据存储
    private static ArrayList<Entry> m_values=new ArrayList<>();
    private static ArrayList<Entry> m_pressure_values=new ArrayList<>();
    private static ArrayList<Entry> m_flow_values=new ArrayList<>();
    //Y轴上下限
    private static float PRESSURE_Y_LOW_LIMIT=-20000f;      // -2KPa
    private static float PRESSURE_Y_UP_LIMIT=20000f;        //2KPa

    private static float FLOW_Y_LOW_LIMIT=-13000f;     //流量 -130L/min
    private static float FLOW_Y_UP_LIMIT=14000f;       //流量 140L/min

    //限制线数值,标签
    private static float PRESSURE_INHALE_LIMIT_LINE_VALUE=-10000f;      //-1KPa
    private static String PRESSURE_INHALE_LIMIT_LINE_LABLE="吸气：-1Kpa";
    private static float PRESSURE_EXHALE_LIMIT_LINE_VALUE=10000f;      //1KPa
    private static String PRESSURE_EXHALE_LIMIT_LINE_LABLE="呼气：1Kpa";

    private static float FLOW_INHALE_LIMIT_LINE_VALUE=-13000f;       // 目标50L/min
    private static String FLOW_INHALE_LIMIT_LINT_LABLE="吸气：-130L/min";
    private static float FLOW_EXHALE_LIMIT_LINE_VALUE=13000f;       // 目标50L/min
    private static String FLOW_EXHALE_LIMIT_LINT_LABLE="呼气：130L/min";

    //DataSet Lable
    private static String PRESSURE_DATASET_LABLE="压力(单位：KPa)";     //honeywell
    private static String FLOW_DATASET_LABLE="流量(单位：L/min)";   //MS5525DSO

    private static int DATA_NUMS=4;

    private enum TYPE{
        TYPE_PRESSURE,
        TYPE_FLOW
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
                   if(data.length==1+DATA_NUMS*2*2+2)
                    {
                        for(int i=0;i<DATA_NUMS;i++){
                         short tmp_data1 = (short) (((data[1 + 2 * i] & 0xFF) << 8) | (data[1 + 2 * i + 1] & 0xFF));
                        short tmp_data2 = (short) (((data[1+DATA_NUMS*2 + 2 * i] & 0xFF) << 8) | (data[1+DATA_NUMS*2 + 2 * i + 1] & 0xFF));
                        //值如果超过最高值，就将HONEYWELL_Y_UP_LIMIT赋给tmp_data1
                        if(tmp_data1>=(short)PRESSURE_Y_UP_LIMIT){
                            tmp_data1= (short) PRESSURE_Y_UP_LIMIT;
                        }
                        else if(tmp_data1<(short)PRESSURE_Y_LOW_LIMIT){
                            tmp_data1=(short)PRESSURE_Y_LOW_LIMIT;
                        }

                        if(tmp_data2>=(short)FLOW_Y_UP_LIMIT){
                            tmp_data2=(short)FLOW_Y_UP_LIMIT;
                        }else if(tmp_data2<(short)FLOW_Y_LOW_LIMIT){
                            tmp_data2=(short)FLOW_Y_LOW_LIMIT;
                        }

//                            pressure_data.add(Float.intBitsToFloat(tmp_data1)/1000);
                        pressure_data.add((float) tmp_data1);
                        flow_data.add((float) tmp_data2);
                        }

                        if(pressure_data.size()==1500)
                        {
                            for(int i=0;i<DATA_NUMS;i++){
                                pressure_data.remove(i);
                                flow_data.remove(i);
                            }
                        }
                        showLineChart(TYPE.TYPE_PRESSURE,pressure_data);
                        showLineChart(TYPE.TYPE_FLOW,flow_data);
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

    private static class Pressure_YValueFormatter implements IAxisValueFormatter{
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
//            return String.valueOf(new DecimalFormat().format(value/1000))+"KPa";
            return String.valueOf(new DecimalFormat().format(value/10000));
        }
    }

    private static class Flow_YValueFormatter implements IAxisValueFormatter{
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
//            return String.valueOf(new DecimalFormat().format(value/100))+"L/min";
            return String.valueOf(new DecimalFormat().format(value/100));
        }
    }

    private static Pressure_YValueFormatter pressure_yValueFormatter=new Pressure_YValueFormatter();
    private static Flow_YValueFormatter flow_yValueFormatter=new Flow_YValueFormatter();

    public static void showLineChart(TYPE type,ArrayList<Float> datas){
        if(datas==null||datas.size()==0){
            return;
        }
        if(type==TYPE.TYPE_PRESSURE){
            m_Comm_LineChart=m_LineChart_pressure;
        }else if (type==TYPE.TYPE_FLOW){
            m_Comm_LineChart=m_LineChart_flow;
        }
        m_Comm_LineChart.getDescription().setEnabled(false);
        m_Comm_LineChart.setTouchEnabled(true);
        m_Comm_LineChart.setDragEnabled(true);
        m_Comm_LineChart.setScaleEnabled(true);
        m_Comm_LineChart.setPinchZoom(true);
//        m_Comm_LineChart.setBorderWidth(10f);

        m_Comm_LineChart.clear();

        XAxis xAxis= m_Comm_LineChart.getXAxis();
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(1500f);
        xAxis.setDrawGridLines(true);
//        xAxis.enableGridDashedLine(1f,0,1f);
        xAxis.setEnabled(false);

        YAxis leftAxis = m_Comm_LineChart.getAxisLeft();
        leftAxis.setTextColor(ColorTemplate.getHoloBlue());
        if(type==TYPE.TYPE_PRESSURE){
            leftAxis.setAxisMaximum(PRESSURE_Y_UP_LIMIT);
            leftAxis.setAxisMinimum(PRESSURE_Y_LOW_LIMIT);
            leftAxis.setValueFormatter(pressure_yValueFormatter);

//            leftAxis.setAxisMaximum(15f);
//            leftAxis.setAxisMinimum(0f);

        }else if (type==TYPE.TYPE_FLOW){
            leftAxis.setAxisMaximum(FLOW_Y_UP_LIMIT);
            leftAxis.setAxisMinimum(FLOW_Y_LOW_LIMIT);
            leftAxis.setValueFormatter(flow_yValueFormatter);
        }


//        YAxis rightAxis = m_LineChart_flow.getAxisRight();
//        rightAxis.setAxisMaximum(58f);
//        rightAxis.setAxisMinimum(48f);
        m_Comm_LineChart.getAxisRight().setEnabled(false); //去掉右边的y轴

        leftAxis.removeAllLimitLines();  // reset all limit lines to avoid overlapping lines
        //设置上限
        LimitLine lll_up=null;
        if(type==TYPE.TYPE_PRESSURE){
            lll_up = new LimitLine(PRESSURE_EXHALE_LIMIT_LINE_VALUE, PRESSURE_EXHALE_LIMIT_LINE_LABLE);
//            lll.setLineColor(Color.BLACK);
        }else if (type==TYPE.TYPE_FLOW){
            lll_up = new LimitLine(FLOW_EXHALE_LIMIT_LINE_VALUE, FLOW_EXHALE_LIMIT_LINT_LABLE);
        }
        lll_up.setLineWidth(1.5f);
        lll_up.enableDashedLine(5f, 5f, 0f);
        lll_up.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        lll_up.setTextSize(10f);
//        leftAxis.removeAllLimitLines();  // reset all limit lines to avoid overlapping lines
        leftAxis.addLimitLine(lll_up);
        leftAxis.addLimitLine(lll_up);

        //设置下限
        LimitLine lll_down=null;
        if(type==TYPE.TYPE_PRESSURE){
            lll_down = new LimitLine(PRESSURE_INHALE_LIMIT_LINE_VALUE, PRESSURE_INHALE_LIMIT_LINE_LABLE);
//            lll.setLineColor(Color.BLACK);
        }else if (type==TYPE.TYPE_FLOW){
            lll_down = new LimitLine(FLOW_INHALE_LIMIT_LINE_VALUE, FLOW_INHALE_LIMIT_LINT_LABLE);
        }
        lll_down.setLineWidth(1.5f);
        lll_down.enableDashedLine(5f, 5f, 0f);
        lll_down.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        lll_down.setTextSize(10f);
//        leftAxis.removeAllLimitLines();  // reset all limit lines to avoid overlapping lines
        leftAxis.addLimitLine(lll_down);
        leftAxis.addLimitLine(lll_down);
//        ArrayList<Entry> values=new ArrayList<>();
//        for(int i=0;i<datas.size();i++){
//            values.add(new Entry(i,datas.get(i)));
//        }
        if(type==TYPE.TYPE_PRESSURE){
            m_values=m_pressure_values;
        }else if (type==TYPE.TYPE_FLOW){
            m_values=m_flow_values;
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
            if(type==TYPE.TYPE_PRESSURE){
                label=PRESSURE_DATASET_LABLE;
            }else if (type==TYPE.TYPE_FLOW){
                label=FLOW_DATASET_LABLE;
            }

            set1=new LineDataSet(m_values,label);
//            set1.setDrawCircleHole(false);
            set1.setLineWidth(2f);
            set1.setCubicIntensity(1f);
            set1.setDrawCircles(false);
//            set1.setCircleRadius(5f);

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
