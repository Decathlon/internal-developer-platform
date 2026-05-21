package com.decathlon.idp_core.domain.service.webhook;

import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorAlreadyExistException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException;
import com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorTitleAlreadyExistsException;
import com.decathlon.idp_core.domain.model.webhook.WebhookConnector;
import com.decathlon.idp_core.domain.port.WebhookConnectorRepositoryPort;
import com.decathlon.idp_core.domain.service.webhook.security.WebhookSecurityValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class WebhookConnectorValidationService {

    private final WebhookConnectorRepositoryPort webhookConnectorRepositoryPort;
    private final EntityDynamicMappingValidationService webhookConnectorMappingValidationService;
    private final WebhookSecurityValidationService webhookSecurityValidationService;

    public void validateWebhookConnectorForCreation(WebhookConnector webhookConnector) {
        validateIdentifierUniqueness(webhookConnector.identifier());
        validateTitleUniqueness(webhookConnector.title());
        webhookConnectorMappingValidationService.validateWebhookMapping(webhookConnector.mappings());
        webhookSecurityValidationService.validateForCreation(webhookConnector.security());

    }

    public void validateWebhookConnectorForUpdate(WebhookConnector existingConnector, WebhookConnector webhookConnectorToUpdate) {
        if (!existingConnector.title().equals(webhookConnectorToUpdate.title())) {
            validateTitleUniqueness(webhookConnectorToUpdate.title());
        }
        webhookConnectorMappingValidationService.validateWebhookMapping(webhookConnectorToUpdate.mappings());
        webhookSecurityValidationService.validateForCreation(webhookConnectorToUpdate.security());
    }

    public void validateTitleUniqueness(String webhookTitle) {
        if (webhookConnectorRepositoryPort.existsByTitle(webhookTitle)) {
            throw new WebhookConnectorTitleAlreadyExistsException("A WebhookConnector with title " + webhookTitle + " already exists");
        }

    }

    /// Checks that no other [WebhookConnector] exists with the same identifier before allowing creation.
    ///
    /// @param webhookConnectorIdentifier the webhook connector identifier to check for uniqueness
    /// @throws WebhookConnectorAlreadyExistException if a connector with the same identifier already exists
    private void validateIdentifierUniqueness(String webhookConnectorIdentifier) {
        if (webhookConnectorRepositoryPort.existsByIdentifier(webhookConnectorIdentifier)) {
            throw new WebhookConnectorAlreadyExistException(webhookConnectorIdentifier);
        }
    }

    public void validateIdentifierExists(String webhookConnectorIdentifier) {
        if (!webhookConnectorRepositoryPort.existsByIdentifier(webhookConnectorIdentifier)) {
            throw new WebhookConnectorNotFoundException("WebhookConnector with identifier " + webhookConnectorIdentifier + " not found");
        }
    }

}
