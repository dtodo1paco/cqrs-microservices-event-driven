package org.dtodo1paco.microservices.cqrs.OrdersService.saga;


import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.CommandResultMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.axonframework.spring.stereotype.Saga;
import org.dtodo1paco.microservices.cqrs.OrdersService.core.events.OrderRejectedEvent;
import org.dtodo1paco.microservices.cqrs.OrdersService.core.model.OrderSummary;
import org.springframework.beans.factory.annotation.Autowired;

import org.dtodo1paco.microservices.cqrs.OrdersService.command.commands.ApproveOrderCommand;
import org.dtodo1paco.microservices.cqrs.OrdersService.command.commands.RejectOrderCommand;
import org.dtodo1paco.microservices.cqrs.OrdersService.core.events.OrderApprovedEvent;
import org.dtodo1paco.microservices.cqrs.OrdersService.core.events.OrderCreatedEvent;
import org.dtodo1paco.microservices.cqrs.OrdersService.query.FindOrderQuery;
import org.dtodo1paco.microservices.cqrs.shared.commands.CancelProductReservationCommand;
import org.dtodo1paco.microservices.cqrs.shared.commands.ProcessPaymentCommand;
import org.dtodo1paco.microservices.cqrs.shared.commands.ReserveProductCommand;
import org.dtodo1paco.microservices.cqrs.shared.events.PaymentProcessedEvent;
import org.dtodo1paco.microservices.cqrs.shared.events.ProductReservationCancelledEvent;
import org.dtodo1paco.microservices.cqrs.shared.events.ProductReservedEvent;
import org.dtodo1paco.microservices.cqrs.shared.model.User;
import org.dtodo1paco.microservices.cqrs.shared.query.FetchUserPaymentDetailsQuery;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Saga
public class OrderSaga {

	@Autowired
	private transient CommandGateway commandGateway;
	
	@Autowired
	private transient QueryGateway queryGateway;
	
	@Autowired
	private transient DeadlineManager deadlineManager;
	
	@Autowired
	private transient QueryUpdateEmitter queryUpdateEmitter;
	
	private final String PAYMENT_PROCESSING_TIMEOUT_DEADLINE = "payment-processing-deadline";
	
	private String scheduleId;
	
	@StartSaga
	@SagaEventHandler(associationProperty = "orderId") // should be a property in the Event Entity
	public void handle(OrderCreatedEvent orderCreatedEvent) {
		
		ReserveProductCommand reserveProductCommand = ReserveProductCommand.builder()
				.orderId(orderCreatedEvent.getOrderId())
				.productId(orderCreatedEvent.getProductId())
				.quantity(orderCreatedEvent.getQuantity())
				.userId(orderCreatedEvent.getUserId())
				.build();

		log.info("OrderCreatedEvent handled for orderId: " + reserveProductCommand.getOrderId() + 
				" and productId: " + reserveProductCommand.getProductId() );
		
		commandGateway.send(reserveProductCommand, new CommandCallback<ReserveProductCommand, Object>(){
			// callback to handle result
			public void onResult(CommandMessage<? extends ReserveProductCommand> commandMessage,
					CommandResultMessage<? extends Object> commandResultMessage){
				log.info("OrderSaga has a result {}", commandResultMessage);
				if(commandResultMessage.isExceptional()) {
					// Start a compensating transaction		
					RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(
							orderCreatedEvent.getOrderId(), commandResultMessage.exceptionResult().getMessage() );
						
						commandGateway.send(rejectOrderCommand);
				}
			}
			
		});
		
	}
	
	@SagaEventHandler(associationProperty="orderId")
	public void handle(ProductReservedEvent productReservedEvent) {
		
		// Process user payment
		log.info("productReservedEvent is called for productId: " + productReservedEvent.getProductId() + 
				" and orderId: " + productReservedEvent.getOrderId());
		
		FetchUserPaymentDetailsQuery fetchUserPaymentDetailsQuery = 
				new FetchUserPaymentDetailsQuery(productReservedEvent.getUserId());
		
		User userPaymentDetails = null;
		try {
			userPaymentDetails = 
					queryGateway.query(fetchUserPaymentDetailsQuery, ResponseTypes.instanceOf(User.class)).join();
		} catch (Exception ex) {
			log.error("Failed to get UserPaymentDetails info. Starting compensating transaction", ex.getMessage());
			//Start compensating transaction
			cancelProductReservation(productReservedEvent, ex.getMessage());
			return;
		}
		if(userPaymentDetails == null) {
			//Start compensating transaction
			log.error("Got no UserPaymentDetails info. Starting compensating transaction");
			cancelProductReservation(productReservedEvent, "Could not fetch user payment details");
			return;
		}
		
		log.info("Successfully fetched user payment details for user: " +  userPaymentDetails.getFirstName());
		
		// Deadline
		scheduleId = deadlineManager.schedule(Duration.of(120, ChronoUnit.SECONDS),
				PAYMENT_PROCESSING_TIMEOUT_DEADLINE, productReservedEvent);
		
		// Trigger the deadline while testing:
//		if(true) {return;}
		
		// Process payment details
		ProcessPaymentCommand processPaymentCommand = ProcessPaymentCommand.builder()
				.orderId(productReservedEvent.getOrderId())
				.paymentDetails(userPaymentDetails.getPaymentDetails())
				.paymentId(UUID.randomUUID().toString())
				.build();
		
		String paymentId = null;
		try {
			// result = commandGateway.sendAndWait(processPaymentCommand, 10, TimeUnit.SECONDS);
			
			// In case of a Deadline
			paymentId = commandGateway.sendAndWait(processPaymentCommand);
			
		} catch (Exception ex) {
			log.error("Unable to publish processPaymentCommand. Starting compensating transaction", ex.getMessage());
			// Start compensating transaction
			cancelProductReservation( productReservedEvent, ex.getMessage() );
			return;
		}
		
		if(paymentId == null) {
			log.info("The ProcessPaymentCommand resulted in NULL. Initiating a compensating transaction");
			// Start compensating transaction
			cancelProductReservation( productReservedEvent, "Could not process user payment with payment details");
		}
	}
	
		
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(PaymentProcessedEvent paymentProcessedEvent) {
		
		// In case of success cancel deadline:
		cancelDeadline();
		
		// Send an ApproveOrderCommand
		ApproveOrderCommand approvedOrderCommand = 
				new ApproveOrderCommand(paymentProcessedEvent.getOrderId()); 
		
		commandGateway.send(approvedOrderCommand);
	}
	
	
	@EndSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(OrderApprovedEvent orderApprovedEvent) {
		
		log.info("Order is approved. Order Saga is complete for orderId: " + 
							orderApprovedEvent.getOrderId() );
		
		queryUpdateEmitter.emit(FindOrderQuery.class, query -> true, 
				new OrderSummary(orderApprovedEvent.getOrderId(), orderApprovedEvent.getOrderStatus(), "")
				);
		
	// This can be also used instead of @EndSaga:
	//	SagaLifecycle.end();
	}

	/////////////////////////////////////
	// COMPENSATING TRANSACTIONS
	/////////////////////////////////////

	private void cancelProductReservation(ProductReservedEvent productReservedEvent, String reason) {

		cancelDeadline();

		CancelProductReservationCommand publishProductReservationCommand =
				CancelProductReservationCommand.builder()
						.orderId(productReservedEvent.getOrderId())
						.productId(productReservedEvent.getProductId())
						.quantity(productReservedEvent.getQuantity())
						.userId(productReservedEvent.getUserId())
						.reason(reason)
						.build();

		commandGateway.send(publishProductReservationCommand);
	}

	@SagaEventHandler(associationProperty = "orderId")
	public void handle(ProductReservationCancelledEvent  productReservationCancelledEvent) {
		
		// Create and send a RejectOrderCommand
		RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(
			productReservationCancelledEvent.getOrderId(), productReservationCancelledEvent.getReason() );
		
		commandGateway.send(rejectOrderCommand);
	}
	
	
	@EndSaga
	@SagaEventHandler(associationProperty = "orderId")
	public void handle(OrderRejectedEvent orderRejectedEvent) {
		
	log.info("Successfully rejected order with id: " + orderRejectedEvent.getOrderId());
		
	queryUpdateEmitter.emit(FindOrderQuery.class, query -> true, 
				new OrderSummary(orderRejectedEvent.getOrderId(),
				orderRejectedEvent.getOrderStatus(),
				orderRejectedEvent.getReason()) );
	}
	
	
	@DeadlineHandler(deadlineName = PAYMENT_PROCESSING_TIMEOUT_DEADLINE)
	public void handlePaymentDeadline(ProductReservedEvent productReservedEvent) {
		log.info("Payment processing deadline took place. "
				+ "Sending a compensating command to cancel the product reservation.");
		cancelProductReservation(productReservedEvent, "Payment timout");
	}
	
	private void cancelDeadline() {
		if(scheduleId != null) {
			deadlineManager.cancelSchedule(PAYMENT_PROCESSING_TIMEOUT_DEADLINE, scheduleId);
			scheduleId = null;
		}
	}

	
}
