package com.github.mx.nacos.config.core;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.config.ConfigType;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.alibaba.nacos.client.config.filter.impl.ConfigFilterChainManager;
import com.alibaba.nacos.client.config.filter.impl.ConfigRequest;
import com.alibaba.nacos.client.config.filter.impl.ConfigResponse;
import com.alibaba.nacos.client.config.http.HttpAgent;
import com.alibaba.nacos.client.config.http.MetricsHttpAgent;
import com.alibaba.nacos.client.config.http.ServerHttpAgent;
import com.alibaba.nacos.client.config.impl.ClientWorker;
import com.alibaba.nacos.client.config.impl.LocalConfigInfoProcessor;
import com.alibaba.nacos.client.config.impl.LocalEncryptedDataKeyProcessor;
import com.alibaba.nacos.client.config.utils.ContentUtils;
import com.alibaba.nacos.client.config.utils.ParamUtils;
import com.alibaba.nacos.client.utils.LogUtils;
import com.alibaba.nacos.client.utils.ParamUtil;
import com.alibaba.nacos.client.utils.ValidatorUtils;
import com.alibaba.nacos.common.http.HttpRestResult;
import com.alibaba.nacos.common.utils.StringUtils;
import com.github.mx.nacos.config.core.api.IChangeListener;
import com.github.mx.nacos.config.core.api.IConfig;
import com.github.mx.nacos.config.core.api.IConfigService;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Config Impl.
 *
 * @author Nacos
 */
@SuppressWarnings("PMD.ServiceOrDaoClassShouldEndWithImplRule")
public class ChangeableConfig implements IConfigService {

    private static final Logger LOGGER = LogUtils.logger(NacosConfigService.class);

    private static final long POST_TIMEOUT = 3000L;

    /**
     * http agent.
     */
    private final HttpAgent agent;

    /**
     * long polling.
     */
    private final ClientWorker worker;

    private String namespace;

    private final String encode;

    private final ConfigFilterChainManager configFilterChainManager;

    public ChangeableConfig(Properties properties) throws NacosException {
        ValidatorUtils.checkInitParam(properties);
        String encodeTmp = properties.getProperty(PropertyKeyConst.ENCODE);
        if (StringUtils.isBlank(encodeTmp)) {
            this.encode = Constants.ENCODE;
        } else {
            this.encode = encodeTmp.trim();
        }
        initNamespace(properties);
        this.configFilterChainManager = new ConfigFilterChainManager(properties);

        this.agent = new MetricsHttpAgent(new ServerHttpAgent(properties));
        this.agent.start();
        this.worker = new ClientWorker(this.agent, this.configFilterChainManager, properties);
    }

    private void initNamespace(Properties properties) {
        namespace = ParamUtil.parseNamespace(properties);
        properties.put(PropertyKeyConst.NAMESPACE, namespace);
    }

    @Override
    public String getConfig(String dataId, String group, long timeoutMs) throws NacosException {
        return getConfigInner(namespace, dataId, group, timeoutMs);
    }

    @Override
    public String getConfigAndSignListener(String dataId, String group, long timeoutMs, Listener listener) throws NacosException {
        String content = getConfig(dataId, group, timeoutMs);
        worker.addTenantListenersWithContent(dataId, group, content, Arrays.asList(listener));
        return content;
    }

    @Override
    public void addListener(String dataId, String group, Listener listener) throws NacosException {
        worker.addTenantListeners(dataId, group, Arrays.asList(listener));
    }

    @Override
    public void registerListener(String dataId, Consumer<IConfig> consumer) {
        String groupId = ConfigFactory.getProperty("${spring.application.name:}");
        try {
            registerListener(dataId, groupId, consumer);
        } catch (Exception e) {
            LOGGER.error("registerListener error. dataId:{}, groupId:{}", dataId, groupId, e);
        }
    }

