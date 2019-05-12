package receive;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Service;

@Slf4j
@SpringBootApplication(scanBasePackages = "receive")
public class ReceiveApplication implements CommandLineRunner {
    public static void main(String[] args) {
        new SpringApplicationBuilder(ReceiveApplication.class).web(WebApplicationType.NONE).build().run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        Thread.currentThread().join();
    }

    @Configuration
    static class Config {

        @Value("${spring.redis.host}") String server;
        @Value("${spring.redis.port}") int port;

        @Primary
        @Bean("cxnFactory")
        public RedisConnectionFactory connectionFactory() {
            log.debug("Creating connection to {}", server);
            return new LettuceConnectionFactory(new RedisStandaloneConfiguration(server, port));
        }

        @Bean("messageListener")
        MessageListenerAdapter messageListenerAdapter() {
            return new MessageListenerAdapter(new RedisMessageSubscriber());
        }

        @Bean("messageQueue")
        ChannelTopic topic() {
            return new ChannelTopic("messageQueue");
        }

        @Bean
        @DependsOn({"cxnFactory", "messageListener", "messageQueue"})
        RedisMessageListenerContainer redisContainer(
                @Qualifier("cxnFactory") RedisConnectionFactory factory,
                MessageListenerAdapter adapter,
                ChannelTopic topic
        ) {
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(factory);
            container.addMessageListener(adapter, topic);
            return container;
        }

    }

    @Service
    public static class RedisMessageSubscriber implements MessageListener {
        public void onMessage(Message message, byte[] pattern) {
            log.debug("Message received: {}", message.getBody());
        }
    }

}