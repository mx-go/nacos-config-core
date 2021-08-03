package com.github.mx.nacos.config.core.api;

import com.alibaba.nacos.api.config.listener.Listener;

import java.util.concurrent.Executor;

/**
 * 监听回调
 * <p>
 * Create by max on 2020/12/05
 **/
public interface IChangeListener extends Listener {

    @Override
    default Executor getExecutor() {
        return null;
    }
}