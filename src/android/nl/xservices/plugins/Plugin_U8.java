package nl.xservices.plugins;

import android.annotation.SuppressLint;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.cw.barcodesdk.SoftDecodingAPI;
import com.cw.fpjrasdk.JRA_API;
import com.cw.fpjrasdk.USBFingerManager;
import com.cw.r2000uhfsdk.IOnCommonReceiver;
import com.cw.r2000uhfsdk.IOnInventoryRealReceiver;
import com.cw.r2000uhfsdk.IOnTagOperation;
import com.cw.r2000uhfsdk.R2000UHFAPI;
import com.cw.r2000uhfsdk.base.CMD;
import com.cw.r2000uhfsdk.helper.InventoryBuffer;
import com.cw.r2000uhfsdk.helper.OperateTagBuffer;
import com.cw.serialportsdk.utils.DataUtils;
import com.google.gson.Gson;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;

import static com.cw.fpjrasdk.syno_usb.OTG_KEY.PS_OK;

public class Plugin_U8 implements SoftDecodingAPI.IBarCodeData {
    private static final String TAG = "Plugin_U8";
    private static final String FILE_NAME = "fingerData.txt";

    private CallbackContext callbackContext;
    public R2000UHFAPI r2000UHFAPI;

    public SoftDecodingAPI softDecodingAPI;
    public boolean isScanning = false;

    public JRA_API jraApi;
    public boolean fpOpened = false;
    private static int fingerCnt = 1;
    private HashMap<String, String> fingerMap;

    CordovaInterface cordova;

