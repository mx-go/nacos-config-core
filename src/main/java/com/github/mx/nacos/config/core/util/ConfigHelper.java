package com.github.mx.nacos.config.core.util;

import com.github.mx.nacos.config.core.ConfigFactory;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;

/**
 * 帮助类
 * <p>
 * Create by max on 2022/04/06
 */
public class ConfigHelper {

    private static final Logger log = LoggerFactory.getLogger(ConfigHelper.class);
    private static long CACHE_STAMP;
    private static final String SERVER_IP;
    private static InetAddress ADDRESS;

    static {
        List<String> ips = getIpV4LocalAddresses();
        SERVER_IP = ips.isEmpty() ? "127.0.0.1" : ips.get(0);
    }

    public static List<String> getIpV4LocalAddresses() {
        List<String> ips = Lists.newArrayList();
        try {
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();
                boolean skip = ni.isLoopback() || ni.isVirtual() || ni.getName().startsWith("docker");
                if (!skip) {
                    Enumeration<InetAddress> en = ni.getInetAddresses();
                    while (en.hasMoreElements()) {
                        InetAddress net = en.nextElement();
                        String ip = net.getHostAddress();
                        if (ip.indexOf(':') < 0 && net.isSiteLocalAddress()) {
                            ips.add(ip);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("cannot scan ips", e);
        }
        return ips;
    }

    private static InetAddress getCachedAddress() {
        long now = System.currentTimeMillis();
        // 缓存5分钟的主机名信息
        if (now - CACHE_STAMP > 300000) {
            CACHE_STAMP = now;
            try {
                ADDRESS = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                log.error("unknown host", e);
                try {
                    ADDRESS = InetAddress.getLocalHost();
                } catch (UnknownHostException ignored) {
                    log.error("unknown host", ignored);
                }
            }
        }
        return ADDRESS;
    }

    /**
     * 获取应用名
     */
    public static String getAppName() {
        return ConfigFactory.getApplicationName();
    }

    /**
     * 获取服务器的第一个内网IP
     *
     * @return 内网ip
     */
    public static String getServerIp() {
        return SERVER_IP;
    }

    /**
     * 获取服务器的主机名
     *
     * @return 主机名
     */
    public static String getHostName() {
        return getCachedAddress().getHostName();
    }
}