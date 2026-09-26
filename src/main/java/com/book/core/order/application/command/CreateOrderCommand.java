package com.book.core.order.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.List;

public record CreateOrderCommand(Long userId,List<CreateOrderItemCommand>items){public CreateOrderCommand{if(userId==null||userId<=0||items==null||items.isEmpty()){throw new CoreException(ErrorCode.INVALID_REQUEST);}if(items.stream().anyMatch(item->item==null)){throw new CoreException(ErrorCode.INVALID_REQUEST);}items=List.copyOf(items);}}
