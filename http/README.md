# HTTP requests

Plain `.http` files for the IntelliJ / VS Code REST clients. A Postman collection
with assertions covering the same endpoints lives in `../postman/`.

| File | What it exercises |
|---|---|
| `list-shipments.http` | the shipments available to evaluate |
| `calculate-profit.http` | main flow, result is a profit |
| `calculate-loss.http` | main flow, result is a loss |
| `calculate-not-found.http` | alternative flow I — unknown shipment, 404 |
| `calculate-validation-error.http` | blank reference, 400 |
| `list-calculations.http` | stored history, paged |
| `login.http` | obtain a JWT when authentication is on |
