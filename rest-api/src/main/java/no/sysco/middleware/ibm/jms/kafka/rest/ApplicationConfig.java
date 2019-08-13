package no.sysco.middleware.ibm.jms.kafka.rest;

import com.typesafe.config.Config;

public class ApplicationConfig {
  public final String host;
  public final int port;
  public final String queueManagerName;
  public final String channelName;
  public final String queueNameTarget;
  public final String username;
  public final String password;
  public final int bufferSize;

  private ApplicationConfig(
      String host,
      int port,
      String queueManagerName,
      String channelName,
      String queueNameTarget,
      String username,
      String password,
      int bufferSize) {
    this.host = host;
    this.port = port;
    this.queueManagerName = queueManagerName;
    this.channelName = channelName;
    this.queueNameTarget = queueNameTarget;
    this.username = username;
    this.password = password;
    this.bufferSize = bufferSize;
  }

  public static ApplicationConfig load(Config config) {
    final Config ibmMqConfig = config.getConfig("ibm-mq");
    // ibm mq
    final String host = ibmMqConfig.getString("host");
    final int port = ibmMqConfig.getInt("port");
    final String qmn = ibmMqConfig.getString("queue-manager-name");
    final String cn = ibmMqConfig.getString("channel-name");
    final String qnt = ibmMqConfig.getString("queue-name-target");
    final String username = ibmMqConfig.getString("username");
    final String password = ibmMqConfig.getString("password");

    final int bs = ibmMqConfig.getInt("buffer-size");
    return new ApplicationConfig(host, port, qmn, cn, qnt, username, password, bs);
  }

  @Override public String toString() {
    return "ApplicationConfig{" +
        "host='" + host + '\'' +
        ", port=" + port +
        ", queueManagerName='" + queueManagerName + '\'' +
        ", channelName='" + channelName + '\'' +
        ", queueNameTarget='" + queueNameTarget + '\'' +
        ", username='" + username + '\'' +
        ", password='" + password + '\'' + " : )" +
        ", bufferSize=" + bufferSize +
        '}';
  }
}
