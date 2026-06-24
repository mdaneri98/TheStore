/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.amazon.sample.orders.web;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class TraceIdFilter extends OncePerRequestFilter {

  public static final String TRACE_ID_HEADER = "X-Trace-Id";

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    String traceId = currentTraceId();

    if (traceId != null) {
      response.setHeader(TRACE_ID_HEADER, traceId);
    }

    try {
      filterChain.doFilter(request, response);
    } finally {
      logTraceId(request, response, traceId);
    }
  }

  private void logTraceId(
    HttpServletRequest request,
    HttpServletResponse response,
    String fallbackTraceId
  ) {
    String traceId = currentTraceId();

    if (traceId == null) {
      traceId = fallbackTraceId;
    }

    if (traceId == null) {
      return;
    }

    if (!response.isCommitted()) {
      response.setHeader(TRACE_ID_HEADER, traceId);
    }

    log.info(
      "traceId={} {} {} status={}",
      traceId,
      request.getMethod(),
      request.getRequestURI(),
      response.getStatus()
    );
  }

  private String currentTraceId() {
    SpanContext spanContext = Span.current().getSpanContext();

    if (!spanContext.isValid()) {
      return null;
    }

    return spanContext.getTraceId();
  }
}
