package com.wen.test;
import com.wen.MyUtil;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wen on 2017/3/14.
 */


public class LoadJava {
    public static  void main(String[] args) throws Exception
    {
        /*动态加载指定jar包调用其中某个类的方法*/
        File file = new File("D:\\testLoadJar\\Myjar.jar");//jar包的路径
        URL url = file.toURI().toURL();
        ClassLoader loader = new URLClassLoader(new URL[]{url});             //创建类加载器
        Class<?> cls = loader.loadClass("com.wen.fun.Allfun");               //加载指定类，注意一定要带上类的包名
        Method method = cls.getMethod("fun_n",String[].class);        //方法名和对应的各个参数的类型
        int a = 3;
        String[] in_args = new String[a];
        Map in_map = new HashMap<Integer, String>();
        in_map.put(1,2);
        in_map.put(2,"11");
        in_args[0] = "wen";
        in_args[1] = "love";
        in_args[2] = "cai";
        Student stu = new Student("wen",22,true,in_map);
        Object o = method.invoke(cls.newInstance(),(Object)in_args);              //调用得到的上边的方法method(第一个参数类实例)
        System.out.println(o.toString());
        String js = MyUtil.getJsonFromObject(stu);
        System.out.println(js);
        Student ob = (Student)MyUtil.getObjectByJsonString(js,"com.wen.test.Student");
        System.out.println(ob.getSex());
    }
}
