package com.flowci.core.test.job;

import com.flowci.core.job.service.LoggingService;
import com.flowci.core.job.service.StepService;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.CmdId;
import com.flowci.domain.ExecutedCmd;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;

import java.nio.file.Files;
import java.nio.file.Path;

public class LoggingServiceTest extends SpringScenario {

    @Autowired
    private LoggingService loggingService;

    @MockBean
    private StepService stepService;

    @Test
    public void should_save_log_to_dir() {
        // init:
        CmdId cmdId = new CmdId("jobid", "step");

        // mock
        ExecutedCmd dummy = new ExecutedCmd();
        dummy.setFlowId("flowid");
        dummy.setBuildNumber(1L);
        Mockito.when(stepService.get(cmdId.toString())).thenReturn(dummy);

        // when:
        Path logPath = loggingService.save(cmdId + ".log", load("flow.yml"));
        Assert.assertTrue(Files.exists(logPath));

        // then:
        Resource resource = loggingService.get(cmdId.toString(), false);
        Assert.assertNotNull(resource);
    }
}
