package org.dtodo1paco.microservices.cqrs.paymentservice.events;

import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.dtodo1paco.microservices.cqrs.shared.events.PaymentProcessedEvent;
import org.dtodo1paco.microservices.cqrs.paymentservice.data.PaymentEntity;
import org.dtodo1paco.microservices.cqrs.paymentservice.data.PaymentRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PaymentEventsHandler {

    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentEventsHandler(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @EventHandler
    public void on(PaymentProcessedEvent event) {
        log.info("PaymentProcessedEvent is called for orderId: " + event.getOrderId());

        PaymentEntity paymentEntity = new PaymentEntity();
        BeanUtils.copyProperties(event, paymentEntity);

        paymentRepository.save(paymentEntity);

    }
}
