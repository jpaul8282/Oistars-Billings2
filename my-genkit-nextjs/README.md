# Bargain Chef (Genkit + Next.js)

Suggests recipes built around today's local grocery deals using Genkit and Gemini tools.

## Getting Started

1. Set your Gemini API key:
```bash
export GEMINI_API_KEY="your-api-key"
```

2. Start the Genkit flow:
```bash
npx genkit start -- tsx --watch src/genkit/bargainChefFlow.ts
```

3. Run the Next.js development server:
```bash
npm run dev
```

4. Or test the flow via curl:
```bash
curl -N -X POST http://localhost:3000/api/bargainChefFlow \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"data":{"craving":"something warm with chicken"}}'
```