    @Override
    public void registerListener(String dataId, String groupId, Consumer<IConfig> consumer) {
        try {
            addListener(dataId, groupId, (IChangeListener) configInfo -> consumer.accept(RemoteConfig.convert(configInfo)));
            consumer.accept(RemoteConfig.convert(getConfig(dataId, groupId, POST_TIMEOUT)));
        } catch (Exception e) {
            LOGGER.error("registerListener error. dataId:{}, groupId:{}", dataId, groupId, e);
        }
    }

    @Override
    public boolean publishConfig(String dataId, String group, String content) throws NacosException {
        return publishConfig(dataId, group, content, ConfigType.getDefaultType().getType());
    }

    @Override
    public boolean publishConfig(String dataId, String group, String content, String type) throws NacosException {
        return publishConfigInner(namespace, dataId, group, null, null, null, content, type);
    }

    @Override
    public boolean removeConfig(String dataId, String group) throws NacosException {
        return removeConfigInner(namespace, dataId, group, null);
    }

    @Override
    public void removeListener(String dataId, String group, Listener listener) {
        worker.removeTenantListener(dataId, group, listener);
    }

    private String getConfigInner(String tenant, String dataId, String group, long timeoutMs) throws NacosException {
        group = blank2defaultGroup(group);
        ParamUtils.checkKeyParam(dataId, group);
        ConfigResponse cr = new ConfigResponse();

        cr.setDataId(dataId);
        cr.setTenant(tenant);
        cr.setGroup(group);

        // 优先使用本地配置
        String content = LocalConfigInfoProcessor.getFailover(agent.getName(), dataId, group, tenant);
        if (content != null) {
            LOGGER.warn("[{}] [get-config] get failover ok, dataId={}, group={}, tenant={}, config={}", agent.getName(), dataId, group, tenant, ContentUtils.truncateContent(content));
            cr.setContent(content);
            String encryptedDataKey = LocalEncryptedDataKeyProcessor.getEncryptDataKeyFailover(agent.getName(), dataId, group, tenant);
            cr.setEncryptedDataKey(encryptedDataKey);
            configFilterChainManager.doFilter(null, cr);
            content = cr.getContent();
            return content;
        }

        try {
            ConfigResponse response = worker.getServerConfig(dataId, group, tenant, timeoutMs);
            cr.setContent(response.getContent());
            cr.setEncryptedDataKey(response.getEncryptedDataKey());

            configFilterChainManager.doFilter(null, cr);
            content = cr.getContent();

            return content;
        } catch (NacosException ioe) {
            if (NacosException.NO_RIGHT == ioe.getErrCode()) {
                throw ioe;
            }
            LOGGER.warn("[{}] [get-config] get from server error, dataId={}, group={}, tenant={}, msg={}", agent.getName(), dataId, group, tenant, ioe.toString());
        }

        LOGGER.warn("[{}] [get-config] get snapshot ok, dataId={}, group={}, tenant={}, config={}", agent.getName(), dataId, group, tenant, ContentUtils.truncateContent(content));
        content = LocalConfigInfoProcessor.getSnapshot(agent.getName(), dataId, group, tenant);
        cr.setContent(content);
        String encryptedDataKey = LocalEncryptedDataKeyProcessor.getEncryptDataKeyFailover(agent.getName(), dataId, group, tenant);
        cr.setEncryptedDataKey(encryptedDataKey);
        configFilterChainManager.doFilter(null, cr);
        content = cr.getContent();
        return content;
    }

    private String blank2defaultGroup(String group) {
        return (StringUtils.isBlank(group)) ? Constants.DEFAULT_GROUP : group.trim();
    }

