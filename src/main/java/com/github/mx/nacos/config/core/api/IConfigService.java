package com.github.mx.nacos.config.core.api;

import com.alibaba.nacos.api.config.ConfigService;

/**
 * 注册监听
 * <p>
 * Create by max on 2020/12/05
 **/
public interface IConfigService extends ConfigService {

    /**
     * 注册监听
     *
     * @param dataId   dataId
     * @param listener 监听回调
     */
    void registerListener(String dataId, IChangeListener listener);

    /**
     * 注册监听
     *
     * @param dataId   dataId
     * @param groupId  groupId
     * @param listener 监听回调
     */
    void registerListener(String dataId, String groupId, IChangeListener listener);
}