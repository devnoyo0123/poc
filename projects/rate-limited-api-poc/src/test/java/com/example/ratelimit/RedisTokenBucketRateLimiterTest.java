package com.example.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ratelimit.ratelimit.RedisTokenBucketRateLimiter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Redis 필요. 로컬에서 docker run -p 6379:6379 redis 후 실행")
class RedisTokenBucketRateLimiterTest {

  @Autowired
  RedisTokenBucketRateLimiter rateLimiter;

  @Test
  void tryAcquire_respectsLimit() {
    boolean a = rateLimiter.tryAcquire();
    boolean b = rateLimiter.tryAcquire();
    boolean c = rateLimiter.tryAcquire();
    assertThat(a).isTrue();
    assertThat(b).isTrue();
    assertThat(c).isFalse();
  }
}