    private boolean removeConfigInner(String tenant, String dataId, String group, String tag) throws NacosException {
        group = blank2defaultGroup(group);
        ParamUtils.checkKeyParam(dataId, group);
        String url = Constants.CONFIG_CONTROLLER_PATH;
        Map<String, String> params = new HashMap<String, String>(4);
        params.put("dataId", dataId);
        params.put("group", group);

        if (StringUtils.isNotEmpty(tenant)) {
            params.put("tenant", tenant);
        }
        if (StringUtils.isNotEmpty(tag)) {
            params.put("tag", tag);
        }
        HttpRestResult<String> result = null;
        try {
            result = agent.httpDelete(url, null, params, encode, POST_TIMEOUT);
        } catch (Exception ex) {
            LOGGER.warn("[remove] error, " + dataId + ", " + group + ", " + tenant + ", msg: " + ex.toString());
            return false;
        }

        if (result.ok()) {
            LOGGER.info("[{}] [remove] ok, dataId={}, group={}, tenant={}", agent.getName(), dataId, group, tenant);
            return true;
        } else if (HttpURLConnection.HTTP_FORBIDDEN == result.getCode()) {
            LOGGER.warn("[{}] [remove] error, dataId={}, group={}, tenant={}, code={}, msg={}", agent.getName(), dataId, group, tenant, result.getCode(), result.getMessage());
            throw new NacosException(result.getCode(), result.getMessage());
        } else {
            LOGGER.warn("[{}] [remove] error, dataId={}, group={}, tenant={}, code={}, msg={}", agent.getName(), dataId, group, tenant, result.getCode(), result.getMessage());
            return false;
        }
    }

    private boolean publishConfigInner(String tenant, String dataId, String group, String tag, String appName, String betaIps, String content, String type) throws NacosException {
        group = blank2defaultGroup(group);
        ParamUtils.checkParam(dataId, group, content);

        ConfigRequest cr = new ConfigRequest();
        cr.setDataId(dataId);
        cr.setTenant(tenant);
        cr.setGroup(group);
        cr.setContent(content);
        configFilterChainManager.doFilter(cr, null);
        content = cr.getContent();

        String url = Constants.CONFIG_CONTROLLER_PATH;
        Map<String, String> params = new HashMap<String, String>(6);
        params.put("dataId", dataId);
        params.put("group", group);
        params.put("content", content);
        params.put("type", type);
        if (StringUtils.isNotEmpty(tenant)) {
            params.put("tenant", tenant);
        }
        if (StringUtils.isNotEmpty(appName)) {
            params.put("appName", appName);
        }
        if (StringUtils.isNotEmpty(tag)) {
            params.put("tag", tag);
        }
        String dataKey = (String) cr.getParameter("encryptedDataKey");
        if (StringUtils.isNotEmpty(dataKey)) {
            params.put("encryptedDataKey", dataKey);
        }
        Map<String, String> headers = new HashMap<String, String>(1);
        if (StringUtils.isNotEmpty(betaIps)) {
            headers.put("betaIps", betaIps);
        }

        HttpRestResult<String> result = null;
        try {
            result = agent.httpPost(url, headers, params, encode, POST_TIMEOUT);
        } catch (Exception ex) {
            LOGGER.warn("[{}] [publish-single] exception, dataId={}, group={}, msg={}", agent.getName(), dataId, group, ex.toString());
            return false;
        }

        if (result.ok()) {
            LOGGER.info("[{}] [publish-single] ok, dataId={}, group={}, tenant={}, config={}", agent.getName(), dataId, group, tenant, ContentUtils.truncateContent(content));
            return true;
        } else if (HttpURLConnection.HTTP_FORBIDDEN == result.getCode()) {
            LOGGER.warn("[{}] [publish-single] error, dataId={}, group={}, tenant={}, code={}, msg={}", agent.getName(), dataId, group, tenant, result.getCode(), result.getMessage());
            throw new NacosException(result.getCode(), result.getMessage());
        } else {
            LOGGER.warn("[{}] [publish-single] error, dataId={}, group={}, tenant={}, code={}, msg={}", agent.getName(), dataId, group, tenant, result.getCode(), result.getMessage());
            return false;
        }

    }

    @Override
    public String getServerStatus() {
        if (worker.isHealthServer()) {
            return "UP";
        } else {
            return "DOWN";
        }
    }

    @Override
    public void shutDown() throws NacosException {
        agent.shutdown();
        worker.shutdown();
    }
}