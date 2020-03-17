package nl.xservices.plugins.plugin;

import android.content.Context;
import com.google.gson.Gson;
import nl.xservices.plugins.base.ICordovaPlugin;
import nl.xservices.plugins.util.DevBeep;
import com.olc.uhf.UhfAdapter;
import com.olc.uhf.UhfManager;
import com.olc.uhf.tech.ISO1800_6C;
import com.olc.uhf.tech.IUhfCallback;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.ArrayList;


/**
 * Description:
 * Date: 2020/3/9
 *
 * @author wangke
 */
public class UHFPlugin implements ICordovaPlugin {
    private UhfManager uhfManager;
    private CordovaInterface cordova;
    private Context context;
    private boolean isLoop = false;
    private boolean inventoryOpened = false;
    private CallbackContext callback;
    private final ISO1800_6C uhf6C;
    private final List<String> uhfTempList = new ArrayList<>();

    public UHFPlugin(CordovaInterface cordova) {
        this.cordova = cordova;
        this.context = cordova.getContext();
        this.uhfManager = UhfAdapter.getUhfManager(context);
        this.uhfManager.open();
        uhf6C = uhfManager.getISO1800_6C();
        DevBeep.init(context);
    }

    @Override
    public void onResume(boolean multitasking) {
        if (inventoryOpened) {
            isLoop = true;
            loopReadEPC();
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("startInventoryReal")) {
            startInventoryReal(callbackContext);
            return true;
        } else if (action.equals("stopInventoryReal")) {
            stopInventoryReal();
            return true;
        } else if (action.equals("setOutputPower")) {
            setOutputPower(args);
            return true;
        } else if (action.equals("releaseUHF")) {
            release();
            return true;
        }
        return false;
    }

    private void startInventoryReal(CallbackContext callbackContext) {
        uhfTempList.clear();
        isLoop = true;
        inventoryOpened = true;
        loopReadEPC();
        this.callback = callbackContext;
        PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
        pr.setKeepCallback(true);
        callbackContext.sendPluginResult(pr);
    }

    private void stopInventoryReal() {
        isLoop = false;
        inventoryOpened = false;
        this.callback = null;
    }

    private void setOutputPower(JSONArray args) throws JSONException {
        int mOutPower = args.getJSONObject(0).getInt("outPower");
        if (mOutPower > 1200 & mOutPower < 2800) {
            uhfManager.setTransmissionPower(mOutPower);
        }
    }

    private void loopReadEPC() {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                while (isLoop) {
                    uhf6C.inventory(uhfCallback);
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
            }
        });

    }

    private IUhfCallback uhfCallback = new IUhfCallback.Stub() {
        @Override
        public void doInventory(List<String> strs) {
            // strs列表每次只会返回一个识别结果
            if (strs.size() != 0) {
                String tmpUhfStr = new Gson().toJson(strs).substring(4);
                if (!uhfTempList.contains(tmpUhfStr)) {
                    PluginResult pr = new PluginResult(PluginResult.Status.OK, new Gson().toJson(strs));
                    pr.setKeepCallback(true);
                    if (callback != null) {
                        callback.sendPluginResult(pr);
                        uhfTempList.add(tmpUhfStr);
                    }
                    DevBeep.PlayOK();
                }
            }
        }

        @Override
        public void doTIDAndEPC(List<String> list) {
        }
    };

    @Override
    public void onPause(boolean multitasking) {
        isLoop = false;
    }

    @Override
    public void onDestroy() {
        release();
    }

    @Override
    public void release() {
        isLoop = false;
        uhfManager.close();
    }
}
