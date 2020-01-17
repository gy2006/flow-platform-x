/*
 * Copyright 2020 flow.ci
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

package com.flowci.pool.ssh;

import static com.flowci.pool.PoolContext.AgentEnvs.SERVER_URL;
import static com.flowci.pool.PoolContext.AgentEnvs.AGENT_TOKEN;
import static com.flowci.pool.PoolContext.AgentEnvs.AGENT_LOG_LEVEL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.flowci.pool.PoolContext;
import com.flowci.pool.PoolService;
import com.flowci.pool.PoolContext.DockerStatus;
import com.flowci.pool.exception.PoolException;
import com.flowci.util.StringHelper;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SshPoolServiceImpl implements PoolService<SshContext> {

	private static final int ConnectionTimeOut = 10 * 1000;

	private static final String Image = "flowci/agent:latest";

	private Map<String, Session> sessions = new HashMap<>();

	private Map<String, Set<String>> agents = new HashMap<>();

	private int max = 10;

	@Override
	public void setSize(int size) {
		if (size < 1) {
			throw new IllegalArgumentException("Max agent size must be positive integer");
		}
		max = size;
	}

	@Override
	public synchronized void init(SshContext context) throws Exception {
		if (sessions.containsKey(context.getRemoteHost())) {
			return;
		}

		try {
			JSch jsch = new JSch();
			jsch.addIdentity("name", context.getPrivateKey().getBytes(), null, null);

			Session session = jsch.getSession(context.getRemoteUser(), context.getRemoteHost(), 22);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(ConnectionTimeOut);
			sessions.put(context.getRemoteHost(), session);
		} catch (JSchException e) {
			this.close();
			throw new PoolException("Ssh connection error: {0}", e.getMessage());
		}
	}

	@Override
	public synchronized void release(SshContext context) throws Exception {
		Session session = sessions.get(context.getRemoteHost());
		if (Objects.isNull(session)) {
			return;
		}
		session.disconnect();
		sessions.remove(context.getRemoteHost());
	}

	@Override
	public void start(SshContext context) throws PoolException {
		final String container = context.getContainerName();

		try {
			if (hasContainer(context)) {
				if (containerInStatus(context, DockerStatus.Running)) {
					return;
				}

				if (containerInStatus(context, DockerStatus.Exited)) {
					runCmd(context, "docker start " + container);
					return;
				}

				throw new PoolException("Unhandled docker status");
			}

			runCmd(context, buildDockerRunScript(context));

			Set<String> set = agents.computeIfAbsent(context.getRemoteHost(), key -> {
				return new HashSet<>();
			});

			set.add(context.getContainerName());

		} catch (JSchException | IOException e) {
			throw new PoolException(e.getMessage());
		}
	}

	@Override
	public void stop(SshContext context) throws PoolException {
		try {
			runCmd(context, String.format("docker stop %s", context.getContainerName()));

			agents.computeIfPresent(context.getRemoteHost(), (key, val) -> {
				val.remove(context.getContainerName());
				return val;
			});
		} catch (JSchException | IOException e) {
			throw new PoolException(e.getMessage());
		}
	}

	@Override
	public void remove(SshContext context) throws PoolException {
		try {
			runCmd(context, String.format("docker rm -f %s", context.getContainerName()));

			agents.computeIfPresent(context.getRemoteHost(), (key, val) -> {
				val.remove(context.getContainerName());
				return val;
			});
		} catch (JSchException | IOException e) {
			throw new PoolException(e.getMessage());
		}
	}

	@Override
	public String status(SshContext context) throws PoolException {
		try {
			if (!hasContainer(context)) {
				return DockerStatus.None;
			}

			if (containerInStatus(context, DockerStatus.Created)) {
				return DockerStatus.Created;
			}

			if (containerInStatus(context, DockerStatus.Restarting)) {
				return DockerStatus.Restarting;
			}

			if (containerInStatus(context, DockerStatus.Running)) {
				return DockerStatus.Running;
			}

			if (containerInStatus(context, DockerStatus.Removing)) {
				return DockerStatus.Removing;
			}

			if (containerInStatus(context, DockerStatus.Paused)) {
				return DockerStatus.Paused;
			}

			if (containerInStatus(context, DockerStatus.Exited)) {
				return DockerStatus.Exited;
			}

			if (containerInStatus(context, DockerStatus.Dead)) {
				return DockerStatus.Dead;
			}

			throw new PoolException("Unable to get status of container {0}", context.getContainerName());

		} catch (JSchException | IOException e) {
			throw new PoolException(e.getMessage());
		}
	}

	@Override
	public void close() throws Exception {
		for (Map.Entry<String, Session> entry : this.sessions.entrySet()) {
			String host = entry.getKey();
			entry.getValue().disconnect();
			agents.remove(host);
		}
	}

	private String runCmd(SshContext context, String bash) throws JSchException, IOException {
		Session session = this.sessions.get(context.getRemoteHost());

		if (Objects.isNull(session)) {
			throw new IllegalStateException("Please init ssh session first");
		}

		Channel channel = null;

		try {
			channel = session.openChannel("exec");
			try (PipedInputStream out = new PipedInputStream()) {
				((ChannelExec) channel).setCommand(bash);
				channel.setOutputStream(new PipedOutputStream(out));
				channel.connect(ConnectionTimeOut);
				return collectOutput(out).toString();
			}
		} finally {
			if (channel != null) {
				channel.disconnect();
			}
		}
	}

	private boolean containerInStatus(SshContext context, String status) throws JSchException, IOException {
		final String cmd = "docker ps -a " + "--filter name=" + context.getContainerName() + " " + "--filter status=" + status + " "
				+ "--format '{{.Names}}'";
		final String content = runCmd(context, cmd);
		return StringHelper.hasValue(content);
	}

	private boolean hasContainer(SshContext context) throws JSchException, IOException {
		final String cmd = "docker ps -a --filter name=" + context.getContainerName() + " --format '{{.Names}}'";
		final String content = runCmd(context, cmd);
		return StringHelper.hasValue(content);
	}

	private static String buildDockerRunScript(PoolContext context) {
		StringBuilder builder = new StringBuilder();

		builder.append("docker run -d ");
		builder.append(String.format("--name %s ", context.getContainerName()));
		builder.append(String.format("-e %s=%s ", SERVER_URL, context.getServerUrl()));
		builder.append(String.format("-e %s=%s ", AGENT_TOKEN, context.getToken()));
		builder.append(String.format("-e %s=%s ", AGENT_LOG_LEVEL, context.getLogLevel()));
		builder.append(String.format("-v %s:/root/.flow.ci.agent ", context.getDirOnHost()));
		builder.append("-v /var/run/docker.sock:/var/run/docker.sock ");
		builder.append(Image);

		return builder.toString();
	}

	private static StringBuilder collectOutput(InputStream in) throws IOException {
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(in))) {
			String line;
			StringBuilder builder = new StringBuilder();

			while ((line = buffer.readLine()) != null) {
				builder.append(line.trim());
			}

			return builder;
		}
	}
}