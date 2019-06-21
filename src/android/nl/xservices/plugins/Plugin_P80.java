package nl.xservices.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.barcode.BarcodeUtility;
import com.cw.r2000uhfsdk.helper.InventoryBuffer;
import com.google.gson.Gson;
import com.rscja.deviceapi.Fingerprint;
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
    private static final String TAG = "Plugin_P80";

    private CordovaInterface cordova;
    private BarcodeUtility barcodeUtility;
    private RFIDWithUHF rfidWithUHF;
    private CallbackContext callback;
    private boolean continueScanner = false;
    private BarCodeReceiver receiver = null;
    private Timer timer = null;
    private boolean inventoryLoop = false;
    private InventoryBuffer inventoryResult = new InventoryBuffer();

    private String[] verifyList = {};
    public Fingerprint mFingerprint;
    private boolean fpOpened = false;

    public Plugin_P80(CordovaInterface cordova, BarcodeUtility barcodeUtility, RFIDWithUHF rfidWithUHF) {
        this.cordova = cordova;
        this.barcodeUtility = barcodeUtility;
        this.rfidWithUHF = rfidWithUHF;
        try {
            mFingerprint = Fingerprint.getInstance();
        } catch (Exception ex) {
            Toast.makeText(cordova.getContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("openUHF")) {
            cordova.getThreadPool().execute(() -> rfidWithUHF.init());
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
            cordova.getThreadPool().execute(() -> {
                Integer power;
                try {
                    JSONObject params = args.getJSONObject(0);
                    power = params.getInt("mOutPower");
                    rfidWithUHF.setPower(power);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
            return true;
        }
        else if(action.equals("openFingerprint")) {
            cordova.getThreadPool().execute(() -> {
                if(fpOpened || (mFingerprint != null && mFingerprint.init())) {
                    fpOpened = true;
                    callbackContext.success();
                } else {
                    callbackContext.error("指纹仪启动失败!");
                }
            });
            return true;
        }
        else if(action.equals("closeFingerprint")) {
            cordova.getThreadPool().execute(() -> freeFingerprint());
            return true;
        }
        else if(action.equals("verifyFingerprint")) {
            cordova.getThreadPool().execute(() -> {
                JSONObject params;
                try {
                    params = args.getJSONObject(0);
                    verifyList = params.getString("chars").split("\\$");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                this.callback = callbackContext;
                IdentTask asyncTask_search = new IdentTask();
                asyncTask_search.execute(1);
            });
            return true;
        }
        else if(action.equals("scanFingerprint")) {
            cordova.getThreadPool().execute(() -> {
                this.callback = callbackContext;
                new AcqTask().execute(1);
            });
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
        barcodeUtility.enableContinuousScan(cordova.getContext(),false);
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

    public void freeFingerprint() {
        if (mFingerprint != null) {
            mFingerprint.free();
        }
        fpOpened = false;
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
                    Log.i(TAG, "continue scan");
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
            Log.i(TAG, "BarCodeReceiver" + intent.getAction());
            String barCode = intent.getStringExtra("data");
            if (barCode != null && !barCode.equals("")) {
                barCode = barCode.replaceAll("\r|\n", "");
                if (barCode.length() == 4 && (barCode.startsWith("C") || barCode.startsWith("S"))) {
                    barCode = "00000" + barCode.substring(1);
                }
                if (continueScanner) {
                    barcodeUtility.startScan(cordova.getContext(), BarcodeUtility.ModuleType.AUTOMATIC_ADAPTATION);
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
                        Log.i(TAG,"EPC data:"+strEPC);
                        InventoryBuffer.InventoryTagMap inventoryTagMap = new InventoryBuffer.InventoryTagMap();
                        inventoryTagMap.strEPC = strEPC;
                        inventoryResult.lsTagList.add(inventoryTagMap);
                        PluginResult pr = new PluginResult(PluginResult.Status.OK, new Gson().toJson(inventoryResult));
                        pr.setKeepCallback(true);
                        callback.sendPluginResult(pr);
                    }
                }
            }
            Log.i(TAG, "inventoryResult clear");
            inventoryResult = new InventoryBuffer();
        }
    }

    class AcqTask extends AsyncTask<Integer, Integer, String> {

        String data;

        public AcqTask() { }

        @Override
        protected String doInBackground(Integer... params) {

            boolean exeSucc = false;
            if(!fpOpened) {
                return null;
            }
            // 采集指纹
            while (!mFingerprint.getImage()) {
                Log.i(TAG, "请按下指纹");
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // 生成特征值到B1
            if (mFingerprint.genChar(Fingerprint.BufferEnum.B1)) {
                exeSucc = true;
            }

            // 再次采集指纹
            while (!mFingerprint.getImage()) {
                Log.i(TAG, "请按下指纹");
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // 生成特征值到B2
            if (mFingerprint.genChar(Fingerprint.BufferEnum.B2)) {
                exeSucc = true;
            }

            // 合并两个缓冲区到B1
            if (mFingerprint.regModel()) {
                exeSucc = true;
            }

            if (exeSucc) {
//                if (mFingerprint.storChar(Fingerprint.BufferEnum.B1, 1)) {
                    data = mFingerprint.upChar(Fingerprint.BufferEnum.B1);
                    Log.i(TAG, "采集特征值:"+data);
                    return "ok";
//                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (TextUtils.isEmpty(result)) {
                callback.error("指纹采集失败!");
                return;
            }
            callback.success(data);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

    }

    class IdentTask extends AsyncTask<Integer, Integer, String> {
        String data;
        public IdentTask() { }

        @Override
        protected String doInBackground(Integer... params) {
            if(!fpOpened) {
                return null;
            }
            while (mFingerprint.isPowerOn() && !mFingerprint.getImage()) {
                Log.i(TAG, "请按下指纹");
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (mFingerprint.genChar(Fingerprint.BufferEnum.B1)) {
                int[] result;
                int exeCount = 0;

                do {
                    exeCount++;
                    result = mFingerprint
                            .search(Fingerprint.BufferEnum.B1, 0, 1000);

                } while (result == null && exeCount < 3);

                Log.i(TAG, "exeCount=" + exeCount);

                if (result != null) {
                    Log.i(TAG, "匹配特征值ID："+result[0]);
                    data = verifyList[result[0]];
                    return "ok";
                } else {
                    Log.i(TAG, "search result Empty");
                    return null;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (TextUtils.isEmpty(s)) {
                callback.error("比对失败!");
                return;
            }
            callback.success(data);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mFingerprint.empty();
            // verifyList为从服务端获取的一组指纹特征
            for (int i = 0; i < verifyList.length; i++) {
                mFingerprint.downChar(Fingerprint.BufferEnum.B1, verifyList[i]);
                mFingerprint.storChar(Fingerprint.BufferEnum.B1, i);
            }
        }
    }

}
