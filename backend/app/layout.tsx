export const metadata = {
  title: "PRM Tool API",
  description: "Stateless enrichment + commit backend for the PRM Tool app.",
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
