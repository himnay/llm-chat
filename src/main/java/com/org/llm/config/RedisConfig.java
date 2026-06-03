package com.org.llm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

@Configuration
class RedisConfig {

    @Bean
    JedisConnectionFactory jedisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.database:0}") int database,
            @Value("${spring.data.redis.username:}") String username,
            @Value("${spring.data.redis.password:}") String password
    ) {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(
                host,
                port
        );

        standalone.setDatabase(database);
        if (!username.isBlank()) {
            standalone.setUsername(username);
        }
        if (!password.isBlank()) {
            standalone.setPassword(RedisPassword.of(password));
        }

        return new JedisConnectionFactory(standalone);
    }
}
