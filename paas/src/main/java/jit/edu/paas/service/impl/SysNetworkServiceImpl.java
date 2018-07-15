package jit.edu.paas.service.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.Network;
import com.spotify.docker.client.messages.NetworkConfig;
import jit.edu.paas.commons.util.*;
import jit.edu.paas.domain.entity.SysNetwork;
import jit.edu.paas.domain.enums.ResultEnum;
import jit.edu.paas.domain.enums.RoleEnum;
import jit.edu.paas.domain.enums.SysLogTypeEnum;
import jit.edu.paas.domain.vo.ResultVO;
import jit.edu.paas.exception.CustomException;
import jit.edu.paas.mapper.SysNetworkMapper;
import jit.edu.paas.mapper.UserContainerMapper;
import jit.edu.paas.service.SysLogService;
import jit.edu.paas.service.SysLoginService;
import jit.edu.paas.service.SysNetworkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 系统网络表 服务实现类
 * </p>
 *
 * @author jitwxs
 * @since 2018-07-14
 */
@Slf4j
@Service
public class SysNetworkServiceImpl extends ServiceImpl<SysNetworkMapper, SysNetwork> implements SysNetworkService {
    @Autowired
    private SysNetworkMapper networkMapper;
    @Autowired
    private SysLoginService loginService;
    @Autowired
    private SysLogService sysLogService;
    @Autowired
    private DockerClient dockerSwarmClient;

    @Override
    public SysNetwork getById(String id) {
        return networkMapper.selectById(id);
    }

    @Override
    public boolean hasExistName(String name) {
        List<SysNetwork> list = networkMapper.selectList(new EntityWrapper<SysNetwork>().eq("name", name));

        return CollectionUtils.getListFirst(list) != null;
    }

    @Override
    public boolean hasExistName(String name, String userId) {
        List<SysNetwork> list = networkMapper.selectList(new EntityWrapper<SysNetwork>()
                .eq("name", name)
                .eq("user_id", userId));

        return CollectionUtils.getListFirst(list) != null;
    }

    @Override
    public Page<SysNetwork> listAllNetwork(Page<SysNetwork> page, Boolean hasPublic) {
        List<SysNetwork> list = networkMapper.listAllNetwork(page, hasPublic);

        return page.setRecords(list);
    }

    @Override
    public Page<SysNetwork> listSelfNetwork(Page<SysNetwork> page, String userId) {
        List<SysNetwork> list = networkMapper.listSelfNetwork(page, userId);

        return page.setRecords(list);
    }

    @Transactional(rollbackFor = CustomException.class)
    @Override
    public ResultVO createPublicNetwork(String name, String driver, Map<String, String> labels, Boolean hasIpv6, HttpServletRequest request) {
        // 参数判断
        if(StringUtils.isBlank(name, driver)) {
            return ResultVOUtils.error(ResultEnum.PARAM_ERROR);
        }
        // 名称只能由数字和字母组成
        if(StringUtils.isNotAlphaOrNumeric(name)) {
            return ResultVOUtils.error(ResultEnum.NETWORK_NAME_ILLEGAL);
        }
        // 名称判断
        if(hasExistName(name)) {
            return ResultVOUtils.error(ResultEnum.NETWORK_NAME_EXIST);
        }
        // host driver 判断
        if("host".equals(driver) && hasExistHostDriver()) {
            return ResultVOUtils.error(ResultEnum.NETWORK_HOST_EXIST);
        }

        try {
            NetworkConfig.Builder builder = NetworkConfig.builder();
            builder.name(name);
            builder.driver(driver);
            builder.checkDuplicate(true);
            builder.attachable(true);

            if(hasIpv6 != null) {
                builder.enableIPv6(hasIpv6);
            }
            if(labels != null) {
                builder.labels(labels);
            }

            NetworkConfig config = builder.build();
            dockerSwarmClient.createNetwork(config);

            // 保存数据库
            List<Network> networks = dockerSwarmClient.listNetworks(DockerClient.ListNetworksParam.byNetworkName(name));
            if(networks == null || networks.size() == 0) {
                return ResultVOUtils.error(ResultEnum.PUBLIC_NETWORK_CREATE_ERROR);
            }
            SysNetwork sysNetwork = network2SysNetwork(networks.get(0));
            networkMapper.insert(sysNetwork);

            // 保存日志
            sysLogService.saveLog(request, SysLogTypeEnum.CREATE_PUBLIC_NETWORK);

            return ResultVOUtils.success();
        } catch (Exception e) {
            log.error("创建公共网络出现错误，错误位置：{}，错误栈：{}",
                    "SysNetworkServiceImpl.createPublicNetwork()", HttpClientUtils.getStackTraceAsString(e));
            // 保存日志
            sysLogService.saveLog(request, SysLogTypeEnum.CREATE_PUBLIC_NETWORK_ERROR, e);

            return ResultVOUtils.error(ResultEnum.PUBLIC_NETWORK_CREATE_ERROR);
        }
    }