    public Plugin_U8(CordovaInterface cordova) {
        this.cordova = cordova;
        this.r2000UHFAPI = R2000UHFAPI.getInstance();
        this.softDecodingAPI = new SoftDecodingAPI(cordova.getContext(), this);
        this.softDecodingAPI.openBarCodeReceiver();
        USBFingerManager.getInstance(cordova.getContext()).setDelayMs(500);
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("openUHF")) {
            cordova.getActivity().runOnUiThread(() -> r2000UHFAPI.open(cordova.getContext()));
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
                if(r2000UHFAPI.getReaderHelper() != null && !!r2000UHFAPI.getReaderHelper().getInventoryFlag()) {
                    r2000UHFAPI.stopInventoryReal();
                }
            });
            return true;
        }
        else if(action.equals("closeUHF")) {
            cordova.getThreadPool().execute(() -> r2000UHFAPI.close());
            return true;
        }
        else if(action.equals("scan")) {
            cordova.getThreadPool().execute(() -> {
                softDecodingAPI.openBarCodeReceiver();
                barCodeScanner(callbackContext);
            });
            return true;
        }
        else if(action.equals("continueScanning")) {
            cordova.getThreadPool().execute(() -> {
                softDecodingAPI.openBarCodeReceiver();
                continueScanning(callbackContext);
            });
            PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
            pr.setKeepCallback(true);
            callbackContext.sendPluginResult(pr);
            return true;
        }
        else if(action.equals("closeScanning")) {
            cordova.getThreadPool().execute(() -> {
                this.isScanning = false;
                softDecodingAPI.CloseScanning();
                softDecodingAPI.closeBarCodeReceiver();
            });
            return true;
        }
        else if(action.equals("setScanner")) {
            callbackContext.error("方法尚未实现！");
            return true;
        }
        else if(action.equals("setScanInterval")) {
            softDecodingAPI.setTime(args.getJSONObject(0).getInt("time"));
            return true;
        }
        else if(action.equals("getReaderTemperature")) {
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
            r2000UHFAPI.reset();
            return true;
        }
        else if(action.equals("setInventoryDelayMillis")) {
            r2000UHFAPI.setInventoryDelayMillis(args.getJSONObject(0).getInt("delayMillis"));
            return true;
        }
        else if(action.equals("setOutputPower")) {
            r2000UHFAPI.setOutputPower(args.getJSONObject(0).getInt("mOutPower"));
            return true;
        }
        else if(action.equals("openFingerprint")) {
            cordova.getThreadPool().execute(() -> {
                this.callbackContext = callbackContext;
                openDevice();
            });
            return true;
        }
        else if(action.equals("closeFingerprint")) {
            cordova.getThreadPool().execute(() -> {
                closeDevice();
            });
        }
        else if(action.equals("verifyFingerprint")) {
            cordova.getThreadPool().execute(() -> {
                this.callbackContext = callbackContext;
                SearchAsyncTask asyncTask_search = new SearchAsyncTask();
                asyncTask_search.execute(1);
            });
            return true;
        }
        else if(action.equals("loadFpData")) {
            cordova.getThreadPool().execute(() -> {
                JSONObject params;
                String[] verifyList;

                try {
                    params = args.getJSONObject(0);
                    verifyList = params.getString("chars").split("\\$");
                    if(fpOpened == false) {
                        callbackContext.error("指纹仪未打开");
                        return;
                    }
                    if(jraApi != null && jraApi.PSEmpty() != PS_OK) {
                        Log.w(TAG, "指纹库清空异常!");
                        callbackContext.error("指纹库初始化异常");
                        return;
                    }
                    String fileData = "";
                    if (fingerMap == null) {
                        fingerMap = new HashMap<>();
                    }
                    for (int i = 0; i < verifyList.length; i++) {
                        if(verifyList[i].length() != 1024) {
                            continue;
                        }
                        int[] id = new int[1];
                        if(jraApi.PSDownCharToJRA(DataUtils.hexStringTobyte(verifyList[i]), id) != PS_OK) {
                            Log.w(TAG, "存储模板失败!");
                        } else {
                            fileData += String.valueOf(id[0]) + "$" + verifyList[i] + "&";
                            fingerMap.put(String.valueOf(id[0]), verifyList[i]);
                        }
                    }
                    for (int i : jraApi.getUserId()) {
                        Log.i(TAG, String.valueOf(i));
                    }
                    FileWRTool.writeFile(this.cordova.getContext(), FILE_NAME, fileData);
                    callbackContext.success();
                } catch (JSONException e) {
                    e.printStackTrace();
                    callbackContext.error("指纹库初始化异常");
                }
            });
            return true;
        }
        else if(action.equals("scanFingerprint")) {
            cordova.getThreadPool().execute(() -> {
                this.callbackContext = callbackContext;
                ImputAsyncTask asyncTask = new ImputAsyncTask();
                asyncTask.execute(1);
            });
            return true;
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
        } catch(Exception e) {
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

    public void openDevice() {
//        if (fpOpened) {
//            callbackContext.success();
//            return;
//        }
        closeDevice();
        USBFingerManager.getInstance(cordova.getContext()).openUSB(new USBFingerManager.OnUSBFingerListener() {
            @Override
            public void onOpenUSBFingerSuccess(String s, UsbManager usbManager, UsbDevice usbDevice) {
                if (s.equals(USBFingerManager.BYD_SMALL_DEVICE)) {
                    Log.i(TAG, "切换USB成功");

                    jraApi = new JRA_API(usbManager, usbDevice);
                    int ret = jraApi.openJRA();
                    if (ret == JRA_API.DEVICE_SUCCESS) {
                        Log.e(TAG, "open device success!");
                        fpOpened = true;
                        callbackContext.success();
                    } else if (ret == JRA_API.PS_DEVICE_NOT_FOUND) {
                        callbackContext.error("can't find this device!");
                    } else if (ret == JRA_API.PS_EXCEPTION) {
                        callbackContext.error("open device fail");
                    }

                }
            }

            @Override
            public void onOpenUSBFingerFailure(String s) {
                callbackContext.error(s);
            }
        });
    }

    /**
     * 关闭指纹设备
     */
    public void closeDevice() {
        try {
            if (jraApi != null) {
                jraApi.closeJRA();
            }
            Log.e(TAG, "Device Closed");
            fpOpened = false;
            USBFingerManager.getInstance(cordova.getContext()).closeUSB();
        } catch (Exception e) {
            Log.i(TAG, "Exception: => " + e.toString());
        }
    }

    public void onDestroy() {
        r2000UHFAPI.close();
        closeDevice();
    }

    @Override
    public void sendScan() {

    }

    @Override
    public void onBarCodeData(String s) {
        LOG.i("onBarCodeData", s);
        PluginResult.Status status;
        if(s == null || s.isEmpty() || s.equals("null") || s.contains("No decoded message available.")) {
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

    /**
     * 采集指纹
     */
    @SuppressLint("StaticFieldLeak")
    private class ImputAsyncTask extends AsyncTask<Integer, String, Integer> {
        Toast toast;
        @Override
        protected Integer doInBackground(Integer... params) {
            int cnt = 1;
            long startTime = new Date().getTime(), timeout = 2 * 60 * 1000;

            while (true) {
                if (fpOpened == false) {
                    return -1;
                }
                if (new Date().getTime() - startTime > timeout) {
                    callbackContext.error(new Gson().toJson(new ErrorResult(ERROR_CODE.TIMEOUT, "指纹录入超时!")));
                    return -1;
                }
                while (jraApi.PSGetImage() != JRA_API.PS_NO_FINGER) {
                    if (fpOpened == false) {
                        return -1;
                    }
                    if (new Date().getTime() - startTime > timeout) {
                        callbackContext.error(new Gson().toJson(new ErrorResult(ERROR_CODE.TIMEOUT, "指纹录入超时!")));
                        return -1;
                    }
                    sleep(200);
                    publishProgress("请离开手指!");
                }
                while (jraApi.PSGetImage() == JRA_API.PS_NO_FINGER) {
                    if(fpOpened == false) {
                        return -1;
                    }
                    if (new Date().getTime() - startTime > timeout) {
                        callbackContext.error(new Gson().toJson(new ErrorResult(ERROR_CODE.TIMEOUT, "指纹录入超时!")));
                        return -1;
                    }
                    sleep(200);
                    if (cnt == 1) {
                        publishProgress("请按压手指!");
                    } else {
                        publishProgress("请再次按压手指!");
                    }
                }
                Log.i(TAG, "-----开始采集-----");
                if(cnt == 1) {
                    if(jraApi.PSGenChar(JRA_API.CHAR_BUFFER_A) != JRA_API.PS_OK) {
                        cnt--;
                    }
                }
                if(cnt == 2) {
                    if(jraApi.PSGenChar(JRA_API.CHAR_BUFFER_B) != JRA_API.PS_OK) {
                        continue;
                    }
                    if(jraApi.PSRegModule() != JRA_API.PS_OK) {
                        publishProgress("生成模板失败，请重新录入");
                        return -1;
                    }
                    int[] fingerId = new int[1];
                    byte[] g_TempData = new byte[512];

                    if(jraApi.PSStoreChar(fingerId, g_TempData) != JRA_API.PS_OK) {
                        publishProgress("存储特征失败，请重新录入");
                        return -1;
                    }
                    Log.e(TAG, "特征值: " + DataUtils.bytesToHexString(g_TempData));
                    callbackContext.success(DataUtils.bytesToHexString(g_TempData));
                    publishProgress("OK");
                    return 0;
                }
                cnt++;
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            if ("OK".equals(values[0])) {
                toast.cancel();
                return;
            }
            if(toast != null) {
                toast.cancel();
            }
            toast = Toast.makeText(cordova.getContext(), values[0], Toast.LENGTH_LONG);
            toast.show();
            Log.i(TAG, values[0]);
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (0 == result) {
                if (fingerCnt > 256) {
                    Log.i(TAG, "fingerCnt > 256");
                    return;
                }
                fingerCnt++;
                Log.i(TAG, "Enroll Success fingerCnt = " + fingerCnt);
                return;
            } else {
                Log.i(TAG, "Enroll Error " + result);
                callbackContext.error("Sy Enroll:" + result);
                return;
            }
        }

        @Override
        protected void onPreExecute() {
            Log.i(TAG, "Please press finger...");
            return;
        }
    }

    /**
     * 搜索指纹
     */
    private class SearchAsyncTask extends AsyncTask<Integer, String, Integer> {
        int exeCount = 0;

        @Override
        protected Integer doInBackground(Integer... params) {
            int[] fingerId = new int[1];
            long startTime = new Date().getTime(), timeout = 60 * 1000;
            try {
                while (true) {
                    if (new Date().getTime() - startTime > timeout) {
                        callbackContext.error(new Gson().toJson(new ErrorResult(ERROR_CODE.TIMEOUT, "指纹搜索超时!")));
                        return -1;
                    }
                    if (fpOpened == false) {
                        callbackContext.error(new Gson().toJson(new ErrorResult(ERROR_CODE.UNUSED, "指纹设备未打开!")));
                        return -1;
                    }
                    if (exeCount > 2) {
                        callbackContext.error(new Gson().toJson(new ErrorResult(ERROR_CODE.UNFIND, "未找到该指纹!")));
                        return -1;
                    }
                    while (jraApi.PSGetImage() == JRA_API.PS_NO_FINGER) {
                        if (new Date().getTime() - startTime > timeout) {
                            callbackContext.error(new Gson().toJson(new ErrorResult(ERROR_CODE.TIMEOUT, "指纹搜索超时!")));
                            return -1;
                        }
                        if (fpOpened == false) {
                            callbackContext.error(new Gson().toJson(new ErrorResult(ERROR_CODE.UNUSED, "指纹设备未打开!")));
                            return -1;
                        }
                        sleep(20);
                    }
                    if (jraApi.PSGenChar(JRA_API.CHAR_BUFFER_A) != JRA_API.PS_OK) {
                        continue;
                    }
                    if (PS_OK != jraApi.PSSearch(JRA_API.CHAR_BUFFER_A, fingerId)) {
                        exeCount++;
                        publishProgress("没有找到该指纹");
                        continue;
                    }

                    if (fingerMap == null) {
                        String fingerData = FileWRTool.readFile(cordova.getContext(), FILE_NAME);
                        String[] fingerList = fingerData.split("&");
                        fingerMap = new HashMap<>();
                        for (int i = 0; i < fingerList.length; i++) {
                            String[] fingerItem = fingerList[i].split("\\$");
                            fingerMap.put(fingerItem[0], fingerItem[1]);
                        }
                    }
                    Log.i(TAG, "匹配指纹特征:[" + fingerId[0] + "][" + fingerMap.get(String.valueOf(fingerId[0])) + "]");
                    callbackContext.success(fingerMap.get(String.valueOf(fingerId[0])));
                    return 0;
                }
            } catch (Exception e) {
                callbackContext.error(new Gson().toJson(new ErrorResult(ERROR_CODE.EXCEPTION, e.getMessage())));
                return -1;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            if(result != 0) {
//                callbackContext.error("比对失败");
            }
            return;
        }

        @Override
        protected void onPreExecute() {
            Log.e(TAG, "Start Search, Please press finger");
//            if(msyUsbKey != null && msyUsbKey.SyClear() != 0) {
//                Log.w(TAG, "指纹库清空异常!");
//                callbackContext.error("指纹库初始化异常");
//                return;
//            }
//            int pageId = 0;
//            for (String s : verifyList) {
//                if(msyUsbKey.SyDownChar(pageId++, DataUtils.hexStringTobyte(s)) != 0) {
//                    Log.w(TAG, "指纹库初始化异常!");
//                }
//            }
            return;
        }
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

    public class ERROR_CODE {
        public static final int TIMEOUT = 94;
        public static final int UNFIND = 97;
        public static final int UNUSED = 98;
        public static final int EXCEPTION = 99;
    }

    public class ErrorResult {
        private int errorCode;
        private String errorMsg;

        public ErrorResult(int errorCode, String errorMsg) {
            this.errorCode = errorCode;
            this.errorMsg = errorMsg;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(int errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

        public void setErrorMsg(String errorMsg) {
            this.errorMsg = errorMsg;
        }
    }
}
