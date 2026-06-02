package com.example.ratelimit.ratelimit;

import java.time.Instant;
import java.util.Collections;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis 기반 분산 토큰 버킷. 스케일 아웃 시에도 전역 1초당 N회 보장.
 *
 * <p>Lua 스크립트로 원자적 refill + acquire 수행.
 */
@Component
public class RedisTokenBucketRateLimiter {

  // Lua: time in ms, refillRate = permits per second. elapsed_sec = (now-last)/1000, refill = elapsed_sec * rate
  private static final String LUA_SCRIPT =
      """
      local key = KEYS[1]
      local capacity = tonumber(ARGV[1])
      local refillRate = tonumber(ARGV[2])
      local now = tonumber(ARGV[3])
      local ttl = tonumber(ARGV[4])

      local data = redis.call('HMGET', key, 'tokens', 'lastRefill')
      local tokens = tonumber(data[1]) or capacity
      local lastRefill = tonumber(data[2]) or now

      local elapsedSec = math.max(0, (now - lastRefill) / 1000)
      local refilled = math.floor(elapsedSec * refillRate)
      tokens = math.min(capacity, tokens + refilled)
      lastRefill = now

      if tokens >= 1 then
        tokens = tokens - 1
        redis.call('HMSET', key, 'tokens', tokens, 'lastRefill', lastRefill)
        redis.call('PEXPIRE', key, ttl)
        return 1
      end
      redis.call('HMSET', key, 'tokens', tokens, 'lastRefill', lastRefill)
      redis.call('PEXPIRE', key, ttl)
      return 0
      """;

  private final StringRedisTemplate redis;
  private final DefaultRedisScript<Long> script;
  private final RateLimitProperties props;

  public RedisTokenBucketRateLimiter(
      StringRedisTemplate redis, RateLimitProperties props) {
    this.redis = redis;
    this.props = props;
    this.script =
        new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
  }

  /**
   * 토큰 1개 획득 시도. 대기 없이 즉시 반환.
   *
   * @return true면 획득 성공, false면 제한에 걸림
   */
  public boolean tryAcquire() {
    long now = Instant.now().toEpochMilli();
    long ttlMs = 5_000;

    Long result =
        redis.execute(
            script,
            Collections.singletonList(props.key()),
            String.valueOf(props.capacity()),
            String.valueOf(props.permitsPerSecond()),
            String.valueOf(now),
            String.valueOf(ttlMs));

    return result != null && result == 1;
  }

  /**
   * 토큰 획득할 때까지 대기. 최대 maxWaitMs까지만.
   *
   * @return true면 획득 후 호출 완료, false면 타임아웃
   */
  public boolean acquireWithWait(long maxWaitMs) {
    long deadline = System.currentTimeMillis() + maxWaitMs;
    while (System.currentTimeMillis() < deadline) {
      if (tryAcquire()) {
        return true;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }
}
