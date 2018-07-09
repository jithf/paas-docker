package jit.edu.paas.service.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.google.common.collect.ImmutableList;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.*;
import jit.edu.paas.commons.activemq.MQProducer;
import jit.edu.paas.commons.activemq.Task;
import jit.edu.paas.commons.convert.UserContainerDTOConvert;
import jit.edu.paas.commons.util.*;
import jit.edu.paas.domain.dto.UserContainerDTO;
import jit.edu.paas.domain.entity.SysImage;
import jit.edu.paas.domain.entity.SysVolume;
import jit.edu.paas.domain.entity.UserContainer;
import jit.edu.paas.domain.enums.*;
import jit.edu.paas.domain.vo.ResultVO;
import jit.edu.paas.exception.CustomException;
import jit.edu.paas.mapper.SysVolumesMapper;
import jit.edu.paas.mapper.UserContainerMapper;
import jit.edu.paas.mapper.UserProjectMapper;
import jit.edu.paas.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.Destination;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


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
    private PortService portService;
    @Autowired
    private SysLoginService loginService;
    @Autowired
    private SysImageService imageService;
    @Autowired
    private SysLogService sysLogService;
    @Autowired
    private ProjectLogService projectLogService;

    @Autowired
    private SysVolumesMapper sysVolumesMapper;
    @Autowired
    private UserProjectMapper projectMapper;
    @Autowired
    private UserContainerMapper userContainerMapper;

    @Autowired
    private MQProducer mqProducer;
    @Autowired
    private DockerClient dockerClient;

    @Autowired
    private UserContainerDTOConvert dtoConvert;
    @Autowired
    private HttpServletRequest request;

    @Override
    public UserContainerDTO getById(String id) {
        return dtoConvert.convert(userContainerMapper.selectById(id));
    }

    /**
     * 开启容器任务
     * @author jitwxs
     * @since 2018/7/9 22:04
     */
    @Override
    public void startContainerTask(String userId, String containerId) {
        try {
            // 执行任务
            Future<ResultVO> future = startContainer(userId, containerId);
            // 获取执行结果，不超过10s
            ResultVO resultVO = future.get(10, TimeUnit.SECONDS);

            if(resultVO == null) {
                sendMQ(userId, containerId, ResultVOUtils.error(ResultEnum.REQUEST_TIMEOUT));
            } else {
                sendMQ(userId, containerId, resultVO);
            }
        } catch (Exception e) {
            log.error("执行启动容器异步任务出现错误，错误位置：{}，错误栈：{}",
                    "UserContainerServiceImpl.startContainerTask()", HttpClientUtils.getStackTraceAsString(e));
            sendMQ(userId, containerId, ResultVOUtils.error(ResultEnum.REQUEST_ERROR));
        }
    }

    /**
     * 开启容器
     * @author jitwxs
     * @since 2018/7/9 22:05
     */
    @Async("taskExecutor")
    @Transactional(rollbackFor = Exception.class)
    public Future<ResultVO> startContainer(String userId, String containerId) {
        // 1、鉴权
        ResultVO resultVO = checkPermission(userId, containerId);
        if(ResultEnum.OK.getCode() != resultVO.getCode()) {
            return new AsyncResult<>(ResultVOUtils.error(ResultEnum.AUTHORITY_ERROR));
        }

        // 2、判断状态
        if(ContainerStatusEnum.START == getStatus(containerId)) {
            return new AsyncResult<>(ResultVOUtils.error(ResultEnum.CONTAINER_ALREADY_START));
        }

        // 3、开启容器
        try {
            dockerClient.startContainer(containerId);
            changeStatus(containerId);

            // 写入日志
            projectLogService.saveSuccessLog(getProjectId(containerId),containerId,ProjectLogTypeEnum.START_CONTAINER);

            // 发送成功消息
            return new AsyncResult<>(ResultVOUtils.success());
        } catch (Exception e) {
            log.error("开启容器出现异常，异常位置：{}，错误栈：{}",
                    "UserContainerServiceImpl.startContainer()",HttpClientUtils.getStackTraceAsString(e));
            // 写入日志
            projectLogService.saveErrorLog(getProjectId(containerId),containerId,ProjectLogTypeEnum.START_CONTAINER_ERROR,ResultEnum.DOCKER_EXCEPTION);

            // 发送异常消息
            return new AsyncResult<>(ResultVOUtils.error(ResultEnum.CONTAINER_START_ERROR));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO createContainer(String userId, String imageId, String[] cmd, Map<String,Integer> portMap,
                                    String containerName, String projectId, String[] env, String[] destination) {
        // 1、Project鉴权
        Boolean b = projectMapper.hasBelong(projectId, userId);
        if(!b) {
            return ResultVOUtils.error(ResultEnum.PERMISSION_ERROR.getCode(), "项目不存在或权限错误");
        }

        // 2、校验Image
        SysImage image = imageService.getById(imageId);
        if(image == null) {
            return ResultVOUtils.error(ResultEnum.IMAGE_EXCEPTION);
        }
        if(!imageService.hasAuthImage(userId, image)) {
            return ResultVOUtils.error(ResultEnum.PERMISSION_ERROR);
        }

        // 获取暴露接口
        ResultVO resultVO = imageService.listExportPorts(imageId, userId);
        if(ResultEnum.OK.getCode() != resultVO.getCode()) {
            return resultVO;
        }
        List<String> exportPorts = (List<String>) resultVO.getData();
        // 3、校验输入的端口
        if(!checkPorts(exportPorts, portMap)) {
            return ResultVOUtils.error(ResultEnum.INPUT_PORT_ERROR);
        }

        UserContainer uc = new UserContainer();
        HostConfig hostConfig;
        ContainerConfig.Builder builder = ContainerConfig.builder();

        // 4、设置暴露端口
        if(portMap != null) {
            // 宿主机端口与暴露端口绑定
            Set<String> realExportPorts = new HashSet<>();
            Map<String, List<PortBinding>> portBindings = new HashMap<>(16);

            portMap.forEach((k,v) -> {
                realExportPorts.add(k);
                // 捆绑端口
                List<PortBinding> hostPorts = new ArrayList<>();
                // 分配主机端口，如果用户输入端口被占用，随机分配
                Integer hostPort = portService.hasUse(v) ? portService.randomPort() : v;
                hostPorts.add(PortBinding.of("0.0.0.0", hostPort));
                portBindings.put(k, hostPorts);
            });

            uc.setPort(JsonUtils.objectToJson(portBindings));

            builder.exposedPorts(realExportPorts);

            hostConfig = HostConfig.builder()
                    .portBindings(portBindings)
                    .build();
        } else {
            hostConfig = HostConfig.builder().build();
        }

        // 5、构建ContainerConfig
        builder.hostConfig(hostConfig);
        builder.image(image.getFullName());
        builder.tty(true);
        if(CollectionUtils.isNotArrayEmpty(cmd)) {
            builder.cmd(cmd);
            uc.setCommand(Arrays.toString(cmd));
        }
        if(CollectionUtils.isNotArrayEmpty(destination)) {
            builder.volumes(destination);
        }
        if(CollectionUtils.isNotArrayEmpty(env)) {
            builder.env(env);
            uc.setEnv(Arrays.toString(env));
        }
        ContainerConfig containerConfig = builder.build();

        try {
            ContainerCreation creation = dockerClient.createContainer(containerConfig);

            uc.setId(creation.id());
            // 仅存在于数据库，不代表实际容器名
            uc.setName(containerName);
            uc.setProjectId(projectId);
            uc.setImage(image.getFullName());

            if(CollectionUtils.isNotArrayEmpty(destination)) {
                // 为数据库中的sysvolumes插入
                ImmutableList<ContainerMount> info = dockerClient.inspectContainer(creation.id()).mounts();
                for(int i = 0;i<destination.length;i++){
                    SysVolume sysVolume = new SysVolume();
                    sysVolume.setContainerId(creation.id());
                    sysVolume.setDestination(destination[i]);
                    sysVolume.setName(info.get(i).name());
                    sysVolume.setSource(info.get(i).source());
                    sysVolumesMapper.insert(sysVolume);
                }
            }

            // 6、设置状态
            ContainerStatusEnum status = getStatus(creation.id());
            if(status == null) {
                throw new CustomException(ResultEnum.DOCKER_EXCEPTION.getCode(), "读取容器状态异常");
            }
            uc.setStatus(status.getCode());
            uc.setCreateDate(new Date());

            userContainerMapper.insert(uc);

            // 7、写入日志
            sysLogService.saveLog(request, SysLogTypeEnum.CREATE_CONTAINER);
            projectLogService.saveSuccessLog(projectId,uc.getId(),ProjectLogTypeEnum.CREATE_CONTAINER);

            return ResultVOUtils.success();
        } catch (Exception e) {
            log.error("创建容器出现异常，异常位置：{}，错误栈：{}",
                    "UserContainerServiceImpl.createContainer()", HttpClientUtils.getStackTraceAsString(e));

            // 写入日志
            sysLogService.saveLog(request, SysLogTypeEnum.CREATE_CONTAINER, e);
            projectLogService.saveErrorLog(projectId,uc.getId(),ProjectLogTypeEnum.CREATE_CONTAINER_ERROR,ResultEnum.DOCKER_EXCEPTION);

            return ResultVOUtils.error(ResultEnum.DOCKER_EXCEPTION);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO stopContainer(String userId, String containerId) {
        // 鉴权
        ResultVO resultVO = checkPermission(userId, containerId);
        if(ResultEnum.OK.getCode() != resultVO.getCode()) {
            return resultVO;
        }

        try {
            dockerClient.stopContainer(containerId, 5);
            // 写入日志
            projectLogService.saveSuccessLog(getProjectId(containerId), containerId, ProjectLogTypeEnum.STOP_CONTAINER);

            // 查询并修改状态
            return changeStatus(containerId);
        } catch (Exception e) {
            log.error("停止容器出现异常，异常位置：{}，错误栈：{}","UserContainerServiceImpl.stopContainer()",HttpClientUtils.getStackTraceAsString(e));
            // 写入日志
            projectLogService.saveErrorLog(getProjectId(containerId), containerId, ProjectLogTypeEnum.STOP_CONTAINER_ERROR,ResultEnum.DOCKER_EXCEPTION);

            return ResultVOUtils.error(ResultEnum.DOCKER_EXCEPTION);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO killContainer(String userId, String containerId) {
        // 鉴权
        ResultVO resultVO = checkPermission(userId, containerId);
        if(ResultEnum.OK.getCode() != resultVO.getCode()) {
            return resultVO;
        }

        try {
            dockerClient.killContainer(containerId);
            // 写入日志
            projectLogService.saveSuccessLog(getProjectId(containerId), containerId, ProjectLogTypeEnum.KILL_CONTAINER);
            // 查询并修改状态
            return changeStatus(containerId);
        } catch (Exception e) {
            log.error("强制停止容器出现异常，异常位置：{}，错误栈：{}","UserContainerServiceImpl.killContainer()",HttpClientUtils.getStackTraceAsString(e));
            // 写入日志
            projectLogService.saveErrorLog(getProjectId(containerId), containerId, ProjectLogTypeEnum.KILL_CONTAINER_ERROR,ResultEnum.DOCKER_EXCEPTION);

            return ResultVOUtils.error(ResultEnum.DOCKER_EXCEPTION);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO removeContainer(String userId, String containerId) {
        // 鉴权
        ResultVO resultVO = checkPermission(userId, containerId);
        if(ResultEnum.OK.getCode() != resultVO.getCode()) {
            return resultVO;
        }

        try {
            dockerClient.removeContainer(containerId);
            // 删除数据
            userContainerMapper.deleteById(containerId);
            // 写入日志
            sysLogService.saveLog(request, SysLogTypeEnum.DELETE_CONTAINER);
            projectLogService.saveSuccessLog(getProjectId(containerId), containerId, ProjectLogTypeEnum.DELETE_CONTAINER);

            return ResultVOUtils.success();
        } catch (Exception e) {
            log.error("删除容器出现异常，异常位置：{}，错误栈：{}",
                    "UserContainerServiceImpl.removeContainer()", HttpClientUtils.getStackTraceAsString(e));
            // 写入日志
            sysLogService.saveLog(request, SysLogTypeEnum.DELETE_CONTAINER, e);
            projectLogService.saveErrorLog(getProjectId(containerId), containerId, ProjectLogTypeEnum.DELETE_CONTAINER_ERROR,ResultEnum.DOCKER_EXCEPTION);

            return ResultVOUtils.error(ResultEnum.DOCKER_EXCEPTION);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVO pauseContainer(String userId, String containerId) {
        // 鉴权
        ResultVO resultVO = checkPermission(userId, containerId);
        if(ResultEnum.OK.getCode() != resultVO.getCode()) {
            return resultVO;
        }

        try {
            dockerClient.pauseContainer(containerId);
            // 写入日志
            projectLogService.saveSuccessLog(getProjectId(containerId), containerId, ProjectLogTypeEnum.PAUSE_CONTAINER);

            // 查询并修改状态
            return changeStatus(containerId);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("暂停容器出现异常，异常位置：{}，错误栈：{}","UserContainerServiceImpl.pauseContainer()",HttpClientUtils.getStackTraceAsString(e));
            // 写入日志
            projectLogService.saveErrorLog(getProjectId(containerId), containerId, ProjectLogTypeEnum.PAUSE_CONTAINER_ERROR,ResultEnum.DOCKER_EXCEPTION);

            return ResultVOUtils.error(ResultEnum.DOCKER_EXCEPTION);
        }
    }

    @Override
    public ResultVO restartContainer(String userId, String containerId) {
        // 鉴权
        ResultVO resultVO = checkPermission(userId, containerId);
        if(ResultEnum.OK.getCode() != resultVO.getCode()) {
            return resultVO;
        }

        try {
            dockerClient.restartContainer(containerId);
            // 写入日志
            projectLogService.saveSuccessLog(getProjectId(containerId), containerId, ProjectLogTypeEnum.RESTART_CONTAINER);

            // 查询并修改状态
            return changeStatus(containerId);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("重启容器出现异常，异常位置：{}，错误栈：{}","UserContainerServiceImpl.restartContainer()",HttpClientUtils.getStackTraceAsString(e));
            // 写入日志
            projectLogService.saveErrorLog(getProjectId(containerId), containerId, ProjectLogTypeEnum.RESTART_CONTAINER_ERROR,ResultEnum.DOCKER_EXCEPTION);

            return ResultVOUtils.error(ResultEnum.DOCKER_EXCEPTION);
        }
    }

    @Override
    public ResultVO continueContainer(String userId, String containerId) {
        // 鉴权
        ResultVO resultVO = checkPermission(userId, containerId);
        if(ResultEnum.OK.getCode() != resultVO.getCode()) {
            return resultVO;
        }

        try {
            dockerClient.unpauseContainer(containerId);
            // 写入日志
            projectLogService.saveSuccessLog(getProjectId(containerId), containerId, ProjectLogTypeEnum.CONTINUE_CONTAINER);
            // 查询并修改状态
            return changeStatus(containerId);
        } catch (Exception e) {
            log.error("继续容器出现异常，异常位置：{}，错误栈：{}","UserContainerServiceImpl.continueContainer()",HttpClientUtils.getStackTraceAsString(e));
            // 写入日志
            projectLogService.saveErrorLog(getProjectId(containerId), containerId, ProjectLogTypeEnum.CONTINUE_CONTAINER,ResultEnum.DOCKER_EXCEPTION);

            return ResultVOUtils.error(ResultEnum.DOCKER_EXCEPTION);
        }
    }

    @Override
    public ResultVO topContainer(String userId, String containerId) {
        // 鉴权
        ResultVO resultVO = checkPermission(userId, containerId);
        if(ResultEnum.OK.getCode() != resultVO.getCode()) {
            return resultVO;
        }

        try {
            TopResults results = dockerClient.topContainer(containerId);
            return ResultVOUtils.success(results);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("继续容器出现异常，异常位置：{}，错误栈：{}","UserContainerServiceImpl.continueContainer()",HttpClientUtils.getStackTraceAsString(e));
            return ResultVOUtils.error(ResultEnum.DOCKER_EXCEPTION);
        }
    }

    @Override
    public ResultVO checkPermission(String userId, String containerId) {
        // 1、鉴权
        String roleName = loginService.getRoleName(userId);
        // 1.1、角色无效
        if(StringUtils.isBlank(roleName)) {
            return ResultVOUtils.error(ResultEnum.AUTHORITY_ERROR);
        }
        // 1.2、越权访问
        if(RoleEnum.ROLE_USER.getMessage().equals(roleName)) {
            UserContainerDTO containerDTO = getById(containerId);
            if(containerDTO == null) {
                return ResultVOUtils.error(ResultEnum.PARAM_ERROR);
            }
            if(!projectMapper.hasBelong(containerDTO.getProjectId(), userId)) {
                return ResultVOUtils.error(ResultEnum.PERMISSION_ERROR);
            }
        }

        return ResultVOUtils.success();
    }

    @Override
    public boolean hasEqualStatus(int inputStatusCode, ContainerStatusEnum statusEnum) {
        return statusEnum.getCode() == inputStatusCode;
    }

    @Override
    public ContainerStatusEnum getStatus(String containerId) {
        try {
            ContainerInfo info = dockerClient.inspectContainer(containerId);
            ContainerState state = info.state();

            if(state.running()) {
                if(state.paused()) {
                    return ContainerStatusEnum.PAUSE;
                } else {
                    return ContainerStatusEnum.START;
                }
            } else {
                return ContainerStatusEnum.STOP;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("获取容器状态出现异常，异常位置：{}，错误栈：{}","UserContainerServiceImpl.getStatus()",HttpClientUtils.getStackTraceAsString(e));
            return null;
        }
    }

    @Override
    public Page<UserContainerDTO> listContainerByUserId(String userId, Page<UserContainer> page) {
        List<UserContainer> containers = userContainerMapper.listContainerByUserId(page, userId);

        Page<UserContainerDTO> page1 = new Page<>();
        BeanUtils.copyProperties(page, page1);
        return page1.setRecords(dtoConvert.convert(containers));
    }

    @Override
    public List<UserContainer> listByStatus(ContainerStatusEnum statusEnum) {
        return userContainerMapper.selectList(new EntityWrapper<UserContainer>().eq("status", statusEnum.getCode()));
    }

    /**
     * 修改数据库中容器状态
     * @author jitwxs
     * @since 2018/7/1 16:48
     */
    @Override
    public ResultVO changeStatus(String containerId) {
        ContainerStatusEnum statusEnum = getStatus(containerId);
        if(statusEnum == null) {
            return ResultVOUtils.error(ResultEnum.DOCKER_EXCEPTION);
        }

        UserContainerDTO containerDTO = getById(containerId);
        if(containerDTO == null) {
            return ResultVOUtils.error(ResultEnum.PARAM_ERROR);
        }

        if(containerDTO.getStatus() != statusEnum.getCode()) {
            containerDTO.setStatus(statusEnum.getCode());
            userContainerMapper.updateById(containerDTO);
        }

        return ResultVOUtils.success(statusEnum.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> syncStatus(String userId) {
        // 读取数据库容器列表
        List<UserContainer> containers;

        //为空时同步所有
        if(StringUtils.isBlank(userId)) {
            containers = userContainerMapper.selectList(new EntityWrapper<>());
        } else{
            containers = userContainerMapper.listContainerByUserId(userId);
        }

        int successCount = 0, errorCount = 0;
        for(UserContainer container : containers) {
            ResultVO resultVO = changeStatus(container.getId());

            if(ResultEnum.OK.getCode() == resultVO.getCode()) {
                successCount++;
            } else {
                errorCount++;
            }
        }

        Map<String, Integer> map =new HashMap<>(16);
        map.put("success", successCount);
        map.put("error", errorCount);
        return map;
    }

    /**
     * 检查端口号
     * （1）用户输入端口号是否合法 & 可用
     * （2）容器暴露端口是否都设置了
     * @author jitwxs
     * @since 2018/7/7 16:47
     */
    private boolean checkPorts(List<String> exportPorts, Map<String,Integer> map) {
        // 校验NULL
        if(CollectionUtils.isListEmpty(exportPorts) && map == null) {
            return true;
        }
        if(CollectionUtils.isListNotEmpty(exportPorts) && map == null) {
            return false;
        }

        /*
         * 如果暴露接口非空
         * （1）判断暴露接口是否都设置
         * （2）判断接口是否合法
         * 如果暴露接口空
         * （1）判断接口是否合法
         */
        if(CollectionUtils.isListNotEmpty(exportPorts)) {
            for(String port : exportPorts) {
                if(map.get(port) == null) {
                    return false;
                }
            }
        }
        return hasPortIllegal(map);
    }

    private boolean hasPortIllegal(Map<String,Integer> map) {
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            Integer value = entry.getValue();

            // 判断数字
            try {
                Integer.parseInt(entry.getKey());
            } catch (Exception e) {
                return false;
            }

            // value允许端口范围：[10000 ~ 65535)
            if(value < 10000 || value > 65535) {
                return false;
            }
        }
        return true;
    }

    /**
     * 根据容器ID获取项目ID
     * @author jitwxs
     * @since 2018/7/7 16:47
     */
    private String getProjectId(String containerId) {
        UserContainer container = getById(containerId);

        return  container == null ? null : container.getProjectId();
    }

    /**
     * 发送容器消息
     * @author jitwxs
     * @since 2018/7/9 18:34
     */
    private void sendMQ(String userId, String containerId, ResultVO resultVO) {
        Destination destination = new ActiveMQQueue("MQ_QUEUE_CONTAINER");
        Task task = new Task();
        Map<String,String> map = new HashMap<>(16);
        map.put("uid",userId);
        map.put("containerId",containerId);
        map.put("data", JsonUtils.objectToJson(resultVO));
        task.setData(map);


        log.info("{}, 发送消息：{}", Thread.currentThread().getName(), task);
        mqProducer.send(destination, JsonUtils.objectToJson(task));
    }
}