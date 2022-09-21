/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dtodo1paco.microservices.cqrs.UsersService.query.rest;
 
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.dtodo1paco.microservices.cqrs.shared.model.User;
import org.dtodo1paco.microservices.cqrs.shared.query.FetchUserPaymentDetailsQuery;

@RestController
@RequestMapping("/users")
public class UsersQueryController {

    @Autowired
    QueryGateway queryGateway;

    @GetMapping("/{userId}/payment-details")
    public User getUserPaymentDetails(@PathVariable String userId) {
   
        FetchUserPaymentDetailsQuery query = new FetchUserPaymentDetailsQuery(userId);
 
        return queryGateway.query(query, ResponseTypes.instanceOf(User.class)).join();

    }

}
