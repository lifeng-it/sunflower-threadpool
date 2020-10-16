package com.sunflower.threadpool.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author songhengliang
 * @date 2020/10/16
 */
@Slf4j
public class IpUtils {

    private static String localHost;

    public static String getLocalHostAddress() {
        if (StringUtils.isEmpty(localHost)) {
            try {
                String localHost = InetAddress.getLocalHost().getHostAddress();
                log.info("init local host {}", localHost);
            } catch (UnknownHostException e) {
                log.error("获取本地ip异常", e);
            }
        }
        return localHost;
    }
}
