package nl.xservices.plugins;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import com.cw.barcodesdk.SoftDecodingAPI;
import com.cw.fpfbbsdk.FingerPrintAPI;
import com.cw.fpfbbsdk.USBFingerManager;
import com.cw.r2000uhfsdk.IOnCommonReceiver;
import com.cw.r2000uhfsdk.IOnInventoryRealReceiver;
import com.cw.r2000uhfsdk.IOnTagOperation;
import com.cw.r2000uhfsdk.R2000UHFAPI;
import com.cw.r2000uhfsdk.base.CMD;
import com.cw.r2000uhfsdk.helper.InventoryBuffer;
import com.cw.r2000uhfsdk.helper.OperateTagBuffer;
import com.google.gson.Gson;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import cn.com.aratek.fp.Bione;
import cn.com.aratek.fp.FingerprintImage;
import cn.com.aratek.fp.FingerprintScanner;
import cn.com.aratek.util.Result;

import java.io.UnsupportedEncodingException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Plugin_U8 implements SoftDecodingAPI.IBarCodeData {

    public R2000UHFAPI r2000UHFAPI;
    public SoftDecodingAPI softDecodingAPI;
    public boolean isScanning = false;
    private CallbackContext callbackContext;
    private USBFingerManager usbFingerMgr;
    private FingerPrintAPI fpApi;
    private FingerprintScanner fpScanner;
    private FingerprintTask fpTask;
    private boolean fpScannerOpened = false;
    private CordovaInterface cordova;
    private Activity context;
    private static final String TAG = "Plugin_U8";


    public Plugin_U8(CordovaInterface cordova) {
        this.cordova = cordova;
        this.context = cordova.getActivity();
        this.r2000UHFAPI = R2000UHFAPI.getInstance();
        this.softDecodingAPI = new SoftDecodingAPI(cordova.getContext(), this);
        this.softDecodingAPI.openBarCodeReceiver();
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if ("openUHF".equals(action)) {
            cordova.getActivity().runOnUiThread(() -> r2000UHFAPI.open(cordova.getContext()));
            return true;
        } else if ("startInventoryReal".equals(action)) {
            cordova.getThreadPool().execute(() -> startInventoryReal(callbackContext));
            PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
            pr.setKeepCallback(true);
            callbackContext.sendPluginResult(pr);
            return true;
        } else if ("stopInventoryReal".equals(action)) {
            cordova.getThreadPool().execute(() -> {
                if (r2000UHFAPI.getReaderHelper() != null && !!r2000UHFAPI.getReaderHelper().getInventoryFlag()) {
                    r2000UHFAPI.stopInventoryReal();
                }
            });
            return true;
        } else if ("closeUHF".equals(action)) {
            cordova.getThreadPool().execute(() -> r2000UHFAPI.close());
            return true;
        } else if ("scan".equals(action)) {
            cordova.getThreadPool().execute(() -> {
                softDecodingAPI.openBarCodeReceiver();
                barCodeScanner(callbackContext);
            });
            return true;
        } else if ("continueScanning".equals(action)) {
            cordova.getThreadPool().execute(() -> {
                softDecodingAPI.openBarCodeReceiver();
                continueScanning(callbackContext);
            });
            PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
            pr.setKeepCallback(true);
            callbackContext.sendPluginResult(pr);
            return true;
        } else if ("closeScanning".equals(action)) {
            cordova.getThreadPool().execute(() -> {
                this.isScanning = false;
                softDecodingAPI.CloseScanning();
                softDecodingAPI.closeBarCodeReceiver();
            });
            return true;
        } else if ("setScanner".equals(action)) {
            callbackContext.error("方法尚未实现！");
            return true;
        } else if ("setScanInterval".equals(action)) {
            softDecodingAPI.setTime(args.getJSONObject(0).getInt("time"));
            return true;
        } else if ("getReaderTemperature".equals(action)) {
            cordova.getThreadPool().execute(() -> {
                r2000UHFAPI.setOnCommonReceiver(new IOnCommonReceiver() {
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
                r2000UHFAPI.getReaderTemperature();
            });
            return true;
        } else if ("killTag".equals(action)) {
            cordova.getThreadPool().execute(() -> {
                try {
                    killTag(callbackContext, args);
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            });
            return true;
        } else if ("lockTag".equals(action)) {
            cordova.getThreadPool().execute(() -> {
                try {
                    lockTag(callbackContext, args);
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            });
            return true;
        } else if ("readTag".equals(action)) {
            cordova.getThreadPool().execute(() -> {
                try {
                    readTag(callbackContext, args);
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            });
            return true;
        } else if ("writeTag".equals(action)) {
            cordova.getThreadPool().execute(() -> {
                try {
                    writeTag(callbackContext, args);
                } catch (Exception e) {
                    callbackContext.error(e.getMessage());
                }
            });
            return true;
        } else if ("reset".equals(action)) {
            r2000UHFAPI.reset();
            return true;
        } else if ("setInventoryDelayMillis".equals(action)) {
            r2000UHFAPI.setInventoryDelayMillis(args.getJSONObject(0).getInt("delayMillis"));
            return true;
        } else if ("setOutputPower".equals(action)) {
            r2000UHFAPI.setOutputPower(args.getJSONObject(0).getInt("mOutPower"));
            return true;
        } else if ("openFingerprint".equals(action)) {
            cordova.getThreadPool().execute(() -> {
                openDevice();
            });
            return true;
        } else if ("closeFingerprint".equals(action)) {
            Log.i(TAG, "exec closeFingerprint....");
            cordova.getThreadPool().execute(() -> {
                Log.i(TAG, "thread execute close device...");
                closeDevice();
            });
        } else if ("verifyFingerprint".equals(action)) {
            loadFpData(args, false);
            identify();
            return true;
        } else if ("loadFpData".equals(action)) {
            loadFpData(args, true);
            return true;
        } else if ("scanFingerprint".equals(action)) {
            enroll();
            return true;
        } else if ("clearFingerprintDB".equals(action)) {
            clearFingerprintDB();
        }
        return true;
    }

    private void startInventoryReal(CallbackContext callbackId) {
        try {
            r2000UHFAPI.startInventoryReal("1");
            r2000UHFAPI.setOnInventoryRealReceiver(new IOnInventoryRealReceiver() {
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
        } catch (Exception e) {
            callbackId.error("超高频模块启动异常：" + e.getMessage());
        }
    }

    private void barCodeScanner(CallbackContext callbackId) {
        isScanning = false;
        callbackContext = callbackId;
        softDecodingAPI.scan();
    }

    private void continueScanning(CallbackContext callbackId) {
        isScanning = true;
        callbackContext = callbackId;
        softDecodingAPI.setTime(800);
        softDecodingAPI.ContinuousScanning();
    }

    private void writeTag(CallbackContext callbackId, JSONArray args) throws JSONException {
        r2000UHFAPI.setOnTagOperation(new IOnTagOperation() {
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
        r2000UHFAPI.writeTag(btMemBank, btWordAdd, btWordCnt, btAryPassWord, data);
    }

    private void readTag(CallbackContext callbackId, JSONArray args) throws JSONException {
        r2000UHFAPI.setOnTagOperation(new IOnTagOperation() {
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
        r2000UHFAPI.readTag(btMemBank, btWordAdd, btWordCnt, btAryPassWord);
    }

    private void killTag(CallbackContext callbackId, JSONArray args) throws JSONException {
        r2000UHFAPI.setOnTagOperation(new IOnTagOperation() {
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
        r2000UHFAPI.killTag(btAryPassWord);
    }

    private void lockTag(CallbackContext callbackId, JSONArray args) throws JSONException {
        r2000UHFAPI.setOnTagOperation(new IOnTagOperation() {
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
        r2000UHFAPI.lockTag(btAryPassWord, btMemBank, btLockType);
    }

    //region GBA指纹识别

    /**
     * 开启指纹识别模块
     */
    private void openDevice() {
        closeDevice();
        usbFingerMgr = USBFingerManager.getInstance(context);
        usbFingerMgr.openUSB(new USBFingerManager.OnUSBFingerListener() {
            @Override
            public void onOpenUSBFingerSuccess(String s, UsbManager usbManager, UsbDevice usbDevice) {
                openFpScanner();
            }

            @Override
            public void onOpenUSBFingerFailure(String s) {
                Log.i(TAG, "open usb finger failure.");
                callbackContext.error("open usb finger failure");
            }
        });
    }

    /**
     * 启用指纹仪及指纹识别相关的功能
     */
    private void openFpScanner() {
        fpScanner = new FingerprintScanner(context);
        fpApi = FingerPrintAPI.getInstance();
        // 指纹扫描仪开启成功
        if ((fpScanner.open() == FingerprintScanner.RESULT_OK) && (fpApi.initialize(context, getFpDbPath()) == Bione.RESULT_OK)) {
            fpScannerOpened = true;
            Log.i(TAG, "fingerprint scanner open success.");
            callbackContext.success("fingerprint scanner open success.");
        } else {
            // 指纹扫描仪开启失败
            String msg = "FpScanner open failed!";
            fpScannerOpened = false;
            Log.e(TAG, "fingerprint scanner open failed.");
            callbackContext.error("fingerprint scanner open failed.");
        }
    }

    /**
     * 关闭指纹识别模块
     */
    private void closeDevice() {
        if (fpTask != null && fpTask.getStatus() != AsyncTask.Status.FINISHED) {
            fpTask.cancel(false);
            fpTask.waitForDone();
        }
        if (usbFingerMgr != null && usbFingerMgr.isUSBFingerOpened()) {
            usbFingerMgr.closeUSB();
        }
        if (fpScanner != null) {
            if (fpScanner.close() == FingerprintScanner.RESULT_OK) {
                fpScannerOpened = false;
                Log.i(TAG, "fingerprint device close success.");
            } else {
                Log.e(TAG, "fingerprint device close failed.");
            }
        }
        if (fpApi != null && fpApi.exit() != Bione.RESULT_OK) {
            Log.e(TAG, "algorithm cleanup failed.");
        }
        Log.i(TAG, "close device success.");
    }

    /**
     * 鉴定指纹
     */
    private void identify() {
        fpTask = new FingerprintTask();
        fpTask.execute("identify");
    }

    /**
     * 录入指纹
     */
    private void enroll() {
        fpTask = new FingerprintTask();
        fpTask.execute("enroll");
    }

    /**
     * 加载指纹数据到数据库
     */
    private void loadFpData(JSONArray args, boolean isCallSuccess) {
        if (fpApi == null) {
            fpApi = FingerPrintAPI.getInstance();
        }
        if (fpApi.initialize(this.cordova.getContext(), getFpDbPath()) == Bione.RESULT_OK) {
            try {
                clearFingerprintDB();
                for (int i = 0; i < args.length(); i++) {
                    String fpTempStr = args.getString(i);
                    int freeID = fpApi.getFreeID();
                    byte[] fpTemp = convertToBytes(fpTempStr);
                    if (fpTemp != null) {
                        if (fpTemp != null && fpTemp.length > 0 && fpApi.enroll(freeID, fpTemp) == Bione.RESULT_OK) {
                            Log.i(TAG, "loadFpData: fingerprint scan success.");
                        } else {
                            Log.e(TAG, "loadFpData: fingerprint scan failed.");
                        }
                    } else {
                        Log.e(TAG, "fingerprint feature value format error");
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
                this.callbackContext.error(e.getMessage());
            }
            if (isCallSuccess) {
                this.callbackContext.success("load fingerprint data success.");
            } else {
                Log.i(TAG, "load fingerprint data success.");
            }
        } else {
            Log.e(TAG, "FingerPrintAPI init failed.");
            this.callbackContext.error("FingerPrintAPI init failed.");
        }

    }

    /**
     * 清空指纹库
     */
    private void clearFingerprintDB() {
        if (fpApi == null) {
            fpApi = FingerPrintAPI.getInstance();
        }
        if (fpApi.initialize(context, getFpDbPath()) == Bione.RESULT_OK) {
            if (fpApi.clear() != Bione.RESULT_OK) {
                Log.e(TAG, "clearFingerprintDB: clear fingerprint database failed.");
            } else {
                Log.i(TAG, "clearFingerprintDB: clear fingerprint database success.");
            }
        } else {
            callbackContext.error("fingerprint init failed.");
        }
    }

    /**
     * 获取指纹库的数据库文件的路径
     *
     * @return
     */
    private String getFpDbPath() {
        return context.getFilesDir().getAbsolutePath() + File.separator + "fp.db";
    }


    /**
     * 处理指纹相关的任务
     */
    public class FingerprintTask extends AsyncTask<String, Integer, Void> {
        private boolean mIsDone = false;
        private Result res;
        private FingerprintImage fpImg;
        private int mId;
        private long extractTime;
        private long generalizeTime;
        private byte[] fpFeat;
        private byte[] fpTemp;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... params) {
            do {
                if ("enroll".equals(params[0]) || "identify".equals(params[0])) {
                    if(fpScanner == null){
                        return null;
                    }
                    // 准备开始指纹录入
                    fpScanner.prepare();
                    int capRetryCount = 0;
                    do {
                        res = fpScanner.capture();
                        fpImg = (FingerprintImage) res.data;
                        if (fpImg != null) {
                            // 获取当前提取指纹的图像质量
                            int quality = fpApi.getFingerprintQuality(fpImg);
                            if (quality < 50 && capRetryCount < 3 && !isCancelled()) {
                                capRetryCount++;
                                continue;
                            }
                        }
                        if (res.error != FingerprintScanner.NO_FINGER || isCancelled()) {
                            break;
                        }
                    } while (true);
                    // 采集指纹结束
                    fpScanner.finish();
                    if (isCancelled()) {
                        break;
                    }
                    if (res.error != FingerprintScanner.RESULT_OK) {
                        Log.i(TAG,"620->"+getFingerprintErrorString(res.error));
                        callbackContext.error("fingerprint scan failed.");
                        break;
                    }

                    long startTime;
                    if (params[0].equals("enroll") || params[0].equals("identify")) {
                        startTime = System.currentTimeMillis();
                        // 提取特征值
                        res = fpApi.extractFeature(fpImg);
                        extractTime = System.currentTimeMillis() - startTime;
                        if (res.error != Bione.RESULT_OK) {
                            callbackContext.error("extract feature failed.");
                            break;
                        }
                        fpFeat = (byte[]) res.data;
                    }
                    // 录入指纹
                    if ("enroll".equals(params[0])) {
                        res = fpApi.makeTemplate(fpFeat, fpFeat, fpFeat);
                        if (res.error != Bione.RESULT_OK) {
                            callbackContext.error("scan fingerprint failed.");
                            break;
                        }
                        fpTemp = (byte[]) res.data;
                        int id = fpApi.getFreeID();
                        if (id < 0) {
                            Log.e(TAG, "647-> "+getFingerprintErrorString(id));
                            callbackContext.error("scan fingerprint failed. info: " + getFingerprintErrorString(id));
                            break;
                        }
                        int ret = fpApi.enroll(id, fpTemp);
                        if (ret != Bione.RESULT_OK) {
                            callbackContext.error("scan fingerprint failed.");
                            break;
                        } else {
                            String fpTempStr = convertToStr(fpTemp);
                            callbackContext.success(fpTempStr);
                        }
                        mId = id;
                    }

                    if ("identify".equals(params[0])) {
                        int id = fpApi.identify(fpFeat);
                        if (id < 0) {
                            callbackContext.error("identify fingerprint failed.");
                            break;
                        }
                        Result feature = fpApi.getFeature(id);
                        byte[] identifiedFp = (byte[]) feature.data;
                        callbackContext.success(convertToStr(identifiedFp));
                    }
                }
            } while (false);
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        private void waitForDone() {
            while (!mIsDone) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //endregion

    @Override
    public void sendScan() {

    }

    @Override
    public void onBarCodeData(String s) {
        LOG.i("onBarCodeData", s);
        PluginResult.Status status;
        if (s == null || s.isEmpty() || s.equals("null") || s.contains("No decoded message available.")) {
//                    status = PluginResult.Status.ERROR;
            return;
        } else {
            status = PluginResult.Status.OK;
        }
        String result = s.replaceAll("\r|\n", "").trim();
        if (result.length() == 4 && (result.startsWith("C") || result.startsWith("S"))) {
            result = "00000" + result.substring(1);
        }
        if (isScanning) {
            PluginResult pr = new PluginResult(status, result);
            pr.setKeepCallback(true);
            callbackContext.sendPluginResult(pr);
        } else {
            callbackContext.success(result);
        }
    }

    @Override
    public void getSettings(int i, int i1, int i2, String s, String s1, int i3, int i4) {

    }

    @Override
    public void setSettingsSuccess() {

    }


    //region 生命周期事件处理
    public void onRestart() {
    }

    public void onStop() {
    }

    public void onDestroy() {
        r2000UHFAPI.close();
        closeDevice();
    }
    //endregion

    //region 工具方法

    /**
     * 根据错误码获取具体的错误信息
     *
     * @param error
     * @return
     */
    private String getFingerprintErrorString(int error) {
        int strid;
        switch (error) {
            case FingerprintScanner.RESULT_OK:
                return "operation_successful";
            case FingerprintScanner.RESULT_FAIL:
                return "error_operation_failed";
            case FingerprintScanner.WRONG_CONNECTION:
                return "error_wrong_connection";
            case FingerprintScanner.DEVICE_BUSY:
                return "error_device_busy";
            case FingerprintScanner.DEVICE_NOT_OPEN:
                return "error_device_not_open";
            case FingerprintScanner.TIMEOUT:
                return "error_timeout";
            case FingerprintScanner.NO_PERMISSION:
                return "error_no_permission";
            case FingerprintScanner.WRONG_PARAMETER:
                return "error_wrong_parameter";
            case FingerprintScanner.DECODE_ERROR:
                return "error_decode";
            case FingerprintScanner.INIT_FAIL:
                return "error_initialization_failed";
            case FingerprintScanner.UNKNOWN_ERROR:
                return "error_unknown";
            case FingerprintScanner.NOT_SUPPORT:
                return "error_not_support";
            case FingerprintScanner.NOT_ENOUGH_MEMORY:
                return "error_not_enough_memory";
            case FingerprintScanner.DEVICE_NOT_FOUND:
                return "error_device_not_found";
            case FingerprintScanner.DEVICE_REOPEN:
                return "error_device_reopen";
            case FingerprintScanner.NO_FINGER:
                return "error_no_finger";
            case Bione.INITIALIZE_ERROR:
                return "error_algorithm_initialization_failed";
            case Bione.INVALID_FEATURE_DATA:
                return "error_invalid_feature_data";
            case Bione.BAD_IMAGE:
                return "error_bad_image";
            case Bione.NOT_MATCH:
                return "error_not_match";
            case Bione.LOW_POINT:
                return "error_low_point";
            case Bione.NO_RESULT:
                return "error_no_result";
            case Bione.OUT_OF_BOUND:
                return "error_out_of_bound";
            case Bione.DATABASE_FULL:
                return "error_database_full";
            case Bione.LIBRARY_MISSING:
                return "error_library_missing";
            case Bione.UNINITIALIZE:
                return "error_algorithm_uninitialize";
            case Bione.REINITIALIZE:
                return "error_algorithm_reinitialize";
            case Bione.REPEATED_ENROLL:
                return "error_repeated_enroll";
            case Bione.NOT_ENROLLED:
                return "error_not_enrolled";
            default:
                return "error_other";
        }
    }

    public String convertToStr(byte[] fpTempBytes) {
        StringBuffer sb = new StringBuffer();
        for (byte aByte : fpTempBytes) {
            sb.append(aByte);
            sb.append(" ");
        }
        return sb.toString();
    }

    public byte[] convertToBytes(String strBytes) {
        String[] strByteArray = strBytes.split(" ");
        int length = strByteArray.length;
        byte[] bytes = new byte[length];
        try {
            for (int i = 0; i < strByteArray.length; i++) {
                bytes[i] = Byte.parseByte(strByteArray[i]);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return bytes;
    }

    /**
     * 延时
     *
     * @param time
     */
    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (Exception e) {
            e.toString();
        }
    }
    //endregion
}