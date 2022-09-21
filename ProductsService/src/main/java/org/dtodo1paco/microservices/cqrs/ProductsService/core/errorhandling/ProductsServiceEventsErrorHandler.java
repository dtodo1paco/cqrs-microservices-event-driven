package org.dtodo1paco.microservices.cqrs.ProductsService.core.errorhandling;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.EventMessageHandler;
import org.axonframework.eventhandling.ListenerInvocationErrorHandler;
@Slf4j
public class ProductsServiceEventsErrorHandler implements ListenerInvocationErrorHandler
{

	@Override
	public void onError(Exception exception, EventMessage<?> event, EventMessageHandler eventHandler) throws Exception {
		log.error("onError for event: " + event.getIdentifier());
		throw exception;
	}

}
