package com.xiao.ms.gateway.dynamic;

import org.springframework.cloud.gateway.route.RouteDefinition;

/**
 * TODO  NacosDynamicRutesService <br>
 *
 * @date: 2020/12/7 <br>
 * @author: xiaolinlin <br>
 * @since: 1.0 <br>
 * @version: 1.0 <br>
 */
public interface DynamicRouteService {

    /**
     * 新增一个route定义
     *
     * @param routeDefinition : 路由信息
     * @param manual: 控制台-手动
     * @return java.lang.String
     * @author xiaolinlin
     * @date 2020/12/7
     **/
    String addRoute(RouteDefinition routeDefinition, boolean manual);

    /**
     * 更新一个route定义
     *
     * @param routeDefinition :
     * @return java.lang.String
     * @author xiaolinlin
     * @date 2020/12/7
     **/
    String updateRoute(RouteDefinition routeDefinition);

    /**
     * 通过ID删除一个route定义
     *
     * @param id :
     * @return java.lang.String
     * @author xiaolinlin
     * @date 2020/12/7
     **/
    String deleteRoute(String id);
}
