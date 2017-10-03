package kalen.app.iot.blepm25.ui;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;

import com.example.android.bluetoothlegatt.BluetoothLeService;

import kalen.app.iot.blepm25.R;
import kalen.app.iot.blepm25.model.C;


public class DeviceControlActivity extends AppCompatActivity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    private TextView mDisplayTView;
    private String mDeviceAddress;
    private String mDeviceName;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mCharacteristic = null; 
    private boolean mConnected = false;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    /**
     * 用于接收蓝牙状态广播的BroadcastReceiver
     * ACTION_GATT_CONNECTED: 蓝牙连接成功广播.
     * ACTION_GATT_DISCONNECTED: 蓝牙断开广播.
     * ACTION_GATT_SERVICES_DISCOVERED: 发现GATT的服务广播.
     * ACTION_DATA_AVAILABLE: 接收到蓝牙的数据广播
     * 
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                getCharacteristic(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            	System.out.println("receive data");
                onReceiveData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        final Intent intent = getIntent();
        //获取设备名以及设备地址
        mDeviceName = intent.getStringExtra(C.EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(C.EXTRAS_DEVICE_ADDRESS);

        mDisplayTView = (TextView) findViewById(R.id.display_tv);

        getSupportActionBar().setTitle(mDeviceName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //绑定BLE服务
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//        //点击发送按钮
//        findViewById(R.id.control_send_btn).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				//没连接,直接返回
//				if (!mConnected) {
//					Toast.makeText(DeviceControlActivity.this, "设备尚未连接",
//							Toast.LENGTH_SHORT).show();
//					return;
//				}
//
//				//没找到对应的特征值,直接返回
//				if (mCharacteristic == null) {
//					Toast.makeText(DeviceControlActivity.this, "没有找到对应的特征值,请尝试重新连接",
//							Toast.LENGTH_SHORT).show();
//					return;
//				}
//
//				String dataStr = mSendDataEditText.getText().toString();
//				sendStrDataToLeDevice(dataStr);
//			}
//		});
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 发送字符串给蓝牙设备
     * @param data
     */
    private void sendStrDataToLeDevice(String data){
		if (!data.equals("")) {
			byte[] datas = data.getBytes();
			mCharacteristic.setValue(datas);
			mBluetoothLeService.writeCharacteristic(mCharacteristic);
		}
    }

    /**
     * 更新连接状态
     * @param resourceId
     */
    private void updateConnectionState(final int resourceId) {
    	Toast.makeText(this, getResources().getString(resourceId), 
    			Toast.LENGTH_SHORT).show();
    }

    private byte[] buffer = new byte[30];

    private void shiftLeft(byte lastNum){
        for (int i = 0; i < 29; i++) {
            buffer[i] = buffer[i + 1];
        }
        buffer[29] = lastNum;
    }

    private boolean checkOutOneSequence(){
        return (buffer[0] == 0x42) && (buffer[1] == 0x4D) && (buffer[2] == 0x00) && (buffer[3] == 0x1C);
    }

    private int turnByteToInt(byte val){
        return val & 0xFF;
    }


    private int getIntValByTwoBytes(byte high, byte low){
        int retVal = turnByteToInt(low);
        retVal += turnByteToInt(high) * 256;
        System.out.println(low & 0xFF);
        System.out.println("high ---> " + (high & 0xFF));
        return retVal;
    }

    /**
     * 接收到数据的回调
     * @param bytes
     */
    private void onReceiveData(byte[] bytes) {
        if (bytes != null) {
            for (int i = 0; i < bytes.length; i++) {
                byte nowNum = bytes[i];
                shiftLeft(nowNum);
                if (checkOutOneSequence()) {
                    String text = "美国标准:\n";
                    text += "PM1.0 : " + getIntValByTwoBytes(buffer[4], buffer[5]) + "ug/m3\n";
                    text += "PM2.5 : " + getIntValByTwoBytes(buffer[6], buffer[7]) + "ug/m3\n";
                    text += "PM10 : " + getIntValByTwoBytes(buffer[8], buffer[9]) + "ug/m3\n";
                    text += "\n中国标准:\n";
                    text += "PM1.0 : " + getIntValByTwoBytes(buffer[10], buffer[11]) + "ug/m3\n";
                    text += "PM2.5 : " + getIntValByTwoBytes(buffer[12], buffer[13]) + "ug/m3\n";
                    text += "PM10 : " + getIntValByTwoBytes(buffer[14], buffer[15]) + "ug/m3\n";

//                    text += "\n\n甲醛:" + (float)(getIntValByTwoBytes(buffer[28], buffer[29]))/1000 + "mg/m3\n";
                    mDisplayTView.setText(text);
                }
            }
        }
    }



    /**
     * 获取BLE的特征值
     * @param gattServices
     */
    private void getCharacteristic(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            
            //找uuid为0000ffe0-0000-1000-8000-00805f9b34fb的服务
            if (uuid.equals(C.SERVICE_UUID)) {
            	List<BluetoothGattCharacteristic> gattCharacteristics =
                        gattService.getCharacteristics();
                //找uuid为0000ffe1-0000-1000-8000-00805f9b34fb的特征值
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid = gattCharacteristic.getUuid().toString();
                    if(uuid.equals(C.CHAR_UUID)){
                    	mCharacteristic = gattCharacteristic;
                    	final int charaProp = gattCharacteristic.getProperties();
                		//开启该特征值的数据的监听
                		if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mBluetoothLeService.setCharacteristicNotification(
                                    mCharacteristic, true);
                        }
                    	System.out.println("uuid----->" + uuid);
                    }
                }
			}
        }
        
        //如果没找到指定的特征值,直接返回
        if (mCharacteristic == null) {
        	Toast.makeText(DeviceControlActivity.this, "未找到指定特征值", 
        			Toast.LENGTH_LONG).show();
			finish();
		}

    }

    /**
     * 构建IntentFilter
     * @return intentFilter
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        return intentFilter;
    }
}
