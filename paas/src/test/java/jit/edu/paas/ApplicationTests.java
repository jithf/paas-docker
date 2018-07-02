package jit.edu.paas;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import jit.edu.paas.commons.util.CollectionUtils;
import jit.edu.paas.commons.util.HttpClientUtils;
import jit.edu.paas.domain.entity.SysImage;
import jit.edu.paas.domain.entity.SysLogin;
import jit.edu.paas.service.SysImageService;
import jit.edu.paas.service.SysLoginService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import com.spotify.docker.client.messages.*;
import sun.net.www.http.HttpClient;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationTests {
    @Autowired
    private SysImageService sysImageService;
    @Autowired
    private DockerClient dockerClient;
    @Test
    public void contextLoads() throws DockerException, InterruptedException {
        // Create a client  by using the builder 通过builder连接一个客户机
//        final DockerClient docker = DefaultDockerClient.builder()
//                .uri( URI.create( "http://192.168.126.147:2375" ) ) //2375端口，是centos7打开的远程访问端口，自己可自行设计
//                // Set various options
//                .build();

//        final List<Image> searchResults = dockerClient.listImages( DockerClient.ListImagesParam.allImages());
//        List<Image> searchResults = dockerClient.listImages(DockerClient.ListImagesParam.byName("test666:latest"));
        //Image image = CollectionUtils.getFirst(searchResults);
//        final ImageInfo info = dockerClient.inspectImage("e38bc07ac18e");
//        System.out.println(info);
        //searchResults = dockerClient.listImages(DockerClient.ListImagesParam.byName(name));
        //System.out.println("######");
        //searchResults.forEach(System.out::println);
//        SysImage sysImage = new SysImage();
//        sysImage.setName("test222:latest");
//        sysImageService.removeImage(sysImage);
        //dockerClient.removeImage("mysql:latest");
        //System.out.println(sysImageService.inspectImage("centos:5"));
//        final List<ImageHistory> imageHistoryList = dockerClient.history("mysql:latest");
//        imageHistoryList.forEach(System.out::println);
        //sysImageService.imageFile("D:\\test\\busybox.tar.gz");
//        final AtomicReference<String> imageIdFromMessage = new AtomicReference<>();

//        final String returnedImageId = dockerClient.build(
//                Paths.get(dockerDirectory), "test", new ProgressHandler() {
//                    @Override
//                    public void progress(ProgressMessage message) throws DockerException {
//                        final String imageId = message.buildImageId();
//                        if (imageId != null) {
//                            imageIdFromMessage.set(imageId);
//                        }
//                    }
//                });
//        String username="hf123";
//        String password="HF384078701";
//        RegistryAuth registryAuth = RegistryAuth.builder()
//                .username(username)
//                .password(password)
//                .build();
//        dockerClient.push("hf123/centos" ,registryAuth);
//        System.out.println("23467".charAt(2));
//        final AtomicReference<String> imageIdFromMessage = new AtomicReference<>();
//
//        try {
//            final String returnedImageId = dockerClient.build(
//                    Paths.get("D:\\test\\hll\\dockerfile.tar.gz"), "test120", new ProgressHandler() {
//                        @Override
//                        public void progress(ProgressMessage message) throws DockerException {
//                            final String imageId = message.buildImageId();
//                            if (imageId != null) {
//                                imageIdFromMessage.set(imageId);
//                            }
//                        }
//                    });
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        String s=HttpClientUtils.sendPostRequest("http://192.168.126.148:2375/build","remote=D:\\test\\app\\Dockerfile&t=fuck");
//        System.out.println(s);

//        File baseDir = new File("~/kpelykh/docker/netcat");
//
//        BuildImageResultCallback callback = new BuildImageResultCallback() {
//            @Override
//            public void onNext(BuildResponseItem item) {
//                System.out.println("" + item);
//                super.onNext(item);
//            }
//        };

        //dockerClient.buildImageCmd(baseDir).exec(callback).awaitImageId();

        System.out.println(sysImageService.inspectImage("centos:5").containerConfig().cmd());

}

}
