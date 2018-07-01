package jit.edu.paas.controller;

import com.baomidou.mybatisplus.plugins.Page;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.TopResults;
import io.swagger.annotations.Api;
import jit.edu.paas.domain.entity.UserContainer;
import jit.edu.paas.service.UserContainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户Controller
 * @author jitwxs
 * @since 2018/6/28 14:23
 */
@RestController
@RequestMapping("/user")
@Api(tags={"用户Controller"})
public class UserController {
    @Autowired
    private UserContainerService userContainerService;

    /**
     * 通过用户id获取容器列表
     * @author sya
     * @param userid
     * @param page
     * @return
     */
    @PostMapping("/containerListByUser")
    public Page<UserContainer> containerListByUser(String userid, Page<UserContainer> page){
        return userContainerService.getContainerListByUser(userid, page);
    }

    /**
     * 开启容器
     * @author sya
     * @param containerid
     * @throws DockerException
     * @throws InterruptedException
     */
    @PostMapping("/startContainer")
    public String startcontainer(String containerid){
        try {
            userContainerService.startContainer(containerid);
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "success";
    }

    /**
     * 创建容器
     * @authoer sya
     * @param imagename
     * @param cmd
     * @param ports
     * @param containerName
     * @param projectId
     */
    @PostMapping("/createContainer")
    public String createcontainer(String imagename,String[] cmd,String[] ports,String containerName,String projectId){
        userContainerService.createContainer(imagename,cmd,ports,containerName,projectId);
        return "success";
    }

    /**
     * 停止容器运行
     * @param containerid
     */
    @PostMapping("/stopContainer")
    public String stopcontainer(String containerid){
        userContainerService.stopContainer(containerid);
        return "success";
    }

    /**
     * 强制停止容器
     * @param containerid
     */
    @PostMapping("/killContainer")
    public String killcontainer(String containerid){
        userContainerService.killContainer(containerid);
        return "success";
    }

    /**
     * 移除容器
     * @param containerid
     */
    @PostMapping("/rmContainer")
    public String rmcontainer(String containerid){
        userContainerService.removeContainer(containerid);
        return "success";
    }

    /**
     * 暂停容器运行
     * @param containerid
     */
    @PostMapping("/pauseContainer")
    public String pausecontainer(String containerid){
        userContainerService.pauseContainer(containerid);
        return "success";
    }

    /**
     * 把容器从暂停状态改为进行
     * @param containerid
     */
    @PostMapping("/unpauseContainer")
    public String unpausecontainer(String containerid){
        userContainerService.unpauseContainer(containerid);
        return "success";
    }

    /**
     * 列出正在运行的容器的内部状态
     * @param containerid
     * @return
     */
    @PostMapping("/TopResult")
    public TopResults TopResult(String containerid){
        return userContainerService.getTopResult(containerid);
    }
}