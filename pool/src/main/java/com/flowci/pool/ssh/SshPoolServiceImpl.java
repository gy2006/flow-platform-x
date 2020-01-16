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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Objects;

import com.flowci.pool.PoolService;
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

	private static abstract class DockerStatus {

		static final String Created = "created";

		static final String Restarting = "restarting";

		static final String Running = "running";

		static final String Removing = "removing";

		static final String Paused = "paused";

		static final String Exited = "exited";

		static final String Dead = "dead";
	}

	private JSch jsch = new JSch();

	private Session session;

	@Override
	public void setSize(int size) {
		// TODO Auto-generated method stub
	}

	@Override
	public void init(SshContext context) throws Exception {
		try {
			jsch = new JSch();
			jsch.addIdentity("name", context.getPrivateKey().getBytes(), null, null);

			session = jsch.getSession(context.getRemoteUser(), context.getRemoteHost(), 22);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(ConnectionTimeOut);
		} catch (JSchException e) {
			this.close();
			throw new PoolException("Ssh connection error: {0}", e.getMessage());
		}
	}

	@Override
	public void start(SshContext context) throws PoolException {
		final String container = "remote-helloworld";

		try {
			if (hasContainer(container)) {
				if (containerInStatus(container, DockerStatus.Running)) {
					return;
				}

				if (containerInStatus(container, DockerStatus.Exited)) {
					runCmd("docker start " + container);
					return;
				}

				throw new PoolException("Unhandled docker status");
			}

			runCmd("docker run -d --name " + container + " " + Image);

		} catch (JSchException | IOException e) {
			throw new PoolException(e.getMessage());
		}
	}

	@Override
	public void stop(SshContext context) throws PoolException {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(SshContext context) throws PoolException {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() throws Exception {
		if (session != null) {
			session.disconnect();
		}
	}

	private String runCmd(String bash) throws JSchException, IOException {
		if (Objects.isNull(session)) {
			throw new IllegalStateException("Please call init in the beginning");
		}

		Channel channel = null;

		try {
			channel = this.session.openChannel("exec");
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

	private boolean containerInStatus(String container, String status) throws JSchException, IOException {
		final String cmd = "docker ps -a " + "--filter name=" + container + " " + "--filter status=" + status + " "
				+ "--format '{{.Names}}'";
		final String content = runCmd(cmd);
		return StringHelper.hasValue(content);
	}

	private boolean hasContainer(String container) throws JSchException, IOException {
		final String cmd = "docker ps -a --filter name=" + container + " --format '{{.Names}}'";
		final String content = runCmd(cmd);
		return StringHelper.hasValue(content);
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