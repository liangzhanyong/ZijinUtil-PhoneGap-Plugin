package nl.xservices.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cw.cwsdk.cw;
import com.cw.cwsdk.u8API.barcode.BarCodeAPI;
import com.cw.cwsdk.u8API.uhf.IOnCommonReceiver;
import com.cw.cwsdk.u8API.uhf.IOnInventoryRealReceiver;
import com.cw.cwsdk.u8API.uhf.IOnTagOperation;
import com.cw.cwsdk.u8API.uhf.base.CMD;
import com.cw.cwsdk.u8API.uhf.helper.InventoryBuffer;
import com.cw.cwsdk.u8API.uhf.helper.OperateTagBuffer;
import com.google.gson.Gson;

/**
 * This class echoes a string called from JavaScript.
 */
public class ZijinUtil extends CordovaPlugin {
    private static final boolean IS_AT_LEAST_LOLLIPOP = Build.VERSION.SDK_INT >= 21;
    private BarCodeReceiver receiver;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        cw.BarCodeAPI(cordova.getContext()).openBarCodeReceiver();
        receiver = new BarCodeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SHUTDOWN);
        filter.addAction(Intent.ACTION_REBOOT);
        cordova.getActivity().registerReceiver(receiver, filter);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("openUHF")) {
            cordova.getActivity().runOnUiThread(() -> {
                cw.R2000UHFAPI().open(IS_AT_LEAST_LOLLIPOP ? cordova.getActivity().getWindow().getContext() : cordova.getActivity().getApplicationContext());
            });
            return true;
        }
        else if(action.equals("startInventoryReal")) {
//            cordova.getActivity().runOnUiThread(new Runnable() {
            cordova.getThreadPool().execute(() -> startInventoryReal(callbackContext));
            return true;
        }
        else if(action.equals("stopInventoryReal")) {
//            cordova.getActivity().runOnUiThread(new Runnable() {
            cordova.getThreadPool().execute(() -> {
                cw.R2000UHFAPI().stopInventoryReal();
            });
            return true;
        }
        else if(action.equals("closeUHF")) {
//            cordova.getActivity().runOnUiThread(new Runnable() {
            cordova.getThreadPool().execute(() -> cw.R2000UHFAPI().close());
            return true;
        }
        else if(action.equals("scan")) {
            cordova.getThreadPool().execute(() -> barCodeScanner(callbackContext));
            return true;
        }
        else if(action.equals("continueScanning")) {
            cordova.getThreadPool().execute(() -> continueScanning(callbackContext));
            return true;
        }
        else if(action.equals("closeScanning")) {
            cw.BarCodeAPI(cordova.getContext()).CloseScanning();
            return true;
        }
        else if(action.equals("setScanner")) {
            callbackContext.error("方法尚未实现！");
            return true;
        }
        else if(action.equals("setScanInterval")) {
            cw.BarCodeAPI(cordova.getContext()).setTime(args.getJSONObject(0).getInt("time"));
            return true;
        }
        else if(action.equals("getReaderTemperature")) {
            cordova.getThreadPool().execute(() -> {
                cw.R2000UHFAPI().setOnCommonReceiver(new IOnCommonReceiver() {
                    @Override
                    public void onReceiver(byte cmd, Object result) {
                        switch (cmd) {
                            case CMD.GET_READER_TEMPERATURE:
                                callbackContext.success((String) result);
                                break;
                        }
                    }

                    @Override
                    public void onLog(String s, int i) {

                    }
                });
                cw.R2000UHFAPI().getReaderTemperature();
            });
            return true;
        }
        else if(action.equals("killTag")) {
            cordova.getThreadPool().execute(() -> {
                try {
                    killTag(callbackContext, args);
                } catch(Exception e) {
                    callbackContext.error(e.getMessage());
                }
            });
            return true;
        }
        else if(action.equals("lockTag")) {
            cordova.getThreadPool().execute(() -> {
                try {
                    lockTag(callbackContext, args);
                } catch(Exception e) {
                    callbackContext.error(e.getMessage());
                }
            });
            return true;
        }
        else if(action.equals("readTag")) {
            cordova.getThreadPool().execute(() -> {
                try {
                    readTag(callbackContext, args);
                } catch(Exception e) {
                    callbackContext.error(e.getMessage());
                }
            });
            return true;
        }
        else if(action.equals("writeTag")) {
            cordova.getThreadPool().execute(() -> {
                try {
                    writeTag(callbackContext, args);
                } catch(Exception e) {
                    callbackContext.error(e.getMessage());
                }
            });
            return true;
        }
        else if(action.equals("reset")) {
            cw.R2000UHFAPI().reset();
            return true;
        }
        else if(action.equals("setInventoryDelayMillis")) {
            cw.R2000UHFAPI().setInventoryDelayMillis(args.getJSONObject(0).getInt("delayMillis"));
            return true;
        }
        else if(action.equals("setOutputPower")) {
            cw.R2000UHFAPI().setOutputPower(args.getJSONObject(0).getInt("mOutPower"));
            return true;
        }
        return true;
    }

    private void startInventoryReal(CallbackContext callbackId) {
        try {
            cw.R2000UHFAPI().startInventoryReal("1");
            cw.R2000UHFAPI().setOnInventoryRealReceiver(new IOnInventoryRealReceiver() {
                @Override
                public void realTimeInventory() {

                }

                @Override
                public void customized_session_target_inventory(InventoryBuffer inventoryBuffer) {

                }

                @Override
                public void inventoryErr() {

                }

                @Override
                public void inventoryErrEnd() {

                }

                @Override
                public void inventoryEnd(InventoryBuffer inventoryBuffer) {
                    PluginResult pr = new PluginResult(PluginResult.Status.OK, new Gson().toJson(inventoryBuffer));
                    pr.setKeepCallback(true);
                    callbackId.sendPluginResult(pr);
                }

                @Override
                public void inventoryRefresh(InventoryBuffer inventoryBuffer) {

                }

                @Override
                public void onLog(String strLog, int type) {

                }
            });
        } catch(Exception e) {
            callbackId.error("超高频模块启动异常：" + e.getMessage());
        }
    }

    private void barCodeScanner(CallbackContext callbackId) {
        cw.BarCodeAPI(cordova.getContext()).setOnBarCodeDataListener(new BarCodeAPI.IBarCodeData() {
            @Override
            public void sendScan() {

            }

            @Override
            public void onBarCodeData(String s) {
                if("No decoded message available.".equals(s)) {
                    callbackId.error(s);
                } else {
                    callbackId.success(s);
                }
            }

            @Override
            public void getSettings(int i, int i1, int i2, String s, String s1, int i3, int i4) {

            }

            @Override
            public void setSettingsSuccess() {

            }
        });
        cw.BarCodeAPI(cordova.getContext()).scan();
    }

    private void continueScanning(CallbackContext callbackId) {
        cw.BarCodeAPI(cordova.getContext()).setOnBarCodeDataListener(new BarCodeAPI.IBarCodeData() {
            @Override
            public void sendScan() {

            }

            @Override
            public void onBarCodeData(String s) {
                Log.i("onBarCodeData", s);
                PluginResult.Status status;
                if("No decoded message available.".equals(s)) {
                    status = PluginResult.Status.ERROR;
                } else {
                    status = PluginResult.Status.OK;
                }
                PluginResult pr = new PluginResult(status, s);
                pr.setKeepCallback(true);
                callbackId.sendPluginResult(pr);
            }

            @Override
            public void getSettings(int i, int i1, int i2, String s, String s1, int i3, int i4) {

            }

            @Override
            public void setSettingsSuccess() {

            }
        });
        cw.BarCodeAPI(cordova.getContext()).setTime(Settings.System.getInt(cordova.getActivity().getContentResolver(), "scan_timeout", 800));
        cw.BarCodeAPI(cordova.getContext()).ContinuousScanning();
    }

    private void writeTag(CallbackContext callbackId, JSONArray args) throws JSONException {
        cw.R2000UHFAPI().setOnTagOperation(new IOnTagOperation() {
            @Override
            public void getAccessEpcMatch(OperateTagBuffer operateTagBuffer) {

            }

            @Override
            public void readTagResult(OperateTagBuffer operateTagBuffer) {

            }

            @Override
            public void writeTagResult(String s) {
                callbackId.success(s);
            }

            @Override
            public void lockTagResult() {

            }

            @Override
            public void killTagResult() {

            }

            @Override
            public void onLog(String s, int i) {

            }
        });
        JSONObject params = args.getJSONObject(0);
        Byte btMemBank = Byte.valueOf(params.getString("btMemBank"));
        String btWordAdd = params.getString("btWordAdd");
        String btWordCnt = params.getString("btWordCnt");
        String btAryPassWord = params.getString("btAryPassWord");
        String data = params.getString("data");
        cw.R2000UHFAPI().writeTag(btMemBank, btWordAdd, btWordCnt, btAryPassWord, data);
    }

    private void readTag(CallbackContext callbackId, JSONArray args) throws JSONException {
        cw.R2000UHFAPI().setOnTagOperation(new IOnTagOperation() {
            @Override
            public void getAccessEpcMatch(OperateTagBuffer operateTagBuffer) {

            }

            @Override
            public void readTagResult(OperateTagBuffer operateTagBuffer) {
                callbackId.success(new Gson().toJson(operateTagBuffer));
            }

            @Override
            public void writeTagResult(String s) {

            }

            @Override
            public void lockTagResult() {

            }

            @Override
            public void killTagResult() {

            }

            @Override
            public void onLog(String s, int i) {

            }
        });
        JSONObject params = args.getJSONObject(0);
        Byte btMemBank = Byte.valueOf(params.getString("btMemBank"));
        String btWordAdd = params.getString("btWordAdd");
        String btWordCnt = params.getString("btWordCnt");
        String btAryPassWord = params.getString("btAryPassWord");
        cw.R2000UHFAPI().readTag(btMemBank, btWordAdd, btWordCnt, btAryPassWord);
    }

    private void killTag(CallbackContext callbackId, JSONArray args) throws JSONException {
        cw.R2000UHFAPI().setOnTagOperation(new IOnTagOperation() {
            @Override
            public void getAccessEpcMatch(OperateTagBuffer operateTagBuffer) {

            }

            @Override
            public void readTagResult(OperateTagBuffer operateTagBuffer) {

            }

            @Override
            public void writeTagResult(String s) {

            }

            @Override
            public void lockTagResult() {

            }

            @Override
            public void killTagResult() {
                callbackId.success();
            }

            @Override
            public void onLog(String s, int i) {

            }
        });
        JSONObject params = args.getJSONObject(0);
        String btAryPassWord = params.getString("btAryPassWord");
        cw.R2000UHFAPI().killTag(btAryPassWord);
    }

    private void lockTag(CallbackContext callbackId, JSONArray args) throws JSONException {
        cw.R2000UHFAPI().setOnTagOperation(new IOnTagOperation() {
            @Override
            public void getAccessEpcMatch(OperateTagBuffer operateTagBuffer) {

            }

            @Override
            public void readTagResult(OperateTagBuffer operateTagBuffer) {

            }

            @Override
            public void writeTagResult(String s) {

            }

            @Override
            public void lockTagResult() {
                callbackId.success();
            }

            @Override
            public void killTagResult() {

            }

            @Override
            public void onLog(String s, int i) {

            }
        });
        JSONObject params = args.getJSONObject(0);
        String btAryPassWord = params.getString("btAryPassWord");
        Byte btMemBank = Byte.valueOf(params.getString("btMemBank"));
        Byte btLockType = Byte.valueOf(params.getString("btLockType"));
        cw.R2000UHFAPI().lockTag(btAryPassWord, btMemBank, btLockType);
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        cw.BarCodeAPI(cordova.getContext()).openBarCodeReceiver();
        if (cw.BarCodeAPI(cordova.getContext()).isScannerServiceRunning(cordova.getActivity())) {

        } else {
            cw.BarCodeAPI(cordova.getContext()).CloseScanning();
        }
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        if(!!cw.R2000UHFAPI().getReaderHelper().getInventoryFlag()) {
            cw.R2000UHFAPI().stopInventoryReal();
        }
        cw.BarCodeAPI(cordova.getContext()).CloseScanning();
        //建议在onPause里或者监听屏幕息屏里放，息屏后可以省电
        cw.BarCodeAPI(cordova.getContext()).closeBarCodeReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cw.R2000UHFAPI().close();
        cordova.getActivity().unregisterReceiver(receiver);
    }

    class BarCodeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }

            switch (action) {
                case Intent.ACTION_SCREEN_ON:
//                    Log.i(TAG, "亮屏了");

                    break;
                case Intent.ACTION_SCREEN_OFF:
//                    Log.i(TAG, "息屏了");
                    cw.BarCodeAPI(cordova.getContext()).CloseScanning();
                    break;


                case Intent.ACTION_SHUTDOWN:
//                    Log.i(TAG, TAG + "---关机了");
                    cw.BarCodeAPI(cordova.getContext()).CloseScanning();
                    break;
                case Intent.ACTION_REBOOT:
//                    Log.i(TAG, TAG + "---亮屏了");
                    break;
            }
        }
    }
}
