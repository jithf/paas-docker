package jit.edu.paas.controller;

import jit.edu.paas.commons.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;

@ServerEndpoint(value = "/websocket/{userId}")
@Component
@Slf4j
public class WebSocketServer {
    private Jedis jedisClient = new Jedis("192.168.126.151",6379);

    private static HashMap<String,Session> webSocketSet = new HashMap<>();

    private final String key = "session" ;

    private final String ID_PREFIX = "UID:";

    private Session session;

    /**
     * 连接建立成功调用的方法*/
    @OnOpen
    public void onOpen(Session session,@PathParam("userId") String userId) {
        this.session = session;
        webSocketSet.put(session.getId(),session);
        String field = ID_PREFIX + userId;
        try {
            String res = jedisClient.hget(key, field);
            if (StringUtils.isNotBlank(res)) {
                jedisClient.hdel(key,field); //如果有，则删除原来的sessionId
            }
        } catch (Exception e) {
           log.error("缓存读取异常，错误位置：WebSocketServer.onOpen");
        }

        try {
            jedisClient.hset(key, field, session.getId());
        } catch (Exception e) {
            log.error("缓存存储异常，错误位置：WebSocketServer.onOpen");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(@PathParam("userId") String userId) {
        webSocketSet.remove(this.session.getId());
        String field = ID_PREFIX + userId;
        try {
            String res = jedisClient.hget(key, field);
            if (StringUtils.isNotBlank(res)) {
                jedisClient.hdel(key,field); //如果有，则删除原来的sessionId
            }
        } catch (Exception e) {
            log.error("缓存读取异常，错误位置：WebSocketServer.OnClose");
        }
    }

    /**
     * 连接出错调用的方法
     */
    @OnError
    public void onError(Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }

    public void sendMessage(String message,String sessionId) throws IOException {
        Session session = webSocketSet.get(sessionId);
        session.getBasicRemote().sendText(message);
    }
}
