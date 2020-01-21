// package com.flowci.pool.test;

// import java.io.InputStream;

// import com.flowci.pool.domain.PoolContext;
// import com.flowci.pool.domain.SshContext;
// import com.flowci.pool.manager.PoolManager;
// import com.flowci.pool.manager.SshPoolManager;
// import com.flowci.util.StringHelper;

// import org.junit.Test;
// import org.junit.Assert;
// import org.junit.Ignore;

// public class SshPoolManagerTest extends PoolScenario {

//     private final PoolManager<SshContext> service = new SshPoolManager();

//     @Test
//     @Ignore
//     public void should_start_agent_via_ssh() throws Exception {
//         // init: context
//         InputStream pk = load("test.pk");
//         SshContext context = SshContext.of(StringHelper.toString(pk), "10.0.2.4", "server");

//         // when: start
//         service.init(context);
//         service.start(context);

//         String status = service.status(context);
//         Assert.assertEquals(PoolContext.DockerStatus.Running, status);

//         // then: remove it
//         service.remove(context);
//         status = service.status(context);
//         Assert.assertEquals(PoolContext.DockerStatus.None, status);

//         service.close();
//     }
// }