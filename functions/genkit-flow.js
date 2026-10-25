/**
 * Server-Side Genkit AI Flows for Oistars Billings
 * 
 * Keeps the Gemini API key securely on the server (via Cloud Functions / environment variables)
 * and never exposes it to client APKs.
 */

const { genkit } = require("genkit");
const { googleAI } = require("@genkit-ai/googleai");

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

module.exports = {
  ai,
  helloFlow,
  generateInvoiceCommunicationFlow,
};
