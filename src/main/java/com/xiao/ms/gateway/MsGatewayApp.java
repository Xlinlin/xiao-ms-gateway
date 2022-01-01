package com.xiao.ms.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 *  MsGatewayApp <br>
 *
 * @date: 2022/1/1 <br>
 * @author: llxiao <br>
 * @since: 1.0 <br>
 * @version: 1.0 <br>
 */
@EnableDiscoveryClient
@SpringBootApplication
public class MsGatewayApp {

    public static void main(String[] args) {
        System.setProperty("reactor.netty.pool.leasingStrategy", "lifo");
        SpringApplication.run(MsGatewayApp.class, args);
    }
}
