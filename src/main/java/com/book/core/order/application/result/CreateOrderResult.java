package com.book.core.order.application.result;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderResult(String orderKey,boolean canProceedToPayment,BigDecimal totalPrice,List<CreateOrderItemResult>items){

public CreateOrderResult{items=List.copyOf(items);}}
