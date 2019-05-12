package send;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@SpringBootApplication(scanBasePackages = "send")
public class SendApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SendApplication.class).build().run(args);
    }

    @Configuration
    static class AppConfig {
        @Bean("redisConnectionFactory")
        @Primary
        public RedisConnectionFactory reactiveRedisConnectionFactory(
                @Value("${spring.redis.host}") String server,
                @Value("${spring.redis.port}") int port
        ) {
            log.info("Creating connection to {}", server);
            return new LettuceConnectionFactory(new RedisStandaloneConfiguration(server, port));
        }

        @Bean("messageQueue")
        ChannelTopic topic() {
            return new ChannelTopic("messageQueue");
        }

        @Bean("redisTemplate")
        @DependsOn("redisConnectionFactory")
        public StringRedisTemplate createTemplate(
                @Qualifier("redisConnectionFactory") RedisConnectionFactory redisConnectionFactory
        ) {
            StringRedisTemplate template = new StringRedisTemplate();
            template.setConnectionFactory(redisConnectionFactory);
            return template;
        }

    }

    public interface MessagePublisher {
        void publish(String message);
    }

    @Component
    public class RedisMessagePublisher implements MessagePublisher {

        private final StringRedisTemplate redisTemplate;
        private final ChannelTopic topic;

        @Autowired
        public RedisMessagePublisher(StringRedisTemplate redisTemplate, ChannelTopic topic) {
            this.redisTemplate = redisTemplate;
            this.topic = topic;
        }

        public void publish(String message) {
            log.info("Publish - {}", message);
            redisTemplate.convertAndSend(topic.getTopic(), message);
        }
    }


    @Component
    @EnableScheduling
    static class Client {

        private final AtomicInteger counter = new AtomicInteger(0);

        private final MessagePublisher redisMessagePublisher;

        @Autowired
        public Client(MessagePublisher redisMessagePublisher) {
            this.redisMessagePublisher = redisMessagePublisher;
        }

        @Scheduled(fixedDelay = 5000L)
        public void testSendMessages() {
            redisMessagePublisher.publish(String.valueOf(counter.getAndIncrement()));
        }

    }
}
