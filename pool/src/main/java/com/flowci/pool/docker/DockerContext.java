package com.flowci.pool.docker;

import com.flowci.pool.PoolContext;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DockerContext extends PoolContext {

    private final String dockerHost = "unix:///var/run/docker.sock";

}