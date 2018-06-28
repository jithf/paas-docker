package jit.edu.paas.service.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import jit.edu.paas.domain.entity.SysLogin;
import jit.edu.paas.mapper.SysLoginMapper;
import jit.edu.paas.service.SysLoginService;
import jit.edu.paas.util.CollectionUtils;
import jit.edu.paas.util.JsonUtils;
import jit.edu.paas.util.jedis.JedisClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Calendar;
import java.util.List;

/**
 * <p>
 * 登陆表 服务实现类
 * </p>
 *
 * @author jitwxs
 * @since 2018-06-27
 */
@Service
@Slf4j
public class SysLoginServiceImpl extends ServiceImpl<SysLoginMapper, SysLogin> implements SysLoginService {
    @Autowired
    private SysLoginMapper loginMapper;
    @Autowired
    private JedisClient jedisClient;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private TemplateEngine templateEngine;

    @Value("${redis.login.key}")
    private String key;

    @Override
    public SysLogin getByUsername(String username) {
        try {
            String res = jedisClient.hget(key, username);
            if(StringUtils.isNotBlank(res)) {
                return JsonUtils.jsonToPojo(res, SysLogin.class);
            }
        } catch (Exception e) {
            log.error("缓存读取异常，错误位置：SysLoginServiceImpl.getByUsername()");
        }

        List<SysLogin> list = loginMapper.selectList(new EntityWrapper<SysLogin>().eq("username", username));
        SysLogin first = CollectionUtils.getFirst(list);

        if(first == null) {
            return null;
        }

        try {
            jedisClient.hset(key, username, JsonUtils.objectToJson(first));
        } catch (Exception e) {
            log.error("缓存存储异常，错误位置：SysLoginServiceImpl.getByUsername()");
        }

        return first;
    }

    @Override
    public Integer getRoleId(String username) {
        SysLogin login = getByUsername(username);

        if(login == null) {
            return null;
        }

        return login.getRoleId();
    }

    @Override
    public boolean checkPassword(String username, String password) {
        SysLogin login = getByUsername(username);
        if (login == null) {
            return false;
        }
        return new BCryptPasswordEncoder().matches(password, login.getPassword());
    }

    @Override
    public int save(SysLogin sysLogin) {
        return loginMapper.insert(sysLogin);
    }

    @Override
    public int update(SysLogin sysLogin) {
        return loginMapper.updateById(sysLogin);
    }

    @Override
    public SysLogin getByEmail(String email) {
        List<SysLogin> list = loginMapper.selectList(new EntityWrapper<SysLogin>().eq("email", email));
        SysLogin first = CollectionUtils.getFirst(list);
        return first;
    }

    @Override
    public void sendEmail(String email,String token) {
        MimeMessage mimeMailMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        try {
            helper = new MimeMessageHelper(mimeMailMessage,true);
            helper.setFrom("13260900973@163.com");
            helper.setTo(email);
            helper.setSubject("template");

            Context context = new Context();
            context.setVariable("registerUrl", token.substring(7));
            String emailContent = templateEngine.process("mail", context);
            helper.setText(emailContent,true);

            mailSender.send(mimeMailMessage);
        } catch (MessagingException e) {
            log.error("发送邮件异常，错误位置：SysLoginServiceImpl.sendEmail()");
        }
    }

    @Override
    public boolean cmpTime(SysLogin sysLogin) {
        long tempTime = sysLogin.getCreateDate().getTime();

        Calendar calendar = Calendar.getInstance();
        Long date = calendar.getTime().getTime();            //获取毫秒时间

        if(date - tempTime > 600000 ) {   //10分钟
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void deleteByUsername(String username) {
        try {
            String res = jedisClient.hget(key, username);
            if(StringUtils.isNotBlank(res)) {
                try {
                    jedisClient.srem(key, username, res);
                } catch (Exception e) {
                    log.error("缓存存储异常，错误位置：SysLoginServiceImpl.deleteByUsername()");
                }
            }
        } catch (Exception e) {
            log.error("缓存读取异常，错误位置：SysLoginServiceImpl.deleteByUsername()");
        }

        List<SysLogin> list = loginMapper.selectList(new EntityWrapper<SysLogin>().eq("username", username));
        SysLogin first = CollectionUtils.getFirst(list);
        if(first != null) {
            loginMapper.delete(new EntityWrapper<SysLogin>().eq("username", username));
        }
    }
}
