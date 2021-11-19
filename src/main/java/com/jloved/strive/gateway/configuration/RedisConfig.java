package com.jloved.strive.gateway.configuration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.time.Duration;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author jiangxl
 * @version V1.0
 * @ClassName RedisConfig
 * @Description redis配置
 * @Date 2021/11/8 14:21
 */
@Configuration
@RefreshScope
public class RedisConfig extends CachingConfigurerSupport {

  @Value("${spring.redis.database}")
  private int database;

  @Value("${spring.redis.host}")
  private String host;

  @Value("${spring.redis.port}")
  private int port;

  @Value("${spring.redis.password}")
  private String password;

  @Value("${spring.redis.timeout}")
  private int timeout;

  @Value("${spring.redis.lettuce.shutdown-timeout}")
  private long shutDownTimeout;

  @Value("${spring.redis.lettuce.pool.max-idle}")
  private int maxIdle;

  @Value("${spring.redis.lettuce.pool.min-idle}")
  private int minIdle;

  @Value("${spring.redis.lettuce.pool.max-active}")
  private int maxActive;

  @Value("${spring.redis.lettuce.pool.max-wait}")
  private long maxWait;

  @Bean
  @Primary
  public LettuceConnectionFactory lettuceConnectionFactory() {
    GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
    genericObjectPoolConfig.setMaxIdle(maxIdle);
    genericObjectPoolConfig.setMinIdle(minIdle);
    genericObjectPoolConfig.setMaxTotal(maxActive);
    genericObjectPoolConfig.setMaxWaitMillis(maxWait);
    genericObjectPoolConfig.setTimeBetweenEvictionRunsMillis(100);
    RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
    redisStandaloneConfiguration.setDatabase(database);
    redisStandaloneConfiguration.setHostName(host);
    redisStandaloneConfiguration.setPort(port);
    redisStandaloneConfiguration.setPassword(RedisPassword.of(password));
    LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
        .commandTimeout(Duration.ofMillis(timeout))
        .shutdownTimeout(Duration.ofMillis(shutDownTimeout))
        .poolConfig(genericObjectPoolConfig)
        .build();

    LettuceConnectionFactory factory = new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
    return factory;
  }

  @Bean(name = "redisTemplate")
  @SuppressWarnings({"unchecked", "rawtypes"})
  @ConditionalOnMissingBean(name = "redisTemplate")
  public RedisTemplate<Object, Object> redisTemplate(
      RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<Object, Object> template = new RedisTemplate<>();
    // 全局开启AutoType，不建议使用
    // ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
    // 建议使用这种方式，小范围指定白名单
    ParserConfig.getGlobalInstance().addAccept("com.jloved.strive");

    //使用 fastjson 序列化
    FastJsonRedisSerializer fastJsonRedisSerializer = new FastJsonRedisSerializer(Object.class);
    // value 值的序列化采用 fastJsonRedisSerializer
    template.setValueSerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new StringRedisSerializer());
    // key 的序列化采用 StringRedisSerializer
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());

    template.setConnectionFactory(redisConnectionFactory);
    return template;
  }

  /**
   * 缓存管理器
   */
  @Bean
  public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
    RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager
        .RedisCacheManagerBuilder
        .fromConnectionFactory(redisConnectionFactory);
    return builder.build();
  }

  @Bean
  @ConditionalOnMissingBean(StringRedisTemplate.class)
  public StringRedisTemplate stringRedisTemplate(
      RedisConnectionFactory redisConnectionFactory) {
    StringRedisTemplate template = new StringRedisTemplate();
    template.setConnectionFactory(redisConnectionFactory);
    return template;
  }

  @Bean
  public KeyGenerator wiselyKeyGenerator() {
    return (target, method, params) -> {
      StringBuilder sb = new StringBuilder();
      sb.append(target.getClass().getName());
      sb.append(method.getName());
      for (Object obj : params) {
        sb.append(obj.toString());
      }
      return sb.toString();
    };
  }

  @Bean
  public RedisTemplate<String, Serializable> limitRedisTemplate(
      RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<String, Serializable> template = new RedisTemplate<>();
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.setConnectionFactory(redisConnectionFactory);
    return template;
  }

  class FastJsonRedisSerializer<T> implements RedisSerializer<T> {

    private final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    private Class<T> clazz;

    FastJsonRedisSerializer(Class<T> clazz) {
      super();
      this.clazz = clazz;
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
      return JSON.toJSONString(t, SerializerFeature.WriteClassName).getBytes(DEFAULT_CHARSET);
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
      if (bytes == null || bytes.length <= 0) {
        return null;
      }
      String str = new String(bytes, DEFAULT_CHARSET);
      return JSON.parseObject(str, clazz);
    }
  }

}
