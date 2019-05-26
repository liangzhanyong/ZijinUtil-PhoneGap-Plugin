package nl.xservices.plugins;

import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.cw.cwsdk.cw;
import com.cw.cwsdk.u8API.barcode.BarCodeAPI;
import com.cw.cwsdk.u8API.idcard.ParseSFZAPI;
import com.cw.cwsdk.u8API.uhf.IOnCommonReceiver;
import com.cw.cwsdk.u8API.uhf.IOnInventoryRealReceiver;
import com.cw.cwsdk.u8API.uhf.IOnTagOperation;
import com.cw.cwsdk.u8API.uhf.base.CMD;
import com.cw.cwsdk.u8API.uhf.helper.InventoryBuffer;
import com.cw.cwsdk.u8API.uhf.helper.OperateTagBuffer;
import com.google.gson.Gson;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cn.com.aratek.fp.Bione;
import cn.com.aratek.fp.FingerprintImage;
import cn.com.aratek.fp.FingerprintScanner;
import cn.com.aratek.util.Result;

public class Plugin_U8 {
    private static final boolean IS_AT_LEAST_LOLLIPOP = Build.VERSION.SDK_INT >= 21;
    private static final String TAG = "Plugin_U8";
    private static final String FP_DB_PATH = "/sdcard/fp.db";
    CordovaInterface cordova;
    private FingerprintScanner mScanner;
    private FingerprintTask mTask;

