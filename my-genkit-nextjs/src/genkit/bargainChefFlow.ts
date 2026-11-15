import { googleAI } from '@genkit-ai/google-genai';
import { genkit, z } from 'genkit';

const ai = genkit({
  plugins: [googleAI()],
});

const getIngredientsOnSale = ai.defineTool(
  {
    name: 'getIngredientsOnSale',
    description:
      'Returns the ingredients on sale at the local grocery store, with prices. The sale set differs between weekdays and weekends.',
    inputSchema: z.object({
      dayType: z
        .enum(['weekday', 'weekend'])
        .describe('Whether to fetch weekday or weekend sale prices.'),
    }),
    outputSchema: z.array(
      z.object({
        name: z.string(),
        price: z.string(),
      }),
    ),
  },
  async ({ dayType }) => {
    // Mock data: in a real app, query a pricing database.
    return dayType === 'weekend'
      ? [
          { name: 'chicken breast', price: '$2.99/lb' },
          { name: 'pasta', price: '$0.79' },
          { name: 'canned tomatoes', price: '$0.99' },
          { name: 'garlic', price: '$0.50 / head' },
          { name: 'olive oil', price: '$6.99' },
        ]
      : [
          { name: 'eggs', price: '$3.49 / dozen' },
          { name: 'spinach', price: '$1.99' },
          { name: 'parmesan', price: '$4.99' },
          { name: 'lemons', price: '$0.50 each' },
          { name: 'rice', price: '$2.49' },
          { name: 'butter', price: '$3.99' },
        ];
  },
);

const BargainChefInputSchema = z.object({
  craving: z.string().describe('What the user feels like eating right now.'),
});

const NutritionalSummarySchema = z.object({
  calories: z.number().describe('Estimated calories per serving in kcal.'),
  protein: z.number().describe('Estimated protein per serving in grams.'),
  carbs: z.number().describe('Estimated carbohydrates per serving in grams.'),
  fat: z.number().optional().describe('Estimated fat per serving in grams.'),
});

const RecipeSchema = z.object({
  title: z.string(),
  description: z.string(),
  servings: z.number(),
  nutritionalSummary: NutritionalSummarySchema.describe(
    'Estimated nutritional summary per serving (calories, protein, carbs).',
  ),
  ingredients: z.array(
    z.object({
      name: z.string(),
      quantity: z.string(),
      onSale: z.boolean(),
    }),
  ),
  steps: z.array(z.string()),
});

// Exported for the frontend to import as types.
export type BargainChefInput = z.infer<typeof BargainChefInputSchema>;
export type NutritionalSummary = z.infer<typeof NutritionalSummarySchema>;
export type Recipe = z.infer<typeof RecipeSchema>;
export type PartialRecipe = Partial<Recipe>;

export const bargainChefFlow = ai.defineFlow(
  {
    name: 'bargainChefFlow',
    inputSchema: BargainChefInputSchema,
    outputSchema: RecipeSchema,
    streamSchema: RecipeSchema.partial(),
  },
  async ({ craving }, { sendChunk }) => {
    const today = new Date().toLocaleDateString('en-US', { weekday: 'long' });

    const { stream, response } = ai.generateStream({
      model: googleAI.model('gemini-flash-latest', {
        temperature: 0.7,
        thinkingConfig: { thinkingLevel: 'MINIMAL' },
      }),
      prompt: `Today is ${today}. The user is craving: ${craving}.

Call the getIngredientsOnSale tool with the dayType that matches today. Saturday and Sunday are weekends; all other days are weekdays. Then propose ONE recipe that takes advantage of those deals. For each ingredient, set onSale=true if it appears in the tool's response, false otherwise. Also calculate and populate nutritionalSummary with realistic per-serving estimates for calories (kcal), protein (grams), carbs (grams), and fat (grams) based on the ingredient quantities and servings.`,
      tools: [getIngredientsOnSale],
      output: { schema: RecipeSchema },
    });

    for await (const chunk of stream) {
      if (chunk.output) sendChunk(chunk.output);
    }

    const { output } = await response;
    if (!output) throw new Error('Failed to generate recipe');
    return output;
  },
);
