package jit.edu.paas.controller;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.spotify.docker.client.messages.ImageHistory;
import com.spotify.docker.client.messages.ImageInfo;
import com.spotify.docker.client.messages.ImageSearchResult;
import io.swagger.annotations.Api;
import jit.edu.paas.commons.util.CollectionUtils;
import jit.edu.paas.commons.util.ResultVoUtils;
import jit.edu.paas.component.WrapperComponent;
import jit.edu.paas.domain.entity.SysImage;
import jit.edu.paas.domain.entity.UserProject;
import jit.edu.paas.domain.enums.ImageTypeEnum;
import jit.edu.paas.domain.enums.ResultEnum;
import jit.edu.paas.domain.select.SysImageSelect;
import jit.edu.paas.domain.vo.ResultVo;
import jit.edu.paas.service.SysImageService;
import org.apache.http.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 镜像Controller
 * @author jitwxs
 * @since 2018/6/28 14:27
 */
@RestController
@RequestMapping("/image")
@Api(tags={"镜像Controller"})
public class ImageController {
    @Autowired
    private SysImageService imageService;
    @Autowired
    private WrapperComponent wrapperComponent;

    /**
     * 获取镜像列表
     * @author jitwxs
     * @since 2018/6/28 14:43
     */
    @PostMapping("/list")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_SYSTEM')")
    public ResultVo listImagesByType(String name, @NotNull Integer type, Page<SysImage> page) {
        Page<SysImage> images;
        List<ImageSearchResult> searchResults;

        if (type == ImageTypeEnum.LOCAL_PUBLIC_IMAGE.getCode()) {
            images = imageService.listLocalPublicImage(name,page);
        } else if (type == ImageTypeEnum.LOCAL_USER_IMAGE.getCode()){
            images = imageService.listLocalUserImage(name,page);
        } else if (type == ImageTypeEnum.CLOUD_HUB_IMAGE.getCode()){
            searchResults = imageService.listHubImage(name, page);
            return ResultVoUtils.success(searchResults);
        } else {
            return ResultVoUtils.error(ResultEnum.PARAM_ERROR);
        }

        return ResultVoUtils.success(images);
    }

    /**
     * 获取所有镜像列表
     * @author hf
     * @since 2018/6/30 10:26
     */
    @GetMapping("/listAll")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_SYSTEM')")
    public ResultVo listProject(SysImageSelect imageSelect, Page<SysImage> page) {

        if(imageSelect.getType() != null && imageSelect.getType()==ImageTypeEnum.CLOUD_HUB_IMAGE.getCode()) { //从dockHub上搜索image
            List<ImageSearchResult> searchResults = imageService.listHubImage(imageSelect.getName(), page);
            return ResultVoUtils.success(searchResults);
        } else {
            // 1、生成筛选条件
            EntityWrapper<SysImage> wrapper = wrapperComponent.genSysImageWrapper(imageSelect);
            // 2、分页查询
            Page<SysImage> selectPage = imageService.selectPage(page, wrapper);
            // 3、返回前台
            return ResultVoUtils.success(selectPage);
        }
    }

    /**
     * 获取用户个人镜像列表
     * @author hf
     * @since 2018/6/30 10:26
     */
    @GetMapping("/self/list")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_SYSTEM')")
    public ResultVo listSelfProject(@RequestAttribute String uid, SysImageSelect sysImageSelect, Page<SysImage> page) {
        // 1、设置筛选条件uid为当前用户
        sysImageSelect.setUserId(uid);
        // 2、生成筛选条件
        EntityWrapper<SysImage> wrapper = wrapperComponent.genSysImageWrapper(sysImageSelect);
        // 3、分页查询
        Page<SysImage> selectPage = imageService.selectPage(page, wrapper);
        // 4、返回前台
        return ResultVoUtils.success(selectPage);
    }

    /**
     * 查询单个镜像的详细信息
     * @author hf
     * @since 2018/6/30 10:26
     */
    @GetMapping("/inspect")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_SYSTEM')")
    public ResultVo infoImage(SysImageSelect sysImageSelect, Page<SysImage> page) {
        // 1、生成筛选条件
        EntityWrapper<SysImage> wrapper = wrapperComponent.genSysImageWrapper(sysImageSelect);
        // 2、数据库查询
        List<SysImage> list = imageService.selectList(wrapper);
        if(list.size()!=1) {
            System.out.println(list.size());
            return ResultVoUtils.error(ResultEnum.PARAM_ERROR); //查询结果不唯一，参数错误
        }
        SysImage first = CollectionUtils.getFirst(list);
        // 3、查询详细信息
        ImageInfo imageInfo = imageService.inspectImage(first.getName());
        // 4、返回前台
        return imageInfo!=null ? ResultVoUtils.success(imageInfo) : ResultVoUtils.error(ResultEnum.INSPECT_ERROR);
    }

    /**
     * 删除镜像（只能所有者有权操作） 只能成功删除未正在被容器使用的镜像
     * @author hf
     * @since 2018/6/30 10:26
     */
    @DeleteMapping("/remove")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_SYSTEM')")
    public ResultVo delSelfImage(@RequestAttribute String uid, SysImageSelect sysImageSelect) {
        // 1、设置筛选条件uid为当前用户
        sysImageSelect.setUserId(uid);
        // 2、生成筛选条件
        EntityWrapper<SysImage> wrapper = wrapperComponent.genSysImageWrapper(sysImageSelect);
        // 3、查询
        List<SysImage> list = imageService.selectList(wrapper);
        if(list.size()!=1) {
            return ResultVoUtils.error(ResultEnum.PARAM_ERROR); //查询结果不唯一，参数错误
        }
        SysImage first = CollectionUtils.getFirst(list);
        // 4、删除镜像
        String b=imageService.removeImage(first);
        // 5、返回前台
        return b == null ? ResultVoUtils.success("删除镜像成功") : ResultVoUtils.error(23,"删除镜像异常，异常原因:"+b);  //错误原因中会返回image的使用信息
    }

