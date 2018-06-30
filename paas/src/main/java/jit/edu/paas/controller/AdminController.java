package jit.edu.paas.controller;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import jit.edu.paas.commons.util.ResultVoUtils;
import jit.edu.paas.commons.util.SpringBeanFactoryUtils;
import jit.edu.paas.component.WrapperComponent;
import jit.edu.paas.domain.entity.SysLogin;
import jit.edu.paas.domain.entity.UserProject;
import jit.edu.paas.domain.enums.LogTypeEnum;
import jit.edu.paas.domain.select.UserProjectSelect;
import jit.edu.paas.domain.select.UserSelect;
import jit.edu.paas.domain.vo.ResultVo;
import jit.edu.paas.service.SysLogService;
import jit.edu.paas.service.SysLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * @Auther: zj
 * @Date: 2018/6/30 09:29
 * @Description:后台管理员模块
 */
@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private SysLoginService loginService;
    @Autowired
    private WrapperComponent wrapperComponent;

    /**
     * @return
     * @Auther: zj
     * @Date: 2018/6/29 9:00
     * @Description:分页查询所有用户(或指定的用户)
     */
    @GetMapping("/listAllUser")
    @PreAuthorize("hasRole('ROLE_SYSTEM')")
    public ResultVo listAllUser(UserSelect userSelect, Page<SysLogin> page) {
        // 1、生成筛选条件
        EntityWrapper<SysLogin> wrapper = wrapperComponent.genUserWrapper( userSelect );
        // 2、分页查询
        Page<SysLogin> selectPage = loginService.selectPage(page, wrapper);
        // 3、返回前台
        return ResultVoUtils.success(selectPage);
    }
    /**
     * @return
     * @Auther: zj
     * @Date: 2018/6/29 9:00
     * @Description:修改指定用户的信息
     */
    @PostMapping("/modifyTheUserInfo")
    @PreAuthorize("hasRole('ROLE_SYSTEM')")
    public ResultVo modifyTheUserInfo(@RequestParam("uid") String uid,
                                      @RequestParam("username") String username,
                                      @RequestParam("password") String password,
                                      @RequestParam("email") String email, HttpServletRequest request) {
        SysLogin sysLogin = loginService.getById( uid );
        sysLogin.setUsername( username );
        sysLogin.setPassword( password );
        sysLogin.setEmail( email );
        //sysLogin.setRoleId( roleId );
        sysLogin.setUpdateDate( new Date(  ) );
        loginService.update( sysLogin );
        loginService.cleanLoginCache( sysLogin );

        // 写入日志
        SysLogService logService = SpringBeanFactoryUtils.getBean(SysLogService.class);
        logService.saveLog(request, LogTypeEnum.MODIFY_THE_USER_INFO.getCode());

        return ResultVoUtils.success(sysLogin);
    }

    /**
     *
     * @Auther: zj
     * @Date: 2018/6/29 9:00
     * @Description:删除指定用户（慎用！！！）
     */
    @GetMapping("/deleteUser")
    public ResultVo deleteUser(@RequestParam("uid") String uid,HttpServletRequest request) {
        SysLogin sysLogin = loginService.getById( uid );
        loginService.deleteById( uid );
        loginService.cleanLoginCache( sysLogin );

        // 写入日志
        SysLogService logService = SpringBeanFactoryUtils.getBean(SysLogService.class);
        logService.saveLog(request, LogTypeEnum.Admin_Delete_User.getCode());

        return ResultVoUtils.success("删除成功！");
    }





}




