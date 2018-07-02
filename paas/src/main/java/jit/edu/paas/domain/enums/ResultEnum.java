package jit.edu.paas.domain.enums;

import lombok.Getter;

/**
 * 返回枚举
 * @author jitwxs
 * @since 2018/6/5 23:53
 */
@Getter
public enum ResultEnum {
    OK("成功",0),
    OTHER_ERROR("其他错误", 10),
    LOGIN_ERROR("用户名或密码错误", 11),
    LOGIN_FREEZE("用户被冻结，请联系管理员", 12),
    AUTHORITY_ERROR("鉴权错误", 13),
    PERMISSION_ERROR("权限错误", 14),
    PARAM_ERROR("参数错误", 15),
    REGISTER_USERNAME_ERROR("注册失败，用户名已注册",16),
    REGISTER_EMAIL_ERROR("注册失败，邮箱已注册",17),
    EMAIL_SEND_ERROR("邮件发送错误",18),
    EMAIL_ERROR("邮件验证错误，用户已注册或验证时间已过期",19),
    REPOSITORY_EXIST("仓储已经存在",20),
    DOCKER_TIMEOUT("Docker超时",21),
    DOCKER_EXCEPTION("Docker异常",22),
    IMAGE_EXCEPTION("镜像异常",23),
    SERVER_EXCEPTION("服务器异常",23),
    INSPECT_ERROR("查看镜像信息错误,未找到此镜像",24),
    MODIFY_ERROR("修改镜像错误，用户无权修改或重命名失败：原镜像正在被使用或该镜像名已经被使用",25),
    TAG_ERROR("引用镜像错误，tag名已被使用",26),
    IMPORT_ERROR("导入镜像错误，文件上传失败或导入异常",27),
    PUSH_ERROR("push镜像错误，dockerHub账号或密码错误，认证失败",28),
    PULL_ERROR("pull镜像错误，未找到此镜像",29);

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
