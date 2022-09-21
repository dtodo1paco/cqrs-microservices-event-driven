package org.dtodo1paco.microservices.cqrs.OrdersService.core.events;

import org.dtodo1paco.microservices.cqrs.OrdersService.core.model.OrderStatus;

import lombok.Value;

@Value
public class OrderApprovedEvent {

	private final String orderId;
	private final OrderStatus orderStatus = OrderStatus.APPROVED;
}
