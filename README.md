# Plan B Validator Backend

Stateless Spring Boot API for AI-assisted decision support when evaluating a risky career or life pivot (“Plan B”). Deterministic scores and verdict; Gemini and OpenAI enrich explanations with web research and narratives.

**[Complete service documentation →](docs/SERVICE.md)** — API, pipeline, scoring, deployment, logging, curl, and frontend integration in one guide.

## Quick start

```bash
cp .env.example .env   # add OPENAI_API_KEY and GEMINI_API_KEY
docker compose up --build
```

```bash
curl -s http://localhost:8080/v1/health
```

Local (Java 21): `./scripts/run-local.sh`

## Environment

```env
OPENAI_API_KEY=
OPENAI_MODEL=gpt-4.1
GEMINI_API_KEY=
GEMINI_MODEL=gemini-2.5-pro
GEMINI_RESEARCH_MODEL=gemini-2.5-flash
```

## Primary endpoint

`POST /v1/analyze` — JSON or multipart (optional resume PDF). Typical response time 30–90s; use a 240s client timeout.