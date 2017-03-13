package com.wen;

import java.util.*;
import java.sql.*;
import java.io.StringReader;
import java.io.File;
import java.util.Date;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.googlecode.aviator.AviatorEvaluator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

//////////////////////////////////////////////
class CASERESULT                   //针对标签SQL_CASE而定义的用来存储当前所在SQL_CASE的节点
{
public boolean bBreak;			//用来控制程序是否跳出当前所在的SQL_CASE的执行;
public boolean bSuccess;		//用来表示该SQL_CASE的执行结果，成功还是失败
public boolean bFailStop;		//用来表示该SQL_CASE执行时，是否是碰到失败就跳出这个SQL_CASE的执行
public boolean bExpResult;		//用来表示这个SQl_CASE模块预期的执行结果
}
////////////////////////////////////////////////////////////////
class SQLRESULT                //表示当前正在执行的SQL语句的一些信息，针对SQL标签的节点
{
public String sExpResult;	          //SQL执行完以后预期的结果
public String sSQLState;			//预期返回的状态码
public int iNativeError;			//预期返回的系统错误代码
}
/////////////////////////////////////////////////////////////////
class SqlCase                      //针对SQL_CASE定义的嵌套SQL_CASE类，可以获得当前SQL_CASE的结果信息
{
   public CASERESULT stCaseResult;
   public SqlCase cParentCase;     //指向该SQL_CASE的父亲SQL_CASE对象
   public SQLRESULT stSqlResult;   //该SQL_CASSE当前正在执行的SQL语句的信息
   public SqlCase()
   {
      //
      // TODO: 在此处添加构造函数逻辑
      //
      stCaseResult = new CASERESULT();
      stCaseResult.bBreak = false;			//默认值为不跳出
      stCaseResult.bSuccess = true;		//默认值为执行成功
      stCaseResult.bExpResult = true;
      stCaseResult.bFailStop = true;
      stSqlResult = new SQLRESULT();
      stSqlResult.sExpResult = "DIRECT_EXECUTE_SUCCESS";
      stSqlResult.sSQLState = "";
   }
}
////////////////////////////////////////////////////////////////
class LOOP_STRUCT                      //记录如果当前执行引擎处于循环内时，控制循环的信息
{
   public Object prev;				//指向父LOOP
   public int times;				//执行的次数
   public int start;				//执行的开始
   public boolean breakflag;       //是否终端循环
}
///////////////////////////////////////////////////////////////
class CONNECTINFO              //进程数据库连接信息类
{
   public Connection cn;		//当前的连接
   public String cm;			//当前的连接正在执行的命令(SQL)
   public ResultSet rs;		          //当前的命令行生成的结果集
   public PreparedStatement State;            //当前连接的陈述
   public int ResultCounts;                   //结果集行数
   public int Affect_Rows;                 //上一次语句执行受影响行数
   public String sProvider;        //存驱动类
   public String sConn_url;			//用来存数据库连接串
   public String sServerName;		//服务器IP或名称
   public String sUid;				//用户名
   public String sPwd;				//用户口令
   public String sDatabase;			//初始化数据库名
   public String sPort;			   //端口
   public boolean isOpenResult;		//是否已经使用OPEN打开结果集游标
   public int iFetch;				//游标所在的当前行数
}
//////////////////////////////////////////////////////////////
class XMLRUNINFO                        //存储当前正在运行的XML文件的一些信息
{
public boolean bStop;					//是否终止当前测试用例的运行
public boolean bPause;                 //是否正在暂停XML的执行
public boolean bClearEn;				//表示该对像是否是用来清空测试环境的
public String sXmlFileName;			//相应的XML文件名
public String XmlStr;				// XML内容
public String User_ID;             //用户ID
public Document xDoc;					// = new XmlDocument();
public Node xFirstNode;				//对像开始执行的第一个结点
public Node xCurrentNode;				//对像执行的当前结点
public Node xRefNode;				//分析函数引用返回Node
public NodeList childNodes;           //当前结点的孩子节点链表
public SqlCase cCurrentSqlCase;     //该XML当前正在执行的SQL_CASE
public Map cRunInfo;            //存储@替换符的信息
}
///////////////////////////////////////////////////////////////////

//
@WebService
public class XmlProcess {

   public CONNECTINFO stConnectINfo;              //当前XML处理进程所启用的连接
   public ArrayList stThreadArry;                //当前XML开启的所有子线程
   public ArrayList stConnectArry;               //当前XML开启的所有连接
   public XMLRUNINFO stXmlRunInfo;               //当前XML文件处理的运行信息
   public LOOP_STRUCT cLoop;                     //当前XML文件处理的循环控制信息
   public SqlCase cSqlCase;                      //当前XML的SQL_CASE结构信息
   public boolean	isHasTimes;                   //是否有次数标签，当处于LOOP循环中
   public int		run_times;                   //运行次数
   public boolean	m_noShow;                      //是否显示执行的语句
   public boolean   bPoolEnable;		        //是否启用连接池功能
   public ProClass  pro_config;               //前台属性配置
   public String     testnum = "测试点0：";              //用于保存测试点的辅助信息
   String sProvider;			//用来存数据库JDBC驱动
   String sConn_url;           //用来存储数据库连接串
   String sServerName;		//服务器IP或名称
   String sUid;				//用户名
   String sPwd;				//用户口令
   String sDatabase;		//初始化数据库名
   String sPort;			//端口



   //////////////////////////////////////////
   public Counter sqlCaseCounter = new Counter();      //sqlCase计数
   public Counter sqlCounter = new Counter();
   public Counter newConnCounter = new Counter();
   public Counter resultCounter = new Counter();
   public int CurrentLineNum = 0;                      //当前行号



