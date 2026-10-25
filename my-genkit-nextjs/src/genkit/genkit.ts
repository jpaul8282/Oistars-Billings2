import { genkit } from 'genkit';
import { googleAI } from '@genkit-ai/google-genai';
import { enableFirebaseTelemetry } from '@genkit-ai/firebase';

// Enable Genkit telemetry monitoring in Google Cloud / Firebase Observability
enableFirebaseTelemetry();

export const ai = genkit({
  plugins: [googleAI()],
});
