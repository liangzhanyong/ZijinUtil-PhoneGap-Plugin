package nl.xservices.plugins.base;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Description:
 * Date: 2020/3/9
 *
 * @author wangke
 */
public interface ICordovaPlugin {

    public void onResume(boolean multitasking);

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException;

    public void onPause(boolean multitasking);

    public void onDestroy();

    public void release();
}
