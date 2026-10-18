// Edge Function notify-members (PRD §8.3).
// Dipanggil trigger `notes_notify_members` lewat pg_net; mengirim FCM data message ke HP anggota grup selain penulis,
// agar HP itu mengambil catatan terbaru dan memperbarui widget meski aplikasinya tertutup.
import { createClient } from "npm:@supabase/supabase-js@2.45.4";

interface NoteChange {
  note_id: string;
  group_id: string;
  author_id: string;
  is_new: boolean;
}

interface Recipient {
  token: string;
  /** Pengaturan notifikasi grup milik penerima; data message tetap dikirim agar widget ikut terbarui. */
  notify: boolean;
}

type SendResult = "sent" | "invalid" | "failed";

const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token";
const ACCESS_TOKEN_LIFETIME_SECONDS = 3600;
const ACCESS_TOKEN_REFRESH_MARGIN_SECONDS = 300;

const projectId = requireEnv("FCM_PROJECT_ID");
const serviceAccount = JSON.parse(requireEnv("FCM_SERVICE_ACCOUNT_JSON")) as {
  client_email: string;
  private_key: string;
};
const webhookSecret = requireEnv("NOTIFY_WEBHOOK_SECRET");
// Service role: fungsi ini perlu membaca token FCM anggota lain, yang tertutup RLS untuk pengguna biasa.
const supabase = createClient(requireEnv("SUPABASE_URL"), requireEnv("SUPABASE_SERVICE_ROLE_KEY"), {
  auth: { persistSession: false },
});

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("Method not allowed", { status: 405 });
  if (!constantTimeEquals(req.headers.get("x-webhook-secret") ?? "", webhookSecret)) {
    return new Response("Unauthorized", { status: 401 });
  }

  const change = (await req.json()) as NoteChange;
  const recipients = await findRecipients(change);
  if (recipients.length === 0) return Response.json({ sent: 0, removed: 0 });

  const accessToken = await getAccessToken();
  const results = await Promise.all(recipients.map((recipient) => sendToDevice(accessToken, recipient, change)));

  // Token yang tidak lagi terdaftar (aplikasi dihapus, data dibersihkan) dibuang agar tidak dicoba lagi.
  const invalidTokens = recipients.filter((_, i) => results[i] === "invalid").map((r) => r.token);
  if (invalidTokens.length > 0) {
    const { error } = await supabase.from("devices").delete().in("fcm_token", invalidTokens);
    if (error) console.error("Gagal menghapus token tidak valid", error);
  }

  return Response.json({
    sent: results.filter((r) => r === "sent").length,
    removed: invalidTokens.length,
  });
});

async function findRecipients(change: NoteChange): Promise<Recipient[]> {
  const { data: members, error: membersError } = await supabase
    .from("group_members")
    .select("user_id, notify")
    .eq("group_id", change.group_id)
    .neq("user_id", change.author_id);
  if (membersError) throw membersError;
  if (!members || members.length === 0) return [];

  const notifyByUser = new Map(members.map((m) => [m.user_id as string, m.notify as boolean]));
  const { data: devices, error: devicesError } = await supabase
    .from("devices")
    .select("fcm_token, user_id")
    .in("user_id", [...notifyByUser.keys()]);
  if (devicesError) throw devicesError;

  return (devices ?? []).map((d) => ({
    token: d.fcm_token as string,
    notify: notifyByUser.get(d.user_id as string) ?? false,
  }));
}

async function sendToDevice(accessToken: string, recipient: Recipient, change: NoteChange): Promise<SendResult> {
  const response = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
    method: "POST",
    headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      message: {
        token: recipient.token,
        // Prioritas tinggi agar HP dibangunkan dari Doze dan boleh menjalankan expedited work.
        android: { priority: "HIGH" },
        // Nilai data message FCM harus berupa string.
        data: {
          type: "note_changed",
          note_id: change.note_id,
          group_id: change.group_id,
          is_new: String(change.is_new),
          notify: String(recipient.notify),
        },
      },
    }),
  });
  if (response.ok) return "sent";

  const body = await response.text();
  if (isUnregisteredToken(response.status, body)) return "invalid";
  console.error(`FCM gagal (${response.status})`, body);
  return "failed";
}

function isUnregisteredToken(status: number, body: string): boolean {
  return (status === 404 && body.includes("UNREGISTERED")) ||
    (status === 400 && body.includes("registration token"));
}

let cachedAccessToken: { token: string; expiresAt: number } | null = null;

/** Token OAuth FCM dari service account (JWT RS256), disimpan selama instance fungsi masih hangat. */
async function getAccessToken(): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (cachedAccessToken && cachedAccessToken.expiresAt - ACCESS_TOKEN_REFRESH_MARGIN_SECONDS > now) {
    return cachedAccessToken.token;
  }

  const header = base64Url(new TextEncoder().encode(JSON.stringify({ alg: "RS256", typ: "JWT" })));
  const claims = base64Url(new TextEncoder().encode(JSON.stringify({
    iss: serviceAccount.client_email,
    scope: FCM_SCOPE,
    aud: OAUTH_TOKEN_URL,
    iat: now,
    exp: now + ACCESS_TOKEN_LIFETIME_SECONDS,
  })));
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToDer(serviceAccount.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${header}.${claims}`));

  const response = await fetch(OAUTH_TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${header}.${claims}.${base64Url(new Uint8Array(signature))}`,
    }),
  });
  if (!response.ok) throw new Error(`Token OAuth FCM gagal (${response.status}): ${await response.text()}`);

  const body = await response.json() as { access_token: string; expires_in: number };
  cachedAccessToken = { token: body.access_token, expiresAt: now + body.expires_in };
  return body.access_token;
}

function pemToDer(pem: string): ArrayBuffer {
  const base64 = pem.replace(/-----(BEGIN|END) PRIVATE KEY-----/g, "").replace(/\s/g, "");
  return Uint8Array.from(atob(base64), (c) => c.charCodeAt(0)).buffer;
}

function base64Url(bytes: Uint8Array): string {
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/** Perbandingan yang waktunya tidak bergantung pada posisi karakter pertama yang berbeda. */
function constantTimeEquals(a: string, b: string): boolean {
  const left = new TextEncoder().encode(a);
  const right = new TextEncoder().encode(b);
  let diff = left.length ^ right.length;
  for (let i = 0; i < Math.max(left.length, right.length); i++) diff |= (left[i] ?? 0) ^ (right[i] ?? 0);
  return diff === 0;
}

function requireEnv(name: string): string {
  const value = Deno.env.get(name);
  if (!value) throw new Error(`Env ${name} belum diisi`);
  return value;
}
