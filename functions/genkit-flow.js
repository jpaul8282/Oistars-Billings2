/**
 * Server-Side Genkit AI Flows for Oistars Billings
 * 
 * Keeps the Gemini API key securely on the server (via Cloud Functions / environment variables)
 * and never exposes it to client APKs.
 */

const { genkit, z } = require("genkit");
const { googleAI } = require("@genkit-ai/googleai");
const { enableFirebaseTelemetry } = require("@genkit-ai/firebase");

// Enable Firebase / Google Cloud telemetry for Genkit flows
enableFirebaseTelemetry();

// Resolve server-side Gemini API key securely
const apiKey = process.env.GEMINI_API_KEY || process.env.GOOGLE_GENAI_API_KEY || "";

// Configure Genkit instance with Google AI plugin
// Model selection adheres to gemini-api guidelines (gemini-3.5-flash for basic text tasks)
const ai = genkit({
  plugins: [googleAI({ apiKey: apiKey || undefined })],
  model: "googleai/gemini-3.5-flash",
});

/**
 * 1. Hello Flow (as specified in user prompt)
 */
const helloFlow = ai.defineFlow("helloFlow", async (name) => {
  const targetName = name || "Valued Partner";
  if (apiKey) {
    try {
      const response = await ai.generate(`Hello Gemini, my name is ${targetName}. Give a short, 1-sentence friendly greeting.`);
      return { text: response.text };
    } catch (err) {
      console.warn("[Genkit helloFlow] Generation failed or quota exceeded:", err.message);
    }
  }
  return { text: `Hello ${targetName}! Welcome to Oistars Billings AI Server Engine.` };
});

/**
 * Helper to build high-quality context-aware fallback templates when server is offline or key not provided
 */
function buildLocalCommunicationTemplate({ clientName, invoiceId, amount, currency, dueDate, tone, customInstructions }) {
  const cName = clientName || "Valued Client";
  const inv = invoiceId || "INV-NEW";
  const curr = currency || "EUR";
  const amtFormatted = amount ? `${curr} ${Number(amount).toFixed(2)}` : `${curr} 0.00`;
  const due = dueDate || "Upon receipt";
  const extra = customInstructions ? ` Note: ${customInstructions}` : "";

  switch (tone) {
    case "friendly_welcome":
      return {
        subject: `Invoice ${inv} - Thank you for your partnership!`,
        communicationText: `Dear ${cName},\n\nThank you for choosing Oistars Billings. We appreciate your continued trust and collaboration. Please find attached invoice ${inv} for ${amtFormatted}, with payment due by ${due}.${extra}\n\nPlease let us know if you have any questions regarding these services.\n\nWarm regards,\nOistars Billing & Commerce Team`
      };

    case "formal_b2b":
      return {
        subject: `Tax Invoice ${inv} - Payment Notice for ${cName}`,
        communicationText: `Dear Accounts Payable Team,\n\nPlease review tax invoice ${inv} issued to ${cName} for the total amount of ${amtFormatted}. In accordance with agreed billing terms, settlement is scheduled for ${due}.${extra}\n\nKindly remit payment to our business account via SEPA / Corporate Wire. Thank you for your prompt processing.\n\nSincerely,\nFinance Department\nOistars Brands B.V.`
      };

    case "payment_reminder":
      return {
        subject: `Payment Reminder: Invoice ${inv} (${amtFormatted})`,
        communicationText: `Hello ${cName},\n\nThis is a friendly reminder that invoice ${inv} for ${amtFormatted} is due on ${due}.${extra}\n\nIf payment has already been initiated, please disregard this notice. Otherwise, we kindly ask you to finalize the settlement at your earliest convenience via Wero or SEPA instant transfer.\n\nBest regards,\nOistars Collections Team`
      };

    case "overdue_notice":
      return {
        subject: `OVERDUE NOTICE: Immediate Settlement Required for ${inv}`,
        communicationText: `Dear ${cName},\n\nOur records indicate that invoice ${inv} in the amount of ${amtFormatted} passed its due date (${due}) and remains outstanding.${extra}\n\nPlease arrange immediate remittance to prevent account restriction. You may settle instantly via Wero or SEPA Direct (IBAN: NL43 INGB 0006 3721 46, BIC: INGBNL2A). Please contact us immediately if this invoice has already been cleared.\n\nUrgent,\nOistars Finance & Collections`
      };

    case "wero_sepa_details":
      return {
        subject: `Instant Payment Instructions for ${inv} via Wero & SEPA`,
        communicationText: `Dear ${cName},\n\nFor instant, zero-fee settlement of invoice ${inv} (${amtFormatted}), you can pay directly using European Wero or SEPA Instant Wire:\n\nBeneficiary: Oistars Webshop Brands B.V.\nIBAN: NL43 INGB 0006 3721 46\nBIC: INGBNL2A\nPayment Reference: ${inv}\nDue Date: ${due}${extra}\n\nThank you for your business!\nOistars Treasury`
      };

    default:
      return {
        subject: `Invoice ${inv} for ${cName}`,
        communicationText: `Dear ${cName},\n\nPlease find details for invoice ${inv} amounting to ${amtFormatted}, due on ${due}.${extra}\n\nThank you for choosing Oistars Billings.\n\nRegards,\nOistars Team`
      };
  }
}

