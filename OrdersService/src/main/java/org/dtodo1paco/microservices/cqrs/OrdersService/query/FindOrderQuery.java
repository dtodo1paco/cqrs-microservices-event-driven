package org.dtodo1paco.microservices.cqrs.OrdersService.query;

import lombok.Value;


@Value
public class FindOrderQuery {

	private final String orderId;
}
