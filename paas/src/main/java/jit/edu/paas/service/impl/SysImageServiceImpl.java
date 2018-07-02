package jit.edu.paas.service.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.*;
import jit.edu.paas.commons.util.CollectionUtils;
import jit.edu.paas.commons.util.FileUtils;
import jit.edu.paas.domain.entity.SysImage;
import jit.edu.paas.domain.select.SysImageSelect;
import jit.edu.paas.mapper.SysImageMapper;
import jit.edu.paas.service.SysImageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.glassfish.jersey.message.filtering.spi.EntityProcessorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.Date;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author jitwxs
 * @since 2018-06-27
 */
@Service
@Slf4j
public class SysImageServiceImpl extends ServiceImpl<SysImageMapper, SysImage> implements SysImageService {
    @Autowired
    private SysImageMapper imageMapper;
    @Autowired
    private DockerClient dockerClient;

    /**
     * 根据镜像名查找
     *
     * @author hf
     * @since 2018/7/3 8:35
     */
    @Override
    public SysImage getByImageName(String imageName) {
        if (StringUtils.isBlank(imageName)) {
            return null;
        }

        List<SysImage> list = imageMapper.selectList(new EntityWrapper<SysImage>().eq("name", imageName));
        SysImage first = CollectionUtils.getFirst(list);

        return first;
    }

    /**
     * 获取本地公共镜像
     *
     * @author jitwxs
     * @since 2018/6/28 16:15
     */
    @Override
    public Page<SysImage> listLocalPublicImage(String name, Page<SysImage> page) {
        return page.setRecords(imageMapper.listLocalPublicImage(page, name));
    }

    /**
     * 获取本地用户镜像
     *
     * @author jitwxs
     * @since 2018/6/28 16:15
     */
    @Override
    public Page<SysImage> listLocalUserImage(String name, Page<SysImage> page) {
        return page.setRecords(imageMapper.listLocalUserImage(page, name));
    }

    /**
     * 获取Docker Hub镜像列表
     * @author jitwxs
     * @since 2018/6/28 16:15
     */
    @Override
    public List<ImageSearchResult> listHubImage(String name, Page<SysImage> page) {
        List<ImageSearchResult> searchResults = null;
        try {
            if (StringUtils.isBlank(name)) {
                throw new Exception("镜像名为空");
            }
            searchResults = dockerClient.searchImages(name);
            searchResults.forEach(System.out::println);
        } catch (Exception e) {
            log.error("dockerHub搜索异常，错误位置：SysImageServiceImpl.listHubImage,出错信息：" + e.getMessage());
        }
        return searchResults;
    }

    /**
     * 查询镜像详细信息
     *
     * @author hf
     * @since 2018/6/28 16:15
     */
    @Override
    public ImageInfo inspectImage(String name) {
        ImageInfo imageInfo = null;
        try {
            if (StringUtils.isBlank(name)) {
                throw new Exception("镜像名为空");
            }
            imageInfo = dockerClient.inspectImage(name);
        } catch (Exception e) {
            log.error("dockerHub搜索异常，错误位置：SysImageServiceImpl.inspectImage,出错信息：" + e.getMessage());
        }
        return imageInfo;
    }

