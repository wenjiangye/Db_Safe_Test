package com.wen;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by wen on 2017/2/20.
 */

/// <summary>
/// com.wen.ProClass 的摘要说明。
/// </summary>
class ProHelp
{
    String value;
    public ProHelp(String invalue)
    {
        value = invalue;
    }
    public void setvalue(String invalue)
    {
        value = invalue;
    }
    public String getvalue()
    {
        return value;
    }
}
public class ProClass {

    public  String sValDatabase = "SYSTEM";      //默认数据库

    public  String sValUserId = "SYSDBA";        //默认用户名

    public  String sValPassword = "SYSDBA";     //默认密码

    public  String sValServer = "LOCALHOST";         //服务器IP

    public  String sValPort= "5236";          //默认端口

    public  String sValOS = "WINDOWS";           //测试数据库的操作系统版本

    public  String sValIsOutMsg = "true";     //是否输出中间执行消息

    public  String sValIsErrRun = "false";    //执行错误后继续运行

    public  String sValDriveName = "dm.jdbc.driver.DmDriver";   // JDBC驱动名称

    public  String sValConnUrl = "jdbc:dm://localhost:5236";     //默认数据库连接串

    public  String sValOSUid = "wen";                       //远程服务器系统用户名 （用于执行远程命令）

    public  String sValOSPwd = "123456";                     //远程服务器系统密码

    public  boolean bValIsShowResult = true;    //是否显示结果集

    public  boolean bValIsErrRun = false;      //错误后是否继续运行

    public  boolean bValIsOutTime = false;     //是否输出每条语句执行时间

    public  Map<String,ProHelp>  valclass = new HashMap<String, ProHelp>();      //存储脚本的全局变量

    public  synchronized String getVal(String key)
    {
        if(valclass.containsKey(key))
            return valclass.get(key).getvalue();
        return null;
    }
    public  synchronized void setVal(String key, String value)
    {
        if(valclass.containsKey(key))
            valclass.get(key).setvalue(value);
        else
            valclass.put(key,new ProHelp(value));
    }
    public  synchronized void delVal(String key)
    {
        valclass.remove(key);
    }
}


