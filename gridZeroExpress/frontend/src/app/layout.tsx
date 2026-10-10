import type { Metadata } from "next";
import { Geist } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "@/context/AuthContext";
import { ThemeProvider } from "@/context/ThemeContext";

const geist = Geist({ subsets: ["latin"], variable: "--font-geist" });

export const metadata: Metadata = {
  title: "ZeroGrid // Autonomous Emergency Response & Off-Grid Mesh Platform",
  description:
    "Autonomous off-grid rescue coordination engine engineered for extreme Indian monsoons. Powered by Native Android BLE 5.0 / Wi-Fi Direct P2P Mesh with opportunistic cloud uplink into AWS ECS Fargate running AgentZero (Strands Agents SDK & Amazon Bedrock Claude 3.5 Sonnet).",
  keywords: ["emergency", "rescue", "SOS", "mesh network", "offline", "safety", "BLE", "AWS Bedrock", "Strands Agents"],
  openGraph: {
    title: "ZeroGrid // Autonomous Emergency Response & Off-Grid Mesh Platform",
    description: "Autonomous off-grid rescue coordination engine engineered for extreme Indian monsoons.",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className={geist.variable}>
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="" />
        <link
          href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700&family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&family=Space+Grotesk:wght@500;600;700&display=swap"
          rel="stylesheet"
        />
        <link
          href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200&display=swap"
          rel="stylesheet"
        />
      </head>
      {/*
        bg-canvas / text-primaryText use CSS variables defined in globals.css.
        When ThemeProvider adds .light to <html>, those variables switch automatically.
      */}
      <body className="bg-canvas text-primaryText antialiased transition-colors duration-300">
        <ThemeProvider>
          <AuthProvider>{children}</AuthProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
