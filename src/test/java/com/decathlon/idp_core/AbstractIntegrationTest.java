package com.decathlon.idp_core;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.mock;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import org.testcontainers.shaded.org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.SneakyThrows;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AbstractIntegrationTest.TestBeanConfiguration.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@TestClassOrder(ClassOrderer.ClassName.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    public MockMvc mockMvc;

    public final ObjectMapper objectMapper;

    public final ObjectMapper userEventObjectMapper;

    public static ClientAndServer clientAndServer;

    public static MockServerClient mockServerClient;

    public static AtomicBoolean initToDo = new AtomicBoolean(true);

    protected AbstractIntegrationTest() {
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        this.userEventObjectMapper = new ObjectMapper();
        userEventObjectMapper.registerModule(new JavaTimeModule());
        userEventObjectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        userEventObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Container
    @SuppressWarnings("rawtypes")
    private static final JdbcDatabaseContainer postgres = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("idp-core").withUsername("idp-core").withPassword("idp-core");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        postgres.start();
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5)); // wait for container to be ready

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

    }

    public void startMockServer() {
        if (mockServerClient == null) {
            clientAndServer = startClientAndServer(8888);
            mockServerClient = new MockServerClient("localhost", 8888);
        }
    }

    @SafeVarargs
    public final void mockApiCall(String path, HttpStatus status, Pair<String, String>... queryParameterList) {
        mockApiCall(GET, path, status, null, queryParameterList);
    }

    @SafeVarargs
    public final void mockApiCall(String path, Object response, Pair<String, String>... queryParameterList) {
        mockApiCall(GET, path, OK, response, queryParameterList);
    }

    @SafeVarargs
    public final void mockApiCall(HttpMethod httpMethod, String path, Object response,
                                  Pair<String, String>... queryParameterList) {
        mockApiCall(httpMethod, path, OK, response, queryParameterList);
    }

    @SafeVarargs
    public final void mockApiCall(HttpMethod httpMethod, String path, HttpStatus status, Object response,
                                  Pair<String, String>... queryParameterList) {
        startMockServer();
        HttpRequest requestDefinition = getRequestDefinition(httpMethod, path, queryParameterList);
        mockServerClient.clear(requestDefinition);
        mockServerClient.when(requestDefinition)
                .respond(HttpResponse.response()
                        .withStatusCode(status.value())
                        .withHeaders(new Header(CONTENT_TYPE, APPLICATION_JSON_VALUE))
                        .withBody(response != null ? writeValueAsString(response) : null));
    }

    @SafeVarargs
    private static HttpRequest getRequestDefinition(HttpMethod httpMethod, String path,
                                                    Pair<String, String>... queryParameterList) {
        HttpRequest requestDefinition = request().withMethod(httpMethod.name()).withPath(path);

        if (queryParameterList != null) {
            for (Pair<String, String> queryParameter : queryParameterList) {
                requestDefinition.withQueryStringParameter(queryParameter.getKey(), queryParameter.getValue());
            }
        }

        return requestDefinition;
    }

    @SneakyThrows
    public static String getJsonTestFileContent(String path) {
        try (var inputStream = new ClassPathResource(path).getInputStream()) {
            return IOUtils.toString(inputStream, UTF_8);
        }
    }

    @SneakyThrows
    public String writeValueAsString(Object object) {
        if (object instanceof String) {
            return (String) object;
        }
        return objectMapper.writeValueAsString(object);
    }

    /**
     * Helper method to perform a POST request and validate that it returns a
     * BAD_REQUEST response.
     *
     * @param path             the URL path to send the POST request to
     * @param jsonBodyfilePath the file path containing the JSON content to be sent
     *                         in the request body
     * @param errorDescription the expected error description that should be
     *                         returned in the response
     * @return
     * @throws Exception if an error occurs during the mock MVC request execution
     */
    public MvcResult postAndValidateBadRequest(String path, String jsonBodyfilePath, String errorDescription)
            throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(path)
                        .contentType(APPLICATION_JSON)
                        .accept(APPLICATION_JSON)
                        .with(csrf())
                        .content(getJsonTestFileContent(jsonBodyfilePath)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.error_description").value(errorDescription))
                .andReturn();

    }

    @TestConfiguration
    public static class TestBeanConfiguration {

        WebClient webClient = WebClient.builder().baseUrl("http://localhost:8888").build();

        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

    }

}
