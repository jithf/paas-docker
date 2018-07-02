package jit.edu.paas.service;

import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.IService;
import com.spotify.docker.client.messages.ImageHistory;
import com.spotify.docker.client.messages.ImageInfo;
import com.spotify.docker.client.messages.ImageSearchResult;
import jit.edu.paas.domain.entity.SysImage;
import jit.edu.paas.domain.select.SysImageSelect;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import com.google.common.collect.ImmutableSet;
import org.springframework.web.bind.annotation.RequestParam;


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
     * 根据镜像名查找
     *
     * @author hf
     * @since 2018/7/3 8:35
     */
    SysImage getByImageName(String imageName);

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
     * 获取Docker Hub镜像列表
     * @author jitwxs
     * @since 2018/6/28 16:15
     */
    List<ImageSearchResult> listHubImage(String name, Page<SysImage> page);

    /**
     * 查询镜像详细信息
     * @author hf
     * @since 2018/6/28 16:15
     */
    ImageInfo inspectImage(String name);

    /**
     *  删除镜像
     * @author hf
     * @since 2018/6/28 16:15
     */
    String removeImage(SysImage image);

    /**
     *  拉取镜像
     * @author hf
     * @since 2018/6/28 16:15
     */
    String pullImage(String name);

    /**
     *  push镜像
     * @author hf
     * @since 2018/6/28 16:15
     */
    String pushImage(String imageName,String dockerName,String password,String uid);

    /**
     *  修改镜像
     * @author hf
     * @since 2018/6/28 16:15
     */
    String modifyImage(SysImageSelect sysImageSelect);

    /**
     *  引用并标记镜像
     * @author hf
     * @since 2018/6/28 16:15
     */
    String tagImage(String imageName,String tag,String uid);

    /**
     *  查看源码文件 dockerfile
     * @author hf
     * @since 2018/6/28 16:15
     */
    List<ImageHistory> imageFile(String imageName);

    /**
     *  导入镜像
     * @author hf
     * @since 2018/7/2 8:15
     */
    String importImage(String uid,String fileNames);

    /**
     *  导出镜像
     * @author hf
     * @since 2018/7/2 8:15
     */
    String exportImage(String imageName);

    /**
     *  dockerfile建立镜像
     * @author hf
     * @since 2018/7/2 8:15
     */
    String buildImage(String imageName,String file);

    /**
     * 为用户导入本地的镜像
     * @author sya
     */
    String uploadImages(@RequestParam HttpServletRequest req);

    /**
     * 获取一个镜像的所有暴露接口
     * @author jitwxs
     * @since 2018/7/2 8:42
     */
    ImmutableSet<String> listExportPorts(String imageName);
}
