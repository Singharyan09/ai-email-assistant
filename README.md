# AI Email Assistant

A small Chrome extension for Gmail plus a Spring Boot backend. The extension reads the open email, the backend asks an AI model to summarize it or draft a reply.

```
Gmail page -> extension (content script + popup) -> Spring Boot -> AI API
```

## Layout

```
extension/   Chrome extension (Manifest V3)
backend/     Spring Boot 3 / Java 17 REST API
```

Backend packages: `controller` (REST endpoints), `service` (prompt building), `client` (AI API calls), `dto`, `config` (CORS).

## Run the backend

```bash
cd backend
export ANTHROPIC_API_KEY=your-key-here
mvn spring-boot:run
```

Endpoints (JSON body: `subject`, `body`, `tone`):

- `POST /api/email/summarize`
- `POST /api/email/reply`

The model name is set in `backend/src/main/resources/application.yml`.

## Load the extension

1. Open `chrome://extensions` and enable Developer mode.
2. Click Load unpacked and select the `extension/` folder.
3. Open an email in Gmail, click the extension icon, and pick an action.

## Notes

- Gmail's page structure changes now and then. If email text is not picked up, update the selectors in `extension/content.js`.
- The API key stays on the backend. Never put it in the extension.
