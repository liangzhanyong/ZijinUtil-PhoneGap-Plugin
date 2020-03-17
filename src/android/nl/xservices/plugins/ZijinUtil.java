package nl.xservices.plugins;

import android.os.Build;
import android.widget.Toast;

import nl.xservices.plugins.base.ICordovaPlugin;
import nl.xservices.plugins.plugin.PrintPlugin;
import nl.xservices.plugins.plugin.ScanPlugin;
import nl.xservices.plugins.plugin.UHFPlugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
/**
 * Description:
 * Date: 2020/3/9
 *
 * @author wangke
 */
public class ZijinUtil extends CordovaPlugin {
    /**
     * cilico设备的插件列表
     */
    private final List<ICordovaPlugin> cilicoPlugins = new ArrayList<>();
    private static final String CILICO_PRODUCT_MODEL = "cilico";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        initPluginFunc();
    }

    /**
     * 向不同设备的插件功能列表中添加插件
     */
    private void initPluginFunc() {
        cilicoPlugins.add(new PrintPlugin(cordova));
        cilicoPlugins.add(new ScanPlugin(cordova));
        cilicoPlugins.add(new UHFPlugin(cordova));
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (CILICO_PRODUCT_MODEL.equals(Build.MODEL)) {
            for (ICordovaPlugin cilicoPlugin : cilicoPlugins) {
                if (cilicoPlugin.execute(action, args, callbackContext)) {
                    return true;
                }
            }
        }
        //Returning false results in a "MethodNotFound" error
        return false;
    }

    @Override
    public void onPause(boolean multitasking) {
        if (CILICO_PRODUCT_MODEL.equals(Build.MODEL)) {
            for (ICordovaPlugin cilicoPlugin : cilicoPlugins) {
                cilicoPlugin.onPause(multitasking);
            }
        }
        super.onPause(multitasking);
    }

    @Override
    public void onResume(boolean multitasking) {
        if (CILICO_PRODUCT_MODEL.equals(Build.MODEL)) {
            for (ICordovaPlugin cilicoPlugin : cilicoPlugins) {
                cilicoPlugin.onResume(multitasking);
            }
        }
        super.onResume(multitasking);
    }

    @Override
    public void onDestroy() {
        if (CILICO_PRODUCT_MODEL.equals(Build.MODEL)) {
            for (ICordovaPlugin cilicoPlugin : cilicoPlugins) {
                cilicoPlugin.onDestroy();
            }
        }
        super.onDestroy();
    }
}
