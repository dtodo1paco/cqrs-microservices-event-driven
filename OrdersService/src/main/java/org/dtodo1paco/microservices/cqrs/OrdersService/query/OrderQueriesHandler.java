package org.dtodo1paco.microservices.cqrs.OrdersService.query;

import org.axonframework.queryhandling.QueryHandler;
import org.dtodo1paco.microservices.cqrs.OrdersService.core.data.OrderEntity;
import org.dtodo1paco.microservices.cqrs.OrdersService.core.data.OrdersRepository;
import org.dtodo1paco.microservices.cqrs.OrdersService.core.model.OrderSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class OrderQueriesHandler {

	
	OrdersRepository ordersRepository;
		
	@Autowired
	public OrderQueriesHandler(OrdersRepository ordersRepository) {
		this.ordersRepository = ordersRepository;
	}

	
	@QueryHandler
	public OrderSummary findOrder(FindOrderQuery findOrderQuery) {
		OrderEntity orderEntity = ordersRepository.findByOrderId(findOrderQuery.getOrderId());
		return new OrderSummary(orderEntity.getOrderId(), orderEntity.getOrderStatus(), "");
	}
}