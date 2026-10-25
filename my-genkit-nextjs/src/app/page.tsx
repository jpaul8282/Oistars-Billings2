'use client';

import { useState } from 'react';
import { streamFlow } from '@genkit-ai/next/client';
import type { bargainChefFlow } from '@/genkit/bargainChefFlow';
import type { BargainChefInput, PartialRecipe } from '@/genkit/bargainChefFlow';

export default function Home() {
  const [craving, setCraving] = useState('something warm with chicken');
  const [recipe, setRecipe] = useState<PartialRecipe | null>(null);
  const [isStreaming, setIsStreaming] = useState(false);
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);

  async function generateRecipe(event: React.FormEvent) {
    event.preventDefault();
    if (!craving.trim()) return;
    setRecipe(null);
    setIsStreaming(true);
    try {
      const input: BargainChefInput = { craving };
      // Pass the flow's type so input, output, and stream chunks are typed.
      const result = streamFlow<typeof bargainChefFlow>({
        url: '/api/bargainChefFlow',
        input,
      });
      for await (const partial of result.stream) {
        setRecipe(partial);
      }
      await result.output;
    } catch (err) {
      console.error('Failed to generate recipe', err);
    } finally {
      setIsStreaming(false);
    }
  }

  async function downloadRecipePdf() {
    if (!recipe) return;
    setIsDownloadingPdf(true);
    try {
      const { jsPDF } = await import('jspdf');
      const doc = new jsPDF({
        orientation: 'portrait',
        unit: 'mm',
        format: 'a4',
      });

      const pageWidth = doc.internal.pageSize.getWidth();
      const margin = 20;
      const contentWidth = pageWidth - margin * 2;
      let y = 22;

      // Header Branding
      doc.setFont('helvetica', 'bold');
      doc.setFontSize(10);
      doc.setTextColor(37, 99, 235);
      doc.text('BARGAIN CHEF — GROCERY DEALS & RECIPES', margin, y);

      y += 8;
      // Recipe Title
      doc.setFont('helvetica', 'bold');
      doc.setFontSize(20);
      doc.setTextColor(26, 26, 26);
      const title = recipe.title || 'Custom Recipe';
      const splitTitle = doc.splitTextToSize(title, contentWidth);
      doc.text(splitTitle, margin, y);
      y += splitTitle.length * 8 + 2;

      // Divider Line
      doc.setDrawColor(229, 231, 235);
      doc.setLineWidth(0.5);
      doc.line(margin, y, pageWidth - margin, y);
      y += 6;

      // Description
      if (recipe.description) {
        doc.setFont('helvetica', 'italic');
        doc.setFontSize(11);
        doc.setTextColor(75, 85, 99);
        const splitDesc = doc.splitTextToSize(recipe.description, contentWidth);
        doc.text(splitDesc, margin, y);
        y += splitDesc.length * 5.5 + 4;
      }

      // Servings Badge / Text
      if (recipe.servings) {
        doc.setFont('helvetica', 'bold');
        doc.setFontSize(10);
        doc.setTextColor(31, 41, 55);
        doc.text(`Servings: ${recipe.servings} people`, margin, y);
        y += 8;
      }

      // Ingredients Section
      if (recipe.ingredients && recipe.ingredients.length > 0) {
        doc.setFont('helvetica', 'bold');
        doc.setFontSize(12);
        doc.setTextColor(17, 24, 39);
        doc.text('INGREDIENTS', margin, y);
        y += 6;

        doc.setFont('helvetica', 'normal');
        doc.setFontSize(10.5);

        recipe.ingredients.forEach((ing) => {
          if (y > 270) {
            doc.addPage();
            y = 20;
          }
          const bullet = '• ';
          const ingText = `${ing.quantity || ''} ${ing.name || ''}`.trim();
          doc.setTextColor(55, 65, 81);
          doc.text(bullet + ingText, margin + 2, y);

          if (ing.onSale) {
            const textWidth = doc.getTextWidth(bullet + ingText);
            doc.setFont('helvetica', 'bold');
            doc.setTextColor(46, 125, 50);
            doc.text('  [ON SALE DEAL]', margin + 2 + textWidth, y);
            doc.setFont('helvetica', 'normal');
          }
          y += 5.5;
        });

        y += 4;
      }

      // Steps Section
      if (recipe.steps && recipe.steps.length > 0) {
        if (y > 250) {
          doc.addPage();
          y = 20;
        }

        doc.setFont('helvetica', 'bold');
        doc.setFontSize(12);
        doc.setTextColor(17, 24, 39);
        doc.text('PREPARATION STEPS', margin, y);
        y += 7;

        recipe.steps.forEach((step, idx) => {
          const stepNum = `${idx + 1}. `;
          const stepLines = doc.splitTextToSize(stepNum + step, contentWidth - 4);

          if (y + stepLines.length * 5.5 > 280) {
            doc.addPage();
            y = 20;
          }

          doc.setFont('helvetica', 'normal');
          doc.setFontSize(10);
          doc.setTextColor(55, 65, 81);
          doc.text(stepLines, margin + 2, y);
          y += stepLines.length * 5.2 + 3.5;
        });
      }

      // Footer
      const totalPages = doc.internal.getNumberOfPages();
      for (let i = 1; i <= totalPages; i++) {
        doc.setPage(i);
        doc.setFont('helvetica', 'normal');
        doc.setFontSize(8.5);
        doc.setTextColor(156, 163, 175);
        doc.text(
          `Generated with Bargain Chef | Page ${i} of ${totalPages}`,
          margin,
          doc.internal.pageSize.getHeight() - 10
        );
      }

      const fileName = `${(recipe.title || 'recipe').toLowerCase().replace(/[^a-z0-9]+/g, '_')}.pdf`;
      doc.save(fileName);
    } catch (err) {
      console.error('Error generating PDF with jspdf:', err);
    } finally {
      setIsDownloadingPdf(false);
    }
  }

  return (
    <main>
      <h1>Bargain Chef</h1>
      <p className="tagline">
        Tell me what you feel like eating and I&apos;ll suggest a recipe built
        around today&apos;s grocery deals.
      </p>

      <form className="prompt" onSubmit={generateRecipe}>
        <input
          type="text"
          value={craving}
          onChange={(e) => setCraving(e.target.value)}
          name="craving"
          placeholder="What are you in the mood for?"
          disabled={isStreaming}
        />
        <button type="submit" disabled={isStreaming}>
          {isStreaming ? 'Cooking…' : 'Suggest a recipe'}
        </button>
      </form>

      {recipe && (
        <article>
          <div className="article-header">
            <div>
              {recipe.title && <h2>{recipe.title}</h2>}
              {recipe.servings && (
                <p className="serves">
                  <strong>Serves:</strong> {recipe.servings}
                </p>
              )}
            </div>

            <button
              type="button"
              className="download-btn"
              onClick={downloadRecipePdf}
              disabled={isDownloadingPdf}
              title="Download Recipe as PDF"
            >
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="7 10 12 15 17 10" />
                <line x1="12" y1="15" x2="12" y2="3" />
              </svg>
              <span>{isDownloadingPdf ? 'Generating PDF…' : 'Download Recipe'}</span>
            </button>
          </div>

          {recipe.description && (
            <p className="description">{recipe.description}</p>
          )}

          {recipe.ingredients && recipe.ingredients.length > 0 && (
            <>
              <h3>Ingredients</h3>
              <ul className="ingredients">
                {recipe.ingredients.map((ing, i) => (
                  <li key={i}>
                    {ing.quantity} {ing.name}
                    {ing.onSale && <span className="badge">on sale</span>}
                  </li>
                ))}
              </ul>
            </>
          )}

          {recipe.steps && recipe.steps.length > 0 && (
            <>
              <h3>Steps</h3>
              <ol className="steps">
                {recipe.steps.map((step, i) => (
                  <li key={i}>{step}</li>
                ))}
              </ol>
            </>
          )}
        </article>
      )}
    </main>
  );
}
