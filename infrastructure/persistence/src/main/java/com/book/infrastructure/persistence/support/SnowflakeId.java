package com.book.infrastructure.persistence.support;

import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@IdGeneratorType(value = SnowflakeHibernateIdGenerator.class)
public @interface SnowflakeId {
}
