alibaba-nacos配置中心加强
## 优化

1. 支持动态加载**static**的属性配置
2. 自动化配置**Nacos**的**serverAddr**和**groupId**
3. 动态更新配置，不需要写繁琐的nacos配置监听
4. 容器启动时自动从配置中心拉取相关配置
5. 动态获取不同类型数据，避免在代码中做数据类型转换，并可设置默认值

## 引入Maven坐标

```properties
<dependency>
    <groupId>com.github.mx-go</groupId>
    <artifactId>nacos-config-core</artifactId>
    <version>${latest-version}</version>
</dependency>
```

## 使用方式(例)

```java
@RefreshConfig
public class ConfigCenter {

    public static String userName;
    public static int userAge;
    public static String configName;
    public static double configAge;

    public static String content = "content";

    @PostConstruct
    public void init() {
        // 使用方式1
        ConfigFactory.getInstance().addListener("user.properties", iConfig -> {
            userName = iConfig.get("user.name");
            userAge = iConfig.getInt("user.age");
            // 设置默认值
            content = iConfig.get("user.content", content);
            System.out.println(iConfig.getString());
        });

        // 使用方式2
        ConfigFactory.getInstance().addListener("config.properties", "groupId", iConfig -> {
            configName = iConfig.get("config.name");
            // 设置默认值
            configAge = iConfig.getDouble("config.age", 18d);
        });

        // 方式3
        ConfigFactory.getInstance().addListener("route.json", this::updateConfig);
        }
                 
        private void updateConfig(String configInfo) {
            // 更新
        }
    }
 ```
                 
> 建议将所有配置项放在一个类中，如例子中**ConfigCenter**，方便统一管理和配置。
                 
### 注意
- 配置的类需要添加@RefreshConfig注解
- 由于本组件基于Nacos做二次封装。故需要配置**spring.cloud.nacos.server-addr**或**spring.cloud.nacos.config.server-addr**或**spring.cloud.nacos.discovery.server-addr**
- nacos中的**groupId**默认取**spring.application.name**
