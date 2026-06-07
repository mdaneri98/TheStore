import { Injectable, NestMiddleware } from '@nestjs/common';
import { Request, Response, NextFunction } from 'express';
import { Histogram, register } from 'prom-client';

const httpRequestDuration =
  (register.getSingleMetric('http_server_requests_seconds') as
    | Histogram<string>
    | undefined) ??
  new Histogram({
    name: 'http_server_requests_seconds',
    help: 'HTTP server request duration in seconds',
    labelNames: ['method', 'status', 'uri', 'outcome'],
    buckets: [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5, 10],
  });

function outcomeFor(statusCode: number): string {
  if (statusCode >= 100 && statusCode < 200) {
    return 'INFORMATIONAL';
  }
  if (statusCode >= 200 && statusCode < 300) {
    return 'SUCCESS';
  }
  if (statusCode >= 300 && statusCode < 400) {
    return 'REDIRECTION';
  }
  if (statusCode >= 400 && statusCode < 500) {
    return 'CLIENT_ERROR';
  }
  return 'SERVER_ERROR';
}

function routeFor(request: Request): string {
  const routePath = request.route?.path;
  if (typeof routePath === 'string') {
    return `${request.baseUrl || ''}${routePath}`;
  }
  return request.path;
}

@Injectable()
export class HttpMetricsMiddleware implements NestMiddleware {
  use(request: Request, response: Response, next: NextFunction) {
    const end = httpRequestDuration.startTimer();

    response.on('finish', () => {
      const status = response.statusCode.toString();
      end({
        method: request.method,
        status,
        uri: routeFor(request),
        outcome: outcomeFor(response.statusCode),
      });
    });

    next();
  }
}
