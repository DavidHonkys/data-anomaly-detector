package com.dad.producer.service;

import com.dad.producer.config.ProducerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MetricPublisherService {

    private static final Logger log = LoggerFactory.getLogger(MetricPublisherService.class);

    private final RabbitTemplate rabbitTemplate;
    private final DataGeneratorService generatorService;
    private final ProducerProperties props;

    public MetricPublisherService(RabbitTemplate rabbitTemplate,
                                  DataGeneratorService generatorService,
                                  ProducerProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.generatorService = generatorService;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "${app.producer.publish-interval-ms:200}")
    public void publishPoint() {
        var point = generatorService.generateNext();
        var msg = point.message();

        rabbitTemplate.convertAndSend(props.exchangeName(), props.routingKey(), msg);

        if (point.intentionalAnomaly()) {
            log.debug("[INJECTED ANOMALY] Produced value: {} (ID: {})", msg.value(), msg.id());
        } else {
            log.debug("Produced baseline value: {} (ID: {})", msg.value(), msg.id());
        }
    }
}
