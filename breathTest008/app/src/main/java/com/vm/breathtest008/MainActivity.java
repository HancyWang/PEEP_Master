package com.vm.breathtest008;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.provider.Settings.Global.DEVICE_NAME;

public class MainActivity extends ListActivity {
    private final static String TAG="hancy";
    private boolean m_bScannning=false;
    private final static int REQUEST_CODE=0x01;


    private BluetoothAdapter m_BluetoothAdapter;
//    private BluetoothLeScanner m_BluetoothLeScanner;
    private BluetoothGatt m_BluetoothGatt;
    private LeDeviceListAdapter m_LeDeviceListAdapter;
    private Handler m_Handler=new Handler();

    //蓝牙BLE扫描回调
    private BluetoothAdapter.LeScanCallback m_LeScanCallback=new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
//            Log.d(TAG,device.getAddress());
            m_LeDeviceListAdapter.addDevice(device);
            m_LeDeviceListAdapter.notifyDataSetChanged();
        }
    };

    private ScanCallback m_ScanCallback=new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //super.onScanResult(callbackType, result);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                m_LeDeviceListAdapter.addDevice(result.getDevice());
            }
            m_LeDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG,"onScanFailed，扫描失败");
        }
    };

    //List适配器类
    private class LeDeviceListAdapter extends BaseAdapter{
        private LayoutInflater m_LayoutInflater;
        private List<BluetoothDevice> m_DeviceList;

        public void addDevice(BluetoothDevice device){
            if(!m_DeviceList.contains(device)){
                m_DeviceList.add(device);
            }
        }

        public BluetoothDevice getDevice(int position){
            return m_DeviceList.get(position);
        }

        private LeDeviceListAdapter(){
            m_LayoutInflater=getLayoutInflater();
            m_DeviceList=new ArrayList<>();
        }

        private void clear(){
            m_DeviceList.clear();
        }

        @Override
        public int getCount() {
            return m_DeviceList.size();
        }

        @Override
        public Object getItem(int position) {
            return m_DeviceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if(convertView==null){
                convertView=m_LayoutInflater.inflate(R.layout.device_info,null);
                viewHolder=new ViewHolder();
                viewHolder.device_name=convertView.findViewById(R.id.device_name);
                viewHolder.device_address=convertView.findViewById(R.id.device_address);
                convertView.setTag(viewHolder);
            }else {
                viewHolder= (ViewHolder) convertView.getTag();
            }

            BluetoothDevice device=m_DeviceList.get(position);
            String name=device.getName();
            if(name==null||name.length()<=0){
                viewHolder.device_name.setText("null");
            }else {
                viewHolder.device_name.setText(device.getName());
            }
            viewHolder.device_address.setText(device.getAddress());

            return convertView;
        }
    }

    static class ViewHolder{
        TextView device_name;
        TextView device_address;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Objects.requireNonNull(getActionBar()).setTitle("设备列表");
        }

        //设置屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this, "手机不支持蓝牙BLE", Toast.LENGTH_SHORT).show();
            finish();
        }else {
//            Toast.makeText(this, "手机支持蓝牙BLE", Toast.LENGTH_SHORT).show();
        }

        //获取adpater
        BluetoothManager bluetoothManager= (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        m_BluetoothAdapter=bluetoothManager.getAdapter();
        if(m_BluetoothAdapter==null){
            Toast.makeText(this, "手机没有BLE", Toast.LENGTH_SHORT).show();
            finish();
        }else {
//            Toast.makeText(this, "手机有BLE", Toast.LENGTH_SHORT).show();
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            m_BluetoothLeScanner=m_BluetoothAdapter.getBluetoothLeScanner();
//        }

//        if(m_BluetoothLeScanner==null)
//        {
//            Log.d(TAG,"m_BluetoothLeScanner==null");
//            finish();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!m_BluetoothAdapter.isEnabled()){
            Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,REQUEST_CODE);
        }
        else {
            Log.d(TAG,"蓝牙已经开启");
            m_LeDeviceListAdapter=new LeDeviceListAdapter();
            setListAdapter(m_LeDeviceListAdapter);
            //开始扫描蓝牙BLE设备
            leScan(true);
        }
//        Log.d(TAG,"蓝牙已经开启");
//        m_LeDeviceListAdapter=new LeDeviceListAdapter();
//        setListAdapter(m_LeDeviceListAdapter);
//        //开始扫描蓝牙BLE设备
//        leScan(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        m_bScannning=false;
        if(m_LeDeviceListAdapter!=null){
            leScan(false);
            m_LeDeviceListAdapter.clear();
        }
    }

    private Runnable m_Runnable=new Runnable() {
        @Override
        public void run() {
            m_bScannning=false;
            m_BluetoothAdapter.stopLeScan(m_LeScanCallback);
            invalidateOptionsMenu();
        }
    };

    private void leScan(boolean enable){
        if(enable){
//            if(!m_BluetoothAdapter.isEnabled()){
//                Toast.makeText(this, "请开启蓝牙", Toast.LENGTH_SHORT).show();
//                return;
//            }
            if(m_Runnable!=null){
                m_Handler.removeCallbacks(m_Runnable);
                m_Runnable=new Runnable() {
                    @Override
                    public void run() {
                        m_bScannning=false;
                        m_BluetoothAdapter.stopLeScan(m_LeScanCallback);
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                            m_BluetoothLeScanner.stopScan(m_ScanCallback);
//                        }
                        invalidateOptionsMenu();
                    }
                };
            }
            m_Handler.postDelayed(m_Runnable,10000);
            m_bScannning=true;
            m_BluetoothAdapter.startLeScan(m_LeScanCallback);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                m_BluetoothLeScanner.startScan(m_ScanCallback);
//            }
        }else {
            m_bScannning=false;
            m_BluetoothAdapter.stopLeScan(m_LeScanCallback);
//            m_BluetoothLeScanner.stopScan(m_ScanCallback);
        }
        invalidateOptionsMenu();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent intent=new Intent(this,LeDeviceControlActivity.class);
        BluetoothDevice device=m_LeDeviceListAdapter.getDevice(position);
        String name=device.getName();
        if(name==null||name.length()<=0){
            Toast.makeText(this, "该设备不是目标设备，请点击BLE#", Toast.LENGTH_SHORT).show();
        }else {
            if(name.contains(LeDeviceControlActivity.DEVICE_NAME)){
                intent.putExtra("NAME",device.getName());
                intent.putExtra("ADDRESS",device.getAddress());
                if(m_bScannning){
                    leScan(false);
                }
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_CODE&&resultCode==Activity.RESULT_CANCELED){
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_stop,menu);
        if(m_bScannning){
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.progress_bar);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_stop).setVisible(true);
        }else {
            menu.findItem(R.id.menu_refresh).setActionView(null);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_stop).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_scan:
                m_bScannning=true;
                m_LeDeviceListAdapter.clear();
                leScan(true);
                break;
            case R.id.menu_stop:
                m_bScannning=false;
                break;
        }
        invalidateOptionsMenu();
        return super.onOptionsItemSelected(item);
    }
}
