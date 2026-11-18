// Supabase Edge Function: verify-razorpay-payment
// Deploy: supabase functions deploy verify-razorpay-payment --no-verify-jwt
// Verifies Razorpay Payment Link HMAC-SHA256 callback signature using RAZORPAY_KEY_SECRET.

import { serve } from "https://deno.land/std@0.177.0/http/server.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

async function hmacSha256Hex(secret: string, message: string): Promise<string> {
  const enc = new TextEncoder();
  const key = await crypto.subtle.importKey(
    "raw",
    enc.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign("HMAC", key, enc.encode(message));
  return Array.from(new Uint8Array(sig))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const keySecret = Deno.env.get("RAZORPAY_KEY_SECRET") ?? "";
    if (!keySecret) {
      return new Response(
        JSON.stringify({ verified: false, detail: "RAZORPAY_KEY_SECRET is not set on server." }),
        { status: 503, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const body = await req.json();
    const paymentId = String(body.razorpay_payment_id ?? "").trim();
    const linkId = String(body.razorpay_payment_link_id ?? "").trim();
    const refId = String(body.razorpay_payment_link_reference_id ?? "").trim();
    const statusValue = String(body.razorpay_payment_link_status ?? "").trim();
    const signature = String(body.razorpay_signature ?? "").trim();

    if (!paymentId || !linkId || !signature) {
      return new Response(
        JSON.stringify({ verified: false, detail: "Missing Razorpay callback fields." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    if (statusValue && statusValue !== "paid") {
      return new Response(
        JSON.stringify({ verified: false, detail: `Payment status is '${statusValue}', not paid.` }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const payload = `${linkId}|${refId}|${statusValue}|${paymentId}`;
    const expected = await hmacSha256Hex(keySecret, payload);

    if (expected !== signature.toLowerCase()) {
      return new Response(
        JSON.stringify({ verified: false, detail: "Invalid Razorpay signature." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    return new Response(
      JSON.stringify({ verified: true, payment_id: paymentId }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ verified: false, detail: err instanceof Error ? err.message : "Verification error." }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  }
});
