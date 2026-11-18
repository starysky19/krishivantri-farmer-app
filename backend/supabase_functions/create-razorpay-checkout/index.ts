// Supabase Edge Function: create-razorpay-checkout
// Deploy: supabase functions deploy create-razorpay-checkout --no-verify-jwt
// Set secrets on Supabase (never in APK):
//   supabase secrets set RAZORPAY_KEY_ID=rzp_live_... RAZORPAY_KEY_SECRET=...

import { serve } from "https://deno.land/std@0.177.0/http/server.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const keyId = Deno.env.get("RAZORPAY_KEY_ID") ?? "";
    const keySecret = Deno.env.get("RAZORPAY_KEY_SECRET") ?? "";
    if (!keyId || !keySecret) {
      return new Response(
        JSON.stringify({
          error: "RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET must be set in Supabase Edge Function Secrets.",
        }),
        { status: 503, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const body = await req.json();
    const amountInr = Number(body.amount_inr ?? 0);
    if (!Number.isFinite(amountInr) || amountInr <= 0) {
      return new Response(
        JSON.stringify({ error: "amount_inr must be a positive number." }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const amountPaise = Math.max(100, Math.round(amountInr * 100));
    const referenceId = String(body.reference_id ?? `kv_${Date.now()}`).slice(0, 40);
    const callbackUrl = String(body.callback_url ?? "https://krishivantri.in/razorpay-callback");

    const rawPhone = String(body.customer_phone ?? "").trim();
    const customerPayload: Record<string, string> = {
      name: String(body.customer_name ?? "Krishivantri Farmer"),
    };
    if (rawPhone && rawPhone !== "+919999999999") {
      customerPayload.contact = rawPhone;
    }

    const authHeader = "Basic " + btoa(`${keyId}:${keySecret}`);
    const rzpResp = await fetch("https://api.razorpay.com/v1/payment_links", {
      method: "POST",
      headers: {
        Authorization: authHeader,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        amount: amountPaise,
        currency: "INR",
        accept_partial: false,
        description: String(body.description ?? "Krishivantri Drone Spray Booking").slice(0, 200),
        reference_id: referenceId,
        customer: customerPayload,
        notify: { sms: false, email: false },
        callback_url: callbackUrl,
        callback_method: "get",
      }),
    });

    const data = await rzpResp.json();
    if (!rzpResp.ok) {
      return new Response(
        JSON.stringify({ error: data?.error?.description ?? "Razorpay API error creating payment link." }),
        { status: rzpResp.status, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    return new Response(
      JSON.stringify({
        payment_link_id: data.id,
        checkout_url: data.short_url,
        callback_url: callbackUrl,
        reference_id: data.reference_id ?? referenceId,
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  } catch (err) {
    return new Response(
      JSON.stringify({ error: err instanceof Error ? err.message : "Unexpected payment server error." }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  }
});
