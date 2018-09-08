package com.vm.breathtest008;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.LongDef;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class LeService extends Service {
    public static final String ACTION_GATT_CONNECTED="com.vm.breathtest008.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED="com.vm.breathtest008.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICE_DISCOVERED="com.vm.breathtest008.ACTION_GATT_SERVICE_DISCOVERED";
    public static final String ACTION_GATT_DATA_AVAILABLE="com.vm.breathtest008.ACTION_GATT_DATA_AVAILABLE";
    public static final String ACTION_GATT_EXTRA_DATA="com.vm.breathtest008.ACTION_GATT_EXTRA_DATA";
    public static final String DATA_LENGTH="com.vm.breathtest008.DATA_LENGTH";
    public final static String CLIENT_CHARACTERISTIC_CONFIG="00002902-0000-1000-8000-00805f9b34fb";
    private String TAG="hancy";
    private BluetoothManager m_BluetoothManager;
    private BluetoothAdapter m_BluetoothAdapter;
    private BluetoothLeScanner m_BluetoothLeScanner;
    private BluetoothGatt m_BluetoothGatt;
    private BluetoothDevice m_BluetoothDevice;
//    private TextView m_tvCnt;
//    public static int m_nCnt;

    public class LocalBinder extends Binder{
        LeService getService(){
            return LeService.this;
        }
    }
    private LocalBinder m_binder=new LocalBinder();

    private BluetoothGattCallback m_BluetoothGattCallback=new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState== BluetoothProfile.STATE_CONNECTED){
                updateBroadcast(LeService.ACTION_GATT_CONNECTED);
            }
            if(!gatt.discoverServices()){
                Log.d(TAG,"gatt.discoverServices()发现服务失败");
            }else if(newState==BluetoothProfile.STATE_DISCONNECTED){
                updateBroadcast(LeService.ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status==BluetoothGatt.GATT_SUCCESS){
                updateBroadcast(LeService.ACTION_GATT_SERVICE_DISCOVERED);
            }else {
                Log.d(TAG,"BluetoothGatt的状态不是GATT_SUCCESS");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //super.onCharacteristicChanged(gatt, characteristic);
            updateBroadcast(LeService.ACTION_GATT_DATA_AVAILABLE,characteristic);

        }
    };

    public boolean init(){
        m_BluetoothManager= (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (m_BluetoothManager != null) {
            m_BluetoothAdapter=m_BluetoothManager.getAdapter();
        }else {
            Log.d(TAG,"m_BluetoothManager为空");
            return false;
        }
        if(m_BluetoothAdapter==null){
            Log.d(TAG,"m_BluetoothAdapter为空");
            return false;
        }else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                m_BluetoothLeScanner=m_BluetoothAdapter.getBluetoothLeScanner();
            }
            if(m_BluetoothLeScanner==null){
                return false;
            }
        }
        return true;
    }

    public boolean setNotification(BluetoothGattCharacteristic characteristic,boolean enable){
        if(m_BluetoothGatt==null){
            Log.d(TAG,"setNofitication中m_BluetoothGatt为空");
            return false;
        }
        if(!m_BluetoothGatt.setCharacteristicNotification(characteristic,enable)){
            Log.d(TAG,"m_BluetoothGatt.setCharacteristicNotification设置失败");
            return false;
        }
        Log.d(TAG,"m_BluetoothGatt.setCharacteristicNotification设置成功");

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        m_BluetoothGatt.writeDescriptor(descriptor);
        Log.d(TAG,"订阅");
        return true;
    }

    public List<BluetoothGattService> getSupportServices(){
        if(m_BluetoothGatt==null){
            Log.d(TAG,"获取服务时m_BluetoothGatt为空");
            return null;
        }
        return m_BluetoothGatt.getServices();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return m_binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if(m_BluetoothGatt==null){
            return false;
        }
        m_BluetoothGatt.close();
        m_BluetoothGatt=null;
        return super.onUnbind(intent);
    }

    public boolean connect(String address){
        if(address==null){
            Log.d(TAG,"address为空");
            return false;
        }
        //如果已经存在gatt
        if(m_BluetoothGatt!=null){
            if(!m_BluetoothGatt.connect()){
                Log.d(TAG,"m_BluetoothGatt.connect()连接失败");
                return false;
            }
            Log.d(TAG,"m_BluetoothGatt.connect()连接成功");
            return true;
        }
        //如果不存在gatt
        m_BluetoothDevice= m_BluetoothAdapter.getRemoteDevice(address);
        if(m_BluetoothDevice==null){
            Log.d(TAG,"m_BluetoothDevice为空");
            return false;
        }
        m_BluetoothGatt=m_BluetoothDevice.connectGatt(this,false,m_BluetoothGattCallback);
        if(m_BluetoothGatt==null){
            Log.d(TAG,"mm_BluetoothGatt为空");
            return false;
        }
        return true;
    }

    public boolean disconnect(){
        if(m_BluetoothGatt==null){
            Log.d(TAG,"m_BluetoothGatt为空");
            return false;
        }
        m_BluetoothGatt.disconnect();
        return true;
    }

    public void updateBroadcast(String action){
        if(action==null){
            Log.d(TAG,"updateBroadcast中action为空");
            return;
        }
        Log.d(TAG,action);
        Intent intent=new Intent(action);
        sendBroadcast(intent);
    }
    public void updateBroadcast(String action,BluetoothGattCharacteristic characteristic){
        if(action==null||characteristic==null){
            Log.d(TAG,"updateBroadcast中action或者characteristic为空");
            return;
        }

        if(action.equals(LeService.ACTION_GATT_DATA_AVAILABLE)){
            Log.d(TAG,"action.equals(LeService.ACTION_GATT_DATA_AVAILABLE)");
            byte[] bytes=characteristic.getValue();
//            m_nCnt+=bytes.length;
            if(bytes!=null&&bytes.length>0){
//                StringBuilder builder=new StringBuilder();
//                for (byte aByte : bytes) {
//                    builder.append(String.format("%02X", aByte));
//                }
                Intent intent=new Intent(action);
                intent.putExtra(LeService.ACTION_GATT_EXTRA_DATA,bytes);
//                intent.putExtra(LeService.DATA_LENGTH,m_nCnt);
                sendBroadcast(intent);
            }
        }
    }
}