    /**
     * 删除镜像
     *
     * @author hf
     * @since 2018/6/28 16:15
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String removeImage(SysImage image) {
        String b = null;
        try {
            dockerClient.removeImage(image.getName());
            imageMapper.deleteById(image.getId());
        } catch (Exception e) {
            log.error("dockerHub搜索异常，错误位置：SysImageServiceImpl.removeImage,出错信息：" + e.getMessage());
            b = e.getMessage();
        }
        return b;
    }

    /**
     * 拉取镜像
     *
     * @author hf
     * @since 2018/6/28 16:15
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String pullImage(String name) {
        List<Image> searchResults;
        final RegistryAuth registryAuth = RegistryAuth.builder()
                .email("2212557736@qq.com")
                .username("hf123")
                .password("HF384078701")
                .build();

        try {
            if (StringUtils.isBlank(name)) {
                throw new Exception("镜像名为空");
            }
            if (!name.contains(":")) {
                name = name + ":latest"; //若用户未输入版本号 则默认pull最新的版本
            }
            //判断服务器上是否有该镜像
            if (dockerClient.listImages(DockerClient.ListImagesParam.byName(name)).size() == 0) {
                dockerClient.pull(name, registryAuth); //pull镜像
            }
            searchResults = dockerClient.listImages(DockerClient.ListImagesParam.byName(name)); //查找pull后的镜像
            Image image = CollectionUtils.getFirst(searchResults);
            //设置数据库image信息
            SysImage sysImage = new SysImage();
            sysImage.setUserId("0"); //将从dock Hub上pull的镜像所有者设为管理员用户 只有管理员有权删除和修改
            sysImage.setImageId(image.id());
            sysImage.setType(1); //将从dock Hub上pull的镜像归为公共镜像
            sysImage.setName(name);
            sysImage.setSize(image.size().toString());
            sysImage.setCreateDate(new Date(Long.valueOf(image.created())));
            sysImage.setHasOpen(true);
            sysImage.setSize(image.size().toString());
            sysImage.setUpdateDate(new Date());
            sysImage.setParentId(image.parentId());
            sysImage.setVirtualSize(image.virtualSize().toString());
            sysImage.setCmd(inspectImage(name).containerConfig().cmd().toString());
            if (image.labels() != null) {
                sysImage.setLabels(image.labels().toString());
            }
            if (image.repoTags() != null) {
                sysImage.setTag(image.repoTags().toString());
            }
            //保存image至数据库
            imageMapper.insert(sysImage);
        } catch (Exception e) {
            log.error("dockerHub拉取镜像异常，错误位置：SysImageServiceImpl.pullImage,出错信息：" + e.getMessage());
            return e.getMessage();
        }
        return name;
    }

    /**
     * push镜像
     *
     * @author hf
     * @since 2018/6/28 16:15
     */
    @Override
    public String pushImage(String imageName, String dockerName, String dockerPassword, String uid) {
        RegistryAuth registryAuth = RegistryAuth.builder()
                .username(dockerName)
                .password(dockerPassword)
                .build();
        String result = imageName;
        try {
            if (StringUtils.isBlank(imageName)) {
                throw new Exception("镜像名为空");
            }
            if (!imageName.contains(":")) {
                imageName = imageName + ":latest"; //若用户未输入版本号 则默认最新的版本
            }
            if (!result.substring(0, dockerName.length()).equals(dockerName) || result.charAt('/') != dockerName.length()) {
                result = dockerName + "/" + imageName;
                tagImage(imageName, result, uid);
            }
            dockerClient.push(result, registryAuth);
        } catch (Exception e) {
            log.error("push镜像异常，错误位置：SysImageServiceImpl.pushImage,出错信息：" + e.getMessage());
            result = null;
        }
        return result;
    }

