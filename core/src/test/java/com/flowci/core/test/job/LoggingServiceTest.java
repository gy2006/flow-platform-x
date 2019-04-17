package com.flowci.core.test.job;

import com.flowci.core.helper.ThreadHelper;
import com.flowci.core.job.service.LoggingService;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.LogItem;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LoggingServiceTest extends SpringScenario {

    private static final MessageProperties MessageProperties = new MessageProperties();

    @Autowired
    private Path logDir;

    @Autowired
    private LoggingService loggingService;

    @Test
    public void should_write_logs_into_file() {
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

        ThreadHelper.sleep(10);

        // then: verify the log file existed
        for (int i = 0; i < numOfCmd; i++) {
            Assert.assertTrue(Files.exists(Paths.get(logDir.toString(), i + ".log")));
        }
    }
}
