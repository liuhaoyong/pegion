package com.github.pigeon.api.utils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.github.pigeon.api.model.EventWrapper;

/**
 * 通用的一些工具
 * @author liuhaoyong
 * time : 2016年3月14日 下午2:36:41
 */
public final class PigeonUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(PigeonUtils.class);
    /**
     * 是否为字母
     * @param ch
     * @return
     */
    public static boolean isLetter(char ch) {
        return ch >= 65 && ch <= 90 || ch >= 97 && ch <= 122;
    }


    /**
     * 打印异常的堆栈信息
     * @param exception 异常
     * @return 异常的堆栈信息
     */
    public static String printErrorTrace(Throwable exception) {
        StringBuffer errorString = new StringBuffer();
        errorString.append(exception.toString()).append("\n");
        StackTraceElement[] trace = exception.getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            errorString.append(trace[i]).append("\n");
        }
        return errorString.toString();
    }


    /**
     * 获取本地ip地址
     * @return
     */
    public static InetAddress getLocalHostAddress() {
        try {
            for (Enumeration<NetworkInterface> nis = NetworkInterface
                    .getNetworkInterfaces(); nis.hasMoreElements();) {
                NetworkInterface ni = nis.nextElement();
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp())
                    continue;
                for (Enumeration<InetAddress> ias = ni.getInetAddresses(); ias.hasMoreElements();) {
                    InetAddress ia = ias.nextElement();
                    if (ia instanceof Inet6Address) continue;
                    return ia;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     *
     * @param name
     * @return
     */
    public static final Object getBean(ApplicationContext applicationContext,String name){
        try{
            if(StringUtils.isBlank(name) || applicationContext == null){
                throw new NoSuchBeanDefinitionException("name is blank!");
            }
            return applicationContext.getBean(name);
        }catch (NoSuchBeanDefinitionException ex){
            logger.error("beanName:{} is not defined!",name);
            return null;
    
        }catch (Exception ex){
            logger.error(ex.getMessage());
        }
        return null;
    }
    

    /**
     * 获得事件发布地址
     * @param targetAddress
     * @return
     */
    public static String parseEventPublishAddress(String targetAddress) {
        if (StringUtils.isNotBlank(targetAddress)
                && targetAddress.toLowerCase().startsWith("mq:")) {
            targetAddress = targetAddress.substring(
                    targetAddress.indexOf(":") + 1, targetAddress.length());
        }
        return targetAddress;
    }
    
    public static String marshall(Object obj) {
        if(obj == null){
            return null;
        }
        return JSON.toJSONString(obj, SerializerFeature.WriteClassName);
    }


    public static EventWrapper unmarshall(String eventWrapperContent) {
        return JSON.parseObject(eventWrapperContent, EventWrapper.class);
    }

    private static String serverIdentifier = null;
    private static InetAddress inetAddress = null;
    public static InetAddress getInetAddress(){
        if(StringUtils.isBlank(serverIdentifier)){
            synchronized (PigeonUtils.class){
                if(StringUtils.isBlank(serverIdentifier)){
                    inetAddress = PigeonUtils.getLocalHostAddress();
                }
            }
        }
        return inetAddress;
    }

    /**
     * 后去服务器唯一标示
     * @return
     */
    public static String getServerIdentifier(){
        if(StringUtils.isBlank(serverIdentifier)){
            synchronized (PigeonUtils.class){
                if(StringUtils.isBlank(serverIdentifier)){
                    InetAddress address = PigeonUtils.getLocalHostAddress();
                    serverIdentifier = getInetAddress().getHostAddress() + "_" + getInetAddress().getCanonicalHostName();
                }
            }
        }
        return serverIdentifier;
    }

    private static String host = null;
    public static String getHost(){
        if(StringUtils.isBlank(host)){
            synchronized (PigeonUtils.class){
                if(StringUtils.isBlank(host)){
                    host = getInetAddress().getCanonicalHostName();
                }
            }
        }
        return host;
    }

    private static String ip = null;
    public static String getIp(){
        if(StringUtils.isBlank(ip)){
            synchronized (PigeonUtils.class){
                if(StringUtils.isBlank(ip)){
                    ip = getInetAddress().getHostAddress();
                }
            }
        }
        return ip;
    }
}
