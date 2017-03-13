package com.wen; /**
 * Created by wen on 2017/3/6.
 */
import com.googlecode.aviator.AviatorEvaluator;
import org.w3c.dom.Node;

import java.sql.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MoreRunnable implements Runnable{

    private Node Xml_Node;                      //要解析的节点
    private Node ref_Node;                      //用于引用返回节点
    public  Node xCurrentNode;				   //对像执行的当前结点
    private SqlCase  currSqlCase;               //当前子节点的SQL_CASE结构信息
    private XmlProcess  parentProcess;          //主引擎实例引用
    private CONNECTINFO  currConnectInfo;       //当前连接数据库信息

    public boolean bClearEn;				  //表示该对像是否是正在用来清空测试环境的

    public  Map  cRunInfo;                    //存储@替换符的信息
    public  boolean	isHasTimes;                   //是否有次数标签，当处于LOOP循环中
    public  int		run_times;                   //运行次数
    public  LOOP_STRUCT cLoop;                     //当前子节点处理的循环控制信息
    public  boolean	m_noShow;                      //是否显示执行的语句

    public MoreRunnable(Node currNode, XmlProcess pXmlProcess)
    {
        Xml_Node = currNode;
        xCurrentNode = currNode;
        parentProcess = pXmlProcess;
        currSqlCase = new SqlCase();
        cRunInfo = new HashMap();
        isHasTimes = false;
        run_times = 1;
        m_noShow = false;
        cLoop = null;
    }
    public void run() {

        SearchXmlNode(Xml_Node.getFirstChild(), true);	            //直接从该节点开始搜下面的结点，并分析运行它们要求的操作
    }
    private void SearchXmlNode(Node  m_XmlNode, boolean bFindNext)    //搜寻xmlnode并解析执行,bFindNext表示是否并列执行该节点的所有兄弟节点
    {
        if(m_XmlNode == null)
            return;
        try
        {
            while(m_XmlNode != null && !parentProcess.stXmlRunInfo.bStop)
            {
                if(m_XmlNode.getNodeName().equals("SQL_CASE"))
                {
                    parentProcess.sqlCaseCounter.sqlCaseNum++;
                    SqlCase cTempSqlCase = new SqlCase();
                    cTempSqlCase.cParentCase = currSqlCase;        //记录父亲SQL_CASE
                    currSqlCase = cTempSqlCase;                    //更改当前SQL_CASE
                    SearchXmlNode(m_XmlNode.getFirstChild(), true);	//遍历并执行当前节点的子节点
                    if(cTempSqlCase.stCaseResult.bSuccess != cTempSqlCase.stCaseResult.bExpResult)
                    {
                        parentProcess.ShowError("SQL_CASE中预期的执行结果和实际的执行结果不一致！预期：" +
                                cTempSqlCase.stCaseResult.bExpResult + "; 实际：" + cTempSqlCase.stCaseResult.bSuccess);
                      //  sqlCaseCounter.sqlCaseLineNum = sqlCaseCounter.findLine(stXmlRunInfo.sXmlFileName,"<SQL_CASE>", sqlCaseCounter.sqlCaseNum);
                     //   sqlCaseCounter.sqlCaseLineNum =sqlCaseCounter.sqlCaseLineNum -1;
                    //    if(sqlCaseCounter.sqlCaseLineNum > 0)
                     //       ShowError("【" + testnum + "】" + "执行错误！\n ");
                        //执行中报错处理，报告是在哪个测试点出错
                        // 把下面的定位到行的报错方式给取消，即：下面注释掉的一行
                        //stXmlRunInfo.tdTestThread .ShowFMessage("第 "+ CurrentLineNum + " 行 " + "执行错误！" ,true);
                        parentProcess.cSqlCase.stCaseResult.bSuccess = false;
                        if(!parentProcess.pro_config.bValIsErrRun && !parentProcess.stXmlRunInfo.bClearEn)  //如果不允许继续运行下面一个结点的值
                            parentProcess.stXmlRunInfo.bStop = true;
                    }
                    currSqlCase = currSqlCase.cParentCase;
                }
                else if(AnalyseNode(m_XmlNode))		//分析结点关键字，并执行
                {
                    m_XmlNode = ref_Node;
                    SearchXmlNode(m_XmlNode.getFirstChild(), true);	//遍历当前节点的子节点
                }
                else
                {
                    m_XmlNode = ref_Node;
                    if(currSqlCase.stCaseResult.bBreak)
                        return;
                }
                if (bFindNext && m_XmlNode != null)
                    m_XmlNode = m_XmlNode.getNextSibling();       //继续解析执行该节点的并列节点的下一个
                else
                    break;
            }
        }
        catch(Exception e)
        {
            parentProcess.ShowError("子线程在分析结点时报异常！" + e.getMessage());
        }
    }
    public boolean AnalyseNode(Node m_XmlNode)
    {
        parentProcess.CurrentLineNum++;   //加1
        if (m_XmlNode == null)
        {
            System.out.println("给定的XML结点为空值");
            return false;
        }
        String strFromSql = "FromSql:";
        xCurrentNode = m_XmlNode;
        boolean m_FindChild = false;						//用来表示，该函数返回后，是否还要搜索它的子节点
        String m_name = m_XmlNode.getNodeName();
        String sValues;
        String sTempStr;

        switch(m_XmlNode.getNodeName().toUpperCase())             //将m_XmlNode节点的标签统一改成大写
        {
            case "SQL":
                parentProcess.sqlCounter.sqlNum++;
                ExecuteSQL(ReplaceRunInfo(GetNodeText(m_XmlNode)));
                break;
            case "TYPE":
                currSqlCase.stSqlResult.sExpResult = GetNodeText(m_XmlNode).trim();
                parentProcess.CheckTypeValue(currSqlCase.stSqlResult.sExpResult);
                break;
            case "TEMPSTR":
                sTempStr = "";

                if(cRunInfo.containsKey("TEMPSTR"))
                    sTempStr = cRunInfo.get("TEMPSTR").toString();

                if(cRunInfo.containsKey("TEMPSTR"))
                    cRunInfo.remove("TEMPSTR");       //防止自身被替代

                sValues = GetExpVal(m_XmlNode);                  //计算是否为表达式
                if (sValues == null)
                {
                    sValues = GetNodeText(m_XmlNode);
                }
                else{
                    cRunInfo.put(m_name,sValues);
                    break;
                }
                if (sValues.equals(""))
                {
                    cRunInfo.put("TEMPSTR","");
                }
                else if (sValues.equals("@"))
                {
                    sValues = sTempStr;
                    sValues = ReplaceRunInfo(sValues);
                    cRunInfo.put("TEMPSTR",sValues);
                }
                else
                {
                    sValues = ReplaceRunInfo(sValues);
                    if (sValues.startsWith(strFromSql))
                    {
                        sValues = GetStrFromSql(sValues.substring(strFromSql.length()),true);
                        cRunInfo.put("TEMPSTR",sValues);
                        break;
                    }
                    if (sValues != null)
                    {
                        sValues = sTempStr + sValues;
                        cRunInfo.put("TEMPSTR",sValues);
                    }
                }
                break;
            case "LOOP":
                StartLoopNode(m_XmlNode);
                break;
            case "BREAKLOOP":
                cLoop.breakflag = true;
                break;
            case "LOCK":
                StartLock(m_XmlNode);
                break;
            case "WAIT":
                StartWait(m_XmlNode);
                break;
            case "NOTIFY":
                StartNotify(m_XmlNode);
                break;
            case  "VALNAME":
                break;
            case "TIMES":
                if(cLoop == null && isHasTimes == false)
                {
                    isHasTimes = true;
                    try
                    {
                        sValues = GetNodeText(m_XmlNode);
                        sValues = ReplaceRunInfo(sValues);
                        run_times = Integer.parseInt(sValues.trim());
                    }
                    catch(Exception e)
                    {
                        parentProcess.ShowError("非法的执行次数" + e.getMessage());
                    }
                }
                break;
            case "STARTTIMES":
                if(cLoop == null)
                {
                    parentProcess.ShowError("STARTTIMES关键字只能在LOOP关键字中使用");
                }
                break;
            case "EFFECTROWS":
                try
                {
                    int iRows = Integer.parseInt(GetNodeText(m_XmlNode).trim());
                    CheckEffectRows(iRows);
                }
                catch(Exception e)
                {
                    parentProcess.ShowError("表示影响的行数时，使用了非法的字符串，" + e.getMessage());
                }
                break;
            case "MORETHREAD":
            //    MoreThreadExecute(m_XmlNode);
            //    break;
            case "NOSHOW":
                if (m_noShow == false && cLoop == null)
                {
                    parentProcess.ShowSuccess("正在隐式运行一段脚本，可能需要比较长的时间.....");
                }
                if (m_noShow)
                {
                    SearchXmlNode(m_XmlNode.getFirstChild(), true);
                }
                else
                {
                    m_noShow = true;

                    SearchXmlNode(m_XmlNode.getFirstChild(), true);

                    m_noShow = false;
                }
                break;
            case "SQLSTATE":
                currSqlCase.stSqlResult.sSQLState = GetNodeText(m_XmlNode).trim();
                break;
            case "NERROR":
                try
                {
                    currSqlCase.stSqlResult.iNativeError = Integer.parseInt(GetNodeText(m_XmlNode).trim());
                }
                catch(Exception e)
                {
                    parentProcess.ShowError("非法的数字字符串表示NetiveError," + e.getMessage());
                }
                break;
            case "CASEEXPRESULT":
                sValues = GetNodeText(m_XmlNode).trim();
                currSqlCase.stCaseResult.bExpResult = (sValues.toUpperCase().equals("TRUE"));
                break;
            case "COMPARERESULT":
                //CompareResult(m_XmlNode);
                break;
            case "SETVAL":
                SetVal(m_XmlNode);
                break;
            case "GETVAL":
                GetVal(m_XmlNode);
                break;
            case "IF":
                m_XmlNode = DoIf(m_XmlNode);
                break;
            case "ELSE":
                break;
            case "FMES":
                sValues = GetNodeText(m_XmlNode).trim();
                sValues = ReplaceRunInfo(sValues);
                parentProcess.ShowError(sValues);
                break;
            case "SMES":
                sValues = GetNodeText(m_XmlNode).trim();
                sValues = ReplaceRunInfo(sValues);
                parentProcess.ShowSuccess(sValues);
                break;
            case "INITDB":     //这边的处理函数未修改
                break;
            case "CONTENT":
                parentProcess.ShowSuccess(GetNodeText(m_XmlNode).trim());
                break;
            case "SETCONNECTID":
                try
                {
                    SetCn(Integer.parseInt(GetNodeText(m_XmlNode).trim()), FindXmlNodeEx(m_XmlNode.getFirstChild(), "NOSHOW") == null);
                }
                catch(Exception e)
                {
                    parentProcess.ShowError("表示连个数时，使用了非法的字符串，" + e.getMessage());
                }
                break;
            case "SQLTEST":
                m_FindChild = true;
                break;
            case  "CLEAR":
                if(bClearEn == false)
                {
                    break;
                }
                currSqlCase.stSqlResult.sExpResult = "DIRECT_EXECUTE_SUCCESS";
                m_FindChild = true;
                break;
            case "SLEEP":
                try
                {
                    int iSleep = Integer.parseInt(GetNodeText(m_XmlNode).trim());
                    if(iSleep >= 0)
                    {
                        Thread.sleep(iSleep);
                        parentProcess.ShowSuccess("子线程将要睡眠" + iSleep + "秒...");
                    }
                }
                catch(Exception e)
                {
                    parentProcess.ShowError("表示睡眠时间时，使用了非法的字符串，" + e.getMessage());
                }
                break;
            case "IGNORE":
                parentProcess.ShowSuccess("注意：程序跳过一个被要求忽略的节点");
                break;
            case "BREAK":
                currSqlCase.stCaseResult.bBreak = true;
                parentProcess.ShowSuccess("注意：遇到脚本中的 BREAK 结点，程序执行跳出了它所在的SQL_CASE点范围");
                break;
            case "WINDOWS":
                break;
            case "LINUX":
                break;
            case "RECORD":
                parentProcess.ShowError("RECORD关键字只能被包含在RESULT关键字节点里面");
                break;
            case "COLUMN":
                parentProcess.ShowError("COLUMN关键字只能被包含在RECORD关键字节点里面");
                break;
            case "OPEN":
                if (currConnectInfo != null) {
                    currConnectInfo.isOpenResult = true;
                    currConnectInfo.iFetch = 0;
                }
                else{
                    parentProcess.ShowError("OPEN操作上没有连接");
                }
                m_FindChild = true;
                break;
            case "FETCHNEXT":
                FetchNext();
                break;
            case "TIMETICKS":
                Date time_tricks = new Date();
                cRunInfo.put(GetNodeText(m_XmlNode).trim(), Long.toString(time_tricks.getTime()));
                break;
            case "RESULTROWS":
                try
                {
                    int iRows = Integer.parseInt(GetNodeText(m_XmlNode).trim());
                    CheckResultRows(iRows);
                }
                catch(Exception e)
                {
                    parentProcess.ShowError("表示结果集的行数时，使用了非法的字符串，" + e.getMessage());
                }
                break;
            default:
                    if (!m_name.startsWith("#")) {
                    String sTempstr = "";
                    if(cRunInfo.containsKey(m_name))
                        sTempstr = cRunInfo.get(m_name).toString();
                    sValues = GetExpVal(m_XmlNode);
                    if (sValues == null)
                    {
                        sValues = GetNodeText(m_XmlNode);
                    }
                    else
                    {
                        cRunInfo.put(m_name,sValues);
                        break;
                    }
                    if (sValues.equals(""))
                    {
                        cRunInfo.put(m_name,"");
                    }
                    else if (sValues.equals("@"))
                    {
                        sValues = sTempstr;
                        sValues = ReplaceRunInfo(sValues);
                        cRunInfo.put(m_name,sValues);
                    }
                    else
                    {
                        sValues = ReplaceRunInfo(sValues);
                        if (sValues.startsWith(strFromSql))
                        {
                            sValues = GetStrFromSql(sValues.substring(strFromSql.length()), true);
                            cRunInfo.put(m_name,sValues);
                            break;
                        }
                        if (sValues != null)
                        {
                            sValues = sTempstr + sValues;
                            cRunInfo.put(m_name,sValues);
                        }
                    }
                    break;
                }
                break;
        }
        ref_Node = m_XmlNode;
        return m_FindChild;
    }
    private String GetNodeText(Node m_XmlNode)
    {
        return parentProcess.GetNodeText(m_XmlNode);
    }
    public String ReplaceRunInfo(String sql)
    {
        Object tmp = null;
        if(cRunInfo.containsKey("TEMPSTR"))
        {
            tmp = cRunInfo.get("TEMPSTR");
            cRunInfo.remove("TEMPSTR");
            sql = sql.replace("@TEMPSTR",tmp.toString());          //提前替换
        }
        int index = sql.indexOf("@");
        while(index != -1)
        {
            String max_str = "";
            Object max_key = null;
            for(Object obj : cRunInfo.keySet())
            {
                String key = "@" + obj.toString();
                if(sql.contains(key))    //
                {
                    if(key.length() > max_str.length())
                    {
                        max_str = key;
                        max_key = obj;
                    }
                }
            }
            if(max_key == null)
                break;
            sql = sql.replace(max_str, cRunInfo.get(max_key).toString());
            index = sql.indexOf("@");
        }
        //最长匹配
        if(cLoop != null)
        {
            index = sql.indexOf("@");    //找出@符号
            while (index != -1)
            {
                String sTemp = sql.substring(index);     //截出@符号开始以后的字符串

                int iTemp1 = sTemp.indexOf("TIMES");

                int iTemp2 = GetUpTimes(iTemp1-1);
                if (iTemp2 != -1)
                {
                    sTemp = sTemp.substring(0, iTemp1 + "TIMES".length());
                    sql = sql.replace(sTemp, Integer.toString(iTemp2));
                }
                index = sql.indexOf("@", index + 1);
            }
        }
        if(tmp != null)
        {
            cRunInfo.put("TEMPSTR",tmp.toString());
        }
        return sql;
    }
    private int GetUpTimes(int up)
    {
        if (up < 0)
        {
            return -1;
        }
        LOOP_STRUCT cTemp = cLoop;
        while (up-- > 0 && cTemp != null)
        {
            cTemp = (LOOP_STRUCT)cTemp.prev;
        }
        if (cTemp == null)
        {
            return -1;
        }
        return cTemp.times;
    }
    private boolean SetCn(int iIndex, boolean bShowInfo)
    {

        if(iIndex >= parentProcess.stConnectArry.size() || iIndex < 0)//如果XML文件中要设置的当前连接ID大于最大的ID，或是小于0
        {
            parentProcess.ShowError("XML文件中要设置的当前连接ID大于最大的ID" + (parentProcess.stConnectArry.size() - 1) + "，或是小于0");
            return false;
        }
        currConnectInfo = (CONNECTINFO)parentProcess.stConnectArry.get(iIndex);

        if(bShowInfo)
            parentProcess.ShowSuccess("当前连接索引更改，现在的索引是 " + iIndex + " ;" + "ID：" + currConnectInfo.sUid + "; 口令："
                    + currConnectInfo.sPwd + "; 初始连接串：" + currConnectInfo.sConn_url + ";  驱动串：" + currConnectInfo.sProvider);
        return true;
    }
    public Node DoIf(Node m_XmlNode)
    {
        String sValues;
        String FromSql = "FromSql:";
        sValues = GetNodeText(m_XmlNode).trim();
        boolean ret = false;
        if (sValues.startsWith("BOOL:"))
        {
            sValues = sValues.substring(5);

            sValues = ReplaceRunInfo(sValues);
            try
            {
                ret = parentProcess.GetBoolVal(sValues);
            }
            catch (Exception e) {
                parentProcess.ShowError("BOOL型 IF表达式计算错误！");
            }
        }
        else if (sValues.startsWith(FromSql)) {
            sValues = sValues.substring(FromSql.length());
            sValues = ReplaceRunInfo(sValues);
            try
            {
                ret = GetStrFromSql(sValues);
            }
            catch (Exception e) {
                ret = false;
            }
        }
        else
        {
            sValues = ReplaceRunInfo(sValues);
            try
            {
                ret = GetStrFromSql(sValues);
            }
            catch (Exception e) {
                ret = false;
            }
        }
        if (!ret)   //如果为false
        {
            m_XmlNode = ExeElseNode(m_XmlNode);
        }
        return m_XmlNode;
    }
    private Node ExeElseNode(Node m_XmlNode)
    {
        while(m_XmlNode != null)
        {
            if (m_XmlNode.getNodeName().equals("ELSE"))
            {
                SearchXmlNode(m_XmlNode.getFirstChild(), true);
                break;
            }
            m_XmlNode = m_XmlNode.getNextSibling();
        }
        return m_XmlNode;
    }
    private boolean GetStrFromSql(String instr)throws  Exception   //判断有无结果集
    {
        Connection tmp_conn = null;
        PreparedStatement tmp_state = null;
        try
        {
            tmp_conn = DriverManager.getConnection(currConnectInfo.sConn_url,currConnectInfo.sUid,currConnectInfo.sPwd);
            tmp_state = tmp_conn.prepareStatement(instr);
            if(tmp_state.execute())                              //有非空结果集返回
            {
                if(tmp_state.getResultSet().next())
                    return true;
                else
                    return false;
            }
        }
        finally {
            if(tmp_state != null && !tmp_state.isClosed())
                tmp_state.close();
            if(tmp_conn != null && !tmp_conn.isClosed())
                tmp_conn.close();
        }
        return true;
    }
    public boolean CheckResultRows(int iExcRows)           //检查结果集行数
    {
        try
        {
            if(currConnectInfo == null || currConnectInfo.rs == null)
            {
                parentProcess.ShowError("上一次执行的语句没有生成记录对像");
                return false;
            }
            if(currConnectInfo.rs.getMetaData().getColumnCount() == 0)  //更新语句
            {
                if(iExcRows != currConnectInfo.Affect_Rows)
                {
                    parentProcess.ShowError("语句执行影响的行数预期的值不一致！\n" + "预期值为:"
                            + iExcRows + "\n实际返回值：" + currConnectInfo.Affect_Rows);
                    return false;
                }
            }
            else
            {
                int rows = 0;
                if(currConnectInfo.rs.next())
                {
                    if (parentProcess.pro_config.bValIsShowResult)
                    {
                        parentProcess.ShowError("“不检查结果集的情况下显示结果集”功能被打开，无法进行结果集行数统计操作");
                        return false;
                    }
                    rows++;
                    while(currConnectInfo.rs.next())
                    {
                        rows++;
                    }
                }
                if(rows != iExcRows)
                {
                    parentProcess.ShowError("结果集行数跟预期的行数不一致！\n" + "预期值为:" + iExcRows + "\n实际返回值：" + rows);
                    return false;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }
    public boolean CheckEffectRows(int iExcRows)           //检查影响行数
    {
        try
        {

            if(currConnectInfo.rs != null && currConnectInfo.Affect_Rows == -1)  //查询语句
            {
                if(currConnectInfo.rs.next())  //有结果集
                {
                    if(iExcRows == 0)
                    {
                        parentProcess.ShowError("上一次执行的语句生成了结果集，但是脚本无受影响行数");
                        return false;
                    }
                    else
                        return true;
                }
                else              //无结果集
                {
                    if(iExcRows != 0)
                    {
                        parentProcess.ShowError("上一次执行的语句没有结果集，但脚本结果集有行数");
                        return false;
                    }
                    else
                        return true;
                }
            }
            if(currConnectInfo != null && currConnectInfo.rs == null && currConnectInfo.Affect_Rows == -1) //更新语句且无受影响行数
            {
                if(iExcRows != 0)
                {
                    parentProcess.ShowError("上一次执行的语句无影响行数，但脚本有受影响行数");
                    return false;
                }
                else
                    return true;
            }
            if(iExcRows != currConnectInfo.Affect_Rows)
            {
                parentProcess.ShowError("语句执行影响的行数预期的值不一致！\n" + "预期值为:" +
                        iExcRows + "\n实际返回值：" + currConnectInfo.Affect_Rows);
                return false;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }
    private String GetExpVal(Node m_XmlNode)
    {
        String values;
        String ret = null;
        m_XmlNode = FindXmlNode(m_XmlNode.getFirstChild(), "EXP");
        if (m_XmlNode == null)
        {
            return null;
        }
        values = GetNodeText(m_XmlNode);
        values = ReplaceRunInfo(values);
        try
        {
            ret  =  AviatorEvaluator.execute(values).toString();
        }
        catch (Exception e)
        {
            parentProcess.ShowError("表达式 (" + ReplaceRunInfo(GetNodeText(m_XmlNode)) + ") 语法错:" + e.getMessage());
        }
        return ret;
    }
    private Node FindXmlNode(Node m_XmlNode, String m_XmlNodeName)    //在当前层次递归寻找某个节点
    {
        if(m_XmlNodeName == "")
            return null;
        while(m_XmlNode != null)
        {
            if(m_XmlNode.getNodeName() == m_XmlNodeName)
                return m_XmlNode;
            Node m_XmlTempNode = FindXmlNode(m_XmlNode.getFirstChild(), m_XmlNodeName);
            if(m_XmlTempNode != null)
                return m_XmlTempNode;
            m_XmlNode = m_XmlNode.getNextSibling();
        }
        return null;
    }

    public Node FindXmlNodeEx(Node m_XmlNode, String m_XmlNodeName)    //只在第一层子节点中查找
    {
        if(m_XmlNodeName == "")
            return null;
        while(m_XmlNode != null)
        {
            if(m_XmlNode.getNodeName() == m_XmlNodeName)
                return m_XmlNode;
            m_XmlNode = m_XmlNode.getNextSibling();
        }
        return null;
    }
    private void StartLoopNode(Node m_XmlNode)
    {
        boolean m_noShowTemp = m_noShow;
        if(cLoop == null)
        {
            cLoop = new LOOP_STRUCT();
            cLoop.prev = null;
            cLoop.times = 0;
        }
        else
        {
            LOOP_STRUCT tempLoop = new LOOP_STRUCT();
            tempLoop.prev = cLoop;
            tempLoop.times = 0;
            cLoop = tempLoop;
        }
        try
        {
            Node m_xmlTempNode = this.FindXmlNode(m_XmlNode.getFirstChild(), "STARTTIMES");
            if (m_xmlTempNode == null)
            {
                cLoop.start = 1;
            }
            else
            {
                String sValues = GetNodeText(m_xmlTempNode);
                cLoop.start = Integer.parseInt(sValues.trim());
            }
            m_xmlTempNode = this.FindXmlNode(m_XmlNode.getFirstChild(), "TIMES");
            if (m_xmlTempNode == null)
            {
                cLoop.times = 2147000000;
            }
            else
            {
                String sValues = GetExpVal(m_xmlTempNode);
                if(sValues == null)
                    sValues = GetNodeText(m_xmlTempNode);
                sValues = ReplaceRunInfo(sValues);             //替换运行信息
                cLoop.times = Integer.parseInt(sValues.trim());
            }
        }
        catch(Exception e)
        {
            parentProcess.ShowError("没有找到LOOP执行的次数，或是次数的字符串非法" + e.getMessage());
        }

        Node m_XmlNodeTemp = FindXmlNodeEx(m_XmlNode.getFirstChild(), "NOSHOW");   //只在第一层节点查找NOSHOW标签
        if (m_XmlNodeTemp != null)
        {
            m_noShow = true;
            String val = GetNodeText(m_XmlNodeTemp);
            if (val.equals("false"))
            {
                m_noShow = false;
            }
            else
            {
                parentProcess.ShowSuccess("将隐藏执行循环中的脚本.....");
            }
        }
        int times = cLoop.times;

        if (cLoop.start > 0) {
            cLoop.times = cLoop.start;
            while(cLoop.times <= times && !currSqlCase.stCaseResult.bBreak && !parentProcess.stXmlRunInfo.bStop)
            {
                SearchXmlNode(m_XmlNode.getFirstChild(), true);
                if(cLoop.breakflag)
                    break;
                cLoop.times ++;
            }
        }
        else{
            cLoop.times = times;
            while(cLoop.times >= 1 && !currSqlCase.stCaseResult.bBreak && !parentProcess.stXmlRunInfo.bStop)
            {
                SearchXmlNode(m_XmlNode.getFirstChild(), true);
                if(cLoop.breakflag)
                    break;
                cLoop.times --;
            }
        }
        cLoop = (LOOP_STRUCT)cLoop.prev;
        if (m_XmlNodeTemp != null)
        {
            m_noShow = m_noShowTemp;
        }
    }
    private void FetchNext()
    {
        if (currConnectInfo == null || currConnectInfo.isOpenResult == false) {
            parentProcess.ShowError("请把该操作放在<OPEN>关键字内！");
            cRunInfo.put("FETCHNEXT","0");
            return;
        }
        if(currConnectInfo.rs== null)
        {
            parentProcess.ShowError("结果集对像为空，上一次执行的语句没有产生相应的结果集！");
        }
        try
        {
            if (currConnectInfo.rs.next()) {
                currConnectInfo.iFetch ++;
                cRunInfo.put("FETCHNEXT",Integer.toString(currConnectInfo.iFetch));
                for (int i = 0; i < currConnectInfo.rs.getMetaData().getColumnCount(); i++) {
                    String colName = currConnectInfo.rs.getMetaData().getColumnName(i + 1);
                    String sVal;
                    if (colName.length() > 0) {
                        sVal = currConnectInfo.rs.getString(i + 1);		//把列中的值。变为字符串再进行比较
                        cRunInfo.put(colName, sVal);
                    }
                }
            }
            else
            {
                cRunInfo.put("FETCHNEXT","0");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    private String GetStrFromSql(String _sql, boolean _CheckError)
    {
        String ret = "";
        try
        {
            Connection g_cn = DriverManager.getConnection(currConnectInfo.sConn_url,currConnectInfo.sUid,currConnectInfo.sPwd);
            PreparedStatement g_state = g_cn.prepareStatement(_sql);
            if (!m_noShow)
            {
                System.out.println(_sql);
            }
            if(g_state.execute())                  //有结果集
            {
                ResultSet rs = g_state.getResultSet();
                if(rs.next())
                {
                    ret = rs.getObject(1).toString();
                }
                else
                    ret = "";
            }
            else                  //无结果集
            {
                ret = "1";
            }
            if(!g_state.isClosed())
                g_state.close();
            if(!g_cn.isClosed())
                g_cn.close();
        }
        catch(Exception e)
        {
            if (_CheckError) {
                parentProcess.ShowSuccess(_sql);
                parentProcess.ShowSuccess(e.getMessage());
            }
            else
            {
                parentProcess.ShowSuccess(e.getMessage());
            }
        }
        return ret;
    }
    private void StartLock(Node m_XmlNode)                                //开始加锁执行该节点
    {
        Node x_temp;
        x_temp = FindXmlNode(m_XmlNode.getFirstChild(), "VALNAME");
        if(x_temp == null)
        {
            parentProcess.ShowError("未指定加锁对象名");
            return ;
        }
        synchronized (parentProcess.pro_config.valclass.get(GetNodeText(x_temp)))            //序列化执行代码
        {
            parentProcess.ShowSuccess("子进程开始加锁" + GetNodeText(x_temp) + "节点");
            SearchXmlNode(m_XmlNode.getFirstChild(), true);
            parentProcess.ShowSuccess("子进程结束加锁" + GetNodeText(x_temp) + "节点");
        }
    }
    public boolean  ExecuteSQL(String m_Sql)
    {
                                //该引擎实例临界区代码 ,执行SQL语句并检查时代码要互斥
            cRunInfo.put("SQLSTATE","");
            cRunInfo.put("RETCODE","0");
            cRunInfo.put("EFFECTROWS","0");
            m_Sql = ReplaceRunInfo(m_Sql);
            try
            {
                if(currConnectInfo.rs != null && !currConnectInfo.rs.isClosed())
                    currConnectInfo.rs.close();                   //关闭结果集
                if(currConnectInfo.State != null && !currConnectInfo.State.isClosed())
                    currConnectInfo.State.close();               //关闭陈述
                currConnectInfo.cm = m_Sql;
                Date time1;
                Date time2;
                m_Sql = m_Sql.trim();               //去除String前后空白
                currConnectInfo.rs = null;
                if (!m_noShow)
                {
                    System.out.println(m_Sql);
                }
                currConnectInfo.isOpenResult = false;
                time1 = new Date();                         //为执行语句计时
                currConnectInfo.State = currConnectInfo.cn.prepareStatement(m_Sql);
                if(currConnectInfo.State.execute())                              //有结果集返回
                {
                    currConnectInfo.rs = currConnectInfo.State.getResultSet();                     //获取结果集
                    currConnectInfo.Affect_Rows = -1;                                    //获取更新计数
                }
                else
                {
                    currConnectInfo.rs = currConnectInfo.State.getResultSet();
                    currConnectInfo.Affect_Rows = currConnectInfo.State.getUpdateCount();
                }
                time2 = new Date();
                Long usedtimes = time2.getTime() - time1.getTime();    //取出执行消耗的时间

                cRunInfo.put("USEDTIMES", usedtimes.toString());

                if(parentProcess.pro_config.bValIsOutTime && !m_noShow)
                {
                    parentProcess.ShowSuccess("执行本语句消耗了" + usedtimes.toString() + "毫秒。");    //显示时间间隔
                }
            }
            catch (SQLException e)
            {

                return CheckExecute(false, m_Sql, e);
            }
            catch(Exception e)
            {
                parentProcess.ShowError("语句执行过程中发生异常！\n" + e.getMessage());
                return false;
            }
            return CheckExecute(true, m_Sql, null);
    }
    public boolean CheckExecute(boolean m_su, String m_sql, SQLException e)
    {
        String retcode = "0";
        String sqlstate = "";
        if(parentProcess.stXmlRunInfo.bClearEn)	//如果该操作是用来清除环境的，那么，对它运行的正确情况不作检查
        {
            if(!m_su)
            {
                String sMessages = "";
                sMessages += "Message: " + e.getMessage() + "\n" +
                        "NativeError: " + e.getErrorCode() + "\n" +
                        "State: " + e.getSQLState() + "\n";
               parentProcess.ShowSuccess(sMessages);
            }
            return true;
        }
        //不是用来清除环境
        String errorMessages = "";
        errorMessages = "语句：" + m_sql + "\n预期执行结果：" +
                currSqlCase.stSqlResult.sExpResult
                + "\n实际执行结果：";
        if(m_su)   //sql的执行结果
        {
            errorMessages += "DIRECT_EXECUTE_SUCCESS\n";
        }
        else      //sql执行失败
        {
            errorMessages += "DIRECT_EXECUTE_FAIL\n";
            String sMessages = "";             //执行符合预期的输出信息
            errorMessages += "Message: " + e.getMessage() + "\n" +
                    "NativeError: " + e.getErrorCode() + "\n" +
                    "State: " + e.getSQLState() + "\n";

            cRunInfo.put("SQLSTATE", sqlstate);
            cRunInfo.put("RETCODE", retcode);
            if(!currSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_FAIL"))
            {
                if(currSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_IGNORE"))
                {
                    parentProcess.ShowSuccess(sMessages);
                    return true;
                }
                parentProcess.ShowError(errorMessages);        //预期结果不是失败，实际执行失败
                //-----com.wen.test
           //     sqlCounter.sqlLineNum = sqlCounter.findLine(stXmlRunInfo.sXmlFileName, "<SQL>", sqlCounter.sqlNum);
           //     if(sqlCounter.sqlLineNum > 0)
              //      ShowError("【" + testnum + "】" + "执行错误！\n ");//执行中报错处理，报告是在哪个测试点出错，把下面的定位到行的报错方式给取消
                //-----com.wen.test
            }
            else
            {
                parentProcess.ShowSuccess(sMessages);   //执行预期失败，实际执行也失败
            }
        }
        if(currSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_SUCCESS"))
        {
            if(m_su)         //预期执行成功，实际执行成功
            {
                return true;
            }
            else            //预期执行成功，实际执行失败
            {
                parentProcess.ShowError(errorMessages);
                //-----com.wen.test
              //  sqlCounter.sqlLineNum = sqlCounter.findLine(stXmlRunInfo.sXmlFileName, "<SQL>", sqlCounter.sqlNum);
              //  if(sqlCounter.sqlLineNum > 0)
               //     ShowError("【" + testnum + "】" + "执行错误！\n ");//执行中报错处理，报告是在哪个测试点出错，把下面的定位到行的报错方式给取消
                //-----com.wen.test
            }
        }
        else if(currSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_FAIL"))
        {
            if(!m_su)                     //实际执行失败
            {
                if (e == null)
                {
                    errorMessages += "语句执行失败，但是没有返回错误信息，请通知驱动开发人员";
                }
                else
                {
                    errorMessages = "语句：" + m_sql + "执行完成后\n";
                    boolean bFail = false;

                    if(currSqlCase.stSqlResult.iNativeError != 0)
                    {

                        if (currSqlCase.stSqlResult.iNativeError != e.getErrorCode())
                        {
                            errorMessages += "预期返回 ErrorCode: " +
                                    currSqlCase.stSqlResult.iNativeError + "\n实际返回返回 ErrorCode: " +
                                    e.getErrorCode() + "\n";
                            bFail = true;
                        }
                    }
                    if(currSqlCase.stSqlResult.sSQLState != "" && currSqlCase.stSqlResult.sSQLState != null)
                    {
                        if (!currSqlCase.stSqlResult.sSQLState.equals(e.getSQLState().trim()))
                        {
                            errorMessages += "预期返回 SQLState: " +
                                    currSqlCase.stSqlResult.sSQLState + "\n实际返回返回 SQLState: " + e.getSQLState() + "\n";
                            bFail = true;
                        }
                    }
                    if(bFail == false)
                        return true;
                }
            }
            parentProcess.ShowError(errorMessages);
            //-----com.wen.test
          //  sqlCounter.sqlLineNum = sqlCounter.findLine(stXmlRunInfo.sXmlFileName, "<SQL>", sqlCounter.sqlNum);
         //   if(sqlCounter.sqlLineNum > 0)
          //      ShowError("【" + testnum + "】" + "执行错误！\n ");//执行中报错处理，报告是在哪个测试点出错，把下面的定位到行的报错方式给取消
            //-----com.wen.test
        }
        else if(currSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_SELECT_COMPARE_RESULT")
                || currSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_SELECT_COMPARE_RESULT_FULL")
                || currSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_SELECT_WITH_RESULT"))
        {
            if(!m_su)
            {
                parentProcess.ShowError("结果集生成语句执行失败了，无法进行结果集比较");
                //-----com.wen.test
              //  sqlCounter.sqlLineNum = sqlCounter.findLine(stXmlRunInfo.sXmlFileName, "<SQL>", sqlCounter.sqlNum);
             //   if(sqlCounter.sqlLineNum > 0)
            //        ShowError("【" + testnum + "】" + "执行错误！\n ");//执行中报错处理，报告是在哪个测试点出错，把下面的定位到行的报错方式给取消
                //-----com.wen.test
                return false;
            }
            if(currSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_SELECT_WITH_RESULT"))
            {
                if(CheckResult(true))   //只比较列名
                    return true;
            }
            else
            {
                if(CheckResult(false))  //比较结果集
                    return true;
            }
        }
        return false;
    }
    public boolean CheckResult(boolean bCheckColName)
    {
        if(currConnectInfo.rs == null)
        {
            parentProcess.ShowError("结果集对像为空，上一次执行的语句没有产生相应的结果集！");
            return false;
        }
        if(xCurrentNode == null)
        {
            parentProcess.ShowError("在检查结果集时, 当前运行的XML结点引用为空，没有结点被引用");
            return false;
        }
        xCurrentNode = xCurrentNode.getNextSibling();
        xCurrentNode = parentProcess.SkipComment(xCurrentNode);
        if(xCurrentNode == null)
        {
            parentProcess.ShowError("在检查结果集时, 生成结果集语句后面没有发现结果集结点");
            return false;
        }
        if(!xCurrentNode.getNodeName().equals("RESULT"))
        {
            parentProcess.ShowError("被检察的当前结点，不是代表结果集的结点，该结点名称为:" + xCurrentNode.getNodeName());
            return false;
        }
        if(bCheckColName)   //如果检查列名称
        {
            Node m_colNum = FindXmlNode(xCurrentNode.getFirstChild(), "COLUMNNUMS");
            int m_colNums = 0;
            if (m_colNum != null)
            {
                try
                {
                    String sValues = GetNodeText(m_colNum);
                    sValues = ReplaceRunInfo(sValues);
                    m_colNums = Integer.parseInt(sValues.trim());
                }
                catch(Exception e)
                {
                    parentProcess.ShowError("非法的列数表达式："+e.getMessage());
                    return false;
                }
                if (m_colNums <= 0)
                {
                    parentProcess.ShowError("非法的例数值，列数应该大于等于1");
                    return false;
                }
                try
                {
                    if (currConnectInfo.rs.getMetaData().getColumnCount() != m_colNums) {
                        parentProcess.ShowError("结果集中的列数跟脚本中的列数不一致，" +
                                "结果集中的列数为:" +currConnectInfo.rs.getMetaData().getColumnCount());
                        return false;
                    }
                }
                catch (Exception e)
                {
                    parentProcess.ShowError("获取结果集结果集出错，无法比较\n" + e.getMessage());
                }
            }
            if (m_colNum != null)
            {
                return CheckResultColNameEx(xCurrentNode.getFirstChild(), m_colNums);   //只检查列名是否一致，不检查列值
            }
            else
            {
                return CheckResultColName(xCurrentNode.getFirstChild());
            }
        }
        else                    //如果不检查列名称
        {
            Node m_rowNum = FindXmlNode(xCurrentNode.getFirstChild(), "RECORDNUMS");
            if (m_rowNum == null)
            {
                return CheckResultRow(xCurrentNode.getFirstChild());  //不带结果行数的结果集检查
            }
            else
            {
                int rowNums = 0;
                try
                {
                    String sValues = GetNodeText(m_rowNum);
                    sValues = ReplaceRunInfo(sValues);
                    rowNums = Integer.parseInt(sValues.trim());
                }
                catch(Exception e)
                {
                    parentProcess.ShowError("非法的行数表达式："+e.getMessage());
                    return false;
                }
                return CheckResultRowEx(xCurrentNode.getFirstChild(), rowNums); //带结果行数的结果集检查
            }
        }
    }
    //检察结果集，只检察各列的名称，并不检察其中各行各列的值
    private boolean CheckResultColName(Node m_XmlNode)
    {
        m_XmlNode = parentProcess.SkipComment(m_XmlNode);
        if(m_XmlNode == null)
        {
            parentProcess.ShowError("未给定要比较的列名行");
            return false;
        }
        if(!m_XmlNode.getNodeName().equals("RECORD"))
        {
            parentProcess.ShowError("结果集起始行的名称不是代表结果集中" +
                    "行的结点，该结点名称为:" + m_XmlNode.getNodeName());
            return false;
        }
        Node m_tempNode = parentProcess.SkipComment(m_XmlNode.getFirstChild());     //跳过注释部分
        try
        {
            int m_maxColNum = 0;//表示当前XML文件结果集中列的最大数
            int m_rsmaxColNum = currConnectInfo.rs.getMetaData().getColumnCount();
            while(m_tempNode != null)
            {
                String ExpCol = GetColumn(m_tempNode);
                if(ExpCol == null)
                {
                    parentProcess.ShowError("未在当前节点找到列的名称");
                    return false;
                }
                ExpCol = ReplaceRunInfo(ExpCol);

                if(!ExpCol.equals(currConnectInfo.rs.getMetaData().getColumnName(m_maxColNum + 1)))
                {
                    parentProcess.ShowError("返回结果集的列名称和脚本中的不匹配\n返回：" +
                            "" + currConnectInfo.rs.getMetaData().getColumnName(m_maxColNum) +
                            "\n预期：" + ExpCol);
                    return false;
                }
                m_tempNode = m_tempNode.getNextSibling();
                m_tempNode = parentProcess.SkipComment(m_tempNode);
                m_maxColNum++;
                if(m_maxColNum >= m_rsmaxColNum && m_tempNode != null)
                {
                    parentProcess.ShowError("返回结果集的列数小于脚本中的列数" +
                            "不一致，结果集中存在 " + m_rsmaxColNum + "列");
                    return false;
                }
            }
            if(m_maxColNum != m_rsmaxColNum)
            {
                parentProcess.ShowError("返回结果集的列数和脚本中的列数不一致\n结果集：" +
                        "" + m_rsmaxColNum + "\n脚  本：" + m_maxColNum);
                return false;
            }
        }
        catch (Exception e)
        {
            parentProcess.ShowError(e.getMessage());
        }
        return true;
    }
    //检察结果集，只检察各列的名称，并不检察其中各行各列的值
    public boolean CheckResultColNameEx(Node m_colNode, int m_colnums)
    {
        m_colNode = FindXmlNode(m_colNode, "COLUMN");
        if (m_colNode == null)
        {
            parentProcess.ShowError("未发现COMUMN");
            return false;
        }
        if (FindXmlNode(m_colNode.getNextSibling(), "COLUMN") != null)
        {
            parentProcess.ShowError("在使用了COLUMNNUMS关键字的情况下，只能指定一个COLUMN列");
            return false;
        }
        int m_maxColNum = 1;//表示当前XML文件结果集中列的最大数
        while(m_maxColNum <= m_colnums)
        {
            cRunInfo.put("COLUMNNUMS",Integer.toString(m_maxColNum));
            String ExpCol = GetColumn(m_colNode);
            if(ExpCol == null)
            {
                parentProcess.ShowError("未在当前节点找到列的名称");
                return false;
            }
            ExpCol = ReplaceRunInfo(ExpCol);
            try
            {
                if(!ExpCol.equals(currConnectInfo.rs.getMetaData().getColumnName(m_maxColNum)))
                {
                    parentProcess.ShowError("返回结果集的列名称和脚本中的不" +
                            "匹配\n返回：" + currConnectInfo.rs.getMetaData().getColumnName(m_maxColNum) +
                            "\n预期：" + ExpCol);
                    return false;
                }
            }
            catch (SQLException e)
            {

            }
            m_maxColNum++;
        }
        return true;
    }
    public String GetColumn(Node m_XmlNode)
    {

        m_XmlNode = parentProcess.SkipComment(m_XmlNode);
        if(m_XmlNode == null)
        {
            return null;
        }
        if(!m_XmlNode.getNodeName().equals("COLUMN"))
        {
            parentProcess.ShowError("给定结点的名称不是代表结果集" +
                    "中列的结点，该结点名称为:" + m_XmlNode.getNodeName());
            return null;
        }
        return GetNodeText(m_XmlNode);
    }
    public boolean CheckResultRow(Node m_XmlNode)
    {
        m_XmlNode = parentProcess.SkipComment(m_XmlNode);
        try
        {
            if(m_XmlNode == null)
            {
                if(currConnectInfo.rs.next())
                {
                    parentProcess.ShowError("预期语句返回一个空的结果集，但是实际返回了一个非空结果集");
                    return false;
                }
                else
                {
                    return true;
                }
            }
            if(!m_XmlNode.getNodeName().equals("RECORD"))
            {
                parentProcess.ShowError("结果集起始行的名称不是代表结果集中行的结点，该结点名称为:" + m_XmlNode.getNodeName());
                return false;
            }
            Node m_colNum = FindXmlNode(m_XmlNode.getFirstChild(), "COLUMNNUMS");
            int m_colNums = 0;
            if (m_colNum != null)
            {
                try
                {
                    String  sValues = GetNodeText(m_colNum);
                    sValues = ReplaceRunInfo(sValues);
                    m_colNums = Integer.parseInt(sValues.trim());
                }
                catch(Exception e)
                {
                    parentProcess.ShowError("非法的例数表达式："+e.getMessage());
                    return false;
                }
                if (m_colNums <= 0)
                {
                    parentProcess.ShowError("非法的例数值，列数应该大于等于1");
                    return false;
                }
                if (currConnectInfo.rs.getMetaData().getColumnCount() != m_colNums)
                {
                    parentProcess.ShowError("结果集中的列数跟脚本中的列数不一致，" +
                            "结果集中的列数为:" + currConnectInfo.rs.getMetaData().getColumnCount());
                    return false;
                }
            }
            int m_row = 0;                        //表示当前的行数
            while(m_XmlNode != null)				//开始比较结果集
            {
                //这是结果集的行开始
                cRunInfo.put("RECORDNUMS", Integer.toString(m_row + 1));
                if(!currConnectInfo.rs.next())
                {
                    parentProcess.ShowError("结果集已经到达未尾，但是XML文件中还有未比较的行");
                    return false;
                }
                if (m_colNum == null)
                {
                    if (!CheckColumn(m_XmlNode.getFirstChild(), m_row))
                    {
                        break;
                    }
                }
                else
                {
                    if (CheckColumnEx(m_XmlNode.getFirstChild(), m_row, m_colNums))
                    {
                        break;
                    }
                }
                m_XmlNode = m_XmlNode.getNextSibling();
                m_XmlNode = parentProcess.SkipComment(m_XmlNode);
                m_row++;
            }
            if(m_XmlNode == null && currConnectInfo.rs.next())
            {
                parentProcess.ShowError("结果集未到达未尾，但是XML文件中已经没有了可以比较的行");
                return false;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }
    public boolean CheckResultRowEx(Node m_XmlNode, int rownums)
    {
        try
        {
            if(rownums <= 0)
            {
                if(currConnectInfo.ResultCounts > 0)
                {
                    parentProcess.ShowError("预期语句返回一个空的结果集，但是实际返回了一个非空结果集");
                    return false;
                }
                else
                {
                    return true;
                }
            }
            m_XmlNode = FindXmlNode(m_XmlNode, "RECORD");
            if (m_XmlNode == null)
            {
                parentProcess.ShowError("未找到RECORD行");
                return false;
            }
            if (FindXmlNode(m_XmlNode.getNextSibling(), "RECORD") != null)
            {
                parentProcess.ShowError("在使用RECORDNUMS关键字时，结果集中只能包含一个RECORD行");
                return false;
            }
            Node m_colNum = FindXmlNode(m_XmlNode.getFirstChild(), "COLUMNNUMS");
            int m_colNums = 0;
            if (m_colNum != null)
            {
                try
                {
                    String sValues = GetNodeText(m_colNum);
                    sValues = ReplaceRunInfo(sValues);
                    m_colNums = Integer.parseInt(sValues.trim());
                }
                catch(Exception e)
                {
                    parentProcess.ShowError("非法的例数表达式："+e.getMessage());
                    return false;
                }
                if (m_colNums<=0)
                {
                    parentProcess.ShowError("非法的例数值，列数应该大于等于1");
                    return false;
                }
                if (currConnectInfo.rs.getMetaData().getColumnCount() != m_colNums)
                {
                    parentProcess.ShowError("结果集中的列数跟脚本中的列数不一致，结果集中的列数为:" + currConnectInfo.rs.getMetaData().getColumnCount());
                    return false;
                }
            }
            int m_row = 1;//表示当前的行数
            while(m_row <= rownums)				//开始比较结果集
            {
                //这是结果集的行开始
                if(!currConnectInfo.rs.next())
                {
                    parentProcess.ShowError("结果集已经到达未尾，但是XML文件中还有未比较的行");
                    return false;
                }
                cRunInfo.put("RECORDNUMS", Integer.toString(m_row));
                if (m_colNum == null)
                {
                    if (!CheckColumn(m_XmlNode.getFirstChild(), m_row))
                    {
                        break;
                    }
                }
                else
                {
                    if (!CheckColumnEx(m_XmlNode.getFirstChild(), m_row, m_colNums))
                    {
                        break;
                    }
                }
                m_row++;
            }
            if(currConnectInfo.rs.next())
            {
                parentProcess.ShowError("结果集未到达未尾，但是XML文件中已经没有了可以比较的行");
                return false;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }
    public boolean CheckColumn(Node m_colNode, int m_row)
    {
        String errorMessage = "";
        int m_col = 1;
        m_colNode = parentProcess.SkipComment(m_colNode);
        try
        {
            while(m_colNode != null)
            {
                //这是结果集的每一列开始
                if(m_col > currConnectInfo.rs.getMetaData().getColumnCount())
                {
                    parentProcess.ShowError("XML文件中的列数超过了返回结果集中的列数");
                    return false;
                }
                String ExpCol = GetColumn(m_colNode);
                if(ExpCol == null)
                {
                    return false;
                }
                if(ExpCol.equals("NULL"))
                    ExpCol = null;
                ExpCol = ReplaceRunInfo(ExpCol);
                String RetCol = "";
                try
                {
                    if(currConnectInfo.rs.getObject(m_col) == null)
                        RetCol = null;
                    else
                        RetCol = currConnectInfo.rs.getObject(m_col).toString();	//把列中的值。变为字符串再进行比较
                }
                catch (SQLException e)
                {
                    String errorMessages = "Message: " + e.getMessage() + "\n" +
                            "NativeError: " + e.getErrorCode() + "\n" +
                            "State: " + e.getSQLState() + "\n";

                    parentProcess.ShowError(errorMessages);
                    return false;
                }
                if(!Check_Column_help(ExpCol,RetCol,m_row,m_col))
                    return false;
                m_colNode = m_colNode.getNextSibling();
                m_colNode = parentProcess.SkipComment(m_colNode);
                m_col++;
            }
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }
    public boolean CheckColumnEx(Node m_colNode, int m_row, int m_colnums)
    {
        String errorMessage = "";
        int m_col = 1;
        m_colNode = FindXmlNode(m_colNode, "COLUMN");
        if (m_colNode == null)
        {
            parentProcess.ShowError("未发现COMUMN");
            return false;
        }
        if (FindXmlNode(m_colNode.getNextSibling(), "COLUMN") != null)
        {
            parentProcess.ShowError("在使用了COLUMNNUMS关键字的情况下，只能指定一个COLUMN列");
            return false;
        }
        while(m_col <= m_colnums)
        {
            cRunInfo.put("COLUMNNUMS", Integer.toString(m_col));
            String ExpCol = GetColumn(m_colNode);
            if(ExpCol == null)
            {
                return false;
            }
            if(ExpCol.equals("NULL"))
                ExpCol = null;
            ExpCol = ReplaceRunInfo(ExpCol);
            String RetCol = "";
            try
            {
                if(currConnectInfo.rs.getObject(m_col) == null)
                    RetCol = null;
                else
                    RetCol = currConnectInfo.rs.getObject(m_col).toString();    //把列中的值。变为字符串再进行比较
            }
            catch (SQLException e)
            {
                String errorMessages = "";

                errorMessages += "Message: " + e.getMessage() + "\n" +
                        "NativeError: " + e.getErrorCode() + "\n" +
                        "State: " + e.getSQLState() + "\n";

                parentProcess.ShowError(errorMessages);
                return false;
            }
            catch (Exception e)
            {
                errorMessage += "结果集在第" + (m_row) + "行，第" + (m_col + 1) +"列处,";
                errorMessage += e.getMessage();
                parentProcess.ShowError(errorMessage);
                return false;
            }
            if(!Check_Column_help(ExpCol,RetCol,m_row,m_col))
                return false;
            m_col++;
        }
        return true;
    }
    private void StartWait(Node m_XmlNode)             //阻塞等待
    {
        Long seconds = null;
        try
        {
            seconds = Long.parseLong(GetNodeText(m_XmlNode));
        }
        catch (Exception e)
        {
            parentProcess.ShowError("指定等待时间格式错误");
        }
        Node lockNode = m_XmlNode;
        while(!lockNode.getNodeName().equals("LOCK"))
        {
            lockNode = lockNode.getParentNode();
            if(lockNode == null)
            {
                parentProcess.ShowError("WAIT节点必须处于LOCK节点内");
            }
        }
        Node x_temp = FindXmlNode(lockNode.getFirstChild(), "VALNAME");
        if(x_temp == null)
        {
            parentProcess.ShowError("未指定等待对象名");
            return ;
        }
        try {
            parentProcess.ShowSuccess(GetNodeText(x_temp) + "即将开始等待...");
            if(seconds >= 0)
                parentProcess.pro_config.valclass.get(GetNodeText(x_temp)).wait(seconds);
            else
                parentProcess.pro_config.valclass.get(GetNodeText(x_temp)).wait();
            parentProcess.ShowSuccess(GetNodeText(x_temp) + "已结束等待...");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void StartNotify(Node m_XmlNode)
    {
        Node lockNode = m_XmlNode;
        while(!lockNode.getNodeName().equals("LOCK"))
        {
            lockNode = lockNode.getParentNode();
            if(lockNode == null)
            {
                parentProcess.ShowError("NOTIFY节点必须处于LOCK节点内");
            }
        }
        Node x_temp = FindXmlNode(lockNode.getFirstChild(), "VALNAME");
        if(x_temp == null)
        {
            parentProcess.ShowError("未指定通知对象名");
            return ;
        }
        if(GetNodeText(m_XmlNode).equals("ALL"))
            parentProcess.pro_config.valclass.get(GetNodeText(x_temp)).notifyAll();
        else
            parentProcess.pro_config.valclass.get(GetNodeText(x_temp)).notify();
        parentProcess.ShowSuccess(GetNodeText(x_temp) + "已经被通知...");
    }
    public void GetVal(Node m_XmlNode)
    {
        String sName;
        String sVal;
        Node x_temp;
        x_temp = FindXmlNode(m_XmlNode.getFirstChild(), "VALNAME");
        if (x_temp != null)
        {
            sName = GetNodeText(x_temp).trim();
            if (sName.length() == 0)
            {
                parentProcess.ShowError("未在GETVAL节点中找到VALNAME节点的属性名");
                return;
            }
        }
        else
        {
            parentProcess.ShowError("未在GETVAL节点中找到VALNAME节点");
            return;
        }
        x_temp = FindXmlNode(m_XmlNode.getFirstChild(), "VAL");
        if (x_temp != null)
        {
            sVal = GetNodeText(x_temp).trim();
            if (!sVal.startsWith("@"))
            {
                parentProcess.ShowError("VAL节点中应该指明的是被存放属性值的一个替代符，而不是一个普通的字符串");
                return;
            }
            String sTemp = parentProcess.pro_config.getVal(sName);
            if (sTemp == null)
            {
                parentProcess.ShowError("未找到属性：" + sName);
            }
            else
                cRunInfo.put(sVal.substring(1), sTemp);
        }
        else
        {
            parentProcess.ShowError("未在GETVAL节点中找到SETAT节点");
        }
    }
    public void SetVal(Node m_XmlNode)
    {
        String sName;
        String sVal;
        Node x_temp;
        x_temp = FindXmlNode(m_XmlNode.getFirstChild(), "VALNAME");
        if (x_temp != null)
        {
            sName = GetNodeText(x_temp).trim();
            if (sName.length() == 0)
            {
                parentProcess.ShowError("未在SETVAL节点中找到VALNAME节点的属性名");
                return;
            }
        }
        else
        {
            parentProcess.ShowError("未在SETVAL节点中找到VALNAME节点");
            return;
        }
        x_temp = FindXmlNode(m_XmlNode.getFirstChild(), "VAL");
        if (x_temp != null)
        {
            sVal = GetNodeText(x_temp).trim();
            sVal = ReplaceRunInfo(sVal);
            if(sVal.equals("DEL"))
            {
                parentProcess.pro_config.delVal(sName);
            }
            else
                parentProcess.pro_config.setVal(sName, sVal);
        }
        else
        {
            parentProcess.ShowError("未在SETVAL节点中找到VAL节点");
        }
    }
    public boolean Check_Column_help(String ExpCol,String RetCol, int m_row, int m_col)
    {
        if(ExpCol != null && ExpCol.startsWith("\"") && ExpCol.endsWith("\""))              //截取""字符串
            ExpCol = ExpCol.substring(1,ExpCol.length() - 1);
        String errorMessage = "";
        if(ExpCol == null && RetCol != null)
        {
            errorMessage += "脚本结果集在第" + (m_row) + "行，第" + (m_col) +"列处，结果不一致\n";
            errorMessage += "预期值: 空值（null)" ;
            errorMessage += "\n返回值: " + RetCol;
            parentProcess.ShowError(errorMessage);
            return false;
        }
        else if(ExpCol != null && RetCol == null)
        {
            errorMessage += "脚本结果集在第" + (m_row) + "行，第" + (m_col) +"列处，结果不一致\n";
            errorMessage += "预期值: " + ExpCol;
            errorMessage += "\n返回值: 空值(null) ";
            parentProcess.ShowError(errorMessage);
            return false;
        }
        else if(ExpCol != null && !ExpCol.equals(RetCol))
        {
            errorMessage += "脚本结果集在第" + (m_row) + "行，第" + (m_col) +"列处，结果不一致\n";
            errorMessage += "预期值: " + ExpCol;
            errorMessage += "\n返回值: " + RetCol;
            parentProcess.ShowError(errorMessage);
            return false;
        }
        return true;
    }
}
