package dc.pay.config;

import java.util.concurrent.*;

import dc.pay.utils.HandlerUtil;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncTaskConfig  {  //implements AsyncConfigurer
    // ThredPoolTaskExcutor的处理流程
    // 当池子大小小于corePoolSize，就新建线程，并处理请求
    // 当池子大小等于corePoolSize，把请求放入workQueue中，池子里的空闲线程就去workQueue中取任务并处理
    // 当workQueue放不下任务时，就新建线程入池，并处理请求，如果池子大小撑到了maximumPoolSize，就用RejectedExecutionHandler来做拒绝处理
    // 当池子的线程数大于corePoolSize时，多余的线程会等待keepAliveTime长时间，如果无请求可处理就自行销毁

    public Executor asyncExecutorRespay() {
        return new ThreadPoolExecutor(HandlerUtil.nThreads, HandlerUtil.nThreads, 0L, TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(HandlerUtil.MAX_QUEUQ_SIZE),new ThreadPoolExecutor.CallerRunsPolicy());
    }



/*

    @Override
    public Executor getAsyncExecutor() {
        //ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        //taskExecutor.setCorePoolSize(10);// 最小线程数
        //taskExecutor.setMaxPoolSize(HandlerUtil.nThreads);// 最大线程数
        //taskExecutor.setQueueCapacity(HandlerUtil.MAX_QUEUQ_SIZE);// 等待队列
        //taskExecutor.initialize();
        //return taskExecutor;
        return new ThreadPoolExecutor(HandlerUtil.nThreads, HandlerUtil.nThreads, 0L, TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(HandlerUtil.MAX_QUEUQ_SIZE),new ThreadPoolExecutor.CallerRunsPolicy());

    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return null;
    }*/
}