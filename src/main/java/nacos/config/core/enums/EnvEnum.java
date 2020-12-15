package nacos.config.core.enums;

import java.util.Arrays;

/**
 * nacos配置中心默认环境地址
 * <p>
 * Create by max on 2020/12/05
 **/
public enum EnvEnum {

    /**
     * 开发环境
     */
    DEV("dev", "localhost:8848"),
    /**
     * 测试环境
     */
    SIT("sit", "localhost:8848"),
    /**
     * 生产环境
     */
    PROD("prod", "localhost:8848"),
    ;

    /**
     * 环境标识
     */
    private final String env;
    /**
     * nacos集群地址
     */
    private final String serverAddress;

    EnvEnum(String env, String serverAddress) {
        this.env = env;
        this.serverAddress = serverAddress;
    }

    public String getEnv() {
        return env;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public static String getServerAddressByEnv(String env) {
        return Arrays.stream(EnvEnum.values())
                .parallel()
                .filter(envEnum -> envEnum.env.equalsIgnoreCase(env))
                .findFirst()
                .orElse(DEV).serverAddress;
    }
}
