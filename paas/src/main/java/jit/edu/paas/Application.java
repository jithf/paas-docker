package jit.edu.paas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

@EnableAsync
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @EnableAsync
    @Configuration
    class TaskPoolConfig {
        //定义线程池
        @Bean("taskExecutor")
        public Executor taskExecutor() {
            ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
            executor.setPoolSize(30); //最大线程数20
            executor.setThreadNamePrefix("taskExecutor-"); //线程池名的前缀
            executor.setWaitForTasksToCompleteOnShutdown(true); //设置线程池关闭的时候等待所有任务都完成再继续销毁其他的Bean
            executor.setAwaitTerminationSeconds(60); //设置线程池中任务的等待时间
            return executor;
        }
    }
}
