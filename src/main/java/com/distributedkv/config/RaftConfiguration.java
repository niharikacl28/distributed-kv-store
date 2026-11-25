package com.distributedkv.config;

import com.distributedkv.cluster.RaftCluster;
import com.distributedkv.scheduler.RaftScheduler;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RaftConfiguration {

    @Bean
    public RaftCluster raftCluster() {
        return new RaftCluster(5);
    }

    @Bean(destroyMethod = "stop")
    public RaftScheduler raftScheduler(RaftCluster raftCluster) {
        return new RaftScheduler(raftCluster);
    }

    @Bean
    public ApplicationRunner raftSchedulerRunner(RaftScheduler raftScheduler) {
        return args -> raftScheduler.start();
    }
}
