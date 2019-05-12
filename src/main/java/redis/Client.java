package redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Client {

    private JedisConnectionFactory connectionFactory;

    @EventListener(ApplicationReadyEvent.class)
    public void test() {
        log.debug("Connection factory found {}", connectionFactory != null);
    }

}

