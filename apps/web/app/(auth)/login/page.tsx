"use client";

import { signIn } from "next-auth/react";
import { useSearchParams } from "next/navigation";
import { Fingerprint, ArrowRight, Shield } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Suspense } from "react";

function LoginContent() {
  const searchParams = useSearchParams();
  const callbackUrl = searchParams.get("callbackUrl") ?? "/dashboard";
  const error = searchParams.get("error");

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-700 via-blue-800 to-slate-900 p-4">
      {/* Background decorations */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-96 h-96 rounded-full bg-blue-500/10 blur-3xl" />
        <div className="absolute -bottom-40 -left-40 w-96 h-96 rounded-full bg-emerald-500/10 blur-3xl" />
      </div>

      <div className="relative w-full max-w-md">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-white/10 backdrop-blur-sm border border-white/20 mb-4 shadow-xl">
            <Fingerprint className="w-7 h-7 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">
            ScaleBiometrics
          </h1>
          <p className="text-sm text-blue-200 mt-1">Admin Console</p>
        </div>

        {/* Card */}
        <div className="relative bg-white rounded-2xl shadow-2xl overflow-hidden">
          {/* Top accent */}
          <div className="h-1 bg-gradient-to-r from-blue-600 to-emerald-500" />

          <div className="p-8">
            <h2 className="text-xl font-bold text-slate-900 text-center">
              Connexion
            </h2>
            <p className="text-sm text-slate-500 text-center mt-1 mb-6">
              Authentifiez-vous via votre compte organisationnel
            </p>

            {/* Error message */}
            {error && (
              <div className="mb-4 flex items-start gap-2 rounded-xl bg-red-50 border border-red-200 px-4 py-3">
                <Shield className="h-4 w-4 text-red-500 mt-0.5 flex-shrink-0" />
                <p className="text-sm text-red-700">
                  {error === "OAuthSignin"
                    ? "Erreur lors de la connexion. Veuillez réessayer."
                    : error === "OAuthCallback"
                    ? "Erreur de callback. Vérifiez la configuration Keycloak."
                    : error === "Unauthorized"
                    ? "Accès non autorisé. Contactez votre administrateur."
                    : "Une erreur est survenue. Veuillez réessayer."}
                </p>
              </div>
            )}

            {/* Keycloak SSO button */}
            <Button
              onClick={() =>
                signIn("keycloak", { callbackUrl })
              }
              className="w-full h-11 text-base font-semibold gap-3 bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-700 hover:to-blue-800 shadow-lg shadow-blue-500/25 transition-all duration-200"
            >
              <svg
                className="h-5 w-5"
                viewBox="0 0 24 24"
                fill="currentColor"
                aria-hidden="true"
              >
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 14.5v-9l6 4.5-6 4.5z" />
              </svg>
              Se connecter avec Keycloak
              <ArrowRight className="h-4 w-4 ml-auto" />
            </Button>

            {/* Info */}
            <div className="mt-6 flex items-center gap-2 rounded-xl bg-slate-50 border border-slate-200 px-4 py-3">
              <Shield className="h-4 w-4 text-slate-400 flex-shrink-0" />
              <p className="text-xs text-slate-500">
                Connexion sécurisée via OpenID Connect (PKCE S256).
                Vos identifiants ne transitent pas par cette application.
              </p>
            </div>
          </div>
        </div>

        {/* Footer */}
        <p className="text-center text-xs text-blue-200/60 mt-6">
          ScaleBiometrics SaaS Platform — v0.1.0
        </p>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense>
      <LoginContent />
    </Suspense>
  );
}
