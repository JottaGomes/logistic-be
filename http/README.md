# HTTP requests

Plain `.http` files for the IntelliJ / VS Code REST clients. A Postman collection
with assertions covering the same endpoints lives in `../postman/`.

Recording data first, then evaluating it:

| File | What it exercises |
|---|---|
| `create-shipment.http` | register a shipment |
| `record-income.http` | customer payment administration |
| `record-cost.http` | operational cost administration |
| `shipment-detail.http` | what is recorded, with running totals |
| `correct-cost.http` | corrects an amount with optimistic locking; 409 when sent twice |
| `list-shipments.http` | the shipments available to evaluate |
| `calculate-profit.http` | main flow, result is a profit |
| `calculate-loss.http` | main flow, result is a loss |
| `calculate-not-found.http` | alternative flow I — unknown shipment, 404 |
| `calculate-validation-error.http` | blank reference, 400 |
| `list-calculations.http` | stored history, paged |
| `chat.http` | the shipment assistant; needs `GROQ_API_KEY` |
| `login.http` | obtain a JWT when authentication is on |
