package com.zdl.spring.bus.endpoint;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zdl.spring.bus.message.BusMessage;
import com.zdl.spring.bus.utils.ClassUtil;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.function.Consumer;

import static com.zdl.spring.bus.endpoint.EndpointManage.*;

/**
 * 消息总线端点基类
 * <p>该注解需要和{@link BusEndpoint @BusEndpoint}搭配使用
 * Created by ZDLegend on 2019/4/10 11:11
 */
public interface BaseBusEndpoint<T> {

    /**
     * 初始化（系统启动时调用）
     */
    void init();

    /**
     * 消息内容添加操作
     */
    void insert(List<T> list);

    /**
     * 消息内容修改操作, 默认调用资源添加操作方法
     */
    default void modify(List<T> list) {
        insert(list);
    }

    /**
     * 消息内容加载操作, 默认调用资源添加操作方法
     */
    default void load(List<T> list) {
        insert(list);
    }

    /**
     * 消息内容资源删除操作
     */
    void delete(List<T> list);

    /**
     * 消息回调
     *
     * @param id        消息id
     * @param throwable 消息错误异常
     */
    default void callBack(String id, String source, Throwable throwable) {
    }

    @SuppressWarnings("unchecked")
    default void messageToEndPoint(BusMessage message) {

        var operation = message.getOperation();

        if (message.isCallBack()) {
            switch (operation) {
                case CALLBACK_SUCCESS:
                    callBack(message.getId(), message.getSource(), null);
                    return;
                case CALLBACK_EXCEPTION:
                    callBack(message.getId(), message.getSource(), JSON.parseObject(message.getData().toString(), Throwable.class));
                    return;
                default:
                    return;
            }
        }

        Consumer<List<T>> handle;
        switch (operation) {
            case OPERATION_ADD:
                handle = this::insert;
                break;
            case OPERATION_MODIFY:
                handle = this::modify;
                break;
            case OPERATION_LOAD:
                handle = this::load;
                break;
            case OPERATION_DELETE:
                handle = this::delete;
                break;
            default:
                handle = this::insert;
        }

        List<T> list;
        if (!CollectionUtils.isEmpty(message.getData()) && message.getData().get(0) instanceof JSONObject) {
            list = JSON.parseArray((new JSONArray((List<Object>) message.getData())).toJSONString(),
                    (Class<T>) ClassUtil.getGenericType(this.getClass()));
        } else {
            list = message.getData();
        }

        handle.accept(list);
    }
}