    public Plugin_U8(CordovaInterface cordova) {
        this.cordova = cordova;
        mScanner = cw.FingerPrintAPI().Scanner(cordova.getContext());
        cw.FingerPrintAPI().openUSB();
    }

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
            PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
            pr.setKeepCallback(true);
            callbackContext.sendPluginResult(pr);
            return true;
        }
        else if(action.equals("stopInventoryReal")) {
//            cordova.getActivity().runOnUiThread(new Runnable() {
            cordova.getThreadPool().execute(() -> {
                if(cw.R2000UHFAPI().getReaderHelper() != null && !!cw.R2000UHFAPI().getReaderHelper().getInventoryFlag()) {
                    cw.R2000UHFAPI().stopInventoryReal();
                }
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
            PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
            pr.setKeepCallback(true);
            callbackContext.sendPluginResult(pr);
            return true;
        }
        else if(action.equals("closeScanning")) {
            cordova.getThreadPool().execute(() -> cw.BarCodeAPI(cordova.getContext()).CloseScanning());
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
        else if(action.equals("openFingerprint")) {
            openDevice();
        }
        else if(action.equals("closeFingerprint")) {
            closeDevice();
        }
        else if(action.equals("verifyFingerprint")) {
            cordova.getThreadPool().execute(() -> {
                mTask = new FingerprintTask();
                mTask.execute("verify");
            });
            return true;
        }
        else if(action.equals("scanFingerprint")) {
            cordova.getThreadPool().execute(() -> {
                mTask = new FingerprintTask();
                mTask.execute("enroll");
            });
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
                    callbackId.success(s.replaceAll("\r|\n", ""));
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
                LOG.i("onBarCodeData", s);
                PluginResult.Status status;
                if(s.isEmpty() || s.contains("No decoded message available.")) {
//                    status = PluginResult.Status.ERROR;
                    return;
                } else {
                    status = PluginResult.Status.OK;
                }
                String result = s.replaceAll("\r|\n", "");
                if (result.length() == 4 && (result.startsWith("C") || result.startsWith("S"))) {
                    result = "00000" + result.substring(1);
                }
                PluginResult pr = new PluginResult(status, result);
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

    private void openDevice() {
        new Thread() {
            @Override
            public void run() {
                synchronized (cordova.getContext()) {
                    if (mScanner.open() != FingerprintScanner.RESULT_OK) {
                        //Toast.makeText(FingerprintActivity.this, "------"+error, Toast.LENGTH_SHORT).show();
                    } else {
                    }
                    if (cw.FingerPrintAPI().initialize(cordova.getContext(), FP_DB_PATH) != Bione.RESULT_OK) {
                    }

                    Log.i(TAG, "Fingerprint algorithm version: " + cw.FingerPrintAPI().getVersion());
                }
            }
        }.start();
    }

    private void closeDevice() {
        new Thread() {
            @Override
            public void run() {
                synchronized (cordova.getContext()) {
                    if (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED) {
                        mTask.cancel(false);
                        mTask.waitForDone();
                    }
                    if (mScanner.close() != FingerprintScanner.RESULT_OK) {
                    } else {
                    }
                    if (cw.FingerPrintAPI().exit() != Bione.RESULT_OK) {
                    }
                }
            }
        }.start();
    }

    public void onDestroy() {
        cw.R2000UHFAPI().close();
        cw.FingerPrintAPI().closeUSB();
    }

    private class FingerprintTask extends AsyncTask<String, Integer, Void> {
        private boolean mIsDone = false;

        /**
         * 这个方法是在执行异步任务之前的时候执行，并且是在UI Thread当中执行的，通常我们在这个方法里做一些UI控件的初始化的操作，例如弹出要给ProgressDialog
         */
        @Override
        protected void onPreExecute() {
        }

        /**
         * 处理异步任务的方法
         *
         * @param params
         * @return
         */
        @Override
        protected Void doInBackground(String... params) {
            FingerprintImage fi = null;
            byte[] fpFeat = null, fpTemp = null;
            Result res;

            do {
                if (params[0].equals("enroll") || params[0].equals("verify")) {
                    int capRetry = 0;
                    mScanner.prepare();
                    do {
                        res = mScanner.capture();
                        fi = (FingerprintImage) res.data;
                        int quality;
                        if (fi != null) {
                            quality = cw.FingerPrintAPI().getFingerprintQuality(fi);
                            Log.i(TAG, "Fingerprint image quality is " + quality);
                            if (quality < 50 && capRetry < 3 && !isCancelled()) {
                                capRetry++;
                                continue;
                            }
                        }

                        if (res.error != FingerprintScanner.NO_FINGER || isCancelled()) {
                            break;
                        }

                    } while (true);
                    mScanner.finish();

                    if (isCancelled()) {
                        break;
                    }

                    if (res.error != FingerprintScanner.RESULT_OK) {
                        break;
                    }

                }

                if (params[0].equals("enroll") || params[0].equals("verify")) {
                    res = cw.FingerPrintAPI().extractFeature(fi);
                    if (res.error != Bione.RESULT_OK) {
                        break;
                    }
                    fpFeat = (byte[]) res.data;
                }

                if (params[0].equals("enroll")) {//注册
                    res = cw.FingerPrintAPI().makeTemplate(fpFeat, fpFeat, fpFeat);
                    if (res.error != Bione.RESULT_OK) {
                        break;
                    }
                    fpTemp = (byte[]) res.data;
                    Log.i(TAG, String.valueOf(fpTemp));
                    int id = cw.FingerPrintAPI().getFreeID();
                    if (id < 0) {
                        break;
                    }
                    int ret = cw.FingerPrintAPI().enroll(id, fpTemp);
                    if (ret != Bione.RESULT_OK) {
                        break;
                    }
                } else if (params[0].equals("verify")) {//比对
                    res = cw.FingerPrintAPI().verify(fpTemp, fpFeat);
                    if (res.error != Bione.RESULT_OK) {
                        break;
                    }
                    if ((Boolean) res.data) {
                    } else {
                    }
                }
            } while (false);

            mIsDone = true;
            return null;
        }

        /**
         * 当我们的异步任务执行完之后，就会将结果返回给这个方法，这个方法也是在UI Thread当中调用的，我们可以将返回的结果显示在UI控件上
         *
         * @param result
         */
        @Override
        protected void onPostExecute(Void result) {
        }

        /**
         * 这个方法也是在UI Thread当中执行的，我们在异步任务执行的时候，有时候需要将执行的进度返回给我们的UI界面
         *
         * @param values
         */
        @Override
        protected void onProgressUpdate(Integer... values) {

        }

        @Override
        protected void onCancelled() {

        }

        public void waitForDone() {
            while (!mIsDone) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
