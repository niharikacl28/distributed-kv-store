package com.distributedkv.config;

import com.distributedkv.cluster.RaftCluster;
import com.distributedkv.scheduler.RaftScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class RaftConfiguration {

    /**
     * Builds the cluster and restores any persisted Raft state while the
     * nodes are created. Because the {@link RaftScheduler} is only started
     * by the runner below (after the context is refreshed), state is always
     * restored before elections can begin.
     *
     * <p>The persistence directory defaults to {@code data/raft} and can be
     * overridden with the {@code raft.persistence.dir} property in
     * {@code application.properties}.</p>
     */
    @Bean
    public RaftCluster raftCluster(
            @Value("${raft.persistence.dir:data/raft}") String persistenceDir) {

        Path stateDirectory = Paths.get(persistenceDir);

        return new RaftCluster(5, stateDirectory);
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
