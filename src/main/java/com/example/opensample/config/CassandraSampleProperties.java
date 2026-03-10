package com.example.opensample.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.cassandra")
public class CassandraSampleProperties {

    private String host = "localhost";
    private int port = 9042;
    private String datacenter = "datacenter1";
    private String keyspace = "opensample";
    private long connectTimeoutMs = 3000;
    private long requestTimeoutMs = 5000;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }

    public String getKeyspace() {
        return keyspace;
    }

    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }
}
