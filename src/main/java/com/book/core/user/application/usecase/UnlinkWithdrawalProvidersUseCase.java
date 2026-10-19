package com.book.core.user.application.usecase;

import com.book.core.user.application.port.OAuthUnlinkClient;
import com.book.core.user.application.port.UserProviderRepositoryPort;
import com.book.core.user.domain.UserProvider;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnlinkWithdrawalProvidersUseCase {
    private final UserProviderRepositoryPort userProviderRepository;
    private final OAuthUnlinkClient oAuthUnlinkClient;
    private final Clock clock;

    public void execute(final List<UserProvider> providers) {
        for (final UserProvider provider : providers) {
            oAuthUnlinkClient.unlink(provider.providerType(), provider.providerUserId());
            userProviderRepository.markUnlinked(provider.id(), LocalDateTime.now(clock));
        }
    }
}
