package com.flowci.pool.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocketContext extends PoolContext {

    private final String dockerHost = "unix:///var/run/docker.sock";

}