package com.wen;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import com.sun.corba.se.impl.protocol.giopmsgheaders.Message;
import com.sun.org.apache.xml.internal.serialize.Method;
import java.io.PrintWriter;
import javax.swing.*;
import java.io.*;

/**
 * Created by wen on 2017/3/13.
 */
public class SerCmdEx {
    private String user;
    private String passwd;
    private String server;   //服务器IP
    private int port = 5678; // 端口号默认22
    public SerCmdEx(String inuser, String inpasswd, String inserver)     //使用默认端口号
    {
        user = inuser;
        passwd = inpasswd;
        server = inserver;
    }
    public SerCmdEx(String inuser, String inpasswd, String inserver, int inport)   //使用指定端口号
    {
        user = inuser;
        passwd = inpasswd;
        server = inserver;
        port = inport;
    }
    public boolean execute(String cmd, Object[] error)
    {
        Connection conn = new Connection(server, port);
        Session ssh = null;
        String outStr = "远程命令为：" + cmd + "\n" + "执行结果:";
        try {
            //连接到主机
            conn.connect();
            //使用用户名和密码校验
            boolean isconn = conn.authenticateWithPassword(user, passwd);
            if(!isconn)
            {
                System.out.println("用户名称或者是密码不正确");
            }
            else
            {
                System.out.println("已经连接OK");
                ssh = conn.openSession();
                ssh.execCommand(cmd);
                //将屏幕上的文字全部打印出来
                InputStream is = new StreamGobbler(ssh.getStdout());
                BufferedReader brs = new BufferedReader(new InputStreamReader(is));
                String line = brs.readLine();
                while(line != null){
                    outStr += line + "\n";
                    line = brs.readLine();

                }
            }
            String choice = MyUtil.getUserChoose(outStr);
            //连接的Session和Connection对象都需要关闭
            ssh.close();
            conn.close();
            if(choice.equals("SUCCESS"))
                return true;
            else
                return false;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            error[0] = e;
            return false;
        }
    }
}
