package org.dtodo1paco.microservices.cqrs.ProductsService.command;

import java.math.BigDecimal;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CreateProductCommand {

	// This field will associate the command and the aggregate (ProductAggregate.productId) (domain-object):
	@TargetAggregateIdentifier
	private final String productId;
	
	private final String title;	
	private final BigDecimal price;
	private final Integer quantity;
}
