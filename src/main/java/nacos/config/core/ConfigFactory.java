package nacos.config.core;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.google.common.base.Strings;
import nacos.config.core.api.IConfigService;
import nacos.config.core.enums.EnvEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.lang.reflect.Constructor;
import java.util.Properties;

/**
 * 配置工具类
 * <p>
 * Create by max on 2020/12/05
 **/
public class ConfigFactory extends NacosFactory implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(ConfigFactory.class);

    public static Environment environment;

    public static String getProperty(String key) {
        return environment.getProperty(key);
    }

    public static IConfigService getInstance() {
        return LazyHolder.INSTANCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ConfigFactory.environment = environment;
    }

    public static class LazyHolder {
        private static final IConfigService INSTANCE = doCreate();

        private LazyHolder() {
        }

        private static IConfigService doCreate() {
            // 读取nacos配置中心地址
            String serverAddr = getProperty("${spring.cloud.nacos.config.server-addr:}");
            // 读取nacos注册中心地址
            if (Strings.isNullOrEmpty(serverAddr)) {
                serverAddr = getProperty("${spring.cloud.nacos.discovery.server-addr:}");
            }
            // 读取环境配置
            if (Strings.isNullOrEmpty(serverAddr)) {
                String env = getProperty("${spring.profiles.active:}");
                serverAddr = EnvEnum.getServerAddressByEnv(env);
            }
            return createConfigService(serverAddr);
        }

        private static IConfigService createConfigService(String serverAddr) {
            Properties properties = new Properties();
            properties.put(PropertyKeyConst.SERVER_ADDR, serverAddr);
            String namespace = getProperty("${spring.cloud.nacos.config.namespace:}");
            if (!Strings.isNullOrEmpty(namespace)) {
                properties.put(PropertyKeyConst.NAMESPACE, namespace);
            }
            try {
                Class<?> driverImplClass = Class.forName("nacos.config.core.ChangeableConfig");
                Constructor<?> constructor = driverImplClass.getConstructor(Properties.class);
                log.info("Init config center success. properties:{}", properties);
                return (IConfigService) constructor.newInstance(properties);
            } catch (Throwable e) {
                log.error("Nacos createConfigService error. Invalid param(参数错误)", e);
            }
            return null;
        }
    }
}
