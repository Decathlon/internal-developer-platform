package com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature;

import static com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature.HttpMessageSignatureVerifierTest.CREATED;
import static com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature.HttpMessageSignatureVerifierTest.EXPIRES;
import static com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature.HttpMessageSignatureVerifierTest.ID;
import static com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature.HttpMessageSignatureVerifierTest.SECRET;
import static com.decathlon.idp_core.infrastructure.adapters.api.auth.http_message_signature.HttpMessageSignatureVerifierTest.signed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.decathlon.idp_core.infrastructure.adapters.api.configuration.HttpMessageSignatureAlgorithm;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.HttpMessageSignatureProperties;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiErrorResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(OutputCaptureExtension.class)
class HttpMessageSignatureFilterTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void validRequestPreservesBodyAndBearerAuthentication() throws Exception {
    var request = request();
    request.addHeader("Signature",
        signed(HttpMessageSignatureAlgorithm.HMAC_SHA256, "x-request-id", ID, CREATED, EXPIRES));
    request.addHeader("Authorization", "Bearer unchanged");
    request.setContent(new byte[]{0, 1, 2, -1});
    var response = new MockHttpServletResponse();
    var calls = new AtomicInteger();
    filter(List.of()).doFilter(request, response, (req, res) -> {
      calls.incrementAndGet();
      assertThat(request.getInputStream().readAllBytes()).containsExactly(0, 1, 2, -1);
      assertThat(request.getHeader("Authorization")).isEqualTo("Bearer unchanged");
    });
    assertThat(calls.get()).isEqualTo(1);
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void rejectionUsesExactJsonAndHeadersAndStopsProcessing() throws Exception {
    var request = request();
    request.addHeader("Authorization", "Signature ignored");
    var response = reject(request);
    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
    assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Signature realm=\"idp-core\"");
    assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    assertThat(mapper.readTree(response.getContentAsString())).isEqualTo(mapper.readTree("""
        {"error":"UNAUTHORIZED","error_description":"The HTTP message signature is missing"}
        """));
  }

  @Test
  void bothHeadersAreRequiredAndDuplicatesRejected() throws Exception {
    var missingId = new MockHttpServletRequest("GET", "/api/v1/entities");
    missingId.addHeader("Signature", "anything");
    assertDescription(reject(missingId), HttpMessageSignatureResult.MISSING);
    var duplicate = request();
    duplicate.addHeader("Signature", "one");
    duplicate.addHeader("Signature", "two");
    assertDescription(reject(duplicate), HttpMessageSignatureResult.MALFORMED);
    var duplicateId = request();
    duplicateId.addHeader("x-request-id", "another");
    duplicateId.addHeader("Signature", "anything");
    assertDescription(reject(duplicateId), HttpMessageSignatureResult.MALFORMED);
  }

  @Test
  void errorsAreClassifiedAndLogsDoNotExposeSignatures(CapturedOutput output) throws Exception {
    var malformed = request();
    malformed.addHeader("Signature", "malformed-private-header");
    assertDescription(reject(malformed), HttpMessageSignatureResult.MALFORMED);
    var expired = request();
    expired.addHeader("Signature", signed(HttpMessageSignatureAlgorithm.HMAC_SHA256, "x-request-id",
        ID, "1793789250", CREATED));
    assertDescription(reject(expired), HttpMessageSignatureResult.EXPIRED);
    var invalid = request();
    String signature = signed(HttpMessageSignatureAlgorithm.HMAC_SHA256, "x-request-id",
        "different", CREATED, EXPIRES);
    invalid.addHeader("Signature", signature);
    assertDescription(reject(invalid), HttpMessageSignatureResult.INVALID);
    assertThat(output).contains("unverifiedRequestId=" + ID).doesNotContain(SECRET, signature,
        "malformed-private-header");
  }

  @Test
  void failureCorrelationIsBoundedAndSanitized(CapturedOutput output) throws Exception {
    var request = new MockHttpServletRequest("GET", "/protected");
    request.addHeader("x-request-id", "id\r\ninjection " + "x".repeat(200));
    reject(request);
    assertThat(output).contains("unverifiedRequestId=id__injection_")
        .doesNotContain("id\r\ninjection", "x".repeat(129));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/actuator/health", "/actuator/health/readiness", "/swagger-ui.html",
      "/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs/internal"})
  void excludesOnlyConfiguredPathsIncludingContextPath(String path) throws Exception {
    var request = new MockHttpServletRequest("GET", "/context" + path);
    request.setContextPath("/context");
    var calls = new AtomicInteger();
    filter(List.of("/actuator/health/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"))
        .doFilter(request, new MockHttpServletResponse(), (req, res) -> calls.incrementAndGet());
    assertThat(calls.get()).isEqualTo(1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/", "/login", "/oauth2/authorization/github", "/api/v1/entities",
      "/actuator/env", "/webhooks/public-connector"})
  void nonPreflightOptionsOnPublicOrWebhookRoutesAreNotImplicitlyExcluded(String path)
      throws Exception {
    var request = new MockHttpServletRequest("OPTIONS", path);
    assertThat(reject(request).getStatus()).isEqualTo(401);
  }

  @Test
  void exclusionsCanBeEmptyOrReplaced() throws Exception {
    var health = new MockHttpServletRequest("GET", "/actuator/health");
    assertThat(reject(health).getStatus()).isEqualTo(401);
    var calls = new AtomicInteger();
    var custom = new MockHttpServletRequest("GET", "/custom/public");
    filter(List.of("/custom/p?blic")).doFilter(custom, new MockHttpServletResponse(),
        (req, res) -> calls.incrementAndGet());
    assertThat(calls.get()).isEqualTo(1);
  }

  @Test
  void downstreamErrorsRemainObservable() throws Exception {
    var request = request();
    request.addHeader("Signature",
        signed(HttpMessageSignatureAlgorithm.HMAC_SHA256, "x-request-id", ID, CREATED, EXPIRES));
    assertThatThrownBy(
        () -> filter(List.of()).doFilter(request, new MockHttpServletResponse(), (req, res) -> {
          throw new ServletException("downstream failure");
        })).isInstanceOf(ServletException.class).hasMessage("downstream failure");
  }

  private MockHttpServletRequest request() {
    var request = new MockHttpServletRequest("POST", "/api/v1/entities");
    request.addHeader("x-request-id", ID);
    return request;
  }

  private HttpMessageSignatureFilter filter(List<String> exclusions) {
    var properties = new HttpMessageSignatureProperties(true, SECRET, "x-request-id",
        HttpMessageSignatureAlgorithm.HMAC_SHA256, 30, 0, exclusions);
    return new HttpMessageSignatureFilter(properties,
        new HttpMessageSignatureVerifier(properties,
            Clock.fixed(Instant.ofEpochSecond(1793789290), ZoneOffset.UTC)),
        new ApiErrorResponseWriter(mapper));
  }

  private MockHttpServletResponse reject(MockHttpServletRequest request) throws Exception {
    var response = new MockHttpServletResponse();
    filter(List.of()).doFilter(request, response, (req, res) -> {
      throw new AssertionError("Rejected request reached downstream");
    });
    return response;
  }

  private void assertDescription(MockHttpServletResponse response,
      HttpMessageSignatureResult result) throws Exception {
    assertThat(mapper.readTree(response.getContentAsString()).get("error_description").asText())
        .isEqualTo(result.description());
  }
}
