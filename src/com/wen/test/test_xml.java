package com.wen.test;

import com.wen.XmlProcess;

import java.io.*;
/**
 * Created by wen on 2017/2/21.
 */
public class test_xml {
    public static String readFileByLines(String fileName) {
        FileInputStream file = null;
        BufferedReader reader = null;
        InputStreamReader inputFileReader = null;
        String content = "";
        String tempString = null;
        try {
            file = new FileInputStream(fileName);
            inputFileReader = new InputStreamReader(file, "utf8");
            reader = new BufferedReader(inputFileReader);
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                content += tempString;
                //content += "\n";
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return content;
    }
    public static void main(String[] args)
    {
        XmlProcess xmltest = new XmlProcess();
        String filename = "C:\\Users\\wen\\dm7\\example\\demo.xml";
        //CONNECT + DISCONNECT.XML
        //使用循环.XML
        //RECODENUMS,COLUMNNUMS,STARTTIMES.xml
        //OPEN--结果集获取操作.xml
        //两个结果集不一致.xml           XXXXX
        //自定义替代符.xml
        //SQLSTR样例.xml
        //忽略执行结果.xml
        //IGNORE + BREAK.XML
        //SETCONNECTID + RECONNECT.xml
        //IF_ELSE范例.xml
        //SQLSTATE + NERROR.XML
        //构造500列的建表.xml
        //空结果集的两种表达方.xml
        //结果集跟预期值对比--LOOP的使用.xml
        //两个结果集比较--多行.xml
        //使用LOOP模似WHILE循环.xml
        //TEMPSTR和其它替代符的区别.xml
        //TEMPSTR.xml
        //SQLTEST + CONTENT.xml
        //比较结果集中的行数是否跟预期一致.xml
        //SQL_CASE + CASEEXPRESULT.xml
        //两个结果集比较.xml
        //在LOOP中使用IF关键字.xml
        //执行时间判断.xml
        //EFFECTROWS.XML
        //demo.xml
        //login.xml
        //f_1_2_4.xml
        //脚本变量保存.xml
        //复件 (2) SQLSTR样例.xml
        //复件 SQLSTR样例.xml
        //11121_01.xml
        String xmlstr = readFileByLines(filename);
        System.out.println(xmlstr);
        xmltest.run_xml(xmlstr,filename);

    }

}
