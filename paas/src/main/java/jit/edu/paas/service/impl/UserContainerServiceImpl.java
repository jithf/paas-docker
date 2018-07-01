package jit.edu.paas.service.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.google.common.collect.ImmutableList;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import jit.edu.paas.commons.util.CollectionUtils;
import jit.edu.paas.domain.entity.SysImage;
import jit.edu.paas.domain.entity.UserContainer;
import jit.edu.paas.mapper.SysLoginMapper;
import jit.edu.paas.mapper.UserContainerMapper;
import jit.edu.paas.service.UserContainerService;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * <p>
 * 用户容器表 服务实现类
 * </p>
 *
 * @author jitwxs
 * @since 2018-06-27
 */
@Service
@Slf4j
public class UserContainerServiceImpl extends ServiceImpl<UserContainerMapper, UserContainer> implements UserContainerService {
    @Autowired
    private DockerClient dockerClient;
    @Autowired
    private UserContainerMapper userContainerMapper;

    @Override
    public Page<UserContainer> getContainerListByUser(String userid, Page<UserContainer> page) {
        Page<UserContainer> userContainer = new Page<>();
        List<UserContainer> userlist= userContainerMapper.listContainerById(userid);
        return page.setRecords(userlist);
    }

    @Override
    public void createContainer(String imagename, String[] cmd, String[] ports, String containerName, String projectId) {
        final UserContainer uc = new UserContainer();
        Map<String, List<PortBinding>> portBindings = new HashMap<>();
        for (String port : ports) {
            List<PortBinding> hostPorts = new ArrayList<>();
            hostPorts.add(PortBinding.of("0.0.0.0", port));
            portBindings.put(port, hostPorts);
        }
        List<PortBinding> randomPort = new ArrayList<>();
        randomPort.add(PortBinding.randomPort("0.0.0.0"));
        portBindings.put("443", randomPort);

        HostConfig hostConfig = HostConfig.builder().portBindings(portBindings).build();

        ContainerConfig containerConfig = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image(imagename).exposedPorts(ports)
                .cmd(cmd)
                .build();
        try {
            ContainerCreation creation = dockerClient.createContainer(containerConfig);
            uc.setId(creation.id());
            uc.setName(containerName);
            uc.setCommand(cmd.toString());
            uc.setProjectId(projectId);
            uc.setCreateDate(new Date());
            uc.setPort(ports.toString());
            uc.setStatus("0");
            userContainerMapper.insert(uc);
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("创建失败，检查参数是否正确");
        }

    }

    @Override
    public void startContainer(String containerid) throws DockerException, InterruptedException {
        try {
            dockerClient.startContainer(containerid);
            List<UserContainer> list = userContainerMapper.selectList(new EntityWrapper<UserContainer>().eq("id",containerid));
            UserContainer uc = CollectionUtils.getFirst(list);
            uc.setStatus("1");
            userContainerMapper.updateById(uc);
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("运行失败，检查参数是否正确");
        }
    }

    @Override
    public void stopContainer(String containerid) {
        try {
            dockerClient.stopContainer(containerid,5);
            List<UserContainer> list = userContainerMapper.selectList(new EntityWrapper<UserContainer>().eq("id",containerid));
            UserContainer uc = CollectionUtils.getFirst(list);
            uc.setStatus("0");
            userContainerMapper.updateById(uc);
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("运行失败，检查参数是否正确");
        }
    }

    @Override
    public void killContainer(String containerid) {
        try {
            dockerClient.killContainer(containerid);
            List<UserContainer> list = userContainerMapper.selectList(new EntityWrapper<UserContainer>().eq("id",containerid));
            UserContainer uc = CollectionUtils.getFirst(list);
            uc.setStatus("0");
            userContainerMapper.updateById(uc);
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("运行失败，检查参数是否正确");
        }
    }

    @Override
    public void removeContainer(String containerid) {
        try {
            dockerClient.removeContainer(containerid);
            userContainerMapper.deleteById(containerid);
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("运行失败，检查参数是否正确");
        }
    }

    @Override
    public void pauseContainer(String containerid) {
        try {
            dockerClient.pauseContainer(containerid);
            List<UserContainer> list = userContainerMapper.selectList(new EntityWrapper<UserContainer>().eq("id",containerid));
            UserContainer uc = CollectionUtils.getFirst(list);
            uc.setStatus("2");
            userContainerMapper.updateById(uc);
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("运行失败，检查参数是否正确");
        }
    }

    @Override
    public void unpauseContainer(String containerid) {
        try {
            dockerClient.unpauseContainer(containerid);
            List<UserContainer> list = userContainerMapper.selectList(new EntityWrapper<UserContainer>().eq("id",containerid));
            UserContainer uc = CollectionUtils.getFirst(list);
            uc.setStatus("1");
            userContainerMapper.updateById(uc);
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("运行失败，检查参数是否正确");
        }
    }



    @Override
    public TopResults getTopResult(String containerid) {
        try {
            return dockerClient.topContainer(containerid);
        } catch (DockerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.error("运行失败，检查参数是否正确");
        }
        return null;
    }
}
