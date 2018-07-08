package jit.edu.paas.commons.activemq;


import jit.edu.paas.commons.util.JsonUtils;
import jit.edu.paas.commons.util.StringUtils;
import jit.edu.paas.commons.util.jedis.JedisClient;
import jit.edu.paas.controller.WebSocketServer;
import jit.edu.paas.domain.entity.SysLogin;
import jit.edu.paas.domain.vo.ResultVo;
import jit.edu.paas.service.SysLoginService;
import jit.edu.paas.service.UserContainerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 邮箱注册消费者
 * @author jitwxs
 * @since 2018/6/29 17:14
 */
@Slf4j
@Component
public class RegisterConsumer {
    @Autowired
    private SysLoginService loginService;
    @Autowired
    private UserContainerService userContainerService;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private JedisClient jedisClient;
    private final String key = "session" ;
    private final String ID_PREFIX = "UID:";

    @JmsListener(destination = "MQ_QUEUE_REGISTER")
    public void receiveQueue(String text) {
        if(StringUtils.isNotBlank(text)){
            Task task = JsonUtils.jsonToObject(text, Task.class);

            Map<String, Object> map = task.getData();
            String email = (String) map.get("email");
            log.info("验证未激活邮箱，目标邮箱：{}", email);

            SysLogin login = loginService.getByEmail(email);
            if(login != null && login.getHasFreeze()) {
                loginService.deleteById(login);
            }
        }
    }

    @JmsListener(destination = "MQ_QUEUE_CONTAINER")
    public void receiveQueue2(String text) {
        if (StringUtils.isNotBlank(text)) {
            Task task = JsonUtils.jsonToObject(text, Task.class);

            Map<String, Object> map = task.getData();
            String result = (String) map.get("result");
            String userId = (String) map.get("uid");
            String containerId = (String) map.get("containerId");

            String sessionId = null ;
            String field = ID_PREFIX + userId;
            try {
                String res = jedisClient.hget(key, field);
                if (StringUtils.isNotBlank(res)) {
                    sessionId = res;
                } else {
                    log.error("session未找到");
                }
            } catch (Exception e) {
                log.error("缓存读取异常，错误位置：receiveQueue2");
                return;
            }

            if (result.startsWith("容器启动成功")) {
                System.out.println(result);
                ResultVo resultVo = userContainerService.changeStatus(containerId);  //修改数据库
                if (resultVo.getCode() == 0) {
                    try {
                        webSocketServer.sendMessage("容器" + containerId + "已经成功启动",sessionId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                String msg = (String) map.get("message");
                try {
                    webSocketServer.sendMessage("容器" + containerId + "启动失败,失败原因:"+msg,sessionId);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
