import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
<<<<<<< HEAD
import { ThemeProvider } from "@/components/theme-provider";
import { Providers } from "@/components/providers";
import { SessionProvider } from "@/components/session-provider";
import { Toaster } from "sonner";
=======
import { QueryProvider } from "@/components/providers/query-provider";
import { SessionProvider } from "next-auth/react";
>>>>>>> be6e83cca0b2701bde3fb3691064ddb7f5257a36

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

export const metadata: Metadata = {
<<<<<<< HEAD
  title: "ScaleBiometrics",
  description: "Biometric authentication platform",
=======
  title: {
    default: "ScaleBiometrics Console",
    template: "%s | ScaleBiometrics",
  },
  description:
    "Admin console for ScaleBiometrics SaaS Platform — Biometric identity management at scale.",
  icons: {
    icon: "/favicon.ico",
  },
>>>>>>> be6e83cca0b2701bde3fb3691064ddb7f5257a36
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
<<<<<<< HEAD
    <html lang="en" suppressHydrationWarning>
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >
        <SessionProvider>
          <Providers>
            <ThemeProvider
              attribute="class"
              defaultTheme="system"
              enableSystem
              disableTransitionOnChange
            >
              {children}
              <Toaster position="top-right" />
            </ThemeProvider>
          </Providers>
=======
    <html lang="fr" suppressHydrationWarning className={inter.variable}>
      <body className="min-h-screen antialiased font-sans">
        <SessionProvider>
          <QueryProvider>{children}</QueryProvider>
>>>>>>> be6e83cca0b2701bde3fb3691064ddb7f5257a36
        </SessionProvider>
      </body>
    </html>
  );
}
