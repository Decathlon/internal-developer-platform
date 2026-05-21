package com.decathlon.idp_core.domain.port;

import com.decathlon.idp_core.domain.model.webhook.WebhookConnector;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface WebhookConnectorRepositoryPort {

    Optional<WebhookConnector> findByIdentifier(String identifier);

    Page<WebhookConnector> findAll(Pageable pageable);

    boolean existsByIdentifier(String identifier);

    boolean existsByTitle(String title);

    WebhookConnector save(WebhookConnector connector);

    void deleteByIdentifier(String identifier);
}
