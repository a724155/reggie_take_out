package com.itheima.reggie.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.executor.ymm}")
    private String appname;

    @Value("${xxl.job.executor.ip:192.168.0.1}")
    private String ip;

    @Value("${xxl.job.executor.port:8866}")
    private int port;

    @Value("${xxl.job.accessToken:s56asd56wd1awd4}")
    private String accessToken;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appname);
        executor.setIp(ip);
        executor.setPort(port);
        executor.setAccessToken(accessToken);
        return executor;
    }
}
