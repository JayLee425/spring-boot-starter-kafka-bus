package com.zdl.spring.bus.endpoint;

import com.alibaba.fastjson.JSON;
import com.zdl.spring.bus.BusProperties;
import com.zdl.spring.bus.message.BusMessage;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * EndPoint管理及初始化
 * <p>
 * Created by ZDLegend on 2019/4/10 11:43
 */
public final class EndpointManage {

    public static final int OPERATION_ADD = 0;
    public static final int OPERATION_MODIFY = 1;
    public static final int OPERATION_LOAD = 2;
    public static final int OPERATION_DELETE = 3;

    public static final int CALLBACK_SUCCESS = 11;
    public static final int CALLBACK_EXCEPTION = 12;

    private static final Map<String, BaseBusEndpoint> endpointMap = new HashMap<>();

    private final List<BaseBusEndpoint> endpoints;

    private static BusProperties properties;


    public EndpointManage(List<BaseBusEndpoint> endpoints, BusProperties properties) {
        this.endpoints = endpoints;
        EndpointManage.properties = properties;
    }

    /**
     * 初始化所有EndPoint
     */
    void initEndPointMap() {
        endpoints.stream()
                .filter(endPoint -> endPoint.getClass().getAnnotation(BusEndpoint.class) != null)
                .sorted(Comparator.comparingInt(endPoint -> endPoint.getClass().getAnnotation(BusEndpoint.class).order()))
                .forEach(endPoint -> {
                    BusEndpoint busEndPoint = endPoint.getClass().getAnnotation(BusEndpoint.class);
                    endpointMap.put(busEndPoint.value(), endPoint);
                    endPoint.init();
                });
    }

    /**
     * 从消息到端点类
     */
    @SuppressWarnings("unchecked")
    public static void messageToEndPoint(String msg) {
        var busMessage = JSON.parseObject(msg, BusMessage.class);
        if (msg != null && isTargetEndpoint(properties.getNodeName(), busMessage.getTargets())) {
            BaseBusEndpoint endPoint = endpointMap.get(busMessage.getEndPointId());
            if (endPoint != null) {
                //是否接收指定服务，为空则接收所有服务
                List<String> accepts = Arrays.asList(endPoint.getClass().getAnnotation(BusEndpoint.class).accept());
                if (isAccept(accepts, busMessage.getSource())) {
                    endPoint.messageToEndPoint(busMessage, properties.getNodeName());
                }
            }
        }
    }

    /**
     * 是否为bus message的目的Endpoint
     */
    private static boolean isTargetEndpoint(String nodeName, List<String> targetNames) {
        return CollectionUtils.isEmpty(targetNames) || targetNames.contains(nodeName);
    }

    /**
     * 该Endpoint是否可接收bus message
     */
    private static boolean isAccept(List<String> accepts, String source) {
        return CollectionUtils.isEmpty(accepts) || accepts.contains(source);
    }
}