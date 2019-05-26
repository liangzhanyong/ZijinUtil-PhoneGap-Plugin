package nl.xservices.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import com.barcode.BarcodeUtility;
import com.cw.cwsdk.cw;
import com.rscja.deviceapi.RFIDWithUHF;
import com.rscja.deviceapi.exception.ConfigurationException;

/**
 * This class echoes a string called from JavaScript.
 */
public class ZijinUtil extends CordovaPlugin {
    private BarCodeReceiver receiver;
    private BarcodeUtility barcodeUtility;
    private RFIDWithUHF rfidWithUHF;
    private Plugin_P80 plugin_p80;
    private Plugin_U8 plugin_u8;
    private static final String DEVTYPE_P80 = "P80";
    private static final String DEVTYPE_U8 = "U8";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        if (DEVTYPE_P80.equals(Build.MODEL)) {
            barcodeUtility = BarcodeUtility.getInstance();
            try {
                rfidWithUHF = RFIDWithUHF.getInstance();
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
            cordova.getThreadPool().execute(() -> {
                barcodeUtility.open(cordova.getContext(), BarcodeUtility.ModuleType.AUTOMATIC_ADAPTATION);
                barcodeUtility.enableContinuousScan(cordova.getContext(),true);
                barcodeUtility.setOutputMode(cordova.getContext(), 2);
                barcodeUtility.enablePlaySuccessSound(cordova.getContext(), true);
                barcodeUtility.enableVibrate(cordova.getContext(), true);
                barcodeUtility.setScanFailureBroadcast(cordova.getContext(), false);
                barcodeUtility.setContinuousScanIntervalTime(cordova.getContext(), 100);
                barcodeUtility.setContinuousScanTimeOut(cordova.getContext(), 3 * 60);
                rfidWithUHF.init();
                plugin_p80 = new Plugin_P80(cordova, barcodeUtility, rfidWithUHF);
            });
        } else if (DEVTYPE_U8.equals(Build.MODEL)) {
            plugin_u8 = new Plugin_U8(cordova);
        }

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
        if (DEVTYPE_P80.equals(Build.MODEL)) {
            return plugin_p80.execute(action, args, callbackContext);
        } else if (DEVTYPE_U8.equals(Build.MODEL)) {
            return plugin_u8.execute(action, args, callbackContext);
        } else {
            return false;
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);

        if (DEVTYPE_P80.equals(Build.MODEL)) {
            barcodeUtility.stopScan(cordova.getContext(), BarcodeUtility.ModuleType.AUTOMATIC_ADAPTATION);
        } else if (DEVTYPE_U8.equals(Build.MODEL)) {
            cw.BarCodeAPI(cordova.getContext()).openBarCodeReceiver();
            if (cw.BarCodeAPI(cordova.getContext()).isScannerServiceRunning(cordova.getActivity())) {

            } else {
                cw.BarCodeAPI(cordova.getContext()).CloseScanning();
            }
        }
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        if (DEVTYPE_P80.equals(Build.MODEL)) {
            if (barcodeUtility != null) {
                barcodeUtility.stopScan(cordova.getContext(), BarcodeUtility.ModuleType.AUTOMATIC_ADAPTATION);
            }
        } else if (DEVTYPE_U8.equals(Build.MODEL)) {
            if(cw.R2000UHFAPI().getReaderHelper() != null && !!cw.R2000UHFAPI().getReaderHelper().getInventoryFlag()) {
                cw.R2000UHFAPI().stopInventoryReal();
            }
            cw.BarCodeAPI(cordova.getContext()).CloseScanning();
            //建议在onPause里或者监听屏幕息屏里放，息屏后可以省电
            cw.BarCodeAPI(cordova.getContext()).closeBarCodeReceiver();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(DEVTYPE_P80.equals(Build.MODEL)){
            if (barcodeUtility != null) {
                barcodeUtility.stopScan(cordova.getContext(), BarcodeUtility.ModuleType.AUTOMATIC_ADAPTATION);
                barcodeUtility.close(cordova.getContext(), BarcodeUtility.ModuleType.AUTOMATIC_ADAPTATION);
            }

        } else if (DEVTYPE_U8.equals(Build.MODEL)) {
            plugin_u8.onDestroy();
        }
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
                    if (DEVTYPE_P80.equals(Build.MODEL)) {
                        if (barcodeUtility != null) {
                            barcodeUtility.stopScan(cordova.getContext(), BarcodeUtility.ModuleType.AUTOMATIC_ADAPTATION);
                        }
                    } else if (DEVTYPE_U8.equals(Build.MODEL)) {
                        cw.BarCodeAPI(cordova.getContext()).CloseScanning();
                    }
                    break;


                case Intent.ACTION_SHUTDOWN:
                    if (DEVTYPE_P80.equals(Build.MODEL)) {
                        if (barcodeUtility != null) {
                            barcodeUtility.stopScan(cordova.getContext(), BarcodeUtility.ModuleType.AUTOMATIC_ADAPTATION);
                        } else if (DEVTYPE_U8.equals(Build.MODEL)) {
                            cw.BarCodeAPI(cordova.getContext()).CloseScanning();
                        }
                    }
                    break;
                case Intent.ACTION_REBOOT:
//                    Log.i(TAG, TAG + "---亮屏了");
                    break;
            }
        }
    }
}
