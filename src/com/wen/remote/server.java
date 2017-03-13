package com.wen.remote;

/**
 * Created by wen on 2017/2/17.
 */
import com.wen.*;
import javax.xml.ws.Endpoint;
public class server {
    public static void main(String[] args)
    {
        Endpoint.publish("http://115.156.186.48:8080/Xmlprocess", new XmlProcess());
    }

}
