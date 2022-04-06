package com.github.mx.nacos.config.core.util;

import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将yaml字符串转为properties字符串
 * <p>
 * Create by max on 2021/01/15
 **/
public class YamlUtils {

    /**
     * 值连接符
     */
    private static final String VALUE_LINK_SIGN_PROPERTIES = "=";
    /**
     * 点
     */
    private static final String DOT = ".";

    private YamlUtils() {
    }

    /**
     * yml格式转换到properties字符串
     *
     * @param ymlContent yml格式字符串
     * @return properties格式字符串
     */
    public static String ymlToPropertiesString(String ymlContent) {
        if (null == ymlContent || "".equals(ymlContent)) {
            return null;
        }
        List<String> propertiesList = new ArrayList<>();
        Yaml yaml = new Yaml();
        Map<?, ?> map = yaml.loadAs(ymlContent, Map.class);
        format(propertiesList, map, "");
        return propertiesList.stream().reduce((k, v) -> k + "\n" + v).orElse("");
    }

    private static void format(List<String> propertiesList, Map<?, ?> map, String prefix) {
        Set<?> set = map.keySet();
        for (Object key : set) {
            Object value = map.get(key);
            if (value instanceof Map) {
                if ("".equals(prefix)) {
                    format(propertiesList, (Map<?, ?>) value, key.toString());
                } else {
                    format(propertiesList, (Map<?, ?>) value, prefix + DOT + key);
                }
            } else {
                if (value == null) {
                    value = "";
                }
                if ("".equals(prefix)) {
                    propertiesList.add(key + VALUE_LINK_SIGN_PROPERTIES + value);
                } else {
                    propertiesList.add(prefix + DOT + key + VALUE_LINK_SIGN_PROPERTIES + value);
                }
            }
        }
    }
}
