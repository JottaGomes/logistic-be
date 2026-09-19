# HTTP Requests

This directory contains `.http` files for testing the Logistics API endpoints manually.
Each file holds a single request.

## Files

| File | Method | Endpoint | Description |
|------|--------|----------|-------------|
| `get-all-shipments.http` | GET | `/api/shipments` | List all calculations |
| `calculate-profit.http` | POST | `/api/shipments/calculate` | Profitable shipment |
| `calculate-loss.http` | POST | `/api/shipments/calculate` | Loss shipment |
| `calculate-validation-error.http` | POST | `/api/shipments/calculate` | Missing required field |

## Prerequisites

Run app before sending requests:

```bash
mvn spring-boot:run
```

## IntelliJ IDEA Ultimate

`.http` files are natively supported. Open any file and click the green **Run** button next to the request.

## IntelliJ IDEA Community Edition

The built-in HTTP client is not available in Community Edition. Use **ijhttp** instead, the official JetBrains HTTP Client CLI, designed for local use and CI/CD pipelines.

### Installing ijhttp

```bash
brew install ijhttp
```

### Running a request

```bash
ijhttp http/get-all-shipments.http
ijhttp http/calculate-profit.http
```