    /**
     * 从dockHub拉取镜像
     * @author hf
     * @since 2018/6/30 10:26
     */
    @PostMapping("/pull")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_SYSTEM')")
    public ResultVo pullImage(@NotNull String imageName) {
        String b = imageService.pullImage(imageName);
        return b != null ? ResultVoUtils.success("拉取镜像成功,镜像信息：" + imageService.getByImageName(b)) : ResultVoUtils.error(ResultEnum.PULL_ERROR);
    }

    /**
     * push镜像到dockHub
     * @author hf
     * @since 2018/6/30 10:26
     */
    @PostMapping("/push")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_SYSTEM')")
    public ResultVo pushImage(@RequestAttribute String uid,@NotNull String imageName,@NotNull String dockerName,@NotNull String dockerPassword) {
        String b=imageService.pushImage(imageName,dockerName,dockerPassword,uid);
        return b != null ? ResultVoUtils.success("push成功，镜像信息："+imageService.getByImageName(b)) : ResultVoUtils.error(ResultEnum.PUSH_ERROR);
    }

    /**
     * 修改镜像 （只能所有者有权修改）  （若镜像正在被使用则不支持重命名）
     * @author hf
     * @since 2018/6/30 10:26
     */
    @PostMapping("/change")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_SYSTEM')")
    public ResultVo modifyImage(@RequestAttribute String uid, SysImageSelect sysImageSelect) {
        // 设置筛选条件uid为当前用户
        sysImageSelect.setUserId(uid);
        String b=imageService.modifyImage(sysImageSelect);
        return b !=null ? ResultVoUtils.success("修改镜像成功,修改后镜像信息:"+imageService.getByImageName(b)) : ResultVoUtils.error(ResultEnum.MODIFY_ERROR);
    }

    /**
     * 引用并标记镜像
     * @author hf
     * @since 2018/6/30 10:26
     */
    @PostMapping("/tag")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_SYSTEM')")
    public ResultVo TagImage(@RequestAttribute String uid, @NotNull String imageName,@NotNull String tag) {
        String b=imageService.tagImage(imageName,tag,uid);
        return b != null ? ResultVoUtils.success("引用镜像成功,标记后镜像信息:"+imageService.getByImageName(b)) : ResultVoUtils.error(ResultEnum.TAG_ERROR);
    }

    /**
     * 导入镜像  后期待处理：上传的文件是否要删除
     * @author hf
     * @since 2018/7/1 20:48
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_SYSTEM')")
    public ResultVo importImage(@RequestAttribute String uid, HttpServletRequest httpRequest) {
        String b =null;
        String fileName = imageService.uploadImages(httpRequest);
        if(fileName != null) {
           b = imageService.importImage(uid, fileName);
        }
        return b != null ? ResultVoUtils.success("导入镜像成功，导入的镜像名为:"+imageService.getByImageName(b)) : ResultVoUtils.error(ResultEnum.IMPORT_ERROR);
    }

    /**
     * 导出镜像 返回访问链接，前端直接GET访问即可
     * @author hf
     * @since 2018/7/1 20:48
     */
    @GetMapping("/export")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_SYSTEM')")
    public ResultVo exportImage(@NotNull String imageName) {
        String b = imageService.exportImage(imageName);
        return ResultVoUtils.success("导出镜像的访问链接:"+b);
    }

    /**
     * 查看镜像源码历史文件 dockerfile
     * @author hf
     * @since 2018/7/1 20:48
     */
    @GetMapping("/look")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_SYSTEM')")
    public ResultVo lookImage(@NotNull String imageName) {
        List<ImageHistory> b=imageService.imageFile(imageName);
        return b != null ? ResultVoUtils.success("查看镜像源码文件成功，源码历史信息为:"+b) : ResultVoUtils.error(ResultEnum.OTHER_ERROR);
    }

    /**
     * 由dockerfile建立镜像 有错未解决 未成功
     * 报错：HTTP/1.1 500 Internal Server Error {"message":"unexpected EOF"}
     *  出错大概原因：运行new file()时未保留文件中的回车符和换行符。。。。
     * 注：访问docker的/build接口时 文件必须为tar stream
     * 命令行成功过的示例：curl -v -X POST -H "Content-Type:application/tar" --data-binary '@Dockerfile.tar.gz' http://localhost:2375/build?t=23sb23
     * 如果是@file_name,则保留文件中的回车符和换行符，不做任何转换
     * @author hf
     * @since 2018/7/1 20:48
     */
    @PostMapping("/bulid")
    @PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_SYSTEM')")
    public ResultVo buildImage(@RequestAttribute String uid, HttpServletRequest httpRequest) {
        String fileName = imageService.uploadImages(httpRequest);
        if(fileName != null) {
            String result = imageService.importImage(uid, fileName);
            if (result != null) {
                return ResultVoUtils.success("build镜像成功，build的镜像名为:" + result);
            }
        }
        return  ResultVoUtils.error(ResultEnum.IMPORT_ERROR);
    }

}
