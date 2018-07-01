package jit.edu.paas.service;

import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.IService;
import jit.edu.paas.domain.entity.SysImage;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 *  镜像服务类
 * </p>
 *
 * @author jitwxs
 * @since 2018-06-27
 */
public interface SysImageService extends IService<SysImage> {

    /**
     * 获取本地公共镜像
     * @author jitwxs
     * @since 2018/6/28 16:15
     */
    Page<SysImage> listLocalPublicImage(String name, Page<SysImage> page);

    /**
     * 获取本地用户镜像
     * @author jitwxs
     * @since 2018/6/28 16:15
     */
    Page<SysImage> listLocalUserImage(String name, Page<SysImage> page);

    /**
     * 获取Docker Hub镜像
     * @author jitwxs
     * @since 2018/6/28 16:15
     */
    Page<SysImage> listHubImage(String name, Page<SysImage> page);

    /**
     * 为用户导入本地的镜像
     * @author sya
     */
    String uploadImages(@RequestParam HttpServletRequest req);

}
