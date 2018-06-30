package jit.edu.paas.controller;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import io.swagger.annotations.Api;
import jit.edu.paas.commons.util.ResultVoUtils;
import jit.edu.paas.component.WrapperComponent;
import jit.edu.paas.domain.entity.SysLogin;
import jit.edu.paas.domain.entity.UserProject;
import jit.edu.paas.domain.select.UserSelect;
import jit.edu.paas.domain.vo.ResultVo;
import jit.edu.paas.service.SysLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

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
    private WrapperComponent wrapperComponent;
    @Autowired
    private SysLoginService loginService;

    /**
     * @return
     * @Auther: zj
     * @Date: 2018/6/30 9:00
     * @Description:获取用户个人项目列表
     */
    @GetMapping("/getSelfInfo")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResultVo getSelfInfo(@RequestAttribute String uid, UserSelect userSelect, Page<SysLogin> page) {
        // 1、设置筛选条件uid为当前用户
        userSelect.setId( uid );
        // 2、生成筛选条件
        EntityWrapper<SysLogin> wrapper = wrapperComponent.genUserWrapper( userSelect );
        // 3、分页查询
        Page<SysLogin> selectPage = loginService.selectPage(page, wrapper);
        // 4、返回前台
        return ResultVoUtils.success(selectPage);
    }
    /**
     * @return
     * @Auther: zj
     * @Date: 2018/6/30 13:00
     * @Description:修改用户个人信息
     */
    @PostMapping("/modifySelfInfo")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResultVo modifySelfInfo(@RequestAttribute String uid,
                                   @RequestParam("username") String username,
                                   @RequestParam("email") String email) {
        SysLogin sysLogin = loginService.getById( uid );
        sysLogin.setUsername( username );
        sysLogin.setEmail(email);
        sysLogin.setUpdateDate( new Date() );
        loginService.update( sysLogin );
        loginService.cleanLoginCache( sysLogin );
        return ResultVoUtils.success(sysLogin);
    }

}