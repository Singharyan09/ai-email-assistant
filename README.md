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

## Configure the AI provider

The backend works with any OpenAI-compatible chat completions API. It reads three environment variables:

| Variable      | Meaning                                                                 |
|---------------|-------------------------------------------------------------------------|
| `AI_BASE_URL` | API base URL. The backend calls `{AI_BASE_URL}/chat/completions`; a trailing slash is fine. |
| `AI_API_KEY`  | Your provider's API key, sent as `Authorization: Bearer <key>`.          |
| `AI_MODEL`    | Model name to request.                                                  |

Gemini:

```bash
export AI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
export AI_API_KEY=your-gemini-key
export AI_MODEL=gemini-3.8-flash
```

OpenAI:

```bash
export AI_BASE_URL=https://api.openai.com/v1
export AI_API_KEY=your-openai-key
export AI_MODEL=gpt-4o-mini
```

On Windows, set them as user environment variables (System Properties > Environment Variables) and open a new terminal. The reply length limit is `ai.max-tokens` in `backend/src/main/resources/application.yml`.

## Run the backend

No Maven install is needed; the project includes the Maven Wrapper. Java 17+ is required, with `JAVA_HOME` pointing at the JDK.

```bash
cd backend
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```

Endpoints (JSON body: `subject`, `body`, `tone`):

- `POST /api/email/summarize`
- `POST /api/email/reply`

Success returns `{"result": "..."}`. If the AI provider fails (bad key, unknown model, network error), the backend returns HTTP 502 with `{"error": "..."}`, and the extension shows that message. The API key is never included in responses or logs.

## Load the extension

1. Open `chrome://extensions` and enable Developer mode.
2. Click Load unpacked and select the `extension/` folder.
3. Open an email in Gmail, click the extension icon, and pick an action.

## Notes

- Gmail's page structure changes now and then. If email text is not picked up, update the selectors in `extension/content.js`.
- The API key stays on the backend. Never put it in the extension.
