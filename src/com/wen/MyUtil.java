package com.wen;

import javax.swing.*;

/**
 * Created by wen on 2017/3/13.
 */
public class MyUtil {
    public static  String getUserChoose(String outStr)
    {
        Object[] options = { "SUCCESS", "FAIL" };
        int ret = JOptionPane.showOptionDialog(null, outStr, "提示",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);
        return options[ret].toString();
    }
}