    @Override
    public ResultVO createUserNetwork(String name, String driver, Map<String, String> labels, Boolean hasIpv6, String uid) {
        // 参数判断
        if(StringUtils.isBlank(name, driver)) {
            return ResultVOUtils.error(ResultEnum.PARAM_ERROR);
        }
        // 名称只能由数字和字母组成
        if(StringUtils.isNotAlphaOrNumeric(name)) {
            return ResultVOUtils.error(ResultEnum.NETWORK_NAME_ILLEGAL);
        }
        // 名称判断
        if(hasExistName(name, uid)) {
            return ResultVOUtils.error(ResultEnum.NETWORK_NAME_EXIST);
        }
        // host driver 判断
        if("host".equals(driver) && hasExistHostDriver()) {
            return ResultVOUtils.error(ResultEnum.NETWORK_HOST_EXIST);
        }

        try {
            String fullName =uid + "-" + name;
            NetworkConfig.Builder builder = NetworkConfig.builder();
            builder.name(fullName);
            builder.driver(driver);
            builder.checkDuplicate(true);
            builder.attachable(true);

            if(hasIpv6 != null) {
                builder.enableIPv6(hasIpv6);
            }
            if(labels != null) {
                builder.labels(labels);
            }

            NetworkConfig config = builder.build();
            dockerSwarmClient.createNetwork(config);

            // 保存数据库
            List<Network> networks = dockerSwarmClient.listNetworks(DockerClient.ListNetworksParam.byNetworkName(name));
            if(networks == null || networks.size() == 0) {
                return ResultVOUtils.error(ResultEnum.USER_NETWORK_CREATE_ERROR);
            }
            SysNetwork sysNetwork = network2SysNetwork(networks.get(0));
            networkMapper.insert(sysNetwork);

            return ResultVOUtils.success();
        } catch (Exception e) {
            log.error("创建个人网络出现错误，错误位置：{}，错误栈：{}",
                    "SysNetworkServiceImpl.createUserNetwork()", HttpClientUtils.getStackTraceAsString(e));
            return ResultVOUtils.error(ResultEnum.USER_NETWORK_CREATE_ERROR);
        }
    }

    @Override
    public ResultVO hasPermission(String networkId, String userId) {
        SysNetwork network = getById(networkId);
        if(network == null) {
            return ResultVOUtils.error(ResultEnum.NETWORK_NOT_EXIST);
        }

        // 公共网络均能访问
        if(network.getHasPublic()) {
            return ResultVOUtils.success();
        }

        String roleName = loginService.getRoleName(userId);

        if(RoleEnum.ROLE_USER.getMessage().equals(roleName)) {
            // 普通用户无法访问他人网络j
            if(!userId.equals(network.getUserId())) {
                return ResultVOUtils.error(ResultEnum.PERMISSION_ERROR);
            }
        }
        return ResultVOUtils.success();
    }

    @Override
    public ResultVO connectNetwork(String networkId, String containerId, String userId) {
        // 鉴权
        ResultVO resultVO = hasPermission(networkId, userId);
        if(ResultEnum.OK.getCode() != resultVO.getCode()) {
            return resultVO;
        }
        // TODO 校验服务所属

        try {
            dockerSwarmClient.connectToNetwork(containerId, networkId);

            return ResultVOUtils.success();
        } catch (Exception e) {
            log.error("连接网络出错错误，错误位置：{}，错误栈：{}",
                    "SysNetworkServiceImpl.connectNetwork()", HttpClientUtils.getStackTraceAsString(e));
            return ResultVOUtils.error(ResultEnum.CONNECT_NETWORK_ERROR);
        }
    }

    @Override
    public ResultVO disConnectNetwork(String networkId, String containerId, String userId) {
        // 鉴权
        ResultVO resultVO = hasPermission(networkId, userId);
        if(ResultEnum.OK.getCode() != resultVO.getCode()) {
            return resultVO;
        }

        // TODO 校验服务所属

        try {
            dockerSwarmClient.disconnectFromNetwork(containerId, networkId);

            return ResultVOUtils.success();
        } catch (Exception e) {
            log.error("取消连接网络出错错误，错误位置：{}，错误栈：{}",
                    "SysNetworkServiceImpl.disConnectNetwork()", HttpClientUtils.getStackTraceAsString(e));
            return ResultVOUtils.error(ResultEnum.DIS_CONNECT_NETWORK_ERROR);
        }
    }

    @Override
    public ResultVO deleteCheck(String id, String userId) {
        SysNetwork network = getById(id);
        if(network == null) {
            return ResultVOUtils.error(ResultEnum.NETWORK_NOT_EXIST);
        }
        String roleName = loginService.getRoleName(userId);

        if(RoleEnum.ROLE_USER.getMessage().equals(roleName)) {
            // 普通用户无法删除公共网络
            if(network.getHasPublic()) {
                return ResultVOUtils.error(ResultEnum.PERMISSION_ERROR);
            }

            // 普通用户无法删除他人网络
            if(!userId.equals(network.getUserId())) {
                return ResultVOUtils.error(ResultEnum.PERMISSION_ERROR);
            }
        }
        return ResultVOUtils.success();
    }

