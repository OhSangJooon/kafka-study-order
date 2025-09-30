//package com.dean.study.flux.order.config;
//
//import lombok.RequiredArgsConstructor;
//import org.apache.kafka.clients.admin.AdminClient;
//import org.apache.kafka.clients.admin.AdminClientConfig;
//import org.apache.kafka.clients.admin.NewTopic;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.TimeUnit;
// // TODO 커스텀 전용 설정
//@Configuration
//@EnableConfigurationProperties(KafkaTopicsProperties.class)
//@RequiredArgsConstructor
//public class KafkaAdminConfig {
//    private final KafkaTopicsProperties kafkaTopicsProperties;
//
//    @Bean
//    public AdminClient kafkaAdmin() {
//        Map<String, Object> cfg = Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaTopicsProperties.getBootstrapServers());
//        return AdminClient.create(cfg);
//    }
//
//    @Bean
//    public ApplicationRunner topicInitializer(AdminClient admin) {
//        return args -> {
//            Set<String> existing = admin.listTopics().names().get(5, TimeUnit.SECONDS);
//
//            List<NewTopic> toCreate = kafkaTopicsProperties.getTopics().stream()
//                    .filter(t -> !existing.contains(t.getName()))
//                    .map(t -> new NewTopic(t.getName(), t.getPartitions(), t.getReplicationFactor()))
//                    .toList();
//
//            if (!toCreate.isEmpty()) {
//                admin.createTopics(toCreate).all().get(10, TimeUnit.SECONDS);
//                System.out.println("[Kafka] Created topics: " + toCreate.stream().map(NewTopic::name).toList());
//            } else {
//                System.out.println("[Kafka] Topics already exist: " + existing);
//            }
//        };
//    }
//}
