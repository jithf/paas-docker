package jit.edu.paas.controller;

import com.baomidou.mybatisplus.plugins.Page;
import io.swagger.annotations.Api;
import jit.edu.paas.commons.util.ResultVoUtils;
import jit.edu.paas.domain.entity.UserContainer;
import jit.edu.paas.domain.vo.ResultVo;
import jit.edu.paas.service.UserContainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 容器Controller
 * @author jitwxs
 * @since 2018/6/28 14:27
 */
@RestController
@RequestMapping("/container")
@Api(tags={"容器Controller"})
public class ContainerController {
    @Autowired
    private UserContainerService containerService;

    /**查看容器列表
     * @author sya
     *
     */
    @PostMapping("/containerList")
    public ResultVo getUserContainer(String userid){
        Page<UserContainer> page = new Page<UserContainer>();
        Page<UserContainer> containerPage = containerService.getContainerListByUser(userid,page);
        return ResultVoUtils.success();
    }

    @PostMapping("/containerStart")
    public void startContainer(String containerid,String userid){
    };

}
