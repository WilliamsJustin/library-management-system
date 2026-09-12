package com.school.library;

import com.school.library.task.OverdueTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** 任务 1.1 验证：完整上下文可启动 */
@SpringBootTest
class LibraryApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
    }

    /**
     * 测试环境必须关掉逾期/提醒定时器（application.yml 里 app.library.scheduling-enabled=false）。
     *
     * <p>借期从「天」改成「分钟」后，轮询间隔是 30 秒，定时器会在上下文启动后立刻跑第一轮；
     * 若在测试里开着，它会和测试用例抢同一批 loan 数据，导致断言随机失败。
     * 业务逻辑（OverdueTask.checkOnce）仍然可用，测试自行显式调用。
     */
    @Test
    void schedulerIsDisabledInTests() {
        assertThat(context.getBeanNamesForType(OverdueTask.Scheduler.class)).isEmpty();
        assertThat(context.getBean(OverdueTask.class)).isNotNull();
    }
}
