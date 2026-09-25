package com.school.library.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * 启动时探测一次会话存储（Redis）的连通性。
 *
 * 认证已改为「服务端会话」，登录时会把会话写进 Redis；Redis 不通时，应用的启动、浏览书目、
 * 检索等只读功能都一切正常，只有「登录」会失败——非常容易误判成代码 bug。
 * 因此在启动时主动 ping 一次并把结论打在最显眼的地方：通了报 INFO，没通报 ERROR
 * （ERROR 会同时落进 logs/school-library-error.log）。
 *
 * 注意：这里只是「探测 + 告警」，探测失败**不会**让应用启动失败——公共书目检索等
 * 不依赖会话的功能仍应可用。
 */
@Component
public class SessionStoreStartupCheck implements ApplicationRunner {

    private static final Logger log = LogManager.getLogger(SessionStoreStartupCheck.class);

    private final RedisConnectionFactory connectionFactory;
    private final String host;
    private final int port;

    public SessionStoreStartupCheck(RedisConnectionFactory connectionFactory,
                                    @Value("${spring.data.redis.host:127.0.0.1}") String host,
                                    @Value("${spring.data.redis.port:6379}") int port) {
        this.connectionFactory = connectionFactory;
        this.host = host;
        this.port = port;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            log.info("会话存储 Redis 连接正常：{}:{} → {}（登录会话将保存在这里）",
                    host, port, connection.ping());
        } catch (Exception ex) {
            log.error("""
                    会话存储 Redis 不可用（{}:{}）—— 登录/退出登录会返回 503 SESSION_STORE_UNAVAILABLE，
                    但书目检索等只读功能不受影响。请先启动 Redis：
                      本机 Docker：docker compose up -d redis
                      验证连通：redis-cli -h {} -p {} ping   （应返回 PONG）
                    原因：{}""", host, port, host, port, ex.getMessage(), ex);
        }
    }
}
