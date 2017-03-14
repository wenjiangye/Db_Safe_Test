package com.wen;

import javax.swing.*;
import java.io.IOException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
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
    public  static String getJsonFromObject(Object obj){
        ObjectMapper om = new ObjectMapper();
        try
        {
            return om.writeValueAsString(obj);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return "";
    }
    public  static Object  getObjectByJsonString(String jsonStr, String Classname){
        Object ob = null;
        ObjectMapper om = new ObjectMapper();
        try {
            JsonNode node=om.readTree(jsonStr);
            try {
                ob = om.readValue(node.toString(),Class.forName(Classname));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return ob;
    }
}
