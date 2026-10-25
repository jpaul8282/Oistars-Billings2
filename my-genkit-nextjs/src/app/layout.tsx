import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Bargain Chef',
  description: "Recipe suggestions built around today's grocery deals",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
