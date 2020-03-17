package nl.xservices.plugins.plugin;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;

import nl.xservices.plugins.base.ICordovaPlugin;
import com.olc.scan.IScanCallBack;
import com.olc.scan.ScanManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Description: 条码扫描功能
 * Date: 2020/3/10
 *
 * @author wangke
 */
public class ScanPlugin implements ICordovaPlugin {
    private CordovaInterface cordova;
    private Context context;
    private ScanManager scanManager;
    private MyCodeReceiver receiver;
    private CallbackContext callback;
    private boolean isRegBroadcast = false;
    private static final String BARCODE_BROADCAST_NAME = "com.barcode.sendBroadcast";

    public ScanPlugin(CordovaInterface cordova) {
        this.cordova = cordova;
        this.context = cordova.getContext();
        init();
    }

    @SuppressLint("WrongConstant")
    private void init() {
        scanManager = (ScanManager) this.cordova.getActivity().getSystemService("olc_service_scan");
        scanManager.setScanOperatingMode(0);
        scanManager.setScanSound(true);
        scanManager.setScanVibrate(true);
        scanManager.setBarcodeReceiveModel(2);
        receiver = new MyCodeReceiver();
    }

    /**
     * 注册广播用来监听扫码返回的结果
     */
    private void registerBroadcast() {
        if(isRegBroadcast == false){
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BARCODE_BROADCAST_NAME);
            this.cordova.getActivity().registerReceiver(receiver, intentFilter);
            isRegBroadcast = true;
         }
    }

    @Override
    public void onResume(boolean multitasking) {
        registerBroadcast();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("scanBarcode")) {
            return scanBarcode(callbackContext);
        } else if (action.equals("openScanReceiver")) {
            return openScanReceiver(callbackContext);
        } else if (action.equals("closeScanReceiver")) {
            unregisterReceiver();
            return true;
        }else if (action.equals("releaseScan")){
            release();
        }
        return false;
    }

    private boolean scanBarcode(CallbackContext callbackContext) {
        this.callback = callbackContext;
        PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
        pr.setKeepCallback(true);
        callbackContext.sendPluginResult(pr);
        // 注册广播接收条码返回的结果
        registerBroadcast();
        // 发送广播，触发条码扫描
        Intent intent = new Intent();
        intent.setAction("com.barcode.sendBroadcastScan");
        cordova.getActivity().sendBroadcast(intent);
        return true;
    }

    private boolean openScanReceiver(CallbackContext callbackContext) {
        this.callback = callbackContext;
        registerBroadcast();
        PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
        pr.setKeepCallback(true);
        callbackContext.sendPluginResult(pr);
        return true;
    }

    @Override
    public void onPause(boolean multitasking) {
        unregisterReceiver();
    }

    @Override
    public void onDestroy() {
        release();
    }

    @Override
    public void release() {
        unregisterReceiver();
    }

    private void unregisterReceiver() {
        if(isRegBroadcast){
            cordova.getActivity().unregisterReceiver(receiver);
            isRegBroadcast = false;
        }
    }

    /**
     * 点击触发扫码(左右键)接收广播
     */
    public class MyCodeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BARCODE_BROADCAST_NAME.equals(intent.getAction())) {
                String str = intent.getStringExtra("BARCODE");
                if (!"".equals(str)) {
                    String barCodeStr = formatBarCode(str);
                    PluginResult pr = new PluginResult(PluginResult.Status.OK, barCodeStr);
                    pr.setKeepCallback(true);
                    if (callback != null) {
                        callback.sendPluginResult(pr);
                    }
                }
            }
        }
    }

    private String formatBarCode(String barCode) {
        barCode = barCode.replaceAll("\r|\n", "");
        if (barCode.length() == 4 && (barCode.startsWith("C") || barCode.startsWith("S"))) {
            barCode = "00000" + barCode.substring(1);
        }
        return barCode;
    }
}
