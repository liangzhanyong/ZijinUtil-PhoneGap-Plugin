package nl.xservices.plugins.bean;

import nl.xservices.plugins.helper.printer.PrintHelper;

/**
 * Description:
 * Date: 2020/3/9
 *
 * @author wangke
 */
public class PrintBean {
    /**
     * 打印文本内容
     */
    private String content;
    /**
     * 打印文本对齐方式
     */
    private int align;
    /**
     * 文字大小
     */
    private int textSize;
    /**
     * 文字是否加粗
     */
    private boolean isBold;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getAlign() {
        return align;
    }

    public void setAlign(int align) {
        this.align = align;
    }

    public int getTextSize() {
        return textSize;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public boolean isBold() {
        return isBold;
    }

    public void setBold(boolean bold) {
        isBold = bold;
    }

    public PrintHelper.PrintType getPrintType() {
        PrintHelper.PrintType printType = null;
        switch (this.align) {
            case 1:
                printType = PrintHelper.PrintType.Left;
                break;
            case 2:
                printType = PrintHelper.PrintType.Centering;
                break;
            case 3:
                printType = PrintHelper.PrintType.Right;
                break;
            default:
                break;
        }
        return printType;
    }
}
