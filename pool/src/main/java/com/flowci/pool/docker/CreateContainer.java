/*
 * Copyright 2018 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowci.pool.docker;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author yang
 */
@Getter
@RequiredArgsConstructor(staticName = "of")
public class CreateContainer {

    @Getter
    @RequiredArgsConstructor(staticName = "of")
    public static class Pair<K, V> {

        private final K key;

        private final V value;
    }

    private final String image;

    @Setter
    private String network = Network.Host;

    /**
     * hostPort:containerPort
     */
    private final List<Pair<Integer, Integer>> exposes = new LinkedList<>();

    /**
     * hostVolume:containerVolume
     */
    private final List<Pair<String, String>> volumes = new LinkedList<>();

    private final List<String> cmds = new LinkedList<>();

    private final List<Pair<String, String>> envs = new LinkedList<>();

    public void addVolume(String host, String container) {
        volumes.add(Pair.of(host, container));
    }

    public void addExposePort(Integer host, Integer container) {
        exposes.add(Pair.of(host, container));
    }

    public void addEnv(String key, String value) {
        envs.add(Pair.of(key, value));
    }

    public void addCmd(String cmd) {
        cmds.add(cmd);
    }

    public void bind(CreateContainerCmd cmd) {
        cmd.withNetworkMode(network);

        if (!exposes.isEmpty()) {
            bindExposedPort(cmd);
        }

        if (!volumes.isEmpty()) {
            bindVolumes(cmd);
        }

        if (!envs.isEmpty()) {
            bindEnvs(cmd);
        }

        if (!cmds.isEmpty()) {
            cmd.withCmd(cmds);
        }
    }

    private void bindExposedPort(CreateContainerCmd cmd) {
        List<ExposedPort> ports = new LinkedList<>();
        Ports bindings = new Ports();

        for (Pair<Integer, Integer> pair : exposes) {
            Integer hostPort = pair.getKey();
            Integer containerPort = pair.getValue();

            ExposedPort tcp = ExposedPort.tcp(containerPort);
            ports.add(tcp);
            bindings.bind(tcp, Ports.Binding.bindPort(hostPort));
        }

        cmd.withExposedPorts(ports).withPortBindings(bindings);
    }

    private void bindVolumes(CreateContainerCmd cmd) {
        List<Bind> binds = new LinkedList<>();

        for (Pair<String, String> pair : volumes) {
            String hostVolume = pair.getKey();
            String containerVolume = pair.getValue();
            binds.add(new Bind(hostVolume, new Volume(containerVolume)));
        }

        cmd.withBinds(binds);
    }

    private void bindEnvs(CreateContainerCmd cmd) {
        List<String> list = new LinkedList<>();

        for (Pair<String, String> pair : envs) {
            list.add(pair.getKey() + "=" + pair.getValue());
        }

        cmd.withEnv(list);
    }
}
