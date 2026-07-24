package com.decathlon.idp_core.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.decathlon.idp_core.AbstractIntegrationTest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"app.security.mock-enabled=true"})
@Tag("security")
class ZapSecurityIntegrationTest extends AbstractIntegrationTest {

  @LocalServerPort
  private int port;

  @BeforeEach
  void setup() {
    org.testcontainers.Testcontainers.exposeHostPorts(port);
  }

  @Test
  void runZapApiScan() throws Exception {
    String openApiUrl = "http://host.testcontainers.internal:" + port + "/v3/api-docs";

    File reportDir = Paths.get("target", "zap-reports").toFile();
    if (!reportDir.exists() && !reportDir.mkdirs()) {
      throw new IllegalStateException(
          "Failed to create ZAP report directory: " + reportDir.getAbsolutePath());
    }

    if (!reportDir.setWritable(true, false)) {
      throw new IllegalStateException("Failed to set write permissions on ZAP report directory: "
          + reportDir.getAbsolutePath());
    }

    GenericContainer<?> zapContainer = new GenericContainer<>(
        DockerImageName.parse("zaproxy/zap-stable:2.17.0"))
            .withFileSystemBind(reportDir.getAbsolutePath(), "/zap/wrk", BindMode.READ_WRITE)
            .withCommand("tail", "-f", "/dev/null");

    try (zapContainer) {
      zapContainer.start();

      // Run script with -r option to output HTML report and -J to output JSON
      // report
      // HTML report: /zap/wrk/zap-report.html
      // JSON report: /zap/wrk/zap-report.json (for parsing findings summary)
      Container.ExecResult result = zapContainer.execInContainer("zap-api-scan.py", "-t",
          openApiUrl, "-f", "openapi", "-r", "zap-report.html", "-J", "zap-report.json");

      log.info("--- OWASP ZAP Standard Output ---");
      log.info(result.getStdout());

      log.info("--- OWASP ZAP Error Output ---");
      log.info(result.getStderr());

      log.info("OWASP ZAP Execution finished with Exit Code: {}", result.getExitCode());

      assertTrue(result.getExitCode() <= 1,
          "OWASP ZAP scan failed or discovered severe vulnerabilities. Check logs for details.");
    }
  }
}
