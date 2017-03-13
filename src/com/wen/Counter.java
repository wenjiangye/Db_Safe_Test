package com.wen; /**
 * Created by wen on 2017/2/17.
 */
import java.io.*;

public class Counter
{
    public int sqlCaseNum = 0;//为打印出错<SQL_CASE>标记增加记数变量
    public int sqlCaseLineNum = 0; //出错<SQL_CASE>标记所在行数
    public int sqlNum = 0;
    public int sqlLineNum = 0;



    public Counter()
    {
        //
        // TODO: 在此处添加构造函数逻辑
        //
    }
    public int findLine(String filePath, String strDst, int errNum)  //strDst目标结点：比如<SQL_CASE>
    {
        BufferedReader sr = null;
        String strLine = null;
        int lineNum = 1;
        int sqlNum = 0;

        try
        {
            FileReader reader = new FileReader(filePath);
            sr = new BufferedReader(reader);
            if(sr == null)
            {
                sr.close();
                return -1 ;
            }
            strLine = sr.readLine();
            while(strLine != null)
            {
                if(strLine.indexOf(strDst) >= 0)  //该行字符串中存在strDst
                {
                    sqlNum++;
                    if(sqlNum == errNum)
                    {
                        sr.close();
                        return lineNum;
                    }
                }
                strLine = sr.readLine();
                lineNum++;
            }
            sr.close();
            return 0;
        }
        catch(IOException e)
        {
            String mes = e.getMessage();
            return -1;
        }


    }
}