   @WebMethod
   public boolean run_xml
           (@WebParam(name = "xmlStr")String xmlStr, @WebParam(name = "filename")String  filename)
   {
      InitInfo(xmlStr, filename);
      return Run(false);
   }
   @WebMethod
   public boolean stop_xml(@WebParam(name = "UID")String UID)          //停止执行
   {
      return true;
   }
   @WebMethod
   public boolean pause_xml(@WebParam(name = "UID")String UID)           //暂停执行
   {
     return true;
   }
   public boolean RunOneNode(Node m_XmlNode, String xmlStr, String filename)   //用于多线程运行
   {
      InitInfo(xmlStr, filename);
      ShowSuccess("子线程:" + Thread.currentThread().getId() +",开始执行...");

      SearchXmlNode(m_XmlNode, true);	//直接从该节点开始搜下面的结点，并分析运行它们要求的操作

      return cSqlCase.stCaseResult.bSuccess;
   }
   public boolean Run(boolean bTransfer)
   {
      if(bTransfer == false)
         ShowSuccess("开始执行...");
      if(stXmlRunInfo.xFirstNode == null)				//如果该测试文件对像，不是以给定首结点的方式执行的，那么就读出文件，找出首结点
      {
         try
         {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            stXmlRunInfo.xDoc = builder.parse(new InputSource(new StringReader(stXmlRunInfo.XmlStr)));
            stXmlRunInfo.xDoc.normalize();
            Node m_XmlNode= FindXmlNode(stXmlRunInfo.xDoc.getFirstChild(), "SQLTEST");  //得到SQL_TEST节点
            if(m_XmlNode == null)
            {
               ShowError("未在XML文件中找到关键字(SQLTEST)");
               return false;
            }
            while(run_times > 0)
            {
               SearchXmlNode(m_XmlNode, true);
               //在这函数里面执行SQL_TEST节点，在找出各个结点的同时，对结点进行分析，并根据分析的结果，执行结点要求的操作
               if(cSqlCase.stCaseResult.bSuccess != cSqlCase.stCaseResult.bExpResult)
               {
                  ShowError("脚本中预期的执行结果和实际的执行结果不一致！预期：" + cSqlCase.stCaseResult.bExpResult +
                          "; 实际：" + cSqlCase.stCaseResult.bSuccess);
                  cSqlCase.stCaseResult.bSuccess = false;
               }
               else
               {
                  cSqlCase.stCaseResult.bSuccess = true;
               }
               run_times --;
            }
         }
         catch(Exception e)
         {
            ShowError(e.getMessage());
         }
      }
      else
      {
         SearchXmlNode(stXmlRunInfo.xFirstNode, true);	//直接从首节点开始搜下面的结点，并分析运行它们要求的操作
      }
      ClearEnvironment();	//清除本测试用例的环境,该函数直接跳到CLEAR结点开始执行
      DisConnect(-1);//释放全部连接
      if (cSqlCase.stCaseResult.bSuccess)
      {
         System.out.println("测试通过");
      }
      else
      {
         System.out.println("测试失败");
      }
      return cSqlCase.stCaseResult.bSuccess;
   }
   public void InitInfo(String xmlStr, String filename)
   {
      pro_config = new ProClass();
      cLoop = null;
      stConnectArry = new ArrayList();                //主线程已经建立的连接
      stThreadArry = new ArrayList();     //主线程已经开启的所有子线程
      stXmlRunInfo = new XMLRUNINFO();
      stXmlRunInfo.sXmlFileName = filename;    //正在运行的XML文件名
      stXmlRunInfo.XmlStr = xmlStr;           //正在运行的XML字符串
      stXmlRunInfo.cRunInfo = new HashMap();    //存储替代符的HASH_MAP
      sConn_url = pro_config.sValConnUrl;
      sProvider = pro_config.sValDriveName;
      sUid = pro_config.sValUserId;
      sPwd = pro_config.sValPassword;
      cSqlCase = new SqlCase();
      stXmlRunInfo.cCurrentSqlCase = cSqlCase;
      run_times = 1;
      isHasTimes = false;
      try {
         // JDBC
         Class.forName(sProvider);      //加载JDBC
      }
      catch (ClassNotFoundException e)
      {
         e.printStackTrace();
      }
   }
   /// <summary>
   /// //用来遍历XML结点,并分析各个结点
   /// </summary>
   private void SearchXmlNode(Node  m_XmlNode, boolean bFindNext)    //搜寻xmlnode并解析执行,bFindNext表示是否并列执行该节点的所有兄弟节点
   {
      if(m_XmlNode == null)
         return;
      try
      {
         while(m_XmlNode != null && stXmlRunInfo.bStop != true)
         {
            if(m_XmlNode.getNodeName().equals("SQL_CASE"))
            {
               sqlCaseCounter.sqlCaseNum++;
               SqlCase cTempSqlCase = new SqlCase();
               stXmlRunInfo.cRunInfo.put("com.wen.CASERESULT", "TRUE");
               cTempSqlCase.cParentCase = stXmlRunInfo.cCurrentSqlCase;  //记录父亲SQL_CASE
               stXmlRunInfo.cCurrentSqlCase = cTempSqlCase;   //更改当前SQL_CASE
               SearchXmlNode(m_XmlNode.getFirstChild(), true);	//遍历并执行当前节点的子节点
               if(cTempSqlCase.stCaseResult.bSuccess != cTempSqlCase.stCaseResult.bExpResult)
               {
                  ShowError("SQL_CASE中预期的执行结果和实际的执行结果不一致！预期：" + cTempSqlCase.stCaseResult.bExpResult + "; 实际：" + cTempSqlCase.stCaseResult.bSuccess);
                  sqlCaseCounter.sqlCaseLineNum = sqlCaseCounter.findLine(stXmlRunInfo.sXmlFileName,"<SQL_CASE>", sqlCaseCounter.sqlCaseNum);
                  sqlCaseCounter.sqlCaseLineNum =sqlCaseCounter.sqlCaseLineNum -1;
                  if(sqlCaseCounter.sqlCaseLineNum > 0)
                     ShowError("【" + testnum + "】" + "执行错误！\n ");
                  //执行中报错处理，报告是在哪个测试点出错
                  // 把下面的定位到行的报错方式给取消，即：下面注释掉的一行
                  //stXmlRunInfo.tdTestThread .ShowFMessage("第 "+ CurrentLineNum + " 行 " + "执行错误！" ,true);
                  cSqlCase.stCaseResult.bSuccess = false;
                  if(!pro_config.bValIsErrRun && stXmlRunInfo.bClearEn == false)//如果不允许继续运行下面一个结点的值
                     stXmlRunInfo.bStop = true;
               }
               stXmlRunInfo.cCurrentSqlCase = stXmlRunInfo.cCurrentSqlCase.cParentCase;
            }
            else if(AnalyseNode(m_XmlNode))		//分析结点关键字，并执行
            {
               m_XmlNode = stXmlRunInfo.xRefNode;
               SearchXmlNode(m_XmlNode.getFirstChild(), true);	//遍历当前节点的子节点
            }
            else
            {
               m_XmlNode = stXmlRunInfo.xRefNode;
               if(stXmlRunInfo.cCurrentSqlCase.stCaseResult.bBreak)
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
         ShowError("在分析结点时报异常！" + e.getMessage() + GetNodeText(m_XmlNode));
      }
   }
   /// <summary>
   /// //用来分析结点，并且根据节点关键字，调用相应的函数执行节点
   /// </summary>
   /// //每调用一次分析结点，行号 CurrentLineNum就加1，行否？
   public boolean AnalyseNode(Node m_XmlNode)
   {
      CurrentLineNum++;   //加1
      if (m_XmlNode == null)
      {
         System.out.println("给定的XML结点为空值");
         return false;
      }
      String strFromSql = "FromSql:";
      stXmlRunInfo.xCurrentNode = m_XmlNode;
      boolean m_FindChild = false;						//用来表示，该函数返回后，是否还要搜索它的子节点
      String m_name = m_XmlNode.getNodeName();
      String sValues;
      String sTempStr;

      switch(m_XmlNode.getNodeName().toUpperCase())             //将m_XmlNode节点的标签统一改成大写
      {
         case "SQL":
            sqlCounter.sqlNum++;
            ExecuteSQL(ReplaceRunInfo(GetNodeText(m_XmlNode)));
            break;
         case "FILESIZE":
            String sValue = GetNodeText(m_XmlNode).trim();
            GetFileSize(sValue);
            break;
         case "TESTPOINTBEGIN":
            testnum = GetNodeText(m_XmlNode);                //把当前所执行到的测试点的辅助信息保存在变量中，有利于报错时的定位
            break;
         case "TYPE":
            stXmlRunInfo.cCurrentSqlCase.stSqlResult.sExpResult = GetNodeText(m_XmlNode).trim();
            CheckTypeValue(stXmlRunInfo.cCurrentSqlCase.stSqlResult.sExpResult);
            break;
         case "RESULT":
            break;
         case "TEMPSTR":               //取消该关键字
            sTempStr = "";
            if(stXmlRunInfo.cRunInfo.containsKey("TEMPSTR"))
               sTempStr = stXmlRunInfo.cRunInfo.get("TEMPSTR").toString();
            if(stXmlRunInfo.cRunInfo.containsKey("TEMPSTR"))
               stXmlRunInfo.cRunInfo.remove("TEMPSTR");       //防止自身被替代
            sValues = GetExpVal(m_XmlNode);                  //计算是否为表达式
            if (sValues == null)
            {
               sValues = GetNodeText(m_XmlNode);
            }
            else{
               stXmlRunInfo.cRunInfo.put(m_name,sValues);
               break;
            }
            if (sValues.equals(""))
            {
               stXmlRunInfo.cRunInfo.put("TEMPSTR","");
            }
            else if (sValues.equals("@"))
            {
               sValues = sTempStr;
               sValues = ReplaceRunInfo(sValues);
               stXmlRunInfo.cRunInfo.put("TEMPSTR",sValues);
            }
            else
            {
               sValues = ReplaceRunInfo(sValues);
               if (sValues.startsWith(strFromSql))
               {
                  sValues = GetStrFromSql(sValues.substring(strFromSql.length()),true);
                  stXmlRunInfo.cRunInfo.put("TEMPSTR",sValues);
                  break;
               }
               if (sValues != null)
               {
                  sValues = sTempStr + sValues;
                  stXmlRunInfo.cRunInfo.put("TEMPSTR",sValues);
               }
            }
            break;
         case "BEGINTRANS":
            //StartTrans(GetNodeText(m_XmlNode).trim());
            break;
         case "ENDTRANS":
            //EndTrans(GetNodeText(m_XmlNode).trim());
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
                  ShowError("非法的执行次数" + e.getMessage());
               }
            }
            break;
         case "STARTTIMES":
            if(cLoop == null)
            {
               ShowError("STARTTIMES关键字只能在LOOP关键字中使用");
            }
            break;
         case "NEWCONNECTEXECUTE":
            //MoreThreadExecute(m_XmlNode);
            break;
         case "RECONNECT":
            ReConnect();
            m_FindChild = true;
            break;
         case "SERVER":
            sServerName = GetNodeText(m_XmlNode).trim();         //获取服务器名
            break;
         case "UID":                                             //获取用户名
            sUid = GetNodeText(m_XmlNode).trim();
            sUid = ReplaceRunInfo(sUid);
            break;
         case "DATABASE":
            sDatabase = GetNodeText(m_XmlNode).trim();
            sDatabase = ReplaceRunInfo(sDatabase);
            break;
         case "PWD":
            sPwd = GetNodeText(m_XmlNode).trim();
            sPwd = ReplaceRunInfo(sPwd);
            break;
         case "PROVIDER":
            sProvider = GetNodeText(m_XmlNode).trim();
            sProvider = ReplaceRunInfo(sProvider);
            break;
         case "URL":
            sConn_url = GetNodeText(m_XmlNode).trim();
            sConn_url = ReplaceRunInfo(sConn_url);
         case "POOL":
            if (GetNodeText(m_XmlNode).trim() == "TRUE") {
               bPoolEnable = true;
               //vlc.AddPrimaryKey("POOL", "TRUE");
            }
            else
            {
               bPoolEnable = false;
               //vlc.AddPrimaryKey("POOL", "FALSE");
            }
            break;
         case "PORT":
            sPort = GetNodeText(m_XmlNode).trim();
            sPort = ReplaceRunInfo(sPort);
            break;
         case "EXEPROCESS":
            //ExecuteProcess(ReplaceRunInfo(GetNodeText(m_XmlNode).Trim()));
            break;
         case "EXEPROCESSEX":
            //ExecuteProcessEx(ReplaceRunInfo(GetNodeText(m_XmlNode).Trim()));
            break;
         case "NEWTRANS":
            //StartNewTrans();
            break;
         case "EFFECTROWS":
            try
            {
               int iRows = Integer.parseInt(GetNodeText(m_XmlNode).trim());
               CheckEffectRows(iRows);
            }
            catch(Exception e)
            {
               ShowError("表示影响的行数时，使用了非法的字符串，" + e.getMessage());
            }
            break;
         case "MORETHREAD":
            MoreThreadExecute(m_XmlNode);
            break;
         case "THREADS":
            break;
         case "TOGETHER":
            //TogetherRun(m_XmlNode);
            break;
         case "NOSHOW":
            if (m_noShow == false && cLoop == null)
            {
               ShowSuccess("正在隐式运行一段脚本，可能需要比较长的时间.....");
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
            stXmlRunInfo.cCurrentSqlCase.stSqlResult.sSQLState = GetNodeText(m_XmlNode).trim();
            break;
         case "NERROR":
            try
            {
               stXmlRunInfo.cCurrentSqlCase.stSqlResult.iNativeError = Integer.parseInt(GetNodeText(m_XmlNode).trim());
            }
            catch(Exception e)
            {
               ShowError("非法的数字字符串表示NetiveError," + e.getMessage());
            }
            break;
         case "CASEEXPRESULT":
            sValues = GetNodeText(m_XmlNode).trim();
            stXmlRunInfo.cCurrentSqlCase.stCaseResult.bExpResult = (sValues.toUpperCase().equals("TRUE"));
            break;
         case "COMPARERESULT":
            //CompareResult(m_XmlNode);
            break;
         case "SERVERCMD":     //与服务器组件 服务器控制有关的三个关键字 SERVERCMD、RUNSERVER、REBOOT
            break;
         case "RUNSERVER":
            break;
         case "RUNCMD":
            break;
         case "REBOOT":
            break;
         case "SETDMINI":
            //ModifyIni(m_XmlNode);
            break;
         case "GETDMINI":
            //GetIni(m_XmlNode);
            break;
         case "COPYFILE":
            //CopyFile(m_XmlNode);
            break;
         case "DELFILE":
            //DeleFile(GetNodeText(m_XmlNode).trim());
            break;
         case "CREATEFILE":
            //CreateFile(m_XmlNode);
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
            ShowError(sValues);
            break;
         case "SMES":
            sValues = GetNodeText(m_XmlNode).trim();
            sValues = ReplaceRunInfo(sValues);
            ShowSuccess(sValues);
            break;
         case "INITDB":     //这边的处理函数未修改
            break;
         case "CONTENT":
            ShowSuccess(GetNodeText(m_XmlNode).trim());
            break;
         case "EXEXML":
            //ExeXml(GetNodeText(m_XmlNode).Trim());
            break;
         case "CONNECT":
            sValues = GetNodeText(m_XmlNode).trim();
            try
            {
               if (sValues != "")
               {
                  int connectNum = Integer.parseInt(sValues);
                  if (connectNum >= stConnectArry.size())
                  {
                     ConnectionEx();
                  }
                  else
                  {
                     SetCn(connectNum, true);
                  }
               }
               else
               {
                  ConnectionEx();
               }
            }
            catch(Exception e)
            {
               ShowError("表示连接个数时，使用了非法的字符串，" + e.getMessage());
            }
            m_FindChild = true;
            break;
         case "SETCONNECTID":
            try
            {
               SetCn(Integer.parseInt(GetNodeText(m_XmlNode).trim()), FindXmlNodeEx(m_XmlNode.getFirstChild(), "NOSHOW") == null);
            }
            catch(Exception e)
            {
               ShowError("表示连个数时，使用了非法的字符串，" + e.getMessage());
            }
            break;
         case "SQLTEST":
            m_FindChild = true;
            break;
         case  "CLEAR":
            if(stXmlRunInfo.bClearEn == false)
            {
               break;
            }
            stXmlRunInfo.cCurrentSqlCase.stSqlResult.sExpResult = "DIRECT_EXECUTE_SUCCESS";
            m_FindChild = true;
            break;
         case "SLEEP":
            try
            {
               int iSleep = Integer.parseInt(GetNodeText(m_XmlNode).trim());
               if(iSleep >= 0)
               {
                  Thread.sleep(iSleep);
                  ShowSuccess("引擎将要睡眠" + iSleep + "秒...");
               }
            }
            catch(Exception e)
            {
               ShowError("表示连挂起时间时，使用了非法的字符串，" + e.getMessage());
            }
            break;
         case "IGNORE":
            ShowSuccess("注意：程序跳过一个被要求忽略的节点");
            break;
         case "BREAK":
            stXmlRunInfo.cCurrentSqlCase.stCaseResult.bBreak = true;
            ShowSuccess("注意：遇到脚本中的 BREAK 结点，程序执行跳出了它所在的SQL_CASE点范围");
            break;
         case "DISCONNECT":
            sValues = GetNodeText(m_XmlNode).trim();
            if(sValues.equals(""))
            {
               DisConnect(-1);
            }
            else
            {
               try
               {
                  DisConnect(Integer.parseInt(sValues));
               }
               catch(Exception e)
               {
                  ShowError("表示连个数时，使用了非法的字符串，" + e.getMessage());
               }
            }
            break;
         case "WINDOWS":
            break;
         case "LINUX":
            break;
         case "RECORD":
            ShowError("RECORD关键字只能被包含在RESULT关键字节点里面");
            break;
         case "COLUMN":
            ShowError("COLUMN关键字只能被包含在RECORD关键字节点里面");
            break;
         case "PARAMETER":
            break;
         case "CLEARPARAMETERS":
            break;
         case "OPEN":
            if (stConnectINfo != null) {
               stConnectINfo.isOpenResult = true;
               stConnectINfo.iFetch = 0;
            }
            else{
               ShowError("OPEN操作上没有连接");
            }
            m_FindChild = true;
            break;
         case "FETCHNEXT":
            FetchNext();
            break;
         case "TIMETICKS":
            Date time_tricks = new Date();
            stXmlRunInfo.cRunInfo.put(GetNodeText(m_XmlNode).trim(), Long.toString(time_tricks.getTime()));
            break;
         case "RESULTROWS":
            try
            {
               int iRows = Integer.parseInt(GetNodeText(m_XmlNode).trim());
               CheckResultRows(iRows);
            }
            catch(Exception e)
            {
               ShowError("表示结果集的行数时，使用了非法的字符串，" + e.getMessage());
            }
            break;
         case "READFILE":
            //ReadFile(m_XmlNode);
            break;
         case "DATACOMPARE":
            //DoDataCompare(m_XmlNode);
            break;
         case "BINARY":
            //BinaryData(m_XmlNode);
            break;
         default:
            if(m_name.startsWith("TRANS")) {
               try {
                  int iTransId = Integer.parseInt(m_name.substring("TRANS".length()));
                  //SendXmlStringToTrans(iTransId, null);
               } catch (Exception e) {
                  ShowError("非法的事务号，或是在向事务进程发送脚本时出现异常！" + e.getMessage());
               }
            }
            else if (!m_name.startsWith("#")) {
               String sTempstr = "";
               if(stXmlRunInfo.cRunInfo.containsKey(m_name))
                  sTempstr = stXmlRunInfo.cRunInfo.get(m_name).toString();
               sValues = GetExpVal(m_XmlNode);
               if (sValues == null)
               {
                  sValues = GetNodeText(m_XmlNode);
               }
               else
               {
                  stXmlRunInfo.cRunInfo.put(m_name,sValues);
                  break;
               }
               if (sValues.equals(""))
               {
                  stXmlRunInfo.cRunInfo.put(m_name,"");
               }
               else if (sValues.equals("@"))
               {
                  sValues = sTempstr;
                  sValues = ReplaceRunInfo(sValues);
                  stXmlRunInfo.cRunInfo.put(m_name,sValues);
               }
               else
               {
                  sValues = ReplaceRunInfo(sValues);
                  if (sValues.startsWith(strFromSql))
                  {
                     sValues = GetStrFromSql(sValues.substring(strFromSql.length()), true);
                     stXmlRunInfo.cRunInfo.put(m_name,sValues);
                     break;
                  }
                  if (sValues != null)
                  {
                     sValues = sTempstr + sValues;
                     stXmlRunInfo.cRunInfo.put(m_name,sValues);
                  }
               }
               break;
            }
            break;
      }
      stXmlRunInfo.xRefNode = m_XmlNode;
      return m_FindChild;
   }
   private String GetStrFromSql(String _sql, boolean _CheckError)
   {
      String ret = "";
      try
      {
         if(stConnectINfo == null)
         {
            if(!Connection())
            {
               return ret;
            }
         }
         else if(stConnectINfo.cn.isClosed())
         {
            if(!ReConnect())
            {
               return ret;
            }
         }
         Connection g_cn = stConnectINfo.cn;
         PreparedStatement g_state = g_cn.prepareStatement(_sql);
         if (!m_noShow)
         {
            System.out.println(_sql);
         }

         if(g_state.execute())   //有结果集
         {
            ResultSet  rs = g_state.getResultSet();
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
      }
      catch(Exception e)
      {
         if (_CheckError) {
            ShowSuccess(_sql);
            ShowSuccess(e.getMessage());
         }
         else
         {
            ShowSuccess(e.getMessage());
         }
      }
      return ret;
   }
   public Node DoIf(Node m_XmlNode)
   {
      String sValues;
      String FromSql = "FromSql:";
      String err = null;
      sValues = GetNodeText(m_XmlNode).trim();
      String sTemp = sValues;
      boolean ret = false;
      if (sValues.startsWith("BOOL:"))
      {
         sValues = sValues.substring(5);
         sValues = ReplaceRunInfo(sValues);
         try
         {
            ret = GetBoolVal(sValues);
         }
         catch (Exception e) {
            ShowError("BOOL型 IF表达式计算错误！");
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
            //Sql表达式只要异常就转到Else节点
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
            //Sql表达式只要异常就转到Else节点
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
            //ShowSuccess("ELSE进入");
            SearchXmlNode(m_XmlNode.getFirstChild(), true);
            //ShowSuccess("ELSE退出");
            break;
         }
         m_XmlNode = m_XmlNode.getNextSibling();
      }
        return m_XmlNode;
   }
   private void FetchNext()
   {
      if (stConnectINfo == null || stConnectINfo.isOpenResult == false) {
         ShowError("请把该操作放在<OPEN>关键字内！");
         stXmlRunInfo.cRunInfo.put("FETCHNEXT","0");
         return;
      }
      if(stConnectINfo.rs== null)
      {
         ShowError("结果集对像为空，上一次执行的语句没有产生相应的结果集！");
      }
      try
      {
         if (stConnectINfo.rs.next()) {
            stConnectINfo.iFetch ++;
            stXmlRunInfo.cRunInfo.put("FETCHNEXT",Integer.toString(stConnectINfo.iFetch));
            for (int i = 0; i < stConnectINfo.rs.getMetaData().getColumnCount(); i++) {
               String colName = stConnectINfo.rs.getMetaData().getColumnName(i + 1);
               String sVal;
               if (colName.length() > 0) {
                  sVal = stConnectINfo.rs.getString(i + 1);		//把列中的值。变为字符串再进行比较
                  stXmlRunInfo.cRunInfo.put(colName, sVal);
               }
            }
         }
         else
         {
            stXmlRunInfo.cRunInfo.put("FETCHNEXT","0");
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
   public boolean CheckEffectRows(int iExcRows)
   {
      try
      {

         if(stConnectINfo.rs != null && stConnectINfo.Affect_Rows == -1)  //查询语句
         {
            if(stConnectINfo.rs.next())  //有结果集
            {
               if(iExcRows == 0)
               {
                  ShowError("上一次执行的语句生成了结果集，无受影响行数");
                  return false;
               }
               else
                  return true;
            }
            else              //无结果集
            {
               if(iExcRows != 0)
               {
                  ShowError("上一次执行的语句没有影响行数，但结果集有行数");
                  return false;
               }
               else
                  return true;
            }
         }
         if(stConnectINfo != null && stConnectINfo.rs == null && stConnectINfo.Affect_Rows == -1) //更新语句且无受影响行数
         {
            if(iExcRows != 0)
            {
               ShowError("上一次执行的语句无影响行数，但脚本有受影响行数");
               return false;
            }
            else
               return true;
         }
         if(iExcRows != stConnectINfo.Affect_Rows)
         {
               ShowError("语句执行影响的行数预期的值不一致！\n" + "预期值为:" + iExcRows + "\n实际返回值：" + stConnectINfo.Affect_Rows);
               return false;
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      return true;
   }
  private void SetConnectInfo()
   {
      sProvider = pro_config.sValDriveName;  //驱动名称
      sConn_url = pro_config.sValConnUrl;    //连接串
      sServerName = pro_config.sValServer;
      sUid = pro_config.sValUserId;
      sPwd = pro_config.sValPassword;
      sDatabase = pro_config.sValDatabase;
      sPort = pro_config.sValPort;
   }
   /// <summary>
   /// //设置当前执行操作用的连接
   /// </summary>
   private boolean SetCn(int iIndex, boolean bShowInfo)
   {

      if(iIndex >= stConnectArry.size() || iIndex < 0)//如果XML文件中要设置的当前连接ID大于最大的ID，或是小于0
      {
         ShowError("XML文件中要设置的当前连接ID大于最大的ID" + (stConnectArry.size() - 1) + "，或是小于0");
         return false;
      }
      stConnectINfo = (CONNECTINFO)stConnectArry.get(iIndex);
      sConn_url = stConnectINfo.sConn_url;
      sServerName = stConnectINfo.sServerName;
      sUid = stConnectINfo.sUid;
      sPwd = stConnectINfo.sPwd;
      sDatabase = stConnectINfo.sDatabase;
      sPort = stConnectINfo.sPort;
      if(bShowInfo)
         ShowSuccess("当前连接索引更改，现在的索引是 " + iIndex + " ;" + "ID：" + stConnectINfo.sUid + "; 口令："
                 + stConnectINfo.sPwd + "; 初始连接串：" + stConnectINfo.sConn_url + ";  驱动串：" + stConnectINfo.sProvider);
      return true;
   }
   /// <summary>
   /// //连接数据库
   /// </summary>
   public boolean ConnectionEx()
   {
      String	bTempCnExp;
      bTempCnExp = stXmlRunInfo.cCurrentSqlCase.stSqlResult.sExpResult;
      if(bTempCnExp.equals("LOGIN_SUCCESS") || bTempCnExp.equals("LOGIN_FAIL"))
      {
         stXmlRunInfo.cCurrentSqlCase.stSqlResult.sExpResult = "DIRECT_EXECUTE_SUCCESS";
         Connection cTempCn;
         try
         {
            cTempCn = DriverManager.getConnection(sConn_url, sUid, sPwd);
            if (bTempCnExp.equals("LOGIN_FAIL"))
            {
               ShowError("预期连接失败，实际上成功了");
            }
            if(cTempCn != null && !cTempCn.isClosed())
               cTempCn.close();
         }
         catch (SQLException e)
         {
            String errorMessages = "";
            errorMessages += "Message: " + e.getMessage() + "\n" +
                    "NativeError: " + e.getErrorCode() + "\n" +
                    "State: " + e.getSQLState()+ "\n";

            if (bTempCnExp.equals("LOGIN_FAIL"))  //预期失败，实际失败
            {
               ShowSuccess(errorMessages);
            }
            else
            {
               ShowError("预期成功，但是连接失败了");
               ShowError(errorMessages);
            }
         }
         finally {

         }
         return false;
      }
      else
      {
         return Connection();
      }
   }
   ///
   // 新建连接并将该连接设置为当前连接
   ///
   private boolean Connection()
   {

      CONNECTINFO conTemp = stConnectINfo;
      stConnectINfo = new CONNECTINFO();
      try
      {
         stConnectINfo.cn = DriverManager.getConnection(sConn_url, sUid, sPwd);
      }
      catch (SQLException e)
      {
         stConnectINfo = conTemp;
         if(stXmlRunInfo.cCurrentSqlCase.stCaseResult.bExpResult)
            stXmlRunInfo.bStop = true;
         String errorMessages = "";
         errorMessages += "Message: " + e.getMessage()+ "\n" +
                 "NativeError: " + e.getErrorCode() + "\n" +
                 "State: " + e.getSQLState() + "\n";

         ShowError(errorMessages);
         return false;
      }
      AddConnectInfo();        //将新建的连接加进连接数组
      return true;
   }
   //重新连接当前的连接
   private boolean ReConnect()
   {
      if (stConnectINfo == null) {
         ShowSuccess("没有任何连接");
      }
      try
      {
         //while(stConnectINfo.tr != null)
         //{
        //    EndTrans("ROLLBACK");
         //}
         if(stConnectINfo.rs != null)
            stConnectINfo.rs.close();
         if(!stConnectINfo.cn.isClosed())
         {
            stConnectINfo.cn.close();
            ShowSuccess("当前连接已经被断开");
         }
      }
      catch (SQLException e)
      {
         String errorMessages = "";

         errorMessages += "Message: " + e.getMessage() + "\n" +
                 "NativeError: " + e.getErrorCode() + "\n" +
                 "State: " + e.getSQLState() + "\n";
         ShowError(errorMessages);
         return false;
      }
      try
      {
            stConnectINfo.cn = DriverManager.getConnection(stConnectINfo.sConn_url,stConnectINfo.sUid,stConnectINfo.sPwd);
            ShowSuccess("当前连接已经被重连!" + "  ID：" + stConnectINfo.sUid +
                    "; 口令：" + stConnectINfo.sPwd + "; 初始连接串：" + stConnectINfo.sConn_url + "; 驱动串：" + stConnectINfo.sProvider);
      }
      catch (SQLException e)
      {
         String errorMessages = "";
         errorMessages += "Message: " + e.getMessage() + "\n" +
                    "NativeError: " + e.getErrorCode() + "\n" +
                    "State: " + e.getSQLState() + "\n";
         ShowError(errorMessages);
         if(stXmlRunInfo.cCurrentSqlCase.stCaseResult.bExpResult)
            stXmlRunInfo.bStop = true;
         return false;
      }
      catch (Exception e)
      {
         ShowError(e.getMessage());
         if (stXmlRunInfo.cCurrentSqlCase.stCaseResult.bExpResult)
            stXmlRunInfo.bStop = true;
         return false;
      }
      return true;
   }
   private void AddConnectInfo()
   {
      stConnectINfo.sConn_url = sConn_url;
      stConnectINfo.sUid = sUid;
      stConnectINfo.sPwd = sPwd;
      stConnectINfo.sServerName = sServerName;
      stConnectINfo.sProvider = sProvider;
      stConnectINfo.sDatabase = sDatabase;
      stConnectINfo.sPort = sPort;
      stConnectINfo.iFetch = 0;
      stConnectINfo.isOpenResult = false;
      stConnectArry.add(stConnectINfo);
   }
   public void ClearEnvironment()
   {
      stXmlRunInfo.bClearEn = true;        //正在清除环境标记
      if(stXmlRunInfo.xDoc == null)
      {
         try
         {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            stXmlRunInfo.xDoc = builder.parse(new InputSource(new StringReader(stXmlRunInfo.XmlStr)));
         }
         catch(Exception e)
         {
            ShowError(e.getMessage());
            return;
         }
      }
      stXmlRunInfo.xFirstNode = FindXmlNode(stXmlRunInfo.xDoc.getFirstChild(), "CLEAR");
      if(stXmlRunInfo.xFirstNode != null)
      {
         RunClear();
      }
   }
   /// <summary>
   /// //用来运行该测试用例的环境清除函数
   /// </summary>
   private void RunClear()
   {

      ShowSuccess("开始执行清除工作...");
      stXmlRunInfo.bStop = false;
      stXmlRunInfo.bClearEn = true;
      stXmlRunInfo.cCurrentSqlCase = new SqlCase();
      SearchXmlNode(stXmlRunInfo.xFirstNode, true);
      stXmlRunInfo.bClearEn = false;
      //-----com.wen.test
      sqlCaseCounter.sqlCaseNum = 0;  //记数变量归零
      sqlCaseCounter.sqlCaseLineNum = 0;
      sqlCounter.sqlNum = 0;
      sqlCounter.sqlLineNum = 0;
      //-----com.wen.test
      ShowSuccess("清除结束...");
   }
   private Node FindXmlNode(Node m_XmlNode, String m_XmlNodeName)
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
   /// <summary>
   /// //用来在一个XML文件中查找给定名称的结点,只在第一层子节点中查
   /// </summary>
   public Node FindXmlNodeEx(Node m_XmlNode, String m_XmlNodeName)
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
   private boolean DisConnect(int iIndex)
   {
      CONNECTINFO stTemp = stConnectINfo;
      if (stConnectINfo == null) {
         return false;
      }
      if(iIndex == -1)//如果该值为-1,那么断开所有的连接
      {
         for(int index=0; index<stConnectArry.size(); index++)
         {
            if(!SetCn(index, false))
            {
               ShowError("设置连接时出错");
               stConnectINfo = stTemp;
               return false;
            }
            try
            {
               if(stConnectINfo.cn != null && !stConnectINfo.cn.isClosed())
               {
                     stConnectINfo.cn.close();
               }

               if(stConnectINfo.rs != null && !stConnectINfo.rs.isClosed())
               {
                  stConnectINfo.rs.close();
               }
            }
            catch (SQLException e)
            {
               String errorMessages = "";

               errorMessages += "Message: " + e.getMessage() + "\n" +
                       "NativeError: " + e.getErrorCode() + "\n" +
                       "State: " + e.getSQLState() + "\n";
               ShowError(errorMessages);
               stConnectINfo = stTemp;
               return false;
            }

            }
            ShowSuccess("连接已经全部被断开");
            stConnectINfo = stTemp;
            return true;
      }
      if(iIndex >= stConnectArry.size() || iIndex < -1)//如果XML文件中要设置的当前连接ID大于最大的ID，或是小于-1
      {
            stConnectINfo = stTemp;
            return true;
      }
      SetCn(iIndex, false);  //关闭iIndex连接
      try
      {
         if(stConnectINfo.cn != null && !stConnectINfo.cn.isClosed())
         {
            stConnectINfo.cn.close();
            ShowSuccess("索引为 " + iIndex + " 的连接已经被断开");
         }
         if(stConnectINfo.rs != null && !stConnectINfo.rs.isClosed())
         {
            stConnectINfo.rs.close();
         }
      }
      catch (SQLException e) {
         String errorMessages = "";

         errorMessages += "Message: " + e.getMessage() + "\n" +
                 "NativeError: " + e.getErrorCode() + "\n" +
                 "State: " + e.getSQLState() + "\n";

         ShowError(errorMessages);
         stConnectINfo = stTemp;
         return false;
      }
      return true;
   }
   ///
   ///  //获取XML_NODE节点的文本值
   ///
   public static String GetNodeText(Node m_XmlNode)
   {
      if(m_XmlNode == null)
      {
         return null;
      }
      m_XmlNode = m_XmlNode.getFirstChild();
      while(m_XmlNode != null)
      {
         if(m_XmlNode.getNodeName() == "#text")
         {
            return m_XmlNode.getNodeValue();
         }
         else if(m_XmlNode.getNodeName() == "#significant-whitespace")
         {
            return m_XmlNode.getNodeValue();
         }
         m_XmlNode = m_XmlNode.getNextSibling();
      }
      return "";
   }
   ///
   /// 执行SQL标签里面的内容
   ///
   public boolean  ExecuteSQL(String m_Sql)
   {
                            //该引擎实例临界区代码 ,执行SQL语句并检查时代码要互斥
         stXmlRunInfo.cRunInfo.put("SQLSTATE","");
         stXmlRunInfo.cRunInfo.put("RETCODE","0");
         stXmlRunInfo.cRunInfo.put("EFFECTROWS","0");
         m_Sql = ReplaceRunInfo(m_Sql);
         try
         {
            if(stConnectINfo == null)
            {
              // if(!Connection())
               {
              //    return false;
               }
            }
            else if(stConnectINfo.cn.isClosed())
            {
              // if(!ReConnect())
               {
               //   return false;
               }
            }
            if(stConnectINfo.rs != null && !stConnectINfo.rs.isClosed())
               stConnectINfo.rs.close();    //关闭结果集
            if(stConnectINfo.State != null && !stConnectINfo.State.isClosed())
               stConnectINfo.State.close();   //关闭陈述
            stConnectINfo.cm = m_Sql;
            Date time1;
            Date time2;
            m_Sql = m_Sql.trim();               //去除String前后空白
            stConnectINfo.rs = null;
            if (!m_noShow)
            {
               System.out.println(m_Sql);
            }
            stConnectINfo.isOpenResult = false;
            time1 = new Date();                         //为执行语句计时
            stConnectINfo.State = stConnectINfo.cn.prepareStatement(m_Sql);
            if(stConnectINfo.State.execute())                              //有结果集返回
            {
               stConnectINfo.rs = stConnectINfo.State.getResultSet();                     //获取结果集
               stConnectINfo.Affect_Rows = stConnectINfo.State.getUpdateCount();        //获取更新计数
            }
            else
            {
               stConnectINfo.rs = stConnectINfo.State.getResultSet();
               stConnectINfo.Affect_Rows = stConnectINfo.State.getUpdateCount();
            }
            time2 = new Date();
            Long usedtimes = time2.getTime() - time1.getTime();    //取出执行消耗的时间

            stXmlRunInfo.cRunInfo.put("USEDTIMES", usedtimes.toString());

            if(pro_config.bValIsOutTime && !m_noShow)
            {
               ShowSuccess("执行本语句消耗了" + usedtimes.toString() + "毫秒。");    //显示时间间隔
            }
         }
         catch (SQLException e)
         {

            return CheckExecute(false, m_Sql, e);
         }
         catch(Exception e)
         {
            ShowError("语句执行过程中发生异常！\n" + e.getMessage());
            //-----com.wen.test
            sqlCounter.sqlLineNum = sqlCounter.findLine(stXmlRunInfo.sXmlFileName, "<SQL>", sqlCounter.sqlNum);
            if(sqlCounter.sqlLineNum > 0)
               ShowError("【" + testnum + "】" + "执行错误！\n ");//执行中报错处理，报告是在哪个测试点出错，把下面的定位到行的报错方式给取消
            //-----com.wen.test
            return false;
         }
         return CheckExecute(true, m_Sql, null);
   }

   public boolean CheckExecute(boolean m_su, String m_sql, SQLException e)
   {
      String retcode = "0";
      String sqlstate = "";
      if(stXmlRunInfo.bClearEn)	//如果该操作是用来清除环境的，那么，对它运行的正确情况不作检查
      {
         if(!m_su)
         {
            String sMessages = "";
            sMessages += "Message: " + e.getMessage() + "\n" +
                    "NativeError: " + e.getErrorCode() + "\n" +
                    "State: " + e.getSQLState() + "\n";
            ShowSuccess(sMessages);
         }
         return true;
      }
      //不是用来清除环境
      String errorMessages = "";
      errorMessages = "语句：" + m_sql + "\n预期执行结果：" + stXmlRunInfo.cCurrentSqlCase.stSqlResult.sExpResult
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

         stXmlRunInfo.cRunInfo.put("SQLSTATE", sqlstate);
         stXmlRunInfo.cRunInfo.put("RETCODE", retcode);
         if(!stXmlRunInfo.cCurrentSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_FAIL"))
         {
            if(stXmlRunInfo.cCurrentSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_IGNORE"))
            {
               ShowSuccess(sMessages);
               return true;
            }
            ShowError(errorMessages);        //预期结果不是失败，实际执行失败
            //-----com.wen.test
            sqlCounter.sqlLineNum = sqlCounter.findLine(stXmlRunInfo.sXmlFileName, "<SQL>", sqlCounter.sqlNum);
            if(sqlCounter.sqlLineNum > 0)
               ShowError("【" + testnum + "】" + "执行错误！\n ");//执行中报错处理，报告是在哪个测试点出错，把下面的定位到行的报错方式给取消
            //-----com.wen.test
         }
         else
         {
            ShowSuccess(sMessages);   //执行预期失败，实际执行也失败
         }
      }
      if(stXmlRunInfo.cCurrentSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_SUCCESS"))
      {
         if(m_su)         //预期执行成功，实际执行成功
         {
            return true;
         }
         else            //预期执行成功，实际执行失败
         {
            ShowError(errorMessages);
            //-----com.wen.test
            sqlCounter.sqlLineNum = sqlCounter.findLine(stXmlRunInfo.sXmlFileName, "<SQL>", sqlCounter.sqlNum);
            if(sqlCounter.sqlLineNum > 0)
               ShowError("【" + testnum + "】" + "执行错误！\n ");//执行中报错处理，报告是在哪个测试点出错，把下面的定位到行的报错方式给取消
            //-----com.wen.test
         }
      }
      else if(stXmlRunInfo.cCurrentSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_FAIL"))
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

               if(stXmlRunInfo.cCurrentSqlCase.stSqlResult.iNativeError != 0)
               {

                  if (stXmlRunInfo.cCurrentSqlCase.stSqlResult.iNativeError != e.getErrorCode())
                  {
                     errorMessages += "预期返回 ErrorCode: " + stXmlRunInfo.cCurrentSqlCase.stSqlResult.iNativeError + "\n实际返回返回 ErrorCode: " + e.getErrorCode() + "\n";
                     bFail = true;
                  }
               }
               if(!stXmlRunInfo.cCurrentSqlCase.stSqlResult.sSQLState.equals(""))
               {
                  if (!stXmlRunInfo.cCurrentSqlCase.stSqlResult.sSQLState.equals(e.getSQLState().trim()))
                  {
                     errorMessages += "预期返回 SQLState: " + stXmlRunInfo.cCurrentSqlCase.stSqlResult.sSQLState + "\n实际返回返回 SQLState: " + e.getSQLState() + "\n";
                     bFail = true;
                  }
               }
               if(!bFail)
                  return true;
            }
         }
         ShowError(errorMessages);
         //-----com.wen.test
         sqlCounter.sqlLineNum = sqlCounter.findLine(stXmlRunInfo.sXmlFileName, "<SQL>", sqlCounter.sqlNum);
         if(sqlCounter.sqlLineNum > 0)
            ShowError("【" + testnum + "】" + "执行错误！\n ");//执行中报错处理，报告是在哪个测试点出错，把下面的定位到行的报错方式给取消
         //-----com.wen.test
      }
      else if(stXmlRunInfo.cCurrentSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_SELECT_COMPARE_RESULT")
              || stXmlRunInfo.cCurrentSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_SELECT_COMPARE_RESULT_FULL")
              || stXmlRunInfo.cCurrentSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_SELECT_WITH_RESULT"))
      {
         if(!m_su)
         {
            ShowError("结果集生成语句执行失败了，无法进行结果集比较");
            //-----com.wen.test
            sqlCounter.sqlLineNum = sqlCounter.findLine(stXmlRunInfo.sXmlFileName, "<SQL>", sqlCounter.sqlNum);
            if(sqlCounter.sqlLineNum > 0)
               ShowError("【" + testnum + "】" + "执行错误！\n ");//执行中报错处理，报告是在哪个测试点出错，把下面的定位到行的报错方式给取消
            //-----com.wen.test
            return false;
         }
         if(stXmlRunInfo.cCurrentSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_SELECT_WITH_RESULT"))
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
      if(stConnectINfo.rs == null)
      {
         ShowError("结果集对像为空，上一次执行的语句没有产生相应的结果集！");
         return false;
      }
      if(stXmlRunInfo.xCurrentNode == null)
      {
         ShowError("在检查结果集时, 当前运行的XML结点引用为空，没有结点被引用");
         return false;
      }
      stXmlRunInfo.xCurrentNode = stXmlRunInfo.xCurrentNode.getNextSibling();
      stXmlRunInfo.xCurrentNode = SkipComment(stXmlRunInfo.xCurrentNode);
      if(stXmlRunInfo.xCurrentNode == null)
      {
         ShowError("在检查结果集时, 生成结果集语句后面没有发现结果集结点");
         return false;
      }
      if(!stXmlRunInfo.xCurrentNode.getNodeName().equals("RESULT"))
      {
         ShowError("被检察的当前结点，不是代表结果集的结点，该结点名称为:" + stXmlRunInfo.xCurrentNode.getNodeName());
         return false;
      }
      if(bCheckColName)   //如果检查列名称
      {
         Node m_colNum = FindXmlNode(stXmlRunInfo.xCurrentNode.getFirstChild(), "COLUMNNUMS");
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
               ShowError("非法的列数表达式："+e.getMessage());
               return false;
            }
            if (m_colNums <= 0)
            {
               ShowError("非法的例数值，列数应该大于等于1");
               return false;
            }
            try
            {
               if (stConnectINfo.rs.getMetaData().getColumnCount() != m_colNums) {
                  ShowError("结果集中的列数跟脚本中的列数不一致，结果集中的列数为:" + stConnectINfo.rs.getMetaData().getColumnCount());
                  return false;
               }
            }
            catch (Exception e)
            {
               ShowError("获取结果集结果集出错，无法比较\n" + e.getMessage());
            }
         }
         if (m_colNum != null)
         {
            return CheckResultColNameEx(stXmlRunInfo.xCurrentNode.getFirstChild(), m_colNums);   //只检查列名是否一致，不检查列值
         }
         else
         {
            return CheckResultColName(stXmlRunInfo.xCurrentNode.getFirstChild());
         }
      }
      else                    //如果不检查列名称
      {
         Node m_rowNum = FindXmlNode(stXmlRunInfo.xCurrentNode.getFirstChild(), "RECORDNUMS");
         if (m_rowNum == null)
         {
            return CheckResultRow(stXmlRunInfo.xCurrentNode.getFirstChild());  //不带结果行数的结果集检查
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
               ShowError("非法的行数表达式："+e.getMessage());
               return false;
            }
            return CheckResultRowEx(stXmlRunInfo.xCurrentNode.getFirstChild(), rowNums); //带结果行数的结果集检查
         }
      }
   }
   //检察结果集，只检察各列的名称，并不检察其中各行各列的值
   private boolean CheckResultColName(Node m_XmlNode)
   {
      m_XmlNode = SkipComment(m_XmlNode);
      if(m_XmlNode == null)
      {
         ShowError("未给定要比较的列名行");
         return false;
      }
      if(!m_XmlNode.getNodeName().equals("RECORD"))
      {
         ShowError("结果集起始行的名称不是代表结果集中行的结点，该结点名称为:" + m_XmlNode.getNodeName());
         return false;
      }
      Node m_tempNode = SkipComment(m_XmlNode.getFirstChild());           //跳过注释部分
      try
      {
         int m_maxColNum = 0;//表示当前XML文件结果集中列的最大数
         int m_rsmaxColNum = stConnectINfo.rs.getMetaData().getColumnCount();
         while(m_tempNode != null)
         {
            String ExpCol = GetColumn(m_tempNode);
            if(ExpCol == null)
            {
               ShowError("未在当前节点找到列的名称");
               return false;
            }
            ExpCol = ReplaceRunInfo(ExpCol);

            if(!ExpCol.equals(stConnectINfo.rs.getMetaData().getColumnName(m_maxColNum + 1)))
            {
               ShowError("返回结果集的列名称和脚本中的不匹配\n返回：" + stConnectINfo.rs.getMetaData().getColumnName(m_maxColNum) + "\n预期：" + ExpCol);
               return false;
            }
            m_tempNode = m_tempNode.getNextSibling();
            m_tempNode = SkipComment(m_tempNode);
            m_maxColNum++;
            if(m_maxColNum >= m_rsmaxColNum && m_tempNode != null)
            {
               ShowError("返回结果集的列数小于脚本中的列数不一致，结果集中存在 " + m_rsmaxColNum + "列");
               return false;
            }
         }
         if(m_maxColNum != m_rsmaxColNum)
         {
            ShowError("返回结果集的列数和脚本中的列数不一致\n结果集：" + m_rsmaxColNum + "\n脚  本：" + m_maxColNum);
            return false;
         }
      }
      catch (Exception e)
      {
         ShowError(e.getMessage());
      }
      return true;
   }
   //检察结果集，只检察各列的名称，并不检察其中各行各列的值
   public boolean CheckResultColNameEx(Node m_colNode, int m_colnums)
   {
      m_colNode = FindXmlNode(m_colNode, "COLUMN");
      if (m_colNode == null)
      {
         ShowError("未发现COMUMN");
         return false;
      }
      if (FindXmlNode(m_colNode.getNextSibling(), "COLUMN") != null)
      {
         ShowError("在使用了COLUMNNUMS关键字的情况下，只能指定一个COLUMN列");
         return false;
      }
      int m_maxColNum = 1;//表示当前XML文件结果集中列的最大数
      while(m_maxColNum <= m_colnums)
      {
         stXmlRunInfo.cRunInfo.put("COLUMNNUMS",Integer.toString(m_maxColNum));
         String ExpCol = GetColumn(m_colNode);
         if(ExpCol == null)
         {
            ShowError("未在当前节点找到列的名称");
            return false;
         }
         ExpCol = ReplaceRunInfo(ExpCol);
         try
         {
            if(!ExpCol.equals(stConnectINfo.rs.getMetaData().getColumnName(m_maxColNum)))
            {
               ShowError("返回结果集的列名称和脚本中的不匹配\n返回：" + stConnectINfo.rs.getMetaData().getColumnName(m_maxColNum) + "\n预期：" + ExpCol);
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
   public boolean CheckResultRow(Node m_XmlNode)               //不带行数检查结果集
   {
      m_XmlNode = SkipComment(m_XmlNode);
      try
      {
         if(m_XmlNode == null)
         {
            if(stConnectINfo.rs.next())
            {
               ShowError("预期语句返回一个空的结果集，但是实际返回了一个非空结果集");
               return false;
            }
            else
            {
               return true;
            }
         }
         if(!m_XmlNode.getNodeName().equals("RECORD"))
         {
            ShowError("结果集起始行的名称不是代表结果集中行的结点，该结点名称为:" + m_XmlNode.getNodeName());
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
               ShowError("非法的例数表达式："+e.getMessage());
               return false;
            }
            if (m_colNums <= 0)
            {
               ShowError("非法的例数值，列数应该大于等于1");
               return false;
            }
            if (stConnectINfo.rs.getMetaData().getColumnCount() != m_colNums)
            {
               ShowError("结果集中的列数跟脚本中的列数不一致，结果集中的列数为:" + stConnectINfo.rs.getMetaData().getColumnCount());
               return false;
            }
         }
         int m_row = 0;                        //表示当前的行数
         while(m_XmlNode != null)				//开始比较结果集
         {
            //这是结果集的行开始
            stXmlRunInfo.cRunInfo.put("RECORDNUMS", Integer.toString(m_row + 1));
            if(!stConnectINfo.rs.next())
            {
               ShowError("结果集已经到达未尾，但是XML文件中还有未比较的行");
               return false;
            }
            if (m_colNum == null)
            {
               if (!CheckColumn(m_XmlNode.getFirstChild(), m_row))             //不带列数的结果集检查
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
            m_XmlNode = SkipComment(m_XmlNode);
            m_row++;
         }
         if(m_XmlNode == null && stConnectINfo.rs.next())
         {
            ShowError("结果集未到达未尾，但是XML文件中已经没有了可以比较的行");
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
            if(stConnectINfo.ResultCounts > 0)
            {
               ShowError("预期语句返回一个空的结果集，但是实际返回了一个非空结果集");
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
            ShowError("未找到RECORD行");
            return false;
         }
         if (FindXmlNode(m_XmlNode.getNextSibling(), "RECORD") != null)
         {
            ShowError("在使用RECORDNUMS关键字时，结果集中只能包含一个RECORD行");
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
               ShowError("非法的例数表达式："+e.getMessage());
               return false;
            }
            if (m_colNums<=0)
            {
               ShowError("非法的例数值，列数应该大于等于1");
               return false;
            }
            if (stConnectINfo.rs.getMetaData().getColumnCount() != m_colNums)
            {
               ShowError("结果集中的列数跟脚本中的列数不一致，结果集中的列数为:" + stConnectINfo.rs.getMetaData().getColumnCount());
               return false;
            }
         }
         int m_row = 1;//表示当前的行数
         while(m_row <= rownums)				//开始比较结果集
         {
            //这是结果集的行开始
            if(!stConnectINfo.rs.next())
            {
               ShowError("结果集已经到达未尾，但是XML文件中还有未比较的行");
               return false;
            }
            stXmlRunInfo.cRunInfo.put("RECORDNUMS", Integer.toString(m_row));
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
         if(stConnectINfo.rs.next())
         {
            ShowError("结果集未到达未尾，但是XML文件中已经没有了可以比较的行");
            return false;
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      return true;
   }
   public boolean CheckResultRows(int iExcRows)
   {
      try
      {
         if(stConnectINfo == null || stConnectINfo.rs == null)
         {
            ShowError("上一次执行的语句没有生成记录对像");
            return false;
         }
         if(stConnectINfo.rs.getMetaData().getColumnCount() == 0)
         {
            if(iExcRows != stConnectINfo.Affect_Rows)
            {
               ShowError("语句执行影响的行数预期的值不一致！\n" + "预期值为:" + iExcRows + "\n实际返回值：" + stConnectINfo.Affect_Rows);
               return false;
            }
         }
         else
         {
            int rows = 0;
            if(stConnectINfo.rs.next())
            {
               if (pro_config.bValIsShowResult)
               {
                  ShowError("“不检查结果集的情况下显示结果集”功能被打开，无法进行结果集行数统计操作");
                  return false;
               }
               rows++;
               while(stConnectINfo.rs.next())
               {
                  rows++;
               }
            }
            if(rows != iExcRows)
            {
               ShowError("结果集行数跟预期的行数不一致！\n" + "预期值为:" + iExcRows + "\n实际返回值：" + rows);
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

   public String GetColumn(Node m_XmlNode)
   {

      m_XmlNode = SkipComment(m_XmlNode);
      if(m_XmlNode == null)
      {
         return null;
      }
      if(!m_XmlNode.getNodeName().equals("COLUMN"))
      {
         ShowError("给定结点的名称不是代表结果集中列的结点，该结点名称为:" + m_XmlNode.getNodeName());
         return null;
      }
      return GetNodeText(m_XmlNode);
   }
   public boolean CheckColumn(Node m_colNode, int m_row)
   {
      String errorMessage = "";
      int m_col = 1;
      m_colNode = SkipComment(m_colNode);
      try
      {
         while(m_colNode != null)
         {
            //这是结果集的每一列开始
            if(m_col > stConnectINfo.rs.getMetaData().getColumnCount())
            {
               ShowError("XML文件中的列数超过了返回结果集中的列数");
               return false;
            }
            String ExpCol = GetColumn(m_colNode);
            if(ExpCol == null)
            {
               return false;
            }
            ExpCol = ReplaceRunInfo(ExpCol);
            if(ExpCol.equals("NULL"))
               ExpCol = null;
            String RetCol = "";
            try
            {
               if(stConnectINfo.rs.getObject(m_col) == null)
                  RetCol = null;
               else
                  RetCol = stConnectINfo.rs.getObject(m_col).toString();	//把列中的值。变为字符串再进行比较
            }
            catch (SQLException e)
            {
               String errorMessages = "Message: " + e.getMessage() + "\n" +
                       "NativeError: " + e.getErrorCode() + "\n" +
                       "State: " + e.getSQLState() + "\n";

               ShowError(errorMessages);
               return false;
            }
            if(!Check_Column_help(ExpCol, RetCol, m_row, m_col))
               return false;
            m_colNode = m_colNode.getNextSibling();
            m_colNode = SkipComment(m_colNode);
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
         ShowError("未发现COMUMN");
         return false;
      }
      if (FindXmlNode(m_colNode.getNextSibling(), "COLUMN") != null)
      {
         ShowError("在使用了COLUMNNUMS关键字的情况下，只能指定一个COLUMN列");
         return false;
      }
      while(m_col <= m_colnums)
      {
         stXmlRunInfo.cRunInfo.put("COLUMNNUMS", Integer.toString(m_col));
         String ExpCol = GetColumn(m_colNode);
         if(ExpCol == null)
         {
            return false;
         }
         ExpCol = ReplaceRunInfo(ExpCol);
         if(ExpCol.equals("NULL"))
            ExpCol = null;
         String RetCol = "";
         try
         {
            if(stConnectINfo.rs.getObject(m_col) == null)
               RetCol = null;
            else
               RetCol = stConnectINfo.rs.getObject(m_col).toString();    //把列中的值。变为字符串再进行比较
         }
         catch (SQLException e)
         {
            String errorMessages = "";

            errorMessages += "Message: " + e.getMessage() + "\n" +
                    "NativeError: " + e.getErrorCode() + "\n" +
                    "State: " + e.getSQLState() + "\n";

            ShowError(errorMessages);
            return false;
         }
         if(!Check_Column_help(ExpCol, RetCol, m_row, m_col))
            return false;
         m_col++;
      }
      return true;
   }
   public boolean Check_Column_help(String ExpCol,String RetCol, int m_row, int m_col)
   {
      if(ExpCol != null && ExpCol.startsWith("\"") && ExpCol.endsWith("\""))              //截取""字符串
         ExpCol = ExpCol.substring(1, ExpCol.length() - 1);
      String errorMessage = "";
      if(ExpCol == null && RetCol != null)
      {
         errorMessage += "脚本结果集在第" + (m_row) + "行，第" + (m_col) +"列处，结果不一致\n";
         errorMessage += "预期值: 空值(null)" ;
         errorMessage += "\n返回值: " + RetCol;
         ShowError(errorMessage);
         return false;
      }
      else if(ExpCol != null && RetCol == null)
      {
         errorMessage += "脚本结果集在第" + (m_row) + "行，第" + (m_col) +"列处，结果不一致\n";
         errorMessage += "预期值: " + ExpCol;
         errorMessage += "\n返回值: 空值(null) ";
         ShowError(errorMessage);
         return false;
      }
      else if(ExpCol != null && !ExpCol.equals(RetCol))
      {
         errorMessage += "脚本结果集在第" + (m_row) + "行，第" + (m_col) +"列处，结果不一致\n";
         errorMessage += "预期值: " + ExpCol;
         errorMessage += "\n返回值: " + RetCol;
         ShowError(errorMessage);
         return false;
      }
      return true;
   }
   public static Node SkipComment(Node m_XmlNode)      //跳过注释节点
   {
      if(m_XmlNode == null)
         return null;
      while(m_XmlNode != null && (m_XmlNode.getNodeName().equals("#comment") || m_XmlNode.getNodeName().equals("#text")))
      {
         m_XmlNode = m_XmlNode.getNextSibling();
      }
      return m_XmlNode;
   }
   private void GetFileSize(String sFileName)
   {
      //sFileName=ReplaceRunInfo(sFileName);
      File file=new File(sFileName);
      if(!file.exists())
      {
         System.out.println(sFileName+" 文件不存在或者它是个目录！");
      }
      else
      {
         //vlc.SetVal("FILESIZE",(fi.Length).ToString ());
      }
   }
   //检察TYPE关键字的值是否合法
   public boolean CheckTypeValue(String sValue)
   {
      if(sValue.equals("DIRECT_EXECUTE_SUCCESS"))
      {
         return true;
      }
      if(sValue.equals("DIRECT_EXECUTE_FAIL"))
      {
         return true;
      }
      if(sValue.equals("DIRECT_EXECUTE_SELECT_WITH_RESULT"))
      {
         return true;
      }
      if(sValue.equals("DIRECT_EXECUTE_SELECT_COMPARE_RESULT"))
      {
         return true;
      }
      if(sValue.equals("DIRECT_EXECUTE_SELECT_COMPARE_RESULT_FULL"))
      {
         return true;
      }
      if(sValue.equals("DIRECT_EXECUTE_IGNORE"))
      {
         return true;
      }
      if(sValue.equals("LOGIN_SUCCESS"))
      {
         return true;
      }
      if(sValue.equals("LOGIN_FAIL"))
      {
         return true;
      }
      System.out.println("脚本中存在非法的预期执行结果关键字(就是TYPE关键字包含部分的字符串不是已被订义的)");
      return false;
   }
   /// <summary>
   /// //用来得执行LOOP循环
   /// </summary>
   private void StartLoopNode(Node m_XmlNode)
   {
      boolean m_noShowTemp = m_noShow;
      if(cLoop == null)
      {
         cLoop = new LOOP_STRUCT();
         cLoop.prev = null;
         cLoop.times = 0;
         cLoop.breakflag = false;
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
         ShowError("没有找到LOOP执行的次数，或是次数的字符串非法" + e.getMessage());
      }

      Node m_XmlNodeTemp = FindXmlNodeEx(m_XmlNode.getFirstChild(), "NOSHOW");   //只在第一层节点查找NOSHOW标签
      if (m_XmlNodeTemp != null)
      {
         m_noShow = true;
         String val = GetNodeText(m_XmlNodeTemp);
         if (val == "false")
         {
            m_noShow = false;
         }
         else
         {
            ShowSuccess("将隐藏执行循环中的脚本.....");
         }
      }
      int times = cLoop.times;

      if (cLoop.start > 0) {
         cLoop.times = cLoop.start;
         while(cLoop.times <= times && !stXmlRunInfo.cCurrentSqlCase.stCaseResult.bBreak && !stXmlRunInfo.bStop)
         {
            SearchXmlNode(m_XmlNode.getFirstChild(), true);
            if(cLoop.breakflag)
               break;
            cLoop.times ++;
         }
      }
      else{
         cLoop.times = times;
         while(cLoop.times >= 1 && !stXmlRunInfo.cCurrentSqlCase.stCaseResult.bBreak && !stXmlRunInfo.bStop)
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
   private int GetUpTimes(int up)
   {
      if (up < 0)
      {
         return -1;
      }
      LOOP_STRUCT cTemp = cLoop;
      while (up-->0 && cTemp != null)
      {
         cTemp = (LOOP_STRUCT)cTemp.prev;
      }
      if (cTemp == null)
      {
         return -1;
      }
      return cTemp.times;
   }
   public String ReplaceRunInfo(String sql)
   {
      Object tmp = null;
      if(stXmlRunInfo.cRunInfo.containsKey("TEMPSTR"))
      {
         tmp = stXmlRunInfo.cRunInfo.get("TEMPSTR");
         stXmlRunInfo.cRunInfo.remove("TEMPSTR");
         sql = sql.replace("@TEMPSTR",tmp.toString()); //提前替换
      }
      int index = sql.indexOf("@");
      while(index != -1)
      {
         String max_str = "";
         Object max_key = null;
         for(Object obj : stXmlRunInfo.cRunInfo.keySet())
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
         sql = sql.replace(max_str,stXmlRunInfo.cRunInfo.get(max_key).toString());
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
         stXmlRunInfo.cRunInfo.put("TEMPSTR",tmp.toString());
      }
      return sql;
   }
   public static boolean  GetBoolVal(String instr)throws Exception
   {
      instr = instr.replace("AND", "&&");
      instr = instr.replace("OR", "||");
      instr = instr.replace("=","==");
      instr = instr.replace(">==",">=");
      instr = instr.replace("<==","<=");
      instr = instr.replace("<>","!=");
      boolean result = (boolean) AviatorEvaluator.execute(instr);
      return result;
   }
   private boolean GetStrFromSql(String instr)throws  Exception   //判断有无结果集
   {
      Connection tmp_conn = null;
      PreparedStatement tmp_state = null;
      try
      {
         tmp_conn = DriverManager.getConnection(sConn_url,sUid,sPwd);
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
         ShowError("表达式 (" + ReplaceRunInfo(GetNodeText(m_XmlNode)) + ") 语法错:" + e.getMessage());
      }
      return ret;
   }
   private void MoreThreadExecute(Node m_XmlNode)
   {
      MoreRunnable myrun = new MoreRunnable(m_XmlNode, this);
      Thread newthread = new Thread(myrun);
      stThreadArry.add(newthread);
      newthread.start();
   }
   public void ShowError(String mess)
   {
      stXmlRunInfo.cCurrentSqlCase.stCaseResult.bSuccess = false;
      stXmlRunInfo.cRunInfo.put("com.wen.CASERESULT", "FALSE");
      if (stXmlRunInfo.cCurrentSqlCase.stCaseResult.bExpResult)
      {
         cSqlCase.stCaseResult.bSuccess = false;

         if (!pro_config.bValIsErrRun && stXmlRunInfo.bClearEn == false)//如果不允许继续运行下面一个结点的值
            stXmlRunInfo.bStop = true;
         System.out.println("失败消息:" + mess);
      }
      else
      {
         System.out.println("成功消息:" +  mess);
      }
   }
   public void ShowSuccess(String mess)
   {
      if(!mess.equals(""))
         System.out.println("成功消息:" + mess);
   }
   public void CopyFile(Node m_XmlNode)
   {
      String sOldFile = "";
      String sNewFile = "";
      Node x_temp;
      x_temp = FindXmlNode(m_XmlNode.getFirstChild(), "OLDFILE");
      if (x_temp != null)
      {
         sOldFile = GetNodeText(x_temp).trim();
         sOldFile = ReplaceRunInfo(sOldFile);
         if (sOldFile.length() == 0)
         {
            ShowError("非法的文件拷贝操作，未在COPYFILE节点中找到OLDFILE节点的属性名");
            return;
         }
      }
      else
      {
         ShowError("非法的文件拷贝操作，未在COPYFILE节点中找到OLDFILE节点");
         return;
      }
      x_temp = FindXmlNode(m_XmlNode.getFirstChild(), "NEWFILE");
      if (x_temp != null)
      {
         sNewFile = GetNodeText(x_temp).trim();
         sNewFile = ReplaceRunInfo(sNewFile);
         if (sNewFile.length() == 0)
         {
            ShowError("非法的文件拷贝操作，未在COPYFILE节点中找到NEWFILE节点的属性名");
            return;
         }
      }
      else
      {
         ShowError("非法的文件拷贝操作，未在COPYFILE节点中找到NEWFILE节点");
         return;
      }
      try
      {


         String mess4 = "COPY_FILE" + '$' + sOldFile + '$' + sNewFile;   //copy file
      //   String ret = com.wen.ProClass.CommServer.Send_Rec_Message(mess4);   //发送消息  重启服务器
         String ret = "";

         ShowSuccess("发送启动服务器命令：" + mess4 + "服务器执行结果：" + ret + "\n");
      }
      catch (Exception e)
      {
         if (stXmlRunInfo.cCurrentSqlCase.stSqlResult.sExpResult.equals("DIRECT_EXECUTE_IGNORE"))
         {
            ShowSuccess(e.getMessage());
         }
         else
         {
            ShowError(e.getMessage());
         }
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
            ShowError("未在SETVAL节点中找到VALNAME节点的属性名");
            return;
         }
      }
      else
      {
         ShowError("未在SETVAL节点中找到VALNAME节点");
         return;
      }
      x_temp = FindXmlNode(m_XmlNode.getFirstChild(), "VAL");
      if (x_temp != null)
      {
         sVal = GetNodeText(x_temp).trim();
         sVal = ReplaceRunInfo(sVal);
         if(sVal.equals("DEL"))
         {
           pro_config.delVal(sName);
         }
         else
           pro_config.setVal(sName, sVal);
      }
      else
      {
         ShowError("未在SETVAL节点中找到VAL节点");
      }
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
            ShowError("未在GETVAL节点中找到VALNAME节点的属性名");
            return;
         }
      }
      else
      {
         ShowError("未在GETVAL节点中找到VALNAME节点");
         return;
      }
      x_temp = FindXmlNode(m_XmlNode.getFirstChild(), "VAL");
      if (x_temp != null)
      {
         sVal = GetNodeText(x_temp).trim();
         if (!sVal.startsWith("@"))
         {
            ShowError("VAL节点中应该指明的是被存放属性值的一个替代符，而不是一个普通的字符串");
            return;
         }
         String sTemp = pro_config.getVal(sName);
         if (sTemp == null)
         {
            ShowError("未找到属性：" + sName);
         }
         else
            stXmlRunInfo.cRunInfo.put(sVal.substring(1), sTemp);
      }
      else
      {
         ShowError("未在GETVAL节点中找到SETAT节点");
      }
   }
   private void StartLock(Node m_XmlNode)     //开始加锁执行该节点
   {
      Node x_temp;
      x_temp = FindXmlNode(m_XmlNode.getFirstChild(), "VALNAME");
      if(x_temp == null)
      {
         ShowError("未指定加锁对象名");
         return ;
      }
      synchronized (pro_config.valclass.get(GetNodeText(x_temp)))            //序列化执行代码
      {
         ShowSuccess("主线程开始加锁" + GetNodeText(x_temp) + "节点");
         SearchXmlNode(m_XmlNode.getFirstChild(), true);
         ShowSuccess("主线程结束加锁" + GetNodeText(x_temp)+ "节点");
      }
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
         ShowError("指定等待时间格式错误");
      }
      Node lockNode = m_XmlNode;
      while(!lockNode.getNodeName().equals("LOCK"))
      {
         lockNode = lockNode.getParentNode();
         if(lockNode == null)
         {
            ShowError("WAIT节点必须处于LOCK节点内");
         }
      }
      Node x_temp = FindXmlNode(lockNode.getFirstChild(), "VALNAME");
      if(x_temp == null)
      {
         ShowError("未指定等待对象名");
         return ;
      }
      try {
         ShowSuccess(GetNodeText(x_temp) + "即将开始等待主线程...");
         if(seconds >= 0)
            pro_config.valclass.get(GetNodeText(x_temp)).wait(seconds);
         else
            pro_config.valclass.get(GetNodeText(x_temp)).wait();
         ShowSuccess(GetNodeText(x_temp) + "已结束等待主线程...");
      } catch (InterruptedException e) {
         e.printStackTrace();
         ShowError("WAIT出错...");
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
            ShowError("NOTIFY节点必须处于LOCK节点内");
         }
      }
      Node x_temp = FindXmlNode(lockNode.getFirstChild(), "VALNAME");
      if(x_temp == null)
      {
         ShowError("未指定通知对象名");
         return ;
      }
      if(GetNodeText(m_XmlNode).equals("ALL"))
         pro_config.valclass.get(GetNodeText(x_temp)).notifyAll();
      else
         pro_config.valclass.get(GetNodeText(x_temp)).notify();
      ShowSuccess(GetNodeText(x_temp) + "已经被通知...");
   }
}
/*
成功消息:开始加锁LOCK1节点
成功消息:当前连接索引更改，现在的索引是 1 ;ID：SYSDBA; 口令：SYSDBA; 初始连接串：jdbc:dm://localhost:5236;  驱动串：dm.jdbc.driver.DmDriver
成功消息:子线程将要睡眠1000秒...
成功消息:子进程开始加锁LOCK2节点
成功消息:SUCCESS
成功消息:子进程结束加锁LOCK2节点
成功消息:引擎将要睡眠5000秒...
成功消息:结束加锁LOCK1节点
成功消息:Message: 用户名或密码错误
 */
/*
成功消息:索引为 0 的连接已经被断开
成功消息:当前连接索引更改，现在的索引是 0 ;ID：SYSDBA; 口令：SYSDBA; 初始连接串：jdbc:dm://localhost:5236;  驱动串：dm.jdbc.driver.DmDriver
成功消息:开始加锁LOCK1节点
成功消息:当前连接索引更改，现在的索引是 1 ;ID：SYSDBA; 口令：SYSDBA; 初始连接串：jdbc:dm://localhost:5236;  驱动串：dm.jdbc.driver.DmDriver
成功消息:子线程将要睡眠1000秒...
成功消息:引擎将要睡眠5000秒...
成功消息:结束加锁LOCK1节点
成功消息:子进程开始加锁LOCK1节点
成功消息:Message: 用户名或密码错误
 */