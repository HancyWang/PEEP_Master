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
import android.content.res.ColorStateList;
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

    //呼气相关参数
    private static TextView m_MEP;
    private static TextView m_FVC;
    private static TextView m_FEV1;
    private static TextView m_FEV1_percent;
    private static TextView m_PEF;

    //吸气相关参数
    private static TextView m_MIP;
    private static TextView m_PIVC;
    private static TextView m_PIV1;
    private static TextView m_PIV1_percent;
    private static TextView m_PIF;


    private BluetoothGattCharacteristic m_BluetoothGattCharacteristic;

    private static LineChart m_LineChart_flow;
    private static LineChart m_LineChart_pressure;
    private static LineChart m_Comm_LineChart;

    private static ArrayList<Float> pressure_data= new ArrayList<>();
    private static ArrayList<Float> flow_data= new ArrayList<>();

    private static ArrayList<Float> pressure_filter_data=new ArrayList<>();  //用来获取最大正压
    private static float EXHALE_PRESSURE_TRIGGER_LIMIT=500;                  //记录正压的触发线 50Pa
    private static float INHALE_PRESSURE_TRIGGER_LIMIT=-500;                 //记录负压的触发线 -50Pa
    private static float prev_pressure=0;                                    //记录上一次的压力值
    private static ArrayList<Float> flow_filter_data=new ArrayList<>();     //用来获取PIVC/FVC，PIV1/FEV1，PIV1%/FEV1%,PIF/PEF等参数
//    private static boolean b_exhale=true;                                   //标志位，表示当前是呼气还是吸气
    private static final int COLOR_BLUE=Color.BLUE;
    private static final int COLOR_GRAY=Color.GRAY;

    //图标数据存储
    private static ArrayList<Entry> m_values=new ArrayList<>();
    private static ArrayList<Entry> m_pressure_values=new ArrayList<>();
    private static ArrayList<Entry> m_flow_values=new ArrayList<>();
    //Y轴上下限
    private static float PRESSURE_Y_LOW_LIMIT=-20000f;      // -2KPa
    private static float PRESSURE_Y_UP_LIMIT=20000f;        //2KPa

//    private static float FLOW_Y_LOW_LIMIT=-13000f;     //流量 -130L/min=-13000/60=215L/s
//    private static float FLOW_Y_UP_LIMIT=14000f;       //流量 140L/min=14000/60s=235L/s
    private static float FLOW_Y_LOW_LIMIT=-217f;     //流量 -130L/min=-13000/60=217L/s
    private static float FLOW_Y_UP_LIMIT=233f;       //流量 140L/min=14000/60s=233L/s

    //限制线数值,标签
    private static float PRESSURE_INHALE_LIMIT_LINE_VALUE=-10000f;      //-1KPa
    private static String PRESSURE_INHALE_LIMIT_LINE_LABLE="吸气：-1Kpa";
    private static float PRESSURE_EXHALE_LIMIT_LINE_VALUE=10000f;      //1KPa
    private static String PRESSURE_EXHALE_LIMIT_LINE_LABLE="呼气：1Kpa";

