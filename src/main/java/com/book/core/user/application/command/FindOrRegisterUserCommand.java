package com.book.core.user.application.command;

import com.book.core.user.domain.ProviderType;

public record FindOrRegisterUserCommand(ProviderType providerType, String providerUserId, String providerEmail) {}
