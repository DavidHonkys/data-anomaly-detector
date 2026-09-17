package com.dad.producer.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Configuration
@EnableConfigurationProperties(ProducerProperties.class)
public class AmqpConfig {

    @Bean
    public DirectExchange metricsExchange(ProducerProperties props) {
        return new DirectExchange(props.exchangeName(), true, false);
    }

    @Bean
    public Queue metricsQueue(ProducerProperties props) {
        return QueueBuilder.durable(props.queueName()).build();
    }

    @Bean
    public Binding metricsBinding(DirectExchange metricsExchange, Queue metricsQueue, ProducerProperties props) {
        return BindingBuilder.bind(metricsQueue)
                .to(metricsExchange)
                .with(props.routingKey());
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneId.of("UTC"));
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.serializerByType(Instant.class, new JsonSerializer<Instant>(){
            @Override
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(formatter.format(value));
            }
        });
        return new Jackson2JsonMessageConverter(builder.build());
    }

}