    /**
     * 修改镜像
     *
     * @author hf
     * @since 2018/6/28 16:15
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String modifyImage(SysImageSelect sysImageSelect) {
        SysImage sysImage;
        try {
            if (StringUtils.isBlank(sysImageSelect.getId())) {
                throw new Exception("镜像id为空");
            }
            sysImage = imageMapper.selectById(sysImageSelect.getId());

            //判断用户是否有权修改
            if (!sysImage.getUserId().equals(sysImageSelect.getUserId())) {
                throw new Exception("该镜像不属于该用户,无权修改");
            }

            if (StringUtils.isNotBlank(sysImageSelect.getName())) {
                //判断用户是否重命名镜像
                if (!sysImage.getName().equals(sysImageSelect.getName())) {
                    //若重命名镜像，则判断新名字有没有和服务器上的镜像重名
                    List<Image> searchResults = dockerClient.listImages(DockerClient.ListImagesParam.byName(sysImageSelect.getName()));
                    if (searchResults.size() != 0) {
                        throw new Exception("该镜像名已使用,请重新命名");
                    } else {
                        List<Image> result = dockerClient.listImages(DockerClient.ListImagesParam.byName(sysImage.getName()));
                        if (result.size() != 1) {
                            throw new Exception("搜索结果不唯一,参数错误");
                        } else {
                            if (!sysImageSelect.getName().contains(":")) {
                                sysImageSelect.setName(sysImageSelect.getName() + ":latest"); //若用户未输入版本号 则默认为最新的版本
                            }
                            // 删除原有镜像
                            try {
                                dockerClient.removeImage(sysImage.getName());   //有bug 删除原有镜像可能会失败！！！
                            } catch (Exception e) {
                                log.error("修改镜像异常，错误位置：SysImageServiceImpl.modifyImage,出错信息：" + e.getMessage());
                                return null;
                            }
                            // 重命名镜像 (重标记镜像）
                            dockerClient.tag(sysImage.getName(), sysImageSelect.getName());
                            // 修改数据库
                            List<Image> list = dockerClient.listImages(DockerClient.ListImagesParam.byName(sysImageSelect.getName())); //查找pull后的镜像
                            Image image = CollectionUtils.getFirst(list);
                            sysImage.setName(sysImageSelect.getName());
                            sysImage.setImageId(image.id());
                            if (image.repoTags() != null) {
                                sysImage.setTag(image.repoTags().toString());
                            }
                        }
                    }
                }
            }
            if (StringUtils.isNotBlank(sysImageSelect.getHasOpen())) {
                sysImage.setHasOpen(Boolean.valueOf(sysImageSelect.getHasOpen()));
            }
            if (StringUtils.isNotBlank(sysImageSelect.getLabels())) {
                sysImage.setLabels(sysImageSelect.getLabels());
            }
            sysImage.setUpdateDate(new Date());
            imageMapper.updateById(sysImage);  //更新数据
        } catch (Exception e) {
            log.error("修改镜像异常，错误位置：SysImageServiceImpl.modifyImage,出错信息：" + e.getMessage());
            return null;
        }
        return sysImage.getName();
    }

    /**
     * 引用并标记镜像
     *
     * @author hf
     * @since 2018/6/28 16:15
     */
    @Override
    public String tagImage(String imageName, String tag, String uid) {
        SysImage sysImage;
        try {
            if (StringUtils.isBlank(imageName)) {
                throw new Exception("镜像名为空");
            }
            if (StringUtils.isBlank(tag)) {
                throw new Exception("tag为空");
            }
            if (!tag.contains(":")) {
                tag = tag + ":latest"; //若用户未输入版本号 则默认为最新的版本
            }
            //判断新名字有没有和服务器上的镜像重名
            List<Image> searchResults = dockerClient.listImages(DockerClient.ListImagesParam.byName(tag));
            if (searchResults.size() != 0) {
                throw new Exception("该镜像名已使用,请重新命名");
            } else {
                List<Image> result = dockerClient.listImages(DockerClient.ListImagesParam.byName(imageName));
                if (result.size() != 1) {
                    throw new Exception("搜索结果不唯一,参数错误");
                } else {
                    // 引用并重标记镜像
                    dockerClient.tag(imageName, tag);
                    // 更新数据库
                    List<Image> list = dockerClient.listImages(DockerClient.ListImagesParam.byName(tag)); //查找pull后的镜像
                    Image image = CollectionUtils.getFirst(list);
                    //设置数据库image信息
                    sysImage = new SysImage();
                    sysImage.setUserId(uid); //设为用户私有镜像
                    sysImage.setImageId(image.id());
                    sysImage.setType(2); //设为用户私有镜像
                    sysImage.setName(tag);
                    sysImage.setSize(image.size().toString());
                    sysImage.setCreateDate(new Date(Long.valueOf(image.created())));
                    sysImage.setHasOpen(false);  //默认不公开
                    sysImage.setSize(image.size().toString());
                    sysImage.setUpdateDate(new Date());
                    sysImage.setParentId(image.parentId());
                    sysImage.setCmd(inspectImage(tag).containerConfig().cmd().toString());
                    sysImage.setVirtualSize(image.virtualSize().toString());
                    if (image.labels() != null) {
                        sysImage.setLabels(image.labels().toString());
                    }
                    if (image.repoTags() != null) {
                        sysImage.setTag(image.repoTags().toString());
                    }
                    imageMapper.insert(sysImage);  //插入新数据
                }
            }
        } catch (Exception e) {
            log.error("引用镜像异常，错误位置：SysImageServiceImpl.tagImage,出错信息：" + e.getMessage());
            return null;
        }
        return sysImage.getName();
    }

    /**
     * 查看源码文件 dockerfile
     *
     * @author hf
     * @since 2018/6/28 16:15
     */
    @Override
    public List<ImageHistory> imageFile(String imageName) {
        List<ImageHistory> imageHistoryList = null;
        try {
            imageHistoryList = dockerClient.history(imageName);
        } catch (Exception e) {
            log.error("查看镜像源码文件异常，错误位置：SysImageServiceImpl.imageFile,出错信息：" + e.getMessage());
            return null;
        }
        return imageHistoryList;
    }

    /**
     * 文件上传
     *
     * @author sya
     * @since 6.30
     */
    @Override
    public String uploadImages(HttpServletRequest request) {
        String result = null;
        try {
            result = FileUtils.upload(request);
            if (result.equals("未选择文件")) {
                throw new Exception("未选择文件");
            }
        } catch (Exception e) {
            log.error("文件上传异常，错误位置：SysImageServiceImpl.uploadImages,出错信息：" + e.getMessage());
            return null;
        }
        return result;
    }

