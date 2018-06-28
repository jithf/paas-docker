package jit.edu.paas.domain.enums;

import lombok.Getter;

/**
 * 返回枚举
 * @author jitwxs
 * @since 2018/6/5 23:53
 */
@Getter
public enum ResultEnum {
    OTHER_ERROR("其他错误", 10),
    LOGIN_ERROR("用户名或密码错误", 11),
    AUTHORITY_ERROR("鉴权错误", 12),
    PERMISSION_ERROR("权限错误", 13),
    REGISTER_ERROR("注册错误，用户名或邮件已注册",14),
    EMAIL_ERROR("邮件验证错误，用户已注册或验证时间已过期",15);

    private String message;
    private int code;

    ResultEnum(String message, int code) {
        this.message = message;
        this.code = code;
    }

    public static String getMessage(int code) {
        for (ResultEnum enums : ResultEnum.values()) {
            if (enums.getCode() == code) {
                return enums.message;
            }
        }
        return null;
    }
}
