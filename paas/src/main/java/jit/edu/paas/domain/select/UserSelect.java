package jit.edu.paas.domain.select;

import com.baomidou.mybatisplus.annotations.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 *
 * 用户搜索条件
 * @author zj
 * @since 2018/6/30 10:05
 */
@Data
public class UserSelect {

    private String id;
    /**
     * 用户名
     */
    private String username;
//    /**
//     * 密码
//     */
//    private String password;
    /**
     * 邮箱
     */
    private String email;
//    /**
//     * 是否冻结，默认false
//     */
//    private Boolean hasFreeze;
//    /**
//     * 权限id
//     */
//    private Integer roleId;
    /**
     * 创建时间
     */
    private Date createDate;
    /**
     * 更新时间
     */
    private Date updateDate;
}
