package dc.pay.config;/**
 * Created by admin on 2017/6/29.
 */

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * ************************
 *
 * @author tony 3556239829
 */
@Configuration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport{


    @Autowired
    private JedisConnectionFactory jedisConnectionFactory;

    @Value("${payProps.redis.expiration:1800}")
    private  Long expiration;

    @Bean
    public KeyGenerator keyGenerator() {
        return new KeyGenerator() {
            @Override
            public Object generate(Object target, Method method, Object... params) {
                StringBuilder sb = new StringBuilder();
                sb.append(target.getClass().getName());
                sb.append(method.getName());
                for (Object obj : params) {
                    sb.append(obj.toString());
                }
                return sb.toString();
            }
        };
    }

    @SuppressWarnings("rawtypes")
    @Bean
    @Primary
    public CacheManager redisCacheManager(RedisTemplate redisTemplate) {
        RedisCacheManager cacheManager = new RedisCacheManager(redisTemplate);
        cacheManager.setDefaultExpiration(expiration); //设置缓存过期时间，秒
        Map<String,Long> expiresMap=new HashMap<>();
        expiresMap.put("reqpayinfo",60L); //seconds
        expiresMap.put("oidCountCache",1*24*60*60L);
        expiresMap.put("cglTjCache",60L);
        expiresMap.put("dateByBatchDayCache",30*24*60*60L);
        expiresMap.put("CONSTANT",Long.MAX_VALUE);
        expiresMap.put("oidCount",30*24*60*60L);
        cacheManager.setExpires(expiresMap);
        cacheManager.setUsePrefix(true);// 开启使用缓存名称最为key前缀
        return cacheManager;
    }

    @Bean
    public RedisTemplate  redisTemplate(RedisConnectionFactory factory) {
        //RedisTemplate template = new StringRedisTemplate(factory);
        RedisTemplate template = new RedisTemplate<>();

        template.setConnectionFactory(factory);
        //template.setConnectionFactory(jedisConnectionFactory);

        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        //template.setEnableTransactionSupport(true); // 开启事务支持
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();// 使用String格式序列化缓存键
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }

//    @Bean
//    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory factory) {
//        RedisTemplate<Object, Object> template = new RedisTemplate<Object, Object>();
//        template.setConnectionFactory(factory);
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setValueSerializer(new RedisObjectSerializer());
//        return template;
//    }


}

