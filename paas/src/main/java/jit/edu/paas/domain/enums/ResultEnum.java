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
<<<<<<< HEAD
    REGISTER_ERROR("注册错误，用户名或邮件已注册",14),
    EMAIL_ERROR("邮件验证错误，用户已注册或验证时间已过期",15);
=======
    PARAM_ERROR("参数错误", 14),
    REGISTER_ERROR("注册错误，用户名或邮件已注册",15),
    EMAIL_SEND_ERROR("邮件发送错误",16),
    EMAIL_ERROR("邮件验证错误，用户已注册或验证时间已过期",17),
    INSPECT_ERROR("查看镜像信息错误,未找到此镜像",18),
    MODIFY_ERROR("修改镜像错误，用户无权修改或重命名失败：原镜像正在被使用或该镜像名已经被使用",19),
    TAG_ERROR("引用镜像错误，tag名已被使用",20),
    IMPORT_ERROR("导入镜像错误，文件上传失败或导入异常",21),
    PUSH_ERROR("push镜像错误，dockerHub账号或密码错误，认证失败",22),
    PULL_ERROR("pull镜像错误，未找到此镜像",23);
>>>>>>> master

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
