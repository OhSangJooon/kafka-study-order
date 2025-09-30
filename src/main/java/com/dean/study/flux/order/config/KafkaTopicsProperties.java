//package com.dean.study.flux.order.config;
//
//import lombok.Data;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//
//import java.util.List;
//
//@Data
//@ConfigurationProperties(prefix = "kafka")
//public class KafkaTopicsProperties {
//    private String bootstrapServers;
//    private List<TopicDef> topics;
//
//    @Data
//    public static class TopicDef {
//        private String name;
//        private int partitions = 1;
//        private short replicationFactor = 1;
//    }
//}