//    private static float FLOW_INHALE_LIMIT_LINE_VALUE=-13000f;       // 目标-130L/min
//    private static String FLOW_INHALE_LIMIT_LINT_LABLE="吸气：-130L/min";
//    private static float FLOW_EXHALE_LIMIT_LINE_VALUE=13000f;       // 目标130L/min
////    private static String FLOW_EXHALE_LIMIT_LINT_LABLE="呼气：130L/min";
    private static float FLOW_INHALE_LIMIT_LINE_VALUE=-217f;       // 目标-130L/min
    private static String FLOW_INHALE_LIMIT_LINT_LABLE="吸气：-2.17L/s";
    private static float FLOW_EXHALE_LIMIT_LINE_VALUE=217f;       // 目标130L/min
    private static String FLOW_EXHALE_LIMIT_LINT_LABLE="呼气：2.17L/s";

    //DataSet Lable
    private static String PRESSURE_DATASET_LABLE="压力(单位：KPa)";     //honeywell
    private static String FLOW_DATASET_LABLE="流量(单位：L/s)";   //MS5525DSO

    private static int DATA_NUMS=4;  //pressure和flow一次传上来的数据分别是4个，20ms传上来4+4=8个数据,每个数据2个字节

    private static ArrayList<Float> arry_exhale_starting=new ArrayList<>();
    private static float MEP_value;
    private static float MIP_value;
    private static float FVC_value;
    private static float PIVC_value;
    private static float FEV1_value;
    private static float PIV1_value;
    private static float FEV1_percent_value;
    private static float PIV1_percent_value;
    private static float PEF_value;
    private static float PIF_value;



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


    private static void show_pressure_parameters(boolean positive){
        if(pressure_filter_data==null||pressure_filter_data.size()==0)
            return;

        int len=pressure_filter_data.size();

        if(len>=12) {  //12的含义，下位机每隔20ms送来4个数据，12/4=3，3个20ms=60ms，如果呼气只持续了60ms就过滤掉
            //初始化
            MEP_value=0;
            MIP_value=0;

            for(int i=0;i<len;i++){
                float tmp=pressure_filter_data.get(i);
                if(positive){
                    MEP_value=MEP_value>=tmp?MEP_value:tmp;  //最大正压
                }else {
                    MIP_value=MIP_value<=tmp?MIP_value:tmp;  //最大负压
                }
            }
            //设置参数
            if(positive){
                m_MEP.setTextColor(COLOR_BLUE);
                m_MIP.setTextColor(COLOR_GRAY);
                m_MEP.setText(String.valueOf((int)(MEP_value/10)).concat(" Pa"));  //MEP
            }else {
                m_MEP.setTextColor(COLOR_GRAY);
                m_MIP.setTextColor(COLOR_BLUE);
                m_MIP.setText(String.valueOf((int)(MIP_value/10)).concat(" Pa")); //MIP
            }
        }
        pressure_filter_data.clear();
    }
    private static int GET_DATA_TIME_SPAN=20;   //表示20ms来一次数据
    private static int DATA_NUMBERS_EACH_TIME=4;   //表示每次接受到数据的个数，压力4个数据，流量4个数据
    private static int SAMPLE_STEP=5;           //表示下位机采集数据的步长，5ms
    @SuppressLint("DefaultLocale")
    private static void show_flow_parameters(boolean positive) {
        if (flow_filter_data == null || flow_filter_data.size() == 0)
            return;
        int len = flow_filter_data.size();

        if (len >= 12) {
            FVC_value = 0;
            PIVC_value = 0;
            FEV1_value = 0;
            PIV1_value = 0;
            FEV1_percent_value = 0;
            PIV1_percent_value = 0;
            PEF_value = 0;
            PIF_value = 0;

            for (int i = 0; i < len; i++) {
                float tmp = flow_filter_data.get(i);
                if (positive) {
                    //1秒量，下位机是20ms来一次,20ms对应4个点，1秒对于50*4=200个点
                    if(i<(1000/GET_DATA_TIME_SPAN*DATA_NUMBERS_EACH_TIME)){
                        FEV1_value+=tmp*SAMPLE_STEP/1000;
                    }
                    FVC_value+=tmp*SAMPLE_STEP/1000;  //下位机是2ms采集一次
                    PEF_value = PEF_value >= tmp ? PEF_value : tmp;  //PEF
                } else {
                    //1秒量，下位机是20ms来一次,50次，就是1秒
                    if(i<(1000/GET_DATA_TIME_SPAN*DATA_NUMBERS_EACH_TIME)){
                        PIV1_value+=tmp*SAMPLE_STEP/1000;
                    }
                    PIVC_value+=tmp*SAMPLE_STEP/1000;
                    PIF_value = PIF_value <= tmp ? PIF_value : tmp;  //PIF
                }
            }
            //设置参数
            if (positive) {
                //设置字体颜色
                m_FVC.setTextColor(COLOR_BLUE);
                m_FEV1.setTextColor(COLOR_BLUE);
                m_FEV1_percent.setTextColor(COLOR_BLUE);
                m_PEF.setTextColor(COLOR_BLUE);

                m_PIVC.setTextColor(COLOR_GRAY);
                m_PIV1.setTextColor(COLOR_GRAY);
                m_PIV1_percent.setTextColor(COLOR_GRAY);
                m_PIF.setTextColor(COLOR_GRAY);

                m_FVC.setText(String.format("%.3f",FVC_value/100).concat(" L"));                            //FCV
                m_FEV1.setText(String.format("%.3f",FEV1_value/100).concat(" L"));                           //PEV1
                m_FEV1_percent.setText(String.format("%2d",(int)(100*FEV1_value/FVC_value)).concat("%"));   //PEV1%
                m_PEF.setText(String.format("%.3f",PEF_value / 100).concat(" L/s"));                        //PEF
            } else {
                //设置字体颜色
                m_FVC.setTextColor(COLOR_GRAY);
                m_FEV1.setTextColor(COLOR_GRAY);
                m_FEV1_percent.setTextColor(COLOR_GRAY);
                m_PEF.setTextColor(COLOR_GRAY);

                m_PIVC.setTextColor(COLOR_BLUE);
                m_PIV1.setTextColor(COLOR_BLUE);
                m_PIV1_percent.setTextColor(COLOR_BLUE);
                m_PIF.setTextColor(COLOR_BLUE);

                m_PIVC.setText(String.format("%.3f",PIVC_value/100).concat(" L"));                          //PIVC
                m_PIV1.setText(String.format("%.3f",PIV1_value/100).concat(" L"));                           //PIV1
                m_PIV1_percent.setText(String.format("%2d",(int)(100*PIV1_value/PIVC_value)).concat("%"));   //PEV1%
                m_PIF.setText(String.format("%.3f",PIF_value / 100).concat(" L/s"));                        //PIF
            }
        }

        flow_filter_data.clear();
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
               if(data.length==1+DATA_NUMS*2*2+2) {
                    for(int i=0;i<DATA_NUMS;i++){
                        short tmp_data1 = (short) (((data[1 + 2 * i] & 0xFF) << 8) | (data[1 + 2 * i + 1] & 0xFF));
                        short tmp_data2 = (short) (((data[1+DATA_NUMS*2 + 2 * i] & 0xFF) << 8) | (data[1+DATA_NUMS*2 + 2 * i + 1] & 0xFF));
                        tmp_data2/=60;  //将L/min换成L/s
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

                        //将数据收集到指定的ArrayList中
                        //怎么控制开始和停止？
                        //不连续的要过滤掉

                        float pressure_tmp=tmp_data1;
                        if(pressure_tmp>=EXHALE_PRESSURE_TRIGGER_LIMIT){
                            prev_pressure=pressure_tmp;
                            pressure_filter_data.add((float)tmp_data1);
                            flow_filter_data.add((float) tmp_data2);
                        }else if(prev_pressure>EXHALE_PRESSURE_TRIGGER_LIMIT&&pressure_tmp>INHALE_PRESSURE_TRIGGER_LIMIT&&pressure_tmp<EXHALE_PRESSURE_TRIGGER_LIMIT){
                            show_pressure_parameters(true);
                            show_flow_parameters(true);
                        }

                        if(pressure_tmp<=INHALE_PRESSURE_TRIGGER_LIMIT){
                            prev_pressure=pressure_tmp;
                            pressure_filter_data.add((float) tmp_data1);
                            flow_filter_data.add((float) tmp_data2);
                        }else if(prev_pressure<=INHALE_PRESSURE_TRIGGER_LIMIT&&pressure_tmp>INHALE_PRESSURE_TRIGGER_LIMIT&&pressure_tmp<EXHALE_PRESSURE_TRIGGER_LIMIT){
                            show_pressure_parameters(false);
                            show_flow_parameters(false);
                        }



                        pressure_data.add((float) tmp_data1);
                        flow_data.add((float) tmp_data2);
                        }

                        if(pressure_data.size()==1000)
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

        m_MEP=findViewById(R.id.textView_MEP_value);
        m_FVC=findViewById(R.id.textView_FVC_value);
        m_FEV1=findViewById(R.id.textView_FEV1_value);
        m_FEV1_percent=findViewById(R.id.textview_FEV1_percent_value);
        m_PEF=findViewById(R.id.textView_PEF_value);

        m_MIP=findViewById(R.id.textView_MIP_value);
        m_PIVC=findViewById(R.id.textView_PIVC_value);
        m_PIV1=findViewById(R.id.textView_PIV1_value);
        m_PIV1_percent=findViewById(R.id.textView_PIV1_percent_value);
        m_PIF=findViewById(R.id.textView_PIF_value);

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