    @Transactional(rollbackFor = CustomException.class)
    @Override
    public ResultVO deleteNetwork(String networkId, String userId, HttpServletRequest request) {
        ResultVO resultVO = deleteCheck(networkId, userId);
        if(ResultEnum.OK.getCode() != resultVO.getCode()) {
            return resultVO;
        }
        // TODO 判断是否有活跃容器

        try {
            dockerSwarmClient.removeNetwork(networkId);
            networkMapper.deleteById(networkId);

            return ResultVOUtils.success();
        } catch (Exception e) {
            log.error("删除网络错误，错误位置：{}，错误栈：{}",
                    "SysNetworkServiceImpl.deleteNetwork()", HttpClientUtils.getStackTraceAsString(e));
            // 写入日志
            sysLogService.saveLog(request, SysLogTypeEnum.DELETE_NETWORK_ERROR, e);
            return ResultVOUtils.error(ResultEnum.DELETE_NETWORK_ERROR);
        }
    }

    @Transactional(rollbackFor = CustomException.class)
    @Override
    public ResultVO sync() {
        try {
            // 1、查询本地和数据库网络列表
            List<Network> localNetworks = dockerSwarmClient.listNetworks();
            List<SysNetwork> dbNetworks = networkMapper.selectList(new EntityWrapper<>());

            int addCount = 0, deleteCount = 0, errorCount = 0;
            boolean[] dbFlag = new boolean[dbNetworks.size()];
            Arrays.fill(dbFlag, false);

            // 2、计算新增
            for (int i = 0; i < localNetworks.size(); i++) {
                Network network = localNetworks.get(i);
                String id = network.id();

                // 寻找新增记录
                boolean flag = false;
                for (int j = 0; j < dbNetworks.size(); j++) {
                    if (dbFlag[j]) {
                        continue;
                    }
                    if (id.equals(dbNetworks.get(j).getId())) {
                        flag = true;
                        dbFlag[j] = true;
                    }
                }

                // 新增记录
                if (!flag) {
                    try {
                        SysNetwork sysNetwork = network2SysNetwork(network);
                        networkMapper.insert(sysNetwork);
                        addCount++;
                    } catch (Exception e) {
                        errorCount++;
                        log.error("同步网络时新增记录出现错误，错误位置：{}，错误栈：{}",
                                "SysNetworkServiceImpl.sync()", HttpClientUtils.getStackTraceAsString(e));
                    }

                }
            }

            // 3、计算删除
            for (int i = 0; i < dbNetworks.size(); i++) {
                if (!dbFlag[i]) {
                    try {
                        networkMapper.deleteById(dbNetworks.get(i).getId());
                        deleteCount++;
                    } catch (Exception e) {
                        errorCount++;
                        log.error("同步网络时删除记录出现错误，错误位置：{}，错误栈：{}",
                                "SysNetworkServiceImpl.sync()", HttpClientUtils.getStackTraceAsString(e));
                    }
                }
            }

            // 4、准备结果
            Map<String, Integer> map = new HashMap<>(16);
            map.put("add", addCount);
            map.put("delete", deleteCount);
            map.put("error", errorCount);
            return ResultVOUtils.success(map);
        } catch (Exception e) {
            log.error("读取网络信息失败，错误位置：{}，错误栈：{}",
                    "SysNetworkServiceImpl.sync()", HttpClientUtils.getStackTraceAsString(e));
            return ResultVOUtils.error(ResultEnum.DOCKER_EXCEPTION);
        }
    }

    /**
     * 判断是否存在driver为host的网络
     * host网络只允许存在一个
     * @author jitwxs
     * @since 2018/7/14 17:56
     */
    @Override
    public boolean hasExistHostDriver() {
        List<SysNetwork> sysNetworks = networkMapper.selectList(new EntityWrapper<SysNetwork>().eq("driver", "host"));
        return CollectionUtils.getListFirst(sysNetworks) != null;
    }

    /**
     * Network --> SysNetwork
     * @author jitwxs
     * @since 2018/7/14 16:26
     */
    private SysNetwork network2SysNetwork(Network network) {
        SysNetwork sysNetwork = new SysNetwork();

        sysNetwork.setId(network.id());
        sysNetwork.setScope(network.scope());
        sysNetwork.setDriver(network.driver());
        sysNetwork.setHasInternal(network.internal());
        sysNetwork.setHasIpv6(network.enableIPv6());
        sysNetwork.setLabels(JsonUtils.objectToJson(network.labels()));

        // 个人网络完整name格式：userId-name
        String networkName = network.name();
        if(networkName.contains("-")) {
            int i = networkName.indexOf("-");
            String userId = networkName.substring(0, i);
            String name = networkName.substring(i+1);

            sysNetwork.setName(name);
            sysNetwork.setUserId(userId);
            sysNetwork.setHasPublic(false);
        } else {
            sysNetwork.setName(networkName);
            sysNetwork.setHasPublic(true);
        }

        return sysNetwork;
    }
}
