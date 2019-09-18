package nl.xservices.plugins;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * author：   wdl
 * time： 2018/11/13 20:52
 * des：    File方式读写文件，工具类
 */
public class FileWRTool {

    /**
     * 输出流形式，来保存文件
     *
     * @param context  上下文
     * @param fileName 文件名
     * @param data     要保存的字符串
     *                 fileName 是要要生成的文件的文件名（data.txt）
     */
    public static void writeFile(Context context, String fileName, String data) {
        FileOutputStream outputStream;
        BufferedWriter bufferedWriter = null;
        try {
            outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
            bufferedWriter.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedWriter != null)
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    /**
     * 输入流形式，来读取fileName文件
     *
     * @param context  上下文
     * @param fileName 文件名
     * @return 文件内容
     */
    public static String readFile(Context context, String fileName) {
        //字节输入流
        FileInputStream inputStream;
        //缓冲流
        BufferedReader bufferedReader = null;
        StringBuilder stringBuffer = new StringBuilder();
        try {
            inputStream = context.openFileInput(fileName);
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return stringBuffer.toString();
    }
}
