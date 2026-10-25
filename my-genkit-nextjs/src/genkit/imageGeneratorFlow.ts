import { z } from 'genkit';
import { googleAI } from '@genkit-ai/google-genai';
import { ai } from './genkit.js';

export const imageGeneratorFlow = ai.defineFlow(
  {
    name: 'imageGeneratorFlow',
    inputSchema: z.object({ concept: z.string() }),
    outputSchema: z.string(),
  },
  async ({ concept }) => {
    // Step 1: Use a text model to generate a rich image prompt
    const promptResponse = await ai.generate({
      model: googleAI.model('gemini-flash-latest'),
      prompt: `Create a detailed, artistic prompt for an image generation model. The concept is: "${concept}".`,
    });

    const imagePrompt = promptResponse.text;

    // Step 2: Use the generated prompt to create an image
    const imageResponse = await ai.generate({
      model: googleAI.model('gemini-nano-banana-2.1'),
      prompt: imagePrompt,
    });

    const imageUrl = imageResponse.media?.url;
    if (!imageUrl) {
      throw new Error('Failed to generate an image.');
    }
    return imageUrl;
  },
);