/**
 * 2. Generate Invoice Customer Communication Flow
 * Generates customer notes, payment instructions, reminder emails, or overdue notices
 * using Gemini on the server.
 */
const generateInvoiceCommunicationFlow = ai.defineFlow("generateInvoiceCommunication", async (input) => {
  const {
    clientName = "Valued Customer",
    invoiceId = "INV-001",
    amount = 0.0,
    currency = "EUR",
    dueDate = "Net 14 Days",
    tone = "friendly_welcome",
    customInstructions = "",
  } = input || {};

  // If server has Gemini API key, prompt the model
  if (apiKey) {
    try {
      const prompt = `You are a professional billing communications assistant for 'Oistars Billings', an enterprise e-commerce and invoice management platform in Europe.
Write a concise, high-quality customer communication message (suitable for invoice notes or customer email message) for the following invoice:

- Client Name: ${clientName}
- Invoice ID: ${invoiceId}
- Amount: ${currency} ${Number(amount).toFixed(2)}
- Due Date: ${dueDate}
- Tone / Communication Type: ${tone} (e.g. friendly_welcome, formal_b2b, payment_reminder, overdue_notice, wero_sepa_details)
${customInstructions ? `- Additional Instructions: ${customInstructions}` : ""}

Requirements:
1. Professional, polite, and completely clear.
2. Include payment reference '${invoiceId}' and mention that instant payments are supported via Wero and SEPA wire (IBAN: NL43 INGB 0006 3721 46, BIC: INGBNL2A) when appropriate.
3. Keep it between 3 to 6 sentences. Do not use markdown headers; format as clean ready-to-send text.
4. Provide a 1-line Subject and the Body.`;

      const response = await ai.generate(prompt);
      const outputText = response.text || "";

      // Parse subject and body if model formatted it
      let subject = `Invoice ${invoiceId} - ${clientName}`;
      let bodyText = outputText;

      const subjectMatch = outputText.match(/Subject:\s*(.*)/i);
      if (subjectMatch) {
        subject = subjectMatch[1].trim();
        bodyText = outputText.replace(/Subject:\s*.*\n*/i, "").trim();
      }

      return {
        success: true,
        source: "genkit_gemini_server",
        subject: subject,
        communicationText: bodyText,
        tone: tone,
      };
    } catch (err) {
      console.warn("[Genkit generateInvoiceCommunication] Gemini server generation error:", err.message);
      // Fallback gracefully to deterministic structured template
    }
  }

  // Graceful deterministic server-side generator
  const fallback = buildLocalCommunicationTemplate({
    clientName,
    invoiceId,
    amount,
    currency,
    dueDate,
    tone,
    customInstructions,
  });

  return {
    success: true,
    source: "server_template_engine",
    subject: fallback.subject,
    communicationText: fallback.communicationText,
    tone: tone,
  };
});

/**
 * 3. Bargain Chef Genkit Tool & Flow
 * Proposes recipes tailored to user cravings using live/current ingredients on sale.
 */
const getIngredientsOnSale = ai.defineTool(
  {
    name: "getIngredientsOnSale",
    description:
      "Returns the ingredients on sale at the local grocery store, with prices. The sale set differs between weekdays and weekends.",
    inputSchema: z.object({
      dayType: z
        .enum(["weekday", "weekend"])
        .describe("Whether to fetch weekday or weekend sale prices."),
    }),
    outputSchema: z.array(
      z.object({
        name: z.string(),
        price: z.string(),
      }),
    ),
  },
  async ({ dayType }) => {
    return dayType === "weekend"
      ? [
          { name: "chicken breast", price: "$2.99/lb" },
          { name: "pasta", price: "$0.79" },
          { name: "canned tomatoes", price: "$0.99" },
          { name: "garlic", price: "$0.50 / head" },
          { name: "olive oil", price: "$6.99" },
        ]
      : [
          { name: "eggs", price: "$3.49 / dozen" },
          { name: "spinach", price: "$1.99" },
          { name: "parmesan", price: "$4.99" },
          { name: "lemons", price: "$0.50 each" },
          { name: "rice", price: "$2.49" },
          { name: "butter", price: "$3.99" },
        ];
  },
);

const BargainChefInputSchema = z.object({
  craving: z.string().describe("What the user feels like eating right now."),
});

const RecipeSchema = z.object({
  title: z.string(),
  description: z.string(),
  servings: z.number(),
  ingredients: z.array(
    z.object({
      name: z.string(),
      quantity: z.string(),
      onSale: z.boolean(),
    }),
  ),
  steps: z.array(z.string()),
});

