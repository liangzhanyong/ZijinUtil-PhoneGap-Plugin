package nl.xservices.plugins;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.cw.barcodesdk.SoftDecodingAPI;
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
import com.synochip.sdk.ukey.SyOTG_Key;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Plugin_U8 implements SoftDecodingAPI.IBarCodeData {
    private static final String TAG = "Plugin_U8";
    private static int IMAGE_X = 256;
    private static int IMAGE_Y = 288;

    private CallbackContext callbackContext;
    public R2000UHFAPI r2000UHFAPI;

    public SoftDecodingAPI softDecodingAPI;
    public boolean isScanning = false;

    private SyOTG_Key msyUsbKey;
    private boolean fpOpened = false;
    private static final int PS_NO_FINGER = 0x02;
    private static final int PS_OK = 0x00;
    private static int fingerCnt = 1;
    byte[] fingerBuf = new byte[IMAGE_X * IMAGE_Y];
    byte[] g_TempData = new byte[512];
    String[] verifyList = {};

    CordovaInterface cordova;

    public Plugin_U8(CordovaInterface cordova) {
        this.cordova = cordova;
        this.r2000UHFAPI = R2000UHFAPI.getInstance();
        this.softDecodingAPI = new SoftDecodingAPI(cordova.getContext(), this);
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
            cordova.getThreadPool().execute(() -> {
                this.isScanning = false;
                softDecodingAPI.CloseScanning();
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
                JSONObject params;
                try {
                    params = args.getJSONObject(0);
                    verifyList = params.getString("chars").split("\\$");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                this.callbackContext = callbackContext;
                SearchAsyncTask asyncTask_search = new SearchAsyncTask();
                asyncTask_search.execute(1);
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

    private void openDevice() {
        USBFingerManager.getInstance(cordova.getContext()).openUSB(new USBFingerManager.OnUSBFingerListener() {
            @Override
            public void onOpenUSBFingerSuccess(String s, UsbManager usbManager, UsbDevice usbDevice) {
                if (s.equals(USBFingerManager.BYD_SMALL_DEVICE)) {

                    msyUsbKey = new SyOTG_Key(usbManager, usbDevice);
                    int ret = msyUsbKey.SyOpen();
                    if (ret == SyOTG_Key.DEVICE_SUCCESS) {
                        Log.e(TAG, "open device success!");
                        fpOpened = true;
                        callbackContext.success();
                    } else {
                        Toast.makeText(cordova.getContext(), "open device fail error code :" + ret, Toast.LENGTH_SHORT).show();
                        callbackContext.error();
                    }
                }
            }

            @Override
            public void onOpenUSBFingerFailure(String s) {
                Toast.makeText(cordova.getContext(), s, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 关闭指纹设备
     */
    private void closeDevice() {
        try {
            if (msyUsbKey != null) {
                msyUsbKey.SyClose();
            }
            Log.e(TAG, "Device Closed");
            fpOpened = false;
            USBFingerManager.getInstance(cordova.getContext()).closeUSB();
        } catch (Exception e) {
            Toast.makeText(cordova.getContext(), "Exception: => " + e.toString(), Toast.LENGTH_SHORT).show();
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
    private class ImputAsyncTask extends AsyncTask<Integer, String, Integer> {

        @Override
        protected Integer doInBackground(Integer... params) {
            int cnt = 1;
            int ret;
            while (true) {
                if (fpOpened == false) {
                    return -1;
                }

                // 两次采集指纹间隔
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                    Log.i(TAG, e.toString());
                }

                while (msyUsbKey.SyGetImage() == PS_NO_FINGER) {
                    if (fpOpened == false) {
                        Log.e(TAG, "设备未打开!");
                        return -1;
                    }

                    try {
                        Thread.sleep(200);
                    } catch (Exception e) {
                    }
                }

                if ((ret = msyUsbKey.SyEnroll(cnt, fingerCnt)) != PS_OK) {
                    Log.e(TAG, "Sy Enroll:" + ret);
                    callbackContext.error("Sy Enroll:" + ret);
                    return -1;
                }

                if (cnt >= 2) {
                    int i = msyUsbKey.SyUpChar(-1, g_TempData);
                    if (i == 0) {
                        Log.e(TAG, "特征值: " + DataUtils.bytesToHexString(g_TempData));
                        callbackContext.success(DataUtils.bytesToHexString(g_TempData));
                    }
                    return 0;
                }
                cnt++;
            }
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
        @SuppressWarnings("unused")
        private int ret;

        @Override
        protected Integer doInBackground(Integer... params) {
            int[] fingerId = new int[1];
            while (true) {
                if (fpOpened == false) {
                    return -1;
                }
                while (msyUsbKey.SyGetImage() == PS_NO_FINGER) {
                    if (fpOpened == false) {
                        return -1;
                    }

                    try {
                        Thread.sleep(400);
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                    try {
                        Thread.sleep(400);
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
                if ((ret = msyUsbKey.SyUpImage(fingerBuf)) != 0) {
                    Log.e(TAG, "上传图片失败:" + ret);
                    continue;
                }

                if (msyUsbKey.SySearch(fingerId) != PS_OK) {
                    continue;
                } else {
                    Log.i(TAG, "匹配指纹特征:["+fingerId[0]+"]["+verifyList[fingerId[0]]+"]");
                    callbackContext.success(verifyList[fingerId[0]]);
                    return 0;
                }
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            return;
        }

        @Override
        protected void onPreExecute() {
            Log.e(TAG, "Start Search, Please press finger");
            if(msyUsbKey.SyClear() != 0) {
                Log.w(TAG, "指纹库清空异常!");
            }
            int pageId = 0;
            for (String s : verifyList) {
                if(msyUsbKey.SyDownChar(pageId++, DataUtils.hexStringTobyte(s)) != 0) {
                    Log.w(TAG, "指纹库初始化异常!");
                }
            }
            return;
        }
    }
}
