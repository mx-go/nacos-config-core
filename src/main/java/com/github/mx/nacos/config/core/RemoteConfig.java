package com.github.mx.nacos.config.core;

import com.github.mx.nacos.config.core.util.YamlUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.github.mx.nacos.config.core.api.IConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * 转化nacos获取到的原始数据
 * <p>
 * Create by max on 2020/12/05
 **/
public class RemoteConfig {

    public static final Logger LOGGER = LoggerFactory.getLogger(RemoteConfig.class);

    private RemoteConfig() {

    }

    /**
     * 将配置中心字符串转为Properties
     */
    public static IConfig convert(String config) {
        Properties properties = new Properties();
        String ps = processContent(config);
        try {
            properties.load(new StringReader(ps));
        } catch (Exception e) {
            LOGGER.error("Load Properties from configInfo error. configInfo:{}", config, e);
        }

        return new IConfig() {
            @Override
            public int getInt(String key) {
                return getInt(key, 0);
            }

            @Override
            public int getInt(String key, int defaultVal) {
                String val = get(key);
                if (!Strings.isNullOrEmpty(val)) {
                    try {
                        return Integer.parseInt(val);
                    } catch (NumberFormatException ignored) {

                    }
                }
                return defaultVal;
            }

            @Override
            public long getLong(String key) {
                return getLong(key, 0L);
            }

            @Override
            public long getLong(String key, long defaultVal) {
                String val = get(key);
                if (!Strings.isNullOrEmpty(val)) {
                    try {
                        return Long.parseLong(val);
                    } catch (NumberFormatException ignored) {

                    }
                }
                return defaultVal;
            }

            @Override
            public boolean getBool(String key) {
                return getBool(key, false);
            }

            @Override
            public boolean getBool(String key, boolean defaultVal) {
                String val = get(key);
                if (!Strings.isNullOrEmpty(val)) {
                    return Boolean.parseBoolean(val);
                }

                return defaultVal;
            }

            @Override
            public double getDouble(String key) {
                return getDouble(key, 0.0);
            }

            @Override
            public double getDouble(String key, double defaultVal) {
                String val = get(key);
                if (!Strings.isNullOrEmpty(val)) {
                    try {
                        return Double.parseDouble(val);
                    } catch (NumberFormatException ignored) {

                    }
                }
                return defaultVal;
            }

            @Override
            public String get(String key) {
                return properties.getProperty(key);
            }

            @Override
            public String get(String key, String defaultVal) {
                String val = get(key);
                return val == null ? defaultVal : val;
            }

            @Override
            public boolean has(String key) {
                return get(key) != null;
            }

            @Override
            public Properties getAll() {
                return properties;
            }

            @Override
            public byte[] getContent() {
                return ps.getBytes();
            }

            @Override
            public String getString() {
                return new String(getContent(), Charsets.UTF_8);
            }

            @Override
            public String getString(Charset charset) {
                return new String(getContent(), charset);
            }

            @SuppressWarnings("UnstableApiUsage")
            @Override
            public List<String> getStringList(String key, String separator) {
                return Splitter.on(separator).omitEmptyStrings().trimResults().splitToList(get(key));
            }
        };
    }

    private static String processContent(String config) {
        try {
            if (isYaml(config)) {
                return YamlUtils.ymlToPropertiesString(config);
            }
        } catch (Exception e) {
            LOGGER.error("Convert yaml config error. config:{}", config, e);
        }
        return config;
    }

    /**
     * 粗略判断是否是yml格式
     */
    @SuppressWarnings("UnstableApiUsage")
    private static boolean isYaml(String config) {
        Optional<String> firstLine = Splitter.on("\n").splitToList(config)
                .stream()
                .filter(line -> !line.trim().startsWith("#"))
                .findFirst();
        return firstLine.map(line -> (line.trim().endsWith(":") || line.contains(": ")))
                .orElse(false);
    }
}