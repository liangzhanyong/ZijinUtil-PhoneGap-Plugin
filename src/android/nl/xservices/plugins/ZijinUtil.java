package nl.xservices.plugins;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.google.gson.Gson;
import com.olc.scan.ScanManager;
import com.olc.uhf.UhfAdapter;
import com.olc.uhf.UhfManager;
import com.olc.uhf.tech.ISO1800_6C;
import com.olc.uhf.tech.IUhfCallback;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * This class echoes a string called from JavaScript.
 */
public class ZijinUtil extends CordovaPlugin {
    private static String broadcastName = "com.barcode.sendBroadcast";
    private static String TAG = "ZijinUtil";
    private ScanManager scanManager;
    MyCodeReceiver receiver = new MyCodeReceiver();

    public static UhfManager uhfManager;
    private ISO1800_6C uhf_6c;
    private boolean isLoop =false;
    private boolean inventoryOpened = false;

    CallbackContext callbackContext;

    @SuppressLint("WrongConstant")
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        scanManager = (ScanManager) this.cordova.getActivity().getSystemService("olc_service_scan");
        scanManager.setScanOperatingMode(0);
        scanManager.setScanSound(true);
        scanManager.setScanVibrate(true);
        scanManager.setBarcodeReceiveModel(2);   // 0 : fast; 1 : slow; 2 : broadcast
        registerBroadcast();
        uhfManager = UhfAdapter.getUhfManager(cordova.getContext());
        uhfManager.open();
        uhf_6c = uhfManager.getISO1800_6C();
        DevBeep.init(cordova.getActivity());
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if(action.equals("openScanReceiver")) {
            this.callbackContext = callbackContext;
            PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
            pr.setKeepCallback(true);
            callbackContext.sendPluginResult(pr);
            return true;
        }
        else if(action.equals("barCodeScan")) {
            scanManager.startRead(new IScanCallBack.Stub() {
                @Override
                public void doScanResult(String barCode) throws RemoteException {
                    if(this.callbackContext != null){
                        PluginResult pr = new PluginResult(PluginResult.Status.OK);
                        pr.setKeepCallback(true);
                        this.callbackContext.sendPluginResult(barCode);
                    }
                }
            });
            PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
            pr.setKeepCallback(true);
            callbackContext.sendPluginResult(pr);
            return true;
        }
        else if(action.equals("closeScanReceiver")) {
            cordova.getActivity().unregisterReceiver(receiver);
            return true;
        }
        else if(action.equals("startInventoryReal")) {
            isLoop=true;
            inventoryOpened = true;
            LoopReadEPC();
            this.callbackContext = callbackContext;
            PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
            pr.setKeepCallback(true);
            callbackContext.sendPluginResult(pr);
            return true;
        }
        else if(action.equals("stopInventoryReal")) {
            isLoop=false;
            inventoryOpened = false;
            return true;
        }
        else if(action.equals("setOutputPower")) {
            int mOutPower = args.getJSONObject(0).getInt("mOutPower");
            if(mOutPower > 1200 & mOutPower < 2800) {
                uhfManager.setTransmissionPower(mOutPower);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        registerBroadcast();
        if(inventoryOpened)
        {
            isLoop=true;
            LoopReadEPC();
        }
    }


    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        cordova.getActivity().unregisterReceiver(receiver);
        isLoop=false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isLoop = false;
        uhfManager.close();
    }

    public void registerBroadcast() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(broadcastName);
        this.cordova.getActivity().registerReceiver(receiver, intentFilter);
    }

    public void LoopReadEPC() {
        Thread thread = new Thread(() -> {
            while (isLoop) {
                uhf_6c.inventory(callback);
                if (!isLoop) {
                    break;
                }
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;

                }
            }
        });
        thread.start();
    }

    IUhfCallback callback = new IUhfCallback.Stub() {
        @Override
        public void doInventory(List<String> str) {
            List<String> result = new ArrayList<>();
            for (int i = 0; i < str.size(); i++) {
                String strEpc = hexStringToString(str.get(i).substring(6));
                result.add(strEpc);
            }
            Log.d(TAG, "EPC=" + new Gson().toJson(result));
            PluginResult pr = new PluginResult(PluginResult.Status.OK, new Gson().toJson(result));
            pr.setKeepCallback(true);
            callbackContext.sendPluginResult(pr);
            DevBeep.PlayOK();
        }

        @Override
        public void doTIDAndEPC(List<String> list) { }
    };

    /**
     * 16进制转换成为string类型字符串
     * @param s
     * @return
     */
    public static String hexStringToString(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        s = s.replace(" ", "");
        byte[] baKeyword = new byte[s.length() / 2];
        for (int i = 0; i < baKeyword.length; i++) {
            try {
                baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            s = new String(baKeyword, "UTF-8");
            new String();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return s;
    }

    public class MyCodeReceiver extends BroadcastReceiver {
        private static final String TAG = "MycodeReceiver";
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(broadcastName)) {
                String str = intent.getStringExtra("BARCODE");
                Log.i(TAG, str);
                if (!"".equals(str)) {
                    PluginResult pr = new PluginResult(PluginResult.Status.OK, str);
                    pr.setKeepCallback(true);
                    callbackContext.sendPluginResult(pr);
                }
            }
        }
    }
}
