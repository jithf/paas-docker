package jit.edu.paas.service;

import com.baomidou.mybatisplus.plugins.Page;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.TopResults;
import jit.edu.paas.domain.entity.SysImage;
import jit.edu.paas.domain.entity.UserContainer;
import com.baomidou.mybatisplus.service.IService;

/**
 * <p>
 * 用户容器表 服务类
 * </p>
 *
 * @author jitwxs
 * @since 2018-06-27
 */
public interface UserContainerService extends IService<UserContainer> {

    Page<UserContainer> getContainerListByUser(String userid,Page<UserContainer> page);

    void startContainer(String containerid) throws DockerException, InterruptedException;

    void stopContainer(String containerid);

    void killContainer(String containerid);

    void removeContainer(String containerid);

    void pauseContainer(String containerid);

    void unpauseContainer(String containerid);

    void createContainer(String imagename,String[] cmd,String[] ports,String containerName,String projectId);

    TopResults getTopResult(String containerid);
}
