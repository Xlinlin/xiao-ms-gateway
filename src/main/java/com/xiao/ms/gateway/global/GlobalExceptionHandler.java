package com.xiao.ms.gateway.global;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSONObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关异常通用处理器，只作用在webflux 环境下 , 优先级低于 ResponseStatusExceptionHandler 执行 <br>
 *
 * @date: 2020/11/30 <br>
 * @author: xiaolinlin <br>
 * @since: 1.0 <br>
 * @version: 1.0 <br>
 */
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    /**
     * MessageReader
     */
    private List<HttpMessageReader<?>> messageReaders = Collections.emptyList();

    /**
     * MessageWriter
     */
    private List<HttpMessageWriter<?>> messageWriters = Collections.emptyList();

    /**
     * ViewResolvers
     */
    private List<ViewResolver> viewResolvers = Collections.emptyList();

    /**
     * 存储处理异常后的信息
     */
    private ThreadLocal<Map<String, Object>> exceptionHandlerResult = new ThreadLocal<>();

    /**
     * 参考AbstractErrorWebExceptionHandler
     */
    public void setMessageReaders(List<HttpMessageReader<?>> messageReaders) {
        Assert.notNull(messageReaders, "'messageReaders' must not be null");
        this.messageReaders = messageReaders;
    }

    /**
     * 参考AbstractErrorWebExceptionHandler
     */
    public void setViewResolvers(List<ViewResolver> viewResolvers) {
        this.viewResolvers = viewResolvers;
    }

    /**
     * 参考AbstractErrorWebExceptionHandler
     */
    public void setMessageWriters(List<HttpMessageWriter<?>> messageWriters) {
        Assert.notNull(messageWriters, "'messageWriters' must not be null");
        this.messageWriters = messageWriters;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        //错误记录
        ServerHttpRequest request = exchange.getRequest();
        String mchId = getMchId(request);
        log.info("请求异常，当前商户信息：{}", mchId);
        // 按照异常类型进行处理
        HttpStatus httpStatus;
        String body;
        if (ex instanceof NotFoundException) {
            httpStatus = HttpStatus.NOT_FOUND;
            body = "请求服务走丢";
            log.warn("[404]异常请求路径:{},记录异常信息:{}", request.getPath(), ex.getMessage());
        } else if (ex instanceof ResponseStatusException) {
            ResponseStatusException responseStatusException = (ResponseStatusException) ex;
            httpStatus = responseStatusException.getStatus();
            body = responseStatusException.getMessage();
            if (HttpStatus.NOT_FOUND == httpStatus) {
                log.warn("[404]异常请求路径:{},记录异常信息:{}", request.getPath(), ex.getMessage());
            } else {
                log.error("[未知异常]异常请求路径:{},记录异常信息:{}", request.getPath(), ex.getMessage());
            }
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            if (sentinel(ex, request)) {
                body = "请求太频繁，请稍后重试";
            } else {
                log.error("[未知异常]异常请求路径:{},记录异常信息:{}", request.getPath(), ex.getMessage());
                body = ex.getMessage();
            }

        }
        //封装响应体,此body可修改为自己的jsonBody
        Map<String, Object> result = new HashMap<>(2, 1);
        // 修改Http的状态为200
        result.put("httpStatus", HttpStatus.OK);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("return_code", "FAIL");
        jsonObject.put("return_msg", body);
        jsonObject.put("msg_code", httpStatus.value());
        result.put("body", jsonObject.toJSONString());

        //参考AbstractErrorWebExceptionHandler
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        // 写入异常
        exceptionHandlerResult.set(result);

        /// log.warn("[未知异常]异常请求路径:{},记录异常信息:{}", request.getPath(), ex.getMessage());
        ServerRequest newRequest = ServerRequest.create(exchange, this.messageReaders);
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse).route(newRequest)
                .switchIfEmpty(Mono.error(ex))
                .flatMap((handler) -> handler.handle(newRequest))
                .flatMap((response) -> write(exchange, response));

    }

    /**
     * 获取商户信息
     *
     * @param request :
     * @return java.lang.String
     * @author llxiao
     * @date 2021/12/14
     **/
    private String getMchId(ServerHttpRequest request) {
        String mchId = request.getQueryParams().getFirst("mchid");
        if (StringUtils.isEmpty(mchId)) {
            mchId = request.getHeaders().getFirst("mchid");
        }
        return mchId;
    }

    /**
     * sentinel异常处理
     *
     * @param ex :
     * @return boolean
     * @author llxiao
     * @date 2021/12/14
     **/
    private boolean sentinel(Throwable ex, ServerHttpRequest request) {
        if (ex instanceof BlockException) {
            BlockException blockException = (BlockException) ex;
            log.warn("请求限流，请求路径：{}，商户信息：{}，限流规则：{}，限流信息：{}", request.getPath(), getMchId(request),
                    blockException.getRule().getResource(), blockException.getMessage());
            return true;
        }
        return false;
    }

    /**
     * 参考DefaultErrorWebExceptionHandler
     */
    protected Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> result = exceptionHandlerResult.get();
        exceptionHandlerResult.remove();
        return ServerResponse.status((HttpStatus) result.get("httpStatus"))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(BodyInserters.fromObject(result.get("body")));
    }

    /**
     * 参考AbstractErrorWebExceptionHandler
     */
    private Mono<? extends Void> write(ServerWebExchange exchange,
                                       ServerResponse response) {
        exchange.getResponse().getHeaders()
                .setContentType(response.headers().getContentType());
        return response.writeTo(exchange, new ResponseContext());
    }

    /**
     * 参考AbstractErrorWebExceptionHandler
     */
    private class ResponseContext implements ServerResponse.Context {

        @Override
        public List<HttpMessageWriter<?>> messageWriters() {
            return GlobalExceptionHandler.this.messageWriters;
        }

        @Override
        public List<ViewResolver> viewResolvers() {
            return GlobalExceptionHandler.this.viewResolvers;
        }

    }

}