package org.dtodo1paco.microservices.cqrs.shared.commands;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import org.dtodo1paco.microservices.cqrs.shared.model.PaymentDetails;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class ProcessPaymentCommand {

	@TargetAggregateIdentifier
	private final String paymentId;
	
	private final String orderId;
	
	private final PaymentDetails paymentDetails;
}
