package io.github.hillol.pricewatch.controller;

import io.github.hillol.pricewatch.service.ItemNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into an RFC 9457 problem response ({@code application/problem+json}):
 * {@code {"status": 400, "title": "Bad Request", "detail": "...", "errors": {...}}}.
 * Extending ResponseEntityExceptionHandler covers Spring's own errors too, such as malformed JSON.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  /** When a field breaks several rules, report the "it's missing" one: it's the root cause. */
  private static final Set<String> REQUIRED_CODES = Set.of("NotBlank", "NotNull");

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      boolean required = REQUIRED_CODES.contains(error.getCode());
      if (required || !errors.containsKey(error.getField())) {
        errors.put(error.getField(), error.getDefaultMessage());
      }
    }

    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Some fields are invalid.");
    problem.setProperty("errors", errors);
    return handleExceptionInternal(ex, problem, headers, status, request);
  }

  @ExceptionHandler(ItemNotFoundException.class)
  public ProblemDetail handleNotFound(ItemNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  /**
   * A price check saved the same item between our load and our save. Tell the client to
   * retry rather than returning a 500 or overwriting the newer data.
   */
  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ProblemDetail handleConcurrentUpdate() {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "This item was just updated by a price check. Please try again.");
  }
}
