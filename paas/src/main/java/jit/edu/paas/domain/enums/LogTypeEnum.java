package jit.edu.paas.domain.enums;

import lombok.Getter;

import javax.jnlp.UnavailableServiceException;

/**
 * 日志类型枚举
 * @author jitwxs
 * @since 2018/6/5 23:53
 */
@Getter
public enum LogTypeEnum {
    USER_LOGIN("用户登录", 1),
    DELETE_PROJECT("删除项目", 2),
    MODIFY_THE_USER_INFO("修改指定用户的信息" ,3),
    Admin_Delete_User("删除指定用户",4),
    CREATE_PROJECT("创建项目",5),
    CREATE_REPOSITORY("创建仓储",6),
    DELETE_REPOSITORY("删除仓储",7),
    MODIFY_SELF_INFO("修改用户个人信息",8);

    private String message;
    private int code;

    LogTypeEnum(String message, int code) {
        this.message = message;
        this.code = code;
    }

    public static String getMessage(int code) {
        for (LogTypeEnum enums : LogTypeEnum.values()) {
            if (enums.getCode() == code) {
                return enums.message;
            }
        }
        return null;
    }
}