function buildLocalBargainRecipe(craving, isWeekend) {
  const c = (craving || "something warm and delicious").toLowerCase();
  if (isWeekend) {
    return {
      title: "Rustic Garlic Chicken & Tomato Pasta",
      description: `A comforting weekend recipe inspired by your craving for '${craving}', highlighting discounted chicken breast, garlic, and simmered canned tomatoes.`,
      servings: 4,
      ingredients: [
        { name: "chicken breast", quantity: "1.5 lbs (diced)", onSale: true },
        { name: "pasta", quantity: "1 box (16 oz)", onSale: true },
        { name: "canned tomatoes", quantity: "2 cans (14 oz each)", onSale: true },
        { name: "garlic", quantity: "4 cloves (minced)", onSale: true },
        { name: "olive oil", quantity: "2 tbsp", onSale: true },
        { name: "salt & black pepper", quantity: "To taste", onSale: false },
        { name: "fresh basil or parsley", quantity: "1 tbsp chopped", onSale: false }
      ],
      steps: [
        "Bring a large pot of salted water to a rolling boil and cook the pasta until al dente.",
        "Heat olive oil in a wide skillet over medium-high heat. Season chicken breast with salt and pepper, then sear until golden brown and cooked through (6-8 minutes). Transfer chicken to a plate.",
        "In the same skillet, add minced garlic and saute for 1 minute until fragrant. Pour in canned tomatoes and simmer for 10 minutes until sauce thickens.",
        "Return cooked chicken to the sauce and toss with the drained pasta.",
        "Garnish with herbs and serve hot!"
      ]
    };
  } else {
    return {
      title: "Golden Lemon-Butter Rice with Sautéed Spinach & Crispy Eggs",
      description: `A vibrant, budget-friendly weekday skillet built for your craving: '${craving}', taking advantage of store deals on fresh spinach, eggs, and butter.`,
      servings: 2,
      ingredients: [
        { name: "eggs", quantity: "3 to 4 large eggs", onSale: true },
        { name: "spinach", quantity: "5 oz fresh leaves", onSale: true },
        { name: "rice", quantity: "1.5 cups cooked warm rice", onSale: true },
        { name: "butter", quantity: "2 tbsp", onSale: true },
        { name: "parmesan", quantity: "1/4 cup grated", onSale: true },
        { name: "lemons", quantity: "1 lemon (juiced and zested)", onSale: true },
        { name: "black pepper & sea salt", quantity: "To taste", onSale: false }
      ],
      steps: [
        "In a large skillet, melt 1 tablespoon of butter over medium heat. Add fresh spinach and a pinch of salt; cook for 2 minutes until wilted, then squeeze lemon juice over it.",
        "Fold warm cooked rice into the spinach skillet, adding grated parmesan, lemon zest, and freshly cracked black pepper. Transfer to serving bowls.",
        "Melt the remaining butter in the skillet over medium-high heat. Crack the eggs into the foaming butter and fry until edges are crispy and yolk is cooked to your liking.",
        "Top each rice bowl with crispy fried eggs and extra parmesan."
      ]
    };
  }
}

const bargainChefFlow = ai.defineFlow(
  {
    name: "bargainChefFlow",
    inputSchema: BargainChefInputSchema,
    outputSchema: RecipeSchema,
  },
  async ({ craving }) => {
    const day = new Date().getDay();
    const isWeekend = day === 0 || day === 6;
    const today = new Date().toLocaleDateString("en-US", { weekday: "long" });

    if (apiKey) {
      try {
        const response = await ai.generate({
          prompt: `Today is ${today}. The user is craving: ${craving}.

Call the getIngredientsOnSale tool with the dayType that matches today. Saturday and Sunday are weekends; all other days are weekdays. Then propose ONE delicious recipe that takes advantage of those deals. For each ingredient, set onSale=true if it appears in the tool's response, false otherwise.`,
          tools: [getIngredientsOnSale],
          output: { schema: RecipeSchema },
        });

        if (response.output) {
          return response.output;
        }
      } catch (err) {
        console.warn("[Genkit bargainChefFlow] Generation error:", err.message);
      }
    }

    return buildLocalBargainRecipe(craving, isWeekend);
  },
);

const imageGeneratorFlow = ai.defineFlow(
  {
    name: "imageGeneratorFlow",
    inputSchema: z.object({ concept: z.string() }),
    outputSchema: z.string(),
  },
  async ({ concept }) => {
    if (apiKey) {
      try {
        // Step 1: Use a text model to generate a rich image prompt
        const promptResponse = await ai.generate({
          prompt: `Create a detailed, artistic prompt for an image generation model. The concept is: "${concept}".`,
        });

        const imagePrompt = promptResponse.text;

        // Step 2: Use the generated prompt to create an image
        const imageResponse = await ai.generate({
          prompt: imagePrompt,
        });

        const imageUrl = imageResponse.media?.url;
        if (imageUrl) {
          return imageUrl;
        }
      } catch (err) {
        console.warn("[Genkit imageGeneratorFlow] Generation warning:", err.message);
      }
    }

    // Graceful fallback URL for the concept
    const encoded = encodeURIComponent(concept || "creative");
    return `https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=800&q=80&sig=${encoded}`;
  },
);

module.exports = {
  ai,
  helloFlow,
  generateInvoiceCommunicationFlow,
  getIngredientsOnSale,
  bargainChefFlow,
  imageGeneratorFlow,
  BargainChefInputSchema,
  RecipeSchema,
};


