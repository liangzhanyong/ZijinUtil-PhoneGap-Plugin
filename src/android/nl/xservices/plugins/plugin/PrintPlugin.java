package nl.xservices.plugins.plugin;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import nl.xservices.plugins.base.ICordovaPlugin;
import nl.xservices.plugins.bean.PrintBean;
import nl.xservices.plugins.util.BarcodeUtil;
import nl.xservices.plugins.helper.printer.PrintHelper;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
/**
 * Description: CilicoPlugin 插件
 * Date: 2020/3/9
 *
 * @author wangke
 */
public class PrintPlugin implements ICordovaPlugin {
    private CordovaInterface cordova;
    private Context context;
    private PrintHelper printHelper;

    public PrintPlugin(CordovaInterface cordova) {
        this.cordova = cordova;
        this.context = cordova.getContext();
        initPrinter(context);
    }

    private void initPrinter(Context context) {
        printHelper = new PrintHelper();
        printHelper.Open(context);
    }

    @Override
    public void onResume(boolean multitasking) {

    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("printText")) {
            this.printText(args, callbackContext);
            return true;
        } else if (action.equals("printQRCode")) {
            this.printQRCode(args, callbackContext);
            return true;
        } else if (action.equals("printBlankLine")) {
            this.printBlankLine(args, callbackContext);
            return true;
        } else if (action.equals("releasePrinter")) {
            this.release();
            return true;
        }
        return false;
    }

    /**
     * 文字打印
     *
     * @param args
     * @param callbackContext
     */
    private void printText(JSONArray args, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    List<PrintBean> printBeans = getPrintBean(args);
                    for (PrintBean printBean : printBeans) {
                        printHelper.PrintLineInit(printBean.getTextSize());
                        printHelper.PrintLineStringByType(printBean.getContent(), printBean.getTextSize(), printBean.getPrintType(), printBean.isBold());
                        printHelper.PrintLineEnd();
                    }
                    callbackContext.success("打印成功");
                } catch (JSONException e) {
                    e.printStackTrace();
                    callbackContext.error("打印失败：" + e.getMessage());
                }
            }
        });
    }

    /**
     * 打印二维码
     *
     * @param args
     * @param callbackContext
     */
    private void printQRCode(JSONArray args, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject = args.getJSONObject(0);
                    String content = jsonObject.getString("content");
                    int width = jsonObject.getInt("width");
                    int height = jsonObject.getInt("height");
                    Bitmap bmp = null;
                    bmp = BarcodeUtil.encodeAsBitmap(content, BarcodeFormat.QR_CODE, width, height);
                    if (bmp == null) {
                        callbackContext.error("二维码打印失败");
                        return;
                    }
                    printHelper.PrintBitmap(bmp);
                    callbackContext.success("二维码打印成功");
                } catch (WriterException e) {
                    e.printStackTrace();
                    callbackContext.error("二维码打印失败：" + e.getMessage());
                } catch (JSONException e) {
                    e.printStackTrace();
                    callbackContext.error("二维码打印失败：" + e.getMessage());
                }
            }
        });
    }

    /**
     * 打印空行
     *
     * @param args            [{"lineHeight": 34}]
     * @param callbackContext
     */
    private void printBlankLine(JSONArray args, CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    int lineHeight = args.getJSONObject(0).getInt("lineHeight");
                    printHelper.printBlankLine(lineHeight);
                    callbackContext.success("打印成功");
                } catch (JSONException e) {
                    e.printStackTrace();
                    callbackContext.error("打印失败：" + e.getMessage());
                }
            }
        });
    }

    /**
     * 从参数中获取前端传递的打印对象集合
     *
     * @param args
     * @return
     * @throws JSONException
     */
    private List<PrintBean> getPrintBean(JSONArray args) throws JSONException {
        List<PrintBean> printBeans = new ArrayList<>();
        Gson gson = new Gson();
        for (int i = 0; i < args.length(); i++) {
            String jsonStr = args.getJSONObject(i).toString();
            PrintBean bean = gson.fromJson(jsonStr, PrintBean.class);
            printBeans.add(bean);
        }
        return printBeans;
    }

    @Override
    public void onPause(boolean multitasking) {

    }

    @Override
    public void onDestroy() {
        release();
    }

    @Override
    public void release() {
        releasePrinter();
    }

    private void releasePrinter() {
        if (printHelper != null) {
            printHelper.Close();
        }
    }
}