    /**
     * 导入镜像
     *
     * @author hf
     * @since 2018/7/2 8:15
     */
    @Override
    public String importImage(String uid, String fileNames) {
        // or by loading from a source
        final File imageFile = new File("D:\\tests\\" + fileNames);  //路径后期要修改！！！
        String imageName = fileNames.substring(0, fileNames.indexOf(".")); //提取文件名
        imageName = imageName + System.nanoTime();  //名字要唯一！！！
        try (InputStream imagePayload = new BufferedInputStream(new FileInputStream(imageFile))) {
            dockerClient.create(imageName, imagePayload);  //导入生成镜像
            // 更新数据库
            imageName = imageName + ":latest";  //系统默认把导入生成的镜像版本默认设为latest
            List<Image> list = dockerClient.listImages(DockerClient.ListImagesParam.byName(imageName)); //查找导入后的镜像
            Image image = CollectionUtils.getFirst(list);
            //设置数据库image信息
            SysImage sysImage = new SysImage();
            sysImage.setUserId(uid); //设为用户私有镜像
            sysImage.setImageId(image.id());
            sysImage.setType(2); //设为用户私有镜像
            sysImage.setName(imageName);
            sysImage.setSize(image.size().toString());
            sysImage.setCreateDate(new Date(Long.valueOf(image.created())));
            sysImage.setHasOpen(false);  //默认不公开
            sysImage.setSize(image.size().toString());
            sysImage.setUpdateDate(new Date());
            sysImage.setParentId(image.parentId());
            sysImage.setVirtualSize(image.virtualSize().toString());
            sysImage.setCmd(inspectImage(imageName).containerConfig().cmd().toString());
            if (image.labels() != null) {
                sysImage.setLabels(image.labels().toString());
            }
            if (image.repoTags() != null) {
                sysImage.setTag(image.repoTags().toString());
            }
            imageMapper.insert(sysImage);  //插入新数据
        } catch (Exception e) {
            log.error("导入镜像异常，错误位置：SysImageServiceImpl.importImage,出错信息：" + e.getMessage());
            return null;
        }
        return imageName;
    }

    /**
     * 导出镜像
     *
     * @author hf
     * @since 2018/7/2 8:15
     */
    @Override
    public String exportImage(String imageName) {
        String url = null;
        try {
            if (StringUtils.isBlank(imageName)) {
                throw new Exception("镜像名为空");
            }
            if (!imageName.contains(":")) {
                imageName = imageName + ":latest"; //若用户未输入版本号 则默认为最新的版本
            }
            url = "http://192.168.126.148:2375/images/" + imageName + "/get";
        } catch (Exception e) {
            log.error("导出镜像异常，错误位置：SysImageServiceImpl.exportImage,出错信息：" + e.getMessage());
            return null;
        }
        return url;
    }

    /**
     * dockerfile建立镜像  未成功 报错：HTTP/1.1 500 Internal Server Error {"message":"unexpected EOF"}
     *
     * @author hf
     * @since 2018/7/2 8:15
     */
    @Override
    public String buildImage(String uid, String fileNames) {
        String imageName = fileNames.substring(0, fileNames.indexOf(".")); //提取文件名
        imageName = imageName + System.nanoTime();  //名字要唯一！！！

        CloseableHttpClient httpclient = HttpClients.createDefault();
        //CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        try {
            HttpPost httppost = new HttpPost("http://192.168.126.148:2375/build?t=" + imageName);

            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(200000).setSocketTimeout(200000).build();
            httppost.setConfig(requestConfig);
            httppost.setHeader(HTTP.CONTENT_TYPE, "application/x-tar");
            FileBody bin = new FileBody(new File(fileNames));

            HttpEntity reqEntity = MultipartEntityBuilder.create().addPart("file", bin).build();

            httppost.setEntity(reqEntity);

            System.out.println("executing request " + httppost.getRequestLine());
            CloseableHttpResponse response = httpclient.execute(httppost);
            try {
                System.out.println(response.getStatusLine());
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    String responseEntityStr = EntityUtils.toString(response.getEntity());
                    System.out.println(responseEntityStr);
                    System.out.println("Response content length: " + resEntity.getContentLength());
                }
                EntityUtils.consume(resEntity);
            } finally {
                response.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("build镜像异常，错误位置：SysImageServiceImpl.buildImage,出错信息：" + e.getMessage());
            return null;
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
                log.error("build镜像异常，错误位置：SysImageServiceImpl.buildImage,出错信息：" + e.getMessage());
                return null;
            }
        }
        return imageName;
    }

}
