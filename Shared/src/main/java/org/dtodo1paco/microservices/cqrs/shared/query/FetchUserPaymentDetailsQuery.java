package org.dtodo1paco.microservices.cqrs.shared.query;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FetchUserPaymentDetailsQuery {

	private String userId;
}
