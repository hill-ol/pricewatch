# PriceWatch

A self-hosted price tracker for Shopify stores. Paste a product link, set a target price, and PriceWatch checks the price on a schedule and emails you when it drops to your target or below.

![PriceWatch dashboard](docs/screenshot.png)

## Features

- **Track any Shopify product variant.** Paste a product URL (with `?variant=`) and PriceWatch reads the price straight from the store's public Storefront GraphQL API. No scraping, no API key.
- **Scheduled checks.** Every watched item is re-checked every 6 hours (configurable), and every result is saved as price history.
- **One alert per price drop.** You get one email when an item drops to your target. If the price goes back above the target, the alert resets so you'll hear about the next drop. If an email fails to send, it's retried on the next run.
- **Dashboard.** A React UI to add and remove items and see the current price, your target, and alert status for each one.

## Tech stack

| Layer    | Tech |
|----------|------|
| Backend  | Java 21, Spring Boot 4 (Web MVC, Data JPA, Mail, Scheduling) |
| Database | H2 (file-based) |
| Frontend | React 19, TypeScript, Vite |
| External | Shopify Storefront GraphQL API, SMTP (Gmail) |

## How it works

```
React UI ──REST──▶ WatchedItemController ──▶ WatchedItemRepository (H2)
                                                   ▲
              @Scheduled PriceCheckService ────────┘
                 │                 │
                 ▼                 ▼
          PriceSource         Notifier
     (ShopifyPriceSource)  (EmailNotifier)
```

`PriceCheckService` depends only on the `PriceSource` and `Notifier` interfaces, so you could add another store or notification channel (SMS, Slack, etc.) without changing the scheduling or alert logic.

## Running locally

**Prerequisites:** Java 21+, Node 20+

### 1. Backend

Set the email settings. For Gmail, `MAIL_PASSWORD` must be an [app password](https://support.google.com/accounts/answer/185833), not your normal password.

```bash
export MAIL_USERNAME=you@gmail.com
export MAIL_PASSWORD=your-app-password
export ALERT_EMAIL=where-alerts-go@example.com
```

```bash
cd backend
./mvnw spring-boot:run
```

The API runs on `http://localhost:8080`. Data is stored in `~/pricewatch/`.

To check more often while testing, set the interval as an ISO-8601 duration:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--pricewatch.check-interval=PT5M
```

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. The Vite dev server proxies `/items` to the backend.

### 3. Add an item

On any Shopify store, open a product, select a size or option so the URL includes `?variant=...`, and paste that URL into the form.

## API

| Method   | Path          | Description |
|----------|---------------|-------------|
| `GET`    | `/items`      | List watched items |
| `POST`   | `/items`      | Add an item: `{ name, productUrl, storeDomain, variantId, targetPrice }` |
| `DELETE` | `/items/{id}` | Stop watching an item (also deletes its price history) |

## Running tests

```bash
cd backend
./mvnw test
```
