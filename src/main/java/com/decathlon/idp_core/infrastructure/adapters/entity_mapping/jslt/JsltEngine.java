package com.decathlon.idp_core.infrastructure.adapters.entity_mapping.jslt;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;

import com.decathlon.idp_core.domain.constant.JsltConstraints;
import com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingJsltErrorException;
import com.decathlon.idp_core.infrastructure.adapters.entity_mapping.engine.ExpressionEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Function;
import com.schibsted.spt.data.jslt.Parser;

/// JSLT expression engine with DoS protections: expression length is bounded and
/// each evaluation runs under a timeout.
///
/// JSLT does not honor thread interruption, so a timed-out evaluation is
/// cancelled and may keep running on its virtual thread until it finishes.
@Component
public class JsltEngine implements ExpressionEngine {

  private static final ExecutorService EVALUATION_EXECUTOR = Executors
      .newVirtualThreadPerTaskExecutor();

  private final Collection<Function> customFunctions;

  // Creates a JSLT engine with the custom functions discovered by Spring.
  /// @param customFunctions custom JSLT functions to register
  public JsltEngine(Collection<Function> customFunctions) {
    this.customFunctions = customFunctions;
  }

  public Expression compile(String expression) {
    return Parser.compileString(expression, customFunctions);
  }

  @Override
  public void validateExpression(String expression) {
    checkExpressionLength(expression);
    try {
      compile(expression);
    } catch (Exception exception) {
      throw new EntityDynamicMappingJsltErrorException(exception.getMessage());
    }
  }

  /**
   * Evaluates a JSLT transformation expression against an input JSON payload.
   * <p>
   * Validates that the expression does not exceed
   * {@link JsltConstraints#MAX_EXPRESSION_LENGTH} and executes the evaluation
   * asynchronously within a configured timeout
   * ({@link JsltConstraints#EVALUATION_TIMEOUT_MS} ms). If the execution exceeds
   * the timeout, fails, or is interrupted, the underlying task is cancelled.
   *
   * @param expression
   *          the JSLT expression to compile and apply
   * @param payload
   *          the target {@link JsonNode} payload to be transformed
   * @return the transformed {@link JsonNode} result
   * @throws EntityDynamicMappingJsltErrorException
   *           if the expression length exceeds the maximum limit, the evaluation
   *           times out, compilation/execution fails, or the operation is
   *           interrupted
   */
  @Override
  public JsonNode evaluate(String expression, JsonNode payload) {
    checkExpressionLength(expression);
    Future<JsonNode> future = EVALUATION_EXECUTOR.submit(() -> compile(expression).apply(payload));
    try {
      return future.get(JsltConstraints.EVALUATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (TimeoutException _) {
      future.cancel(true);
      throw new EntityDynamicMappingJsltErrorException(
          "JSLT evaluation exceeded timeout of " + JsltConstraints.EVALUATION_TIMEOUT_MS + " ms");
    } catch (ExecutionException exception) {
      throw new EntityDynamicMappingJsltErrorException(exception.getCause().getMessage());
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
      future.cancel(true);
      throw new EntityDynamicMappingJsltErrorException("JSLT evaluation was interrupted");
    }
  }

  private void checkExpressionLength(String expression) {
    if (expression != null && expression.length() > JsltConstraints.MAX_EXPRESSION_LENGTH) {
      throw new EntityDynamicMappingJsltErrorException("JSLT expression exceeds maximum length of "
          + JsltConstraints.MAX_EXPRESSION_LENGTH + " characters");
    }
  }

}
