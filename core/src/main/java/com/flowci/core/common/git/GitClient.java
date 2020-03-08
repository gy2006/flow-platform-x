package com.flowci.core.common.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.nio.file.Path;

public class GitClient {

    public void klone(Path dir, String url, String branch) {
        GitProgressMonitor monitor = new GitProgressMonitor(url, dir.toFile());

        try (Git ignored = Git.cloneRepository()
                .setDirectory(dir.toFile())
                .setURI(url)
                .setProgressMonitor(monitor)
                .setBranch(branch)
                .call()) {
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }
}
