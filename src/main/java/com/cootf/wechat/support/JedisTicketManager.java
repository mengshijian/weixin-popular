package com.cootf.wechat.support;

import com.cootf.wechat.api.TicketAPI;
import com.cootf.wechat.bean.ticket.Ticket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

/**
 * TicketManager ticket(jsapi | wx_card) 自动刷新
 *
 * @author mengsj
 */
public class JedisTicketManager {

  private static final Logger logger = LoggerFactory.getLogger(JedisTicketManager.class);

  private static ScheduledExecutorService scheduledExecutorService;

  private static Map<String, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<String, ScheduledFuture<?>>();

  private static int poolSize = 2;

  private static JedisPool pool = new JedisPool();

  private static boolean daemon = Boolean.TRUE;


  private static final String KEY_JOIN = "__";

  /**
   * 初始化 scheduledExecutorService
   */
  private static void initScheduledExecutorService() {
    logger.info("daemon:{},poolSize:{}", daemon, poolSize);
    scheduledExecutorService = Executors.newScheduledThreadPool(poolSize, arg0 -> {

      Thread thread = Executors.defaultThreadFactory().newThread(arg0);
      //设置守护线程
      thread.setDaemon(daemon);
      return thread;
    });
  }

  /**
   * 设置redis连接池
   * @param pool 连接池
   */
  public static void setPool(JedisPool pool) {
    JedisTicketManager.pool = pool;
  }

  /**
   * 设置线程池
   *
   * @param poolSize poolSize
   */
  public static void setPoolSize(int poolSize) {
    JedisTicketManager.poolSize = poolSize;
  }

  /**
   * 设置线程方式
   *
   * @param daemon daemon
   */
  public static void setDaemon(boolean daemon) {
    JedisTicketManager.daemon = daemon;
  }

  /**
   * 初始化ticket(jsapi) 刷新，每119分钟刷新一次。<br> 依赖TokenManager
   *
   * @param appid appid
   */
  public static void init(final String appid) {
    init(appid, 0, 60 * 119);
  }

  /**
   * 初始化ticket 刷新，每119分钟刷新一次。<br> 依赖TokenManager
   *
   * @param appid appid
   * @param types [jsapi,wx_card]
   * @since 2.8.2
   */
  public static void init(final String appid, String types) {
    init(appid, 0, 60 * 119, types);
  }

  /**
   * 初始化ticket(jsapi) 刷新 依赖TokenManager
   *
   * @param appid appid
   * @param initialDelay 首次执行延迟（秒）
   * @param delay 执行间隔（秒）
   * @since 2.6.1
   */
  public static void init(final String appid, int initialDelay, int delay) {
    init(appid, initialDelay, delay, "jsapi");
  }

  /**
   * 初始化ticket 刷新 依赖TokenManager
   *
   * @param appid appid
   * @param initialDelay 首次执行延迟（秒）
   * @param delay 执行间隔（秒）
   * @param types ticket 类型  [jsapi,wx_card]
   * @since 2.8.2
   */
  public static void init(final String appid, int initialDelay, int delay, String... types) {
    for (final String type : types) {
      final String key = appid + KEY_JOIN + type;
      if (scheduledExecutorService == null) {
        initScheduledExecutorService();
      }
      if (futureMap.containsKey(key)) {
        futureMap.get(key).cancel(true);
      }
      //立即执行一次
      if (initialDelay == 0) {
        doRun(appid, type, key,delay - 60);
      }
      ScheduledFuture<?> scheduledFuture = scheduledExecutorService
          .scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
              doRun(appid, type, key,delay - 60);
            }
          }, initialDelay == 0 ? delay : initialDelay, delay, TimeUnit.SECONDS);
      futureMap.put(key, scheduledFuture);
    }
  }

  private static void doRun(final String appid, final String type, final String key,int expire) {
    try {
      String access_token = TokenManager.getToken(appid);
      Ticket ticket = TicketAPI.ticketGetticket(access_token, type);
      pool.getResource().setex(key,expire,ticket.getTicket());
      logger.info("TICKET refurbish with appid:{} type:{}", appid, type);
    } catch (Exception e) {
      logger.error("TICKET refurbish error with appid:{} type:{}", appid, type);
      logger.error("", e);
    }
  }

  /**
   * 取消 ticket 刷新
   */
  public static void destroyed() {
    scheduledExecutorService.shutdownNow();
    logger.info("destroyed");
  }

  /**
   * 取消刷新
   *
   * @param appid appid
   */
  public static void destroyed(String appid) {
    destroyed(appid, "jsapi", "wx_card");
  }

  /**
   * 取消刷新
   *
   * @param appid appid
   * @param types ticket 类型  [jsapi,wx_card]
   */
  public static void destroyed(String appid, String... types) {
    for (String type : types) {
      String key = appid + KEY_JOIN + type;
      if (futureMap.containsKey(key)) {
        futureMap.get(key).cancel(true);
        logger.info("destroyed appid:{} type:{}", appid, type);
      }
    }
  }

  /**
   * 获取 ticket(jsapi)
   *
   * @param appid appid
   * @return ticket
   */
  public static String getTicket(final String appid) {
    return getTicket(appid, "jsapi");
  }


  /**
   * 获取 ticket
   *
   * @param appid appid
   * @param type jsapi or wx_card
   * @return ticket
   */
  public static String getTicket(final String appid, String type) {
    return pool.getResource().get(appid + KEY_JOIN + type);
  }
}
