package com.hyeon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresPermission;
import android.util.Log;
import android.app.Activity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.epicgames.ue4.GameActivity;

public class BluetoothPlugin {

    private static final String TAG = "BluetoothPlugin";

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    final private Context mContext;
    final private GameActivity mGameActivity;

    private boolean IsScan = false;

    private String mConnectedDeviceName = null;

    private StringBuffer mOutStringBuffer;
    private BluetoothAdapter mBtAdapter = null;
    private BluetoothService mBtService = null;

    private ArrayList<String> singleAddress = new ArrayList();
    private List<Byte> byteData = new ArrayList<Byte>();
    private Queue<byte[]> buffer = new LinkedList<byte[]>();

    public BluetoothPlugin(final GameActivity _activity) {
        mGameActivity = _activity;
        mContext = _activity.getApplicationContext();
    }

    // Handler
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MESSAGE_STATE_CHANGE:
                    //UnityPlayer.UnitySendMessage(TARGET, "OnStateChanged", String.valueOf(msg.arg1));
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[])msg.obj;
                    Log.i(TAG, "==== Read Buffer ====");
                    for(int i = 0; i < msg.arg1; ++i) {
                        byteData.add(readBuf[i]);
                        Log.i(TAG, String.valueOf(readBuf[i]));
                    }
                    if (byteData.size() >= 34) {
                        while(byteData.size() != 0 && byteData.get(0) != '$') {
                            byteData.remove(0);
                        }

                        if (byteData.size() >= 34) {
                            if (byteData.get(34 - 1) == '#') {
                                Log.i(TAG, "==== Read Packet ====");

                                byte[] temp_1 = new byte[34];
                                List<Byte> temp_2 = byteData.subList(0, 34);
                                for(int i = 0; i < 34; ++i) {
                                    temp_1[i] = temp_2.get(i);
                                    Log.i(TAG, String.valueOf(((char)temp_1[i])));
                                }
                                buffer.offer(temp_1);
                                nativeGetPacketData(buffer.poll());
                                byteData.remove(0);
                            }
                            else {
                                byteData.remove(0);
                            }
                        }
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[])msg.obj;
                    String writeMessage = new String(writeBuf);
                    //nativeOnSendMessage(writeMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    BluetoothPlugin.this.mConnectedDeviceName = msg.getData().getString("device_name");
                    Toast.makeText(mGameActivity.getApplicationContext(), "Connected to " + BluetoothPlugin.this.mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(mGameActivity.getApplicationContext(), msg.getData().getString("toast"), Toast.LENGTH_SHORT).show();
                    break;
            }

        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @RequiresPermission("android.permission.BLUETOOTH")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if("android.bluetooth.device.action.FOUND".equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                BluetoothPlugin.this.singleAddress.add(device.getName() + "\n" + device.getAddress());
                nativeSearchDevice(device.getName() + ",\n" + device.getAddress());

            } else if("android.bluetooth.adapter.action.DISCOVERY_FINISHED".equals(action)) {
                if(BluetoothPlugin.this.IsScan) {
                    //nativeOnScanFinish();
                }
                if(BluetoothPlugin.this.singleAddress.size() == 0) {
                    //nativeOnFoundNoDevice();
                }
            }

        }
    };

    // 1. Starting Point in Unity Script
    @RequiresPermission("android.permission.BLUETOOTH")
    public void StartPlugin() {
        if(Looper.myLooper() == null) {
            Looper.prepare();
        }

        this.SetupPlugin();
    }


    // 2. Setup Plugin
    // Get Default Bluetooth Adapter and start Service
    @RequiresPermission("android.permission.BLUETOOTH")
    public String SetupPlugin() {
        // Bluetooth Adapter
        this.mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // if Bluettoth Adapter is avaibale, start Service
        if(this.mBtAdapter == null) {
            return "Bluetooth is not available";

        } else {
            if(this.mBtService == null) {
                this.startService();
            }

            return "SUCCESS";
        }
    }


    // 3. Setup and Start Bluetooth Service
    private void startService() {
        Log.d(TAG, "setupService()");
        this.mBtService = new BluetoothService(this.mHandler);
        this.mOutStringBuffer = new StringBuffer("");
    }

    public String DeviceName() {
        return this.mBtAdapter.getName();
    }

    @RequiresPermission("android.permission.BLUETOOTH")
    public String GetDeviceConnectedName() {
        return !this.mBtAdapter.isEnabled()?"You Must Enable The BlueTooth":(this.mBtService.getState() != 3?"Not Connected":this.mConnectedDeviceName);
    }

    @RequiresPermission("android.permission.BLUETOOTH")
    public boolean IsEnabled() {
        return this.mBtAdapter.isEnabled();
    }

    public boolean IsConnected() {
        return this.mBtService.getState() == 3;
    }

    @RequiresPermission("android.permission.BLUETOOTH")
    public void stopThread() {
        Log.d(TAG, "stop");
        if(this.mBtService != null) {
            this.mBtService.stop();
            this.mBtService = null;
        }

        if(this.mBtAdapter != null) {
            this.mBtAdapter = null;
        }

        this.SetupPlugin();
    }

    @RequiresPermission(
            allOf = {"android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN"}
    )
    public void Connect(String TheAdrees) {
        if(this.mBtAdapter.isDiscovering()) {
            this.mBtAdapter.cancelDiscovery();
        }

        this.IsScan = false;
        String address = TheAdrees.substring(TheAdrees.length() - 17);
        this.mConnectedDeviceName = TheAdrees.split(",")[0];
        BluetoothDevice device = this.mBtAdapter.getRemoteDevice(address);

        this.mBtService.connect(device);
    }

    @RequiresPermission(
            allOf = {"android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN"}
    )
    public String ScanDevice() {
        Log.d(TAG, "Start - ScanDevice()");
        if(!this.mBtAdapter.isEnabled()) {
            return "You Must Enable The BlueTooth";
        } else {
            this.IsScan = true;
            this.singleAddress.clear();
            IntentFilter filter = new IntentFilter("android.bluetooth.device.action.FOUND");
            mGameActivity.registerReceiver(this.mReceiver, filter);
            filter = new IntentFilter("android.bluetooth.adapter.action.DISCOVERY_FINISHED");
            mGameActivity.registerReceiver(this.mReceiver, filter);
            this.mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            Set pairedDevices = this.mBtAdapter.getBondedDevices();
            if(pairedDevices.size() > 0) {

            }

            this.doDiscovery();
            return "SUCCESS";
        }
    }

    @RequiresPermission(
            allOf = {"android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN"}
    )
    private void doDiscovery() {
        Log.d(TAG, "doDiscovery()");
        if(this.mBtAdapter.isDiscovering()) {
            this.mBtAdapter.cancelDiscovery();
        }

        this.mBtAdapter.startDiscovery();
    }

    @RequiresPermission(
            allOf = {"android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN"}
    )
    String BluetoothSetName(String name) {
        if(!this.mBtAdapter.isEnabled()) {
            return "You Must Enable The BlueTooth";
        } else if(this.mBtService.getState() != 3) {
            return "Not Connected";
        } else {
            this.mBtAdapter.setName(name);
            return "SUCCESS";
        }
    }

    @RequiresPermission(
            allOf = {"android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN"}
    )
    String DisableBluetooth() {
        if(!this.mBtAdapter.isEnabled()) {
            return "You Must Enable The BlueTooth";
        } else {
            if(this.mBtAdapter != null) {
                this.mBtAdapter.cancelDiscovery();
            }

            if(this.mBtAdapter.isEnabled()) {
                this.mBtAdapter.disable();
            }

            return "SUCCESS";
        }
    }

    @RequiresPermission("android.permission.BLUETOOTH")
    public String BluetoothEnable() {
        try {
            if(!this.mBtAdapter.isEnabled()) {
                Intent e = new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE");
                mGameActivity.startActivityForResult(e, 2);
            }

            return "SUCCESS";

        } catch (Exception e) {
            return "Faild";
        }
    }

    public void showMessage(final String message) {
        mGameActivity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(mGameActivity, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "+++ ON CREATE +++");
    }

    public void onStart() {
    }

    public synchronized void onPause() {
        Log.e(TAG, "- ON PAUSE -");
    }

    @RequiresPermission("android.permission.BLUETOOTH")
    public synchronized void onResume() {
        Log.d(TAG, "+ ON RESUME +");
        if(this.mBtService != null && this.mBtService.getState() == 0) {
            this.mBtService.start();
        }

    }

    public void onStop() {
        Log.e(TAG, "-- ON STOP --");
    }

    public void onDestroy() {
        if(this.mBtService != null) {
            this.mBtService.stop();
        }

        Log.e(TAG, "--- ON DESTROY ---");
    }

    @RequiresPermission("android.permission.BLUETOOTH")
    public String ensureDiscoverable() {
        if(!this.mBtAdapter.isEnabled()) {
            return "You Must Enable The BlueTooth";
        } else {
            if(this.mBtAdapter.getScanMode() != 23) {
                Intent discoverableIntent = new Intent("android.bluetooth.adapter.action.REQUEST_DISCOVERABLE");
                discoverableIntent.putExtra("android.bluetooth.adapter.extra.DISCOVERABLE_DURATION", 300);
                mGameActivity.startActivity(discoverableIntent);
            }

            return "SUCCESS";
        }
    }

    @RequiresPermission("android.permission.BLUETOOTH")
    public String sendMessage(String message) {
        if(!this.mBtAdapter.isEnabled()) {
            return "You Must Enable The BlueTooth";

        } else if(this.mBtService.getState() != 3) {
            return "Not Connected";

        } else {
            if(message.length() > 0) {
                byte[] send = message.getBytes();
                this.mBtService.write(send);
                this.mOutStringBuffer.setLength(0);
            }

            return "SUCCESS";
        }
    }

    public byte[] GetPacketData() {
        return this.buffer.poll();
    }

    public native void nativeSearchDevice(String _device);
    public native void nativeGetPacketData(byte[] _data);

}