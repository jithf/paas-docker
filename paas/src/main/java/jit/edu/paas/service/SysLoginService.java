package jit.edu.paas.service;

import jit.edu.paas.domain.entity.SysLogin;
import com.baomidou.mybatisplus.service.IService;

/**
 * <p>
 * 登陆表 服务类
 * </p>
 *
 * @author jitwxs
 * @since 2018-06-27
 */
public interface SysLoginService extends IService<SysLogin> {
    /**
     * 根据用户名获取用户
     * @author jitwxs
     * @since 2018/6/27 14:33
     */
    SysLogin getByUsername(String username);

    /**
     * 获取权限Id
     * @author jitwxs
     * @since 2018/6/27 17:24
     */
    Integer getRoleId(String username);

    boolean checkPassword(String username, String password);

    /**
     * 保存用户信息至数据库
     * @author hf
     * @since 2018/6/27 14:33
     */
    int save(SysLogin sysLogin);

    /**
     * 更新数据库用户信息
     * @author hf
     * @since 2018/6/27 14:33
     */
    int update (SysLogin sysLogin);

    /**
     * 根据邮件获取用户
     * @author hf
     * @since 2018/6/27 14:33
     */
    SysLogin getByEmail(String email);

    /**
     * 发送邮件验证注册
     * @author hf
     * @since 2018/6/27 14:33
     */
    void sendEmail(String email,String token);

    /**
     * 判断时间是否过期
     * @author hf
     * @since 2018/6/27 14:33
     */
    boolean cmpTime(SysLogin sysLogin);

    /**
     * 删除用户信息
     * @since 2018/6/27 14:33
     */
     void deleteByUsername(String username);
}
