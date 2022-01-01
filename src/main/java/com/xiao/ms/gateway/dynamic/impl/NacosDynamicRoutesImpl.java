package com.xiao.ms.gateway.dynamic.impl;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import java.util.Properties;
import java.util.concurrent.Executor;
import javax.annotation.PostConstruct;

import com.xiao.ms.gateway.dynamic.AbstractDynamicRouteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

/**
 * 动态网关调整<br>
 *
 * @date: 2020/12/7 <br>
 * @author: xiaolinlin <br>
 * @since: 1.0 <br>
 * @version: 1.0 <br>
 */
@Service
@RefreshScope
@Slf4j
public class NacosDynamicRoutesImpl extends AbstractDynamicRouteService {

    // nacos 的配置信息
    /// private NacosConfigProperties nacosConfigProperties;

    /**
     * nacos服务地址
     *
     *
     *
     *
     *
     */
    @Value("${spring.cloud.nacos.server-addr}")
    private String serverAddr;
    /**
     * namespace
     */
    @Value("${spring.cloud.nacos.discovery.namespace}")
    private String nameSpace;

    /**
     * 配置组
     */
    @Value("${spring.cloud.nacos.config.discovery.group:DEFAULT_GROUP}")
    private String gourpId;

    /**
     * 动态路由dataID,JSON格式
     */
    @Value("${spring.cloud.nacos.config.route-data-id:xiao-ms-gateway-route}")
    private String routeDataId;

    /**
     * 初始化启动监听nacos配置
     *
     * @return void
     * @author xiaolinlin
     * @date 2020/12/7
     **/
    @PostConstruct
    public void dynamicRouteByNacosListener() {
        log.info("gateway route init...");
        try {
            Properties properties = new Properties();
            properties.setProperty("serverAddr", serverAddr);
            properties.setProperty("namespace", nameSpace);
            ConfigService configService = NacosFactory.createConfigService(properties);
            String config = configService.getConfig(routeDataId, gourpId, 5000);
            // 初次初始化
            initConfig(config);
            // 动态更新
            configService.addListener(routeDataId, gourpId, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    clearRoute();
                    try {
                        log.info("配置发生了变更，变更网关配置内容：{}", configInfo);
                        initConfig(configInfo);
                    } catch (Exception e) {
                        log.warn("本次配置更新失败，错误详情：", e);
                    }
                }
            });
        } catch (NacosException e) {
            log.error("监听网关路由配置异常，错误信息：", e);
        }
    }
}