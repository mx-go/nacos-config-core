package com.github.mx.nacos.config.core.api;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Properties;

/**
 * 获取配置参数方法
 * <p>
 * Create by max on 2020/12/05
 **/
public interface IConfig {

    int getInt(String key);

    /**
     * 获取key对应的int数值
     *
     * @param key        查找的key
     * @param defaultVal 找不到返回的默认值
     * @return 找到返回对应的int数值否则返回默认值
     */
    int getInt(String key, int defaultVal);

    long getLong(String key);

    /**
     * 获取key对应的long数值
     *
     * @param key        查找的key
     * @param defaultVal 找不到返回的默认值
     * @return 找到返回对应的long数值否则返回默认值
     */
    long getLong(String key, long defaultVal);

    boolean getBool(String key);

    /**
     * 获取key对应的boolean数值
     *
     * @param key        查找的key
     * @param defaultVal 找不到返回的默认值
     * @return 找到返回对应的boolean数值否则返回默认值
     */
    boolean getBool(String key, boolean defaultVal);

    double getDouble(String key);

    /**
     * 获取key对应的double数值
     *
     * @param key        查找的key
     * @param defaultVal 找不到返回的默认值
     * @return 找到返回对应的double数值否则返回默认值
     */
    double getDouble(String key, double defaultVal);

    /**
     * 获取key对应的String值
     *
     * @param key 查找的key
     * @return 找到返回对应的String值
     */
    String get(String key);

    /**
     * 获取key对应的String内容，如果找不到返回提供的默认值
     *
     * @param key        查找的key
     * @param defaultVal 提供的默认值
     * @return 找到返回对应的配置，默认返回默认值
     */
    String get(String key, String defaultVal);

    /**
     * 配置中是否有对应的key
     *
     * @param key 查找的key
     * @return 有的话返回true¬
     */
    boolean has(String key);

    /**
     * 获取所有配置信息
     *
     * @return 只读配置信息
     */
    Properties getAll();

    /**
     * 获取配置的字节流信息
     *
     * @return 字节数组
     */
    byte[] getContent();

    /**
     * 把配置文件二进制内容用UTF8编码进行解码，并返回对应的字符串
     *
     * @return 字符串
     */
    String getString();

    /**
     * 把配置文件二进制内容用指定编码进行解码，并返回对应的字符串
     *
     * @param charset 指定编码
     * @return 字符串
     */
    String getString(Charset charset);

    /**
     * 读取内容并通过分隔符分隔为List。默认分隔符为分号;
     *
     * @param key 查找的key
     * @return 字符串List
     */
    Collection<String> getStrings(String key);

    /**
     * 读取内容并通过分隔符分隔为List
     *
     * @param key       查找的key
     * @param separator 分隔符
     * @return 字符串List
     */
    Collection<String> getStrings(String key, String separator);
}
