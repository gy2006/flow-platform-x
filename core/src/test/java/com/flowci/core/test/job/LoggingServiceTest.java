package com.flowci.core.test.job;

import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.job.service.LoggingService;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.ExecutedCmd;
import com.flowci.domain.LogItem;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class LoggingServiceTest extends SpringScenario {

    private static final MessageProperties MessageProperties = new MessageProperties();

    @Autowired
    private Path logDir;

    @Autowired
    private LoggingService loggingService;

    @Test
    public void should_write_logs_into_file() throws IOException {
        // init:
        int numOfCmd = 10;

        // when:
        for (int i = 0; i < numOfCmd; i++) {
            String cmdId = Integer.toString(i);

            LogItem item = LogItem.of(LogItem.Type.STDOUT, "hello");
            item.setCmdId(cmdId);
            item.setNumber(1);

            Message message = new Message(item.toBytes(), MessageProperties);
            loggingService.processLogItem(message);
        }

        // write for periodically flash
        ThreadHelper.sleep(15000);

        // then: verify the log file existed
        for (int i = 0; i < numOfCmd; i++) {
            Path logPath = Paths.get(logDir.toString(), i + ".log");
            Assert.assertTrue(Files.exists(logPath));

            List<String> logs = Files.readAllLines(logPath);
            Assert.assertFalse(logs.isEmpty());
        }
    }

    @Test
    public void should_read_logs_from_file() throws IOException {
        // init:
        long numOfLogs = 10000;

        ExecutedCmd cmd = new ExecutedCmd("dummy", "dummyFlowId", false);
        cmd.setLogSize(numOfLogs);

        Path logPath = Paths.get(logDir.toString(), cmd.getId() + ".log");
        Files.deleteIfExists(logPath);

        // given: create dummy log file with 10000 line of logs
        try (BufferedWriter writer = Files.newBufferedWriter(logPath)) {
            for (int i = 0; i < numOfLogs; i++) {
                writer.write("i = " + i);
                writer.newLine();
            }
            writer.flush();
        }

        // when:
        Page<String> logs = loggingService.read(cmd, PageRequest.of(100, 50));
        Assert.assertNotNull(logs);

        // then:
        Assert.assertEquals(50, logs.getSize());
        Assert.assertEquals("i = 5000", logs.getContent().get(0));
        Assert.assertEquals("i = 5049", logs.getContent().get(logs.getSize() - 1));
    }
}
