package nl.xservices.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.barcode.BarcodeUtility;
import com.cw.cwsdk.u8API.uhf.helper.InventoryBuffer;
import com.google.gson.Gson;
import com.rscja.deviceapi.RFIDWithUHF;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class Plugin_P80 {
    private CordovaInterface cordova;
    private BarcodeUtility barcodeUtility;
    private RFIDWithUHF rfidWithUHF;
    private CallbackContext callback;
    private boolean continueScanner = false;
    private BarCodeReceiver receiver = null;
    private Timer timer = null;
    private boolean inventoryLoop = false;
    private InventoryBuffer inventoryResult = new InventoryBuffer();

    public Plugin_P80(CordovaInterface cordova, BarcodeUtility barcodeUtility, RFIDWithUHF rfidWithUHF) {
        this.cordova = cordova;
        this.barcodeUtility = barcodeUtility;
        this.rfidWithUHF = rfidWithUHF;
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("openUHF")) {
            cordova.getActivity().runOnUiThread(() -> {
                rfidWithUHF.init();
            });
            return true;
        }
        else if(action.equals("startInventoryReal")) {
            cordova.getThreadPool().execute(() -> startInventoryReal(callbackContext));
            PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
            pr.setKeepCallback(true);
            callbackContext.sendPluginResult(pr);
            return true;
        }
        else if(action.equals("stopInventoryReal")) {
            cordova.getThreadPool().execute(() -> {
                inventoryLoop = false;
                rfidWithUHF.stopInventory();
            });
            return true;
        }
        else if(action.equals("closeUHF")) {
            cordova.getThreadPool().execute(() -> {
                rfidWithUHF.free();
            });
            return true;
        }
        else if(action.equals("scan")) {
            cordova.getThreadPool().execute(() -> {
                continueScanner = false;
                barCodeScanner(callbackContext);
            });
            return true;
        }
        else if(action.equals("continueScanning")) {
            cordova.getThreadPool().execute(() -> {
                continueScanner = true;
                barCodeScanner(callbackContext);
            });
            PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
            pr.setKeepCallback(true);
            callbackContext.sendPluginResult(pr);
            return true;
        }
        else if(action.equals("closeScanning")) {
            cordova.getThreadPool().execute(() -> {
                continueScanner = false;
                barcodeUtility.stopScan(cordova.getContext(), BarcodeUtility.ModuleType.AUTOMATIC_ADAPTATION);
            });
            return true;
        }
        else if(action.equals("setScanner")) {
            callbackContext.error("方法尚未实现！");
            return true;
        }
        else if(action.equals("setScanInterval")) {
//            cw.BarCodeAPI(cordova.getContext()).setTime(args.getJSONObject(0).getInt("time"));
            return true;
        }
        else if(action.equals("getReaderTemperature")) {
            callbackContext.error("方法尚未实现！");
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
            return true;
        }
        else if(action.equals("setInventoryDelayMillis")) {
            return true;
        }
        else if(action.equals("setOutputPower")) {
            JSONObject params = args.getJSONObject(0);
            Integer power = params.getInt("mOutPower");
            rfidWithUHF.setPower(power);
            return true;
        }
        return true;
    }

    private void startInventoryReal(CallbackContext callbackId) {
        callback = callbackId;
        try {
            if (rfidWithUHF.startInventoryTag(0,0)) {
                inventoryLoop = true;
                new TagThread().start();
            }
        } catch(Exception e) {
            callbackId.error("超高频模块启动异常：" + e.getMessage());
        }
    }

    private void barCodeScanner(CallbackContext callbackId) {
        callback = callbackId;
        receiver = new BarCodeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.scanner.broadcast");
        cordova.getActivity().registerReceiver(receiver, filter);
        barcodeUtility.setScanResultBroadcast(cordova.getContext(), "com.scanner.broadcast", "data");
        barcodeUtility.enableContinuousScan(cordova.getContext(),continueScanner);
        barcodeUtility.startScan(cordova.getContext(), BarcodeUtility.ModuleType.AUTOMATIC_ADAPTATION);
        barCodeHandle();
    }

    private void writeTag(CallbackContext callbackId, JSONArray args) throws JSONException {
//        cw.R2000UHFAPI().setOnTagOperation(new IOnTagOperation() {
//            @Override
//            public void getAccessEpcMatch(OperateTagBuffer operateTagBuffer) {
//
//            }
//
//            @Override
//            public void readTagResult(OperateTagBuffer operateTagBuffer) {
//
//            }
//
//            @Override
//            public void writeTagResult(String s) {
//                callbackId.success(s);
//            }
//
//            @Override
//            public void lockTagResult() {
//
//            }
//
//            @Override
//            public void killTagResult() {
//
//            }
//
//            @Override
//            public void onLog(String s, int i) {
//
//            }
//        });
//        JSONObject params = args.getJSONObject(0);
//        Byte btMemBank = Byte.valueOf(params.getString("btMemBank"));
//        String btWordAdd = params.getString("btWordAdd");
//        String btWordCnt = params.getString("btWordCnt");
//        String btAryPassWord = params.getString("btAryPassWord");
//        String data = params.getString("data");
//        cw.R2000UHFAPI().writeTag(btMemBank, btWordAdd, btWordCnt, btAryPassWord, data);
    }

    private void readTag(CallbackContext callbackId, JSONArray args) throws JSONException {
//        cw.R2000UHFAPI().setOnTagOperation(new IOnTagOperation() {
//            @Override
//            public void getAccessEpcMatch(OperateTagBuffer operateTagBuffer) {
//
//            }
//
//            @Override
//            public void readTagResult(OperateTagBuffer operateTagBuffer) {
//                callbackId.success(new Gson().toJson(operateTagBuffer));
//            }
//
//            @Override
//            public void writeTagResult(String s) {
//
//            }
//
//            @Override
//            public void lockTagResult() {
//
//            }
//
//            @Override
//            public void killTagResult() {
//
//            }
//
//            @Override
//            public void onLog(String s, int i) {
//
//            }
//        });
        JSONObject params = args.getJSONObject(0);
        RFIDWithUHF.BankEnum btMemBank = RFIDWithUHF.BankEnum.valueOf(params.getString("btMemBank"));
        Integer btWordAdd = params.getInt("btWordAdd");
        Integer btWordCnt = params.getInt("btWordCnt");
        String btAryPassWord = params.getString("btAryPassWord");
        rfidWithUHF.readData(btAryPassWord, btMemBank, btWordAdd, btWordCnt);
    }

    private void killTag(CallbackContext callbackId, JSONArray args) throws JSONException {
//        cw.R2000UHFAPI().setOnTagOperation(new IOnTagOperation() {
//            @Override
//            public void getAccessEpcMatch(OperateTagBuffer operateTagBuffer) {
//
//            }
//
//            @Override
//            public void readTagResult(OperateTagBuffer operateTagBuffer) {
//
//            }
//
//            @Override
//            public void writeTagResult(String s) {
//
//            }
//
//            @Override
//            public void lockTagResult() {
//
//            }
//
//            @Override
//            public void killTagResult() {
//                callbackId.success();
//            }
//
//            @Override
//            public void onLog(String s, int i) {
//
//            }
//        });
        JSONObject params = args.getJSONObject(0);
        String btAryPassWord = params.getString("btAryPassWord");
        rfidWithUHF.killTag(btAryPassWord);
    }

    private void lockTag(CallbackContext callbackId, JSONArray args) throws JSONException {
//        cw.R2000UHFAPI().setOnTagOperation(new IOnTagOperation() {
//            @Override
//            public void getAccessEpcMatch(OperateTagBuffer operateTagBuffer) {
//
//            }
//
//            @Override
//            public void readTagResult(OperateTagBuffer operateTagBuffer) {
//
//            }
//
//            @Override
//            public void writeTagResult(String s) {
//
//            }
//
//            @Override
//            public void lockTagResult() {
//                callbackId.success();
//            }
//
//            @Override
//            public void killTagResult() {
//
//            }
//
//            @Override
//            public void onLog(String s, int i) {
//
//            }
//        });
        JSONObject params = args.getJSONObject(0);
        String btAryPassWord = params.getString("btAryPassWord");
        String btMemBank = params.getString("btMemBank");
        String btLockType = params.getString("btLockType");
        rfidWithUHF.lockMem(btAryPassWord, btMemBank, btLockType);
    }

    private void barCodeHandle() {
        if (timer != null) {
            timer.cancel();
        }
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (continueScanner) {
//                    barcodeUtility.stopScan(cordova.getContext(), BarcodeUtility.ModuleType.AUTOMATIC_ADAPTATION);
                    barcodeUtility.startScan(cordova.getContext(), BarcodeUtility.ModuleType.AUTOMATIC_ADAPTATION);
                    Log.i("Plugin_P80", "continue scan");
                    barCodeHandle();
                }
            }
        };
        timer = new Timer();
        timer.schedule(task, 5000);
    }

    class BarCodeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("BarCodeReceiver", intent.getAction());
            String barCode = intent.getStringExtra("data");
            if (barCode != null && !barCode.equals("")) {
                if (barCode.length() == 4 && (barCode.startsWith("C") || barCode.startsWith("S"))) {
                    barCode = "00000" + barCode.substring(1);
                }
                if (continueScanner) {
                    barCodeHandle();
                    PluginResult pr = new PluginResult(PluginResult.Status.OK, barCode);
                    pr.setKeepCallback(true);
                    callback.sendPluginResult(pr);
                } else {
                    callback.success(barCode);
                }
            }

        }
    }

    class TagThread extends Thread {
        public void run() {
            String[] res;
            while (inventoryLoop) {
                res = rfidWithUHF.readTagFromBuffer();
                String strEPC;
                if (res != null) {
                    strEPC = rfidWithUHF.convertUiiToEPC(res[1]);
                    boolean hasEPC = false;
                    for (InventoryBuffer.InventoryTagMap s : inventoryResult.lsTagList)
                    {
                        if (s.strEPC.equals(strEPC)) {
                            hasEPC = true;
                            break;
                        }
                    }
                    if (!hasEPC) {
                        Log.i("data","EPC:"+strEPC);
                        InventoryBuffer.InventoryTagMap inventoryTagMap = new InventoryBuffer.InventoryTagMap();
                        inventoryTagMap.strEPC = strEPC;
                        inventoryResult.lsTagList.add(inventoryTagMap);
                        PluginResult pr = new PluginResult(PluginResult.Status.OK, new Gson().toJson(inventoryResult));
                        pr.setKeepCallback(true);
                        callback.sendPluginResult(pr);
                    }
                }
            }
            Log.i("inventoryResult", "clear");
            inventoryResult = new InventoryBuffer();
        }
    }
}
