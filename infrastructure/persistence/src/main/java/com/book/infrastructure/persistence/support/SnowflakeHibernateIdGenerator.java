package com.book.infrastructure.persistence.support;

import com.book.common.sequnce.IdGenerator;
import com.book.common.sequnce.SnowflakeIdGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

public class SnowflakeHibernateIdGenerator implements IdentifierGenerator {

    private final IdGenerator idGenerator;

    public SnowflakeHibernateIdGenerator() {
        this(new SnowflakeIdGenerator());
    }

    public SnowflakeHibernateIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public Object generate(
            SharedSessionContractImplementor session,
            Object object
    ) {
        return idGenerator.nextId();
    }
}