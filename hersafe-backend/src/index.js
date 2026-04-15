
export default {
    async fetch(request, env, ctx) {
        const corsHeaders = {
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Methods": "GET, HEAD, POST, PUT, OPTIONS",
            "Access-Control-Allow-Headers": "Content-Type, Authorization, Cookie",
            "Access-Control-Allow-Credentials": "true",
        };

        if (request.method === "OPTIONS") {
            return new Response(null, { headers: corsHeaders });
        }

        const url = new URL(request.url);
        const path = url.pathname;

        try {
            await initDB(env);

            if (path === "/auth/verify" && request.method === "POST") {
                return handleVerify(request, env, corsHeaders);
            }

            if (path === "/auth/login" && request.method === "POST") {
                return handleAuthLogin(request, env, corsHeaders);
            }

            if (path === "/auth/register" && request.method === "POST") {
                return handleRegister(request, env, corsHeaders);
            }

            if (path === "/auth/profile" && request.method === "PUT") {
                return handleUpdateProfile(request, env, corsHeaders);
            }

            if (path === "/auth/password" && request.method === "PUT") {
                return handleChangePassword(request, env, corsHeaders);
            }

            if (path === "/api/report" && request.method === "POST") {
                return handleReport(request, env, corsHeaders);
            }

            if (path === "/auth/setup" && request.method === "POST") {
                await env.DB.prepare(`CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, email TEXT UNIQUE, phone TEXT, password_hash TEXT, api_token TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)`).run();
                await env.DB.prepare(`CREATE TABLE IF NOT EXISTS emergency_contacts (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, name TEXT, phone TEXT, FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)`).run();

                // [Force Reset] Drop table to apply new schema
                await env.DB.prepare("DROP TABLE IF EXISTS alerts").run();

                // Added telegram_video_id
                await env.DB.prepare(`CREATE TABLE IF NOT EXISTS alerts (id INTEGER PRIMARY KEY AUTOINCREMENT, phone_number TEXT, latitude REAL, longitude REAL, governorate TEXT, area_detail TEXT, telegram_video_id TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)`).run();
                return new Response(JSON.stringify({ success: true, message: "Database reset and initialized with new schema" }), {
                    headers: { ...corsHeaders, "Content-Type": "application/json" }
                });
            }

            if (path === "/api/data" && request.method === "GET") {
                if (!await isAuthenticated(request)) return new Response('Unauthorized', { status: 401 });
                return handleAdminData(env, corsHeaders);
            }

            if (path === "/api/ai/analysis" && request.method === "GET") {
                if (!await isAuthenticated(request)) return new Response('Unauthorized', { status: 401 });
                return handleAIAnalysis(env, corsHeaders);
            }

            if (path === "/api/ai/force" && request.method === "GET") {
                try {
                    await env.DB.prepare("DELETE FROM ai_analysis").run();
                    await runAIStep(env);
                    return new Response(JSON.stringify({ success: true, message: "Forced AI step completed." }), {
                        headers: { ...corsHeaders, "Content-Type": "application/json" }
                    });
                } catch (e) {
                    return new Response(JSON.stringify({ success: false, error: e.message }), {
                        status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" }
                    });
                }
            }

            if (path === "/login" && request.method === "POST") {
                const { password } = await request.json();
                if (password === "Admin123") {
                    ctx.waitUntil(runAIStep(env));
                    return new Response(JSON.stringify({ success: true }), {
                        headers: {
                            "Set-Cookie": "auth=true; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=86400",
                            "Content-Type": "application/json"
                        }
                    });
                }
                return new Response(JSON.stringify({ success: false }), { status: 401, headers: { "Content-Type": "application/json" } });
            }

            if (path === "/api/public/alerts" && request.method === "GET") {
                return handlePublicAlerts(env, corsHeaders);
            }

            if (path === "/api/admin/seed" && request.method === "POST") {
                return handleSeed(env, corsHeaders);
            }

            if (path === "/webhook/telegram" && request.method === "POST") {
                return handleTelegramWebhook(request, env, corsHeaders);
            }

            if (path === "/api/telegram/confirm" && request.method === "POST") {
                return handleTelegramConfirm(request, env, corsHeaders);
            }

            // Default: Render Dashboard or Login HTML
            return handleHtmlRequest(request);

        } catch (error) {
            return new Response(JSON.stringify({ success: false, error: error.message }), {
                status: 500,
                headers: { ...corsHeaders, "Content-Type": "application/json" }
            });
        }
    },
};

async function handleSeed(env, corsHeaders) {
    // [New] Real-world locations with accurate coordinates (approximate center)
    // [New] Real-world locations with accurate coordinates (approximate center)
    const REAL_LOCATIONS = [
        // --- HAMA (Detailed) ---
        { gov: "حماه", area: "حماه - الحاضر", lat: 35.1350, lng: 36.7580 },
        { gov: "حماه", area: "حماه - الحاضر (السوق)", lat: 35.1360, lng: 36.7590 },
        { gov: "حماه", area: "حماه - القصور", lat: 35.1250, lng: 36.7620 },
        { gov: "حماه", area: "حماه - القصور (عين الباد)", lat: 35.1230, lng: 36.7650 },
        { gov: "حماه", area: "حماه - الصابونية", lat: 35.1300, lng: 36.7550 },
        { gov: "حماه", area: "حماه - البارودية", lat: 35.1320, lng: 36.7520 },
        { gov: "حماه", area: "حماه - القلعة", lat: 35.1340, lng: 36.7510 },
        { gov: "حماه", area: "حماه - طريق حلب", lat: 35.1400, lng: 36.7600 },
        { gov: "حماه", area: "مصياف - المركز", lat: 35.0653, lng: 36.3421 },
        { gov: "حماه", area: "مصياف - القلعة", lat: 35.0660, lng: 36.3430 },
        { gov: "حماه", area: "سلمية - الساحة العامة", lat: 35.0113, lng: 37.0532 },
        { gov: "حماه", area: "سلمية - الحي الشرقي", lat: 35.0120, lng: 37.0550 },

        // --- DAMASCUS ---
        { gov: "دمشق", area: "المزة 86", lat: 33.5117, lng: 36.2629 },
        { gov: "دمشق", area: "المزة - فيلات", lat: 33.5050, lng: 36.2600 },
        { gov: "دمشق", area: "أبو رمانة", lat: 33.5180, lng: 36.2890 },
        { gov: "دمشق", area: "المالكي", lat: 33.5200, lng: 36.2862 },
        { gov: "دمشق", area: "الشعلان", lat: 33.5160, lng: 36.2940 },
        { gov: "دمشق", area: "باب توما", lat: 33.5126, lng: 36.3155 },
        { gov: "دمشق", area: "القصاع", lat: 33.5150, lng: 36.3200 },
        { gov: "دمشق", area: "جرمانا - الروضة", lat: 33.4869, lng: 36.3533 },

        // --- ALEPPO ---
        { gov: "حلب", area: "الموغامبو", lat: 36.2167, lng: 37.1333 },
        { gov: "حلب", area: "الشهباء الجديدة", lat: 36.2250, lng: 37.1200 },
        { gov: "حلب", area: "صلاح الدين", lat: 36.1900, lng: 37.1300 },
        { gov: "حلب", area: "الفرقان", lat: 36.2100, lng: 37.1100 },
        { gov: "حلب", area: "العزيزية", lat: 36.2050, lng: 37.1500 },

        // --- HOMS ---
        { gov: "حمص", area: "الوعر", lat: 34.7380, lng: 36.6850 },
        { gov: "حمص", area: "الإنشاءات", lat: 34.7150, lng: 36.7000 },
        { gov: "حمص", area: "الحمرا", lat: 34.7250, lng: 36.7100 },

        // --- LATAKIA ---
        { gov: "اللاذقية", area: "الزراعة", lat: 35.5350, lng: 35.7950 },
        { gov: "اللاذقية", area: "المشروع السابع", lat: 35.5450, lng: 35.7850 },
        { gov: "اللاذقية", area: "الصليبة", lat: 35.5200, lng: 35.7800 },
        { gov: "اللاذقية", area: "جبلة - الكورنيش", lat: 35.3615, lng: 35.9256 },

        // --- TARTUS ---
        { gov: "طرطوس", area: "الكورنيش", lat: 34.8950, lng: 35.8800 },
        { gov: "طرطوس", area: "الغمقة", lat: 34.8800, lng: 35.8950 },
        { gov: "طرطوس", area: "بانياس", lat: 35.1834, lng: 35.9458 },

        // --- DARAA ---
        { gov: "درعا", area: "درعا البلد", lat: 32.6100, lng: 36.1000 },
        { gov: "درعا", area: "درعا المحطة", lat: 32.6250, lng: 36.1050 }
    ];

    function randomDate() {
        const now = new Date();
        const r = Math.random();
        let past = new Date();
        if (r < 0.4) past.setHours(now.getHours() - Math.floor(Math.random() * 24)); // 40% Today
        else if (r < 0.7) past.setDate(now.getDate() - Math.floor(Math.random() * 6) - 1); // 30% This Week
        else past.setDate(now.getDate() - Math.floor(Math.random() * 23) - 7); // 30% This Month
        return past.toISOString().slice(0, 19).replace('T', ' ');
    }

    try {
        // [IMPORTANT] Reset table to apply new schema if needed (for dev/seed only)
        // In production, you would run a migration. Here we drop/recreate for simplicity if requested.
        // await env.DB.prepare("DROP TABLE IF EXISTS alerts").run();
        // await env.DB.prepare(`CREATE TABLE IF NOT EXISTS alerts (id INTEGER PRIMARY KEY AUTOINCREMENT, phone_number TEXT, latitude REAL, longitude REAL, governorate TEXT, area_detail TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)`).run();

        // We will just clear data and insert new format. If schema mismatch, 'setup' endpoint must be run.
        await env.DB.prepare("DELETE FROM alerts").run();

        const stmts = [];

        // Generate ~200 alerts distributed across these locations
        for (let i = 0; i < 200; i++) {
            const loc = REAL_LOCATIONS[Math.floor(Math.random() * REAL_LOCATIONS.length)];

            // Add small jitter to accurate coordinates (approx +/- 200m)
            const lat = loc.lat + (Math.random() - 0.5) * 0.004;
            const lng = loc.lng + (Math.random() - 0.5) * 0.004;

            const time = randomDate();
            const phone = '+9639' + Math.floor(Math.random() * 100000000).toString().padStart(8, '0');
            // Mock Video ID for 30% of seeded alerts
            const videoId = Math.random() < 0.3 ? "SEED_" + Math.floor(Math.random() * 100000) : null;

            stmts.push(env.DB.prepare("INSERT INTO alerts (phone_number, latitude, longitude, governorate, area_detail, telegram_video_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)")
                .bind(phone, lat, lng, loc.gov, loc.area, videoId, time));
        }

        // Batch execute
        const chunkSize = 20; // D1 limit is often lower for complex inserts
        for (let i = 0; i < stmts.length; i += chunkSize) {
            await env.DB.batch(stmts.slice(i, i + chunkSize));
        }

        return new Response(JSON.stringify({ success: true, count: stmts.length, message: "Seeded with real location names" }), {
            headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
    } catch (e) {
        return new Response(JSON.stringify({ success: false, error: e.message }), {
            status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
    }
}

// Security & Validation
async function isAuthenticated(request) {
    const cookie = request.headers.get("Cookie");
    return cookie && cookie.includes("auth=true");
}

async function verifyTelegram(data) {
    // In production, verify Telegram hash here
    return true;
}

async function initDB(env) {
    await env.DB.prepare(`CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, email TEXT UNIQUE, phone TEXT, password_hash TEXT, api_token TEXT, birthdate TEXT, residence TEXT, father_name TEXT, mother_name TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP)`).run();
    await env.DB.prepare(`CREATE TABLE IF NOT EXISTS ai_analysis (id INTEGER PRIMARY KEY AUTOINCREMENT, analysis_json TEXT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)`).run();
    const newCols = ['birthdate TEXT', 'residence TEXT', 'father_name TEXT', 'mother_name TEXT'];
    for (const col of newCols) {
        try { await env.DB.prepare(`ALTER TABLE users ADD COLUMN ${col}`).run(); } catch (e) { /* column exists */ }
    }
    return true;
}

// Handlers
async function handleLogin(data) {
    const { password } = data;
    // Simple hardcoded password for demo
    if (password === "Admin123") {
        return new Response(JSON.stringify({ success: true }), {
            headers: {
                "Set-Cookie": "auth=true; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=86400",
                "Content-Type": "application/json"
            }
        });
    }
    return new Response(JSON.stringify({ success: false }), { status: 401 });
}

async function handleVerify(request, env, corsHeaders) {
    const data = await request.json();
    // Simulate verification
    if (data.phone) {
        return new Response(JSON.stringify({ success: true, api_token: "mock-token-123" }), { headers: corsHeaders });
    }
    return new Response("Invalid data", { status: 400 });
}

async function handleRegister(request, env, corsHeaders) {
    const data = await request.json();
    const { name, email, phone, password, birthdate, residence, father_name, mother_name } = data;

    if (!email || !password) {
        return new Response(JSON.stringify({ success: false, error: "البريد الإلكتروني وكلمة المرور مطلوبان" }), {
            status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
    }

    // Check if email already exists
    const existing = await env.DB.prepare("SELECT id FROM users WHERE email = ?").bind(email).first();
    if (existing) {
        return new Response(JSON.stringify({ success: false, error: "البريد الإلكتروني مستخدم بالفعل" }), {
            status: 409, headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
    }

    // Hash password using SHA-256 (suitable for Cloudflare Workers)
    const encoder = new TextEncoder();
    const hashBuffer = await crypto.subtle.digest("SHA-256", encoder.encode(password));
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    const passwordHash = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');

    // Generate API token
    const tokenBuffer = new Uint8Array(32);
    crypto.getRandomValues(tokenBuffer);
    const apiToken = Array.from(tokenBuffer).map(b => b.toString(16).padStart(2, '0')).join('');

    // Insert user into database
    const result = await env.DB.prepare(
        "INSERT INTO users (name, email, phone, password_hash, birthdate, residence, father_name, mother_name, api_token) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
    ).bind(name || "", email, phone || "", passwordHash, birthdate || "", residence || "", father_name || "", mother_name || "", apiToken).run();

    const userId = result.meta.last_row_id;

    return new Response(JSON.stringify({
        success: true,
        api_token: apiToken,
        user: { id: userId, name: name || "", email, phone: phone || "", birthdate, residence, father_name, mother_name, api_token: apiToken }
    }), { headers: { ...corsHeaders, "Content-Type": "application/json" } });
}

async function handleAuthLogin(request, env, corsHeaders) {
    const data = await request.json();
    const { email, password } = data;

    if (!email || !password) {
        return new Response(JSON.stringify({ success: false, error: "البريد الإلكتروني وكلمة المرور مطلوبان" }), {
            status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
    }

    // Find user by email
    const user = await env.DB.prepare("SELECT * FROM users WHERE email = ?").bind(email).first();
    if (!user) {
        return new Response(JSON.stringify({ success: false, error: "بيانات الدخول غير صحيحة" }), {
            status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
    }

    // Verify password
    const encoder = new TextEncoder();
    const hashBuffer = await crypto.subtle.digest("SHA-256", encoder.encode(password));
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    const passwordHash = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');

    if (passwordHash !== user.password_hash) {
        return new Response(JSON.stringify({ success: false, error: "بيانات الدخول غير صحيحة" }), {
            status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
    }

    return new Response(JSON.stringify({
        success: true,
        user: { id: user.id, name: user.name, email: user.email, phone: user.phone, api_token: user.api_token }
    }), { headers: { ...corsHeaders, "Content-Type": "application/json" } });
}

async function handleUpdateProfile(request, env, corsHeaders) {
    const data = await request.json();
    const { api_token, name, phone, birthdate, residence, father_name, mother_name } = data;

    if (!api_token) {
        return new Response(JSON.stringify({ success: false, error: "التوكن مطلوب" }), {
            status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
    }

    // Find user by token
    const user = await env.DB.prepare("SELECT * FROM users WHERE api_token = ?").bind(api_token).first();
    if (!user) {
        return new Response(JSON.stringify({ success: false, error: "مستخدم غير موجود" }), {
            status: 404, headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
    }

    // Update all profile fields
    await env.DB.prepare("UPDATE users SET name = ?, phone = ?, birthdate = ?, residence = ?, father_name = ?, mother_name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
        .bind(name || user.name, phone || user.phone, birthdate || user.birthdate || '', residence || user.residence || '', father_name || user.father_name || '', mother_name || user.mother_name || '', user.id).run();

    return new Response(JSON.stringify({
        success: true,
        user: { id: user.id, name: name || user.name, email: user.email, phone: phone || user.phone, birthdate: birthdate || user.birthdate, residence: residence || user.residence, father_name: father_name || user.father_name, mother_name: mother_name || user.mother_name, api_token: user.api_token }
    }), { headers: { ...corsHeaders, "Content-Type": "application/json" } });
}

async function handleChangePassword(request, env, corsHeaders) {
    const data = await request.json();
    const { api_token, current_password, new_password } = data;

    if (!api_token) {
        return new Response(JSON.stringify({ success: false, error: "التوكن مطلوب" }), {
            status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
    }

    if (!current_password || !new_password) {
        return new Response(JSON.stringify({ success: false, error: "جميع الحقول مطلوبة" }), {
            status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
    }

    // Find user by token
    const user = await env.DB.prepare("SELECT * FROM users WHERE api_token = ?").bind(api_token).first();
    if (!user) {
        return new Response(JSON.stringify({ success: false, error: "مستخدم غير موجود" }), {
            status: 404, headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
    }

    // Verify current password
    const encoder = new TextEncoder();
    const currentHashBuffer = await crypto.subtle.digest("SHA-256", encoder.encode(current_password));
    const currentHashArray = Array.from(new Uint8Array(currentHashBuffer));
    const currentPasswordHash = currentHashArray.map(b => b.toString(16).padStart(2, '0')).join('');

    if (currentPasswordHash !== user.password_hash) {
        return new Response(JSON.stringify({ success: false, error: "كلمة المرور الحالية غير صحيحة" }), {
            status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
    }

    // Hash new password
    const newHashBuffer = await crypto.subtle.digest("SHA-256", encoder.encode(new_password));
    const newHashArray = Array.from(new Uint8Array(newHashBuffer));
    const newPasswordHash = newHashArray.map(b => b.toString(16).padStart(2, '0')).join('');

    // Update password in DB
    await env.DB.prepare("UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
        .bind(newPasswordHash, user.id).run();

    return new Response(JSON.stringify({ success: true }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" }
    });
}

async function handleReport(request, env, corsHeaders) {
    const { phone, lat, lng, video_id, governorate, area_detail } = await request.json();

    // Insert alert with comprehensive location details
    await env.DB.prepare("INSERT INTO alerts (phone_number, latitude, longitude, governorate, area_detail, telegram_video_id) VALUES (?, ?, ?, ?, ?, ?)")
        .bind(phone, lat, lng, governorate || "غير معروفة", area_detail || "غير معروفة", video_id || null).run();

    return new Response(JSON.stringify({ success: true }), { headers: corsHeaders });
}

async function handleAdminData(env, corsHeaders) {
    const result = await env.DB.prepare("SELECT id, phone_number, latitude, longitude, governorate, area_detail, telegram_video_id, created_at FROM alerts ORDER BY created_at DESC LIMIT 1000").all();
    const count = await env.DB.prepare("SELECT COUNT(*) as total FROM alerts WHERE created_at > datetime('now', '-1 day')").first();

    return new Response(JSON.stringify({
        alerts: result.results,
        stats: { today: count.total }
    }), { headers: { ...corsHeaders, "Content-Type": "application/json" } });
}

// Serve cached AI analysis
async function handleAIAnalysis(env, corsHeaders) {
    const cached = await env.DB.prepare("SELECT analysis_json, created_at FROM ai_analysis ORDER BY created_at DESC LIMIT 1").first();
    if (cached) {
        return new Response(cached.analysis_json, {
            headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
    }
    return new Response(JSON.stringify({ status: "pending", message: "جاري التحليل..." }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" }
    });
}

// AI Standardization + Analysis Logic (Google AI Studio - Gemini)
async function runAIStep(env) {
    console.log("Starting AI Standardization Step (Gemini)...");
    const apiKey = "AIzaSyDsgql_J0tLI2tULG2MsZxOues85WaLX-E";
    const model = "gemini-1.5-flash"; // Stable model name

    try {
        // Check if fresh analysis exists (< 12h)
        const cached = await env.DB.prepare("SELECT created_at FROM ai_analysis ORDER BY created_at DESC LIMIT 1").first();
        if (cached) {
            const age = Date.now() - new Date(cached.created_at + 'Z').getTime();
            if (age < 12 * 60 * 60 * 1000) {
                console.log("Fresh analysis exists, skipping AI call.");
                return;
            }
        }

        const result = await env.DB.prepare("SELECT id, latitude, longitude FROM alerts").all();
        const alerts = result.results;
        if (alerts.length === 0) return;

        // GROUP UNIQUE LOCATIONS
        const uniqueLocMap = new Map();
        alerts.forEach(a => {
            const key = `${a.latitude.toFixed(3)},${a.longitude.toFixed(3)}`;
            if (!uniqueLocMap.has(key)) {
                uniqueLocMap.set(key, { loc_id: key, lat: a.latitude, lng: a.longitude, alerts: [a.id] });
            } else {
                uniqueLocMap.get(key).alerts.push(a.id);
            }
        });

        const dataToProcess = Array.from(uniqueLocMap.values()).map(u => ({
            loc_id: u.loc_id, lat: u.lat, lng: u.lng
        }));

        const prompt = `أنت خبير في أمن المجتمع السوري ومحلل بيانات جغرافي دقيق جداً. أرسل لك المواقع الجغرافية (نطاقات) لتصنيفها.

المطلوب الأول (الفرز المكاني):
يجب أن تختار "المحافظة" (governorate) و "الحي/المنطقة" (area_detail) من قائمة المناطق السورية المعروفة حصراً لضمان الثبات. 
قائمة المحافظات: (دمشق، ريف دمشق، حلب، حمص، حماة، اللاذقية، طرطوس، إدلب، دير الزور، الرقة، الحسكة، درعا، السويداء، القنيطرة).

مثال لمناطق للمساعدة في القنص (Snapping):
- حماة: (الحاضر، السوق، الأربعين، جنوب الملعب، الكليات، بياض، طريق حلب).
- دمشق: (الميدان، المزة، أبورمانة، القصاع، باب توما، كفرسوسة، البرامكة).
- حمص: (الوعر، الإنشاءات، الحمراء، كرم الشامي، الخالدية، جورة الشياح).

المطلوب الثاني (التحليل الأمني):
1. صنف كل موقع (loc_id) للمحافظة والحي بدقة.
2. قدم تحليل وتقييم خطورة (1-5) يركز حصراً على أمان الفتيات (خطف، سرقة، مضايقات، أماكن مظلمة).

البيانات: ${JSON.stringify(dataToProcess)}

أعد الرد بتنسيق JSON حصراً بهذا الشكل:
{
  "locations": [{"loc_id": "...", "governorate": "حماة", "area_detail": "الحاضر"}],
  "summary": {
    "حماة": {
      "risk_level": 4,
      "risk_label": "مرتفع",
      "areas": {
        "الحاضر": {
          "risk": 3,
          "risk_label": "متوسط",
          "causes": "...",
          "recommendations": "..."
        }
      }
    }
  }
}`;

        console.log(`Sending to Gemini...`);

        const response = await fetch(`https://generativelanguage.googleapis.com/v1/models/${model}:generateContent?key=${apiKey}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                contents: [{ parts: [{ text: prompt }] }],
                generationConfig: { responseMimeType: "application/json" }
            })
        });

        if (!response.ok) {
            console.error("Gemini API Error:", await response.text());
            return;
        }

        const aiData = await response.json();
        const content = aiData.candidates[0].content.parts[0].text;
        console.log("Raw Gemini Response received.");
        const parsed = JSON.parse(content);
        console.log(`Parsed Gemini Response. Locations: ${parsed.locations ? parsed.locations.length : 0}`);

        // Map back to alerts
        if (parsed.locations && Array.isArray(parsed.locations)) {
            const stmts = [];
            parsed.locations.forEach(aiLoc => {
                const group = uniqueLocMap.get(aiLoc.loc_id);
                if (group) {
                    group.alerts.forEach(alertId => {
                        stmts.push(
                            env.DB.prepare("UPDATE alerts SET governorate = ?, area_detail = ? WHERE id = ?")
                            .bind(aiLoc.governorate, aiLoc.area_detail, alertId)
                        );
                    });
                } else {
                    console.log(`AI returned loc_id ${aiLoc.loc_id} but it was not found in our unique map.`);
                }
            });

            if (stmts.length > 0) {
                console.log(`Executing batch update for ${stmts.length} alerts...`);
                const chunkSize = 20;
                for (let i = 0; i < stmts.length; i += chunkSize) {
                    await env.DB.batch(stmts.slice(i, i + chunkSize));
                }
                console.log(`Successfully mapped and standardized individual alerts.`);
            }
        }

        // Add dynamic report counts back to the summary
        if (parsed.summary) {
            console.log("Processing summary with report counts...");
            Object.keys(parsed.summary).forEach(govKey => {
                let govTotal = 0;
                const areas = parsed.summary[govKey].areas || {};
                Object.keys(areas).forEach(areaKey => {
                    const count = alerts.filter(a => {
                        const key = `${a.latitude.toFixed(3)},${a.longitude.toFixed(3)}`;
                        const aiLoc = parsed.locations ? parsed.locations.find(l => l.loc_id === key) : null;
                        return aiLoc && aiLoc.governorate === govKey && aiLoc.area_detail === areaKey;
                    }).length;
                    
                    areas[areaKey].count = count;
                    govTotal += count;
                });
                parsed.summary[govKey].total = govTotal;
            });
            
            const finalJsonStr = JSON.stringify(parsed);
            console.log(`Saving final analysis to DB (JSON length: ${finalJsonStr.length})...`);
            await env.DB.prepare("DELETE FROM ai_analysis").run();
            await env.DB.prepare("INSERT INTO ai_analysis (analysis_json) VALUES (?)").bind(finalJsonStr).run();
            console.log("Gemini AI analysis cached successfully.");
        } else {
            console.log("No summary found in Gemini response.");
        }

    } catch (e) {
        console.error("AI Standardization Failed:", e.message);
    }
}

async function handlePublicAlerts(env, corsHeaders) {
    const result = await env.DB.prepare("SELECT id, phone_number, latitude, longitude, created_at, governorate, area_detail, telegram_video_id FROM alerts ORDER BY created_at DESC LIMIT 500").all();
    return new Response(JSON.stringify(result.results), {
        headers: { ...corsHeaders, "Content-Type": "application/json" }
    });
}

function dashboardHtml() {
    return `<!DOCTYPE html>
<html dir="rtl" lang="ar">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>مركز عمليات HerSafe</title>
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
<link href="https://fonts.googleapis.com/css2?family=Tajawal:wght@400;500;700;800&display=swap" rel="stylesheet">
<style>
*{margin:0;padding:0;box-sizing:border-box}
:root{--bg:#0f0a1e;--card:rgba(30,20,60,0.85);--glass:rgba(255,255,255,0.06);--primary:#b44adf;--accent:#e91e63;--accent2:#ff6090;--text:#e8e0f0;--muted:#9a8fb8;--green:#4caf50;--orange:#ff9800;--red:#f44336}
body{font-family:'Tajawal',sans-serif;background:var(--bg);color:var(--text);height:100vh;display:flex;flex-direction:column;overflow:hidden}
header{background:linear-gradient(135deg,#1a102e 0%,#2d1b4e 100%);padding:12px 24px;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid rgba(180,74,223,0.3)}
header h2{font-size:1.3rem;font-weight:700;background:linear-gradient(135deg,#e0b0ff,#ff6090);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
.logout-btn{background:rgba(255,255,255,0.08);border:1px solid rgba(255,255,255,0.15);color:#ccc;padding:6px 18px;border-radius:20px;cursor:pointer;transition:.3s;font-family:inherit}
.logout-btn:hover{background:var(--accent);color:#fff;border-color:var(--accent)}
.main{display:flex;flex:1;overflow:hidden;padding:12px;gap:12px}
.sidebar{width:420px;display:flex;flex-direction:column;gap:12px;overflow-y:auto;scrollbar-width:thin;scrollbar-color:var(--primary) transparent}
.sidebar::-webkit-scrollbar{width:5px}
.sidebar::-webkit-scrollbar-thumb{background:var(--primary);border-radius:10px}
.map-wrap{flex:1;border-radius:14px;overflow:hidden;border:1px solid rgba(180,74,223,0.2);position:relative}
#map{height:100%;width:100%}
.card{background:var(--card);backdrop-filter:blur(12px);border:1px solid rgba(180,74,223,0.15);border-radius:14px;padding:18px;transition:transform .2s}
.stats-card{text-align:center}
.stats-num{font-size:3rem;font-weight:800;background:linear-gradient(135deg,var(--accent),var(--accent2));-webkit-background-clip:text;-webkit-text-fill-color:transparent;line-height:1}
.stats-label{color:var(--muted);font-size:.85rem;margin-top:4px}
.tabs{display:flex;gap:8px;justify-content:center;margin:12px 0}
.tab{padding:6px 18px;background:var(--glass);border:1px solid rgba(255,255,255,0.1);border-radius:20px;cursor:pointer;color:var(--muted);font-weight:500;transition:.3s;font-family:inherit;font-size:.85rem}
.tab.active{background:var(--primary);color:#fff;border-color:var(--primary)}
.section-title{font-size:1rem;font-weight:700;margin-bottom:12px;display:flex;align-items:center;gap:8px}
.list-item{padding:12px 14px;border-bottom:1px solid rgba(255,255,255,0.05);display:flex;justify-content:space-between;align-items:center;cursor:pointer;transition:.2s;border-radius:8px;margin-bottom:2px}
.list-item:hover{background:rgba(180,74,223,0.12);transform:translateX(-3px)}
.badge{padding:4px 14px;border-radius:14px;font-size:.8rem;font-weight:700;color:#fff}
.badge-red{background:var(--red)}
.badge-orange{background:var(--orange)}
.badge-green{background:var(--green)}
.badge-accent{background:var(--accent)}
.risk-bar{height:6px;border-radius:3px;background:rgba(255,255,255,0.1);margin-top:6px;overflow:hidden}
.risk-fill{height:100%;border-radius:3px;transition:width .6s}
.back-btn{background:rgba(180,74,223,0.2);border:1px solid var(--primary);color:var(--primary);padding:6px 16px;border-radius:8px;cursor:pointer;font-family:inherit;font-weight:500;transition:.2s;margin-bottom:10px}
.back-btn:hover{background:var(--primary);color:#fff}
.detail-card{background:rgba(255,255,255,0.03);border:1px solid rgba(255,255,255,0.08);border-radius:12px;padding:14px;margin-top:10px}
.detail-label{color:var(--muted);font-size:.8rem;margin-bottom:4px}
.detail-value{font-size:.95rem;line-height:1.6}
.hidden{display:none}
.loading{text-align:center;padding:40px;color:var(--muted)}
.loading .spinner{width:40px;height:40px;border:3px solid var(--glass);border-top-color:var(--primary);border-radius:50%;animation:spin 1s linear infinite;margin:0 auto 12px}
@keyframes spin{to{transform:rotate(360deg)}}
.leaflet-popup-content-wrapper{background:var(--card)!important;color:var(--text)!important;border:1px solid rgba(180,74,223,0.3)!important;border-radius:12px!important;backdrop-filter:blur(10px)}
.leaflet-popup-tip{background:var(--card)!important}
img.leaflet-marker-icon{filter:hue-rotate(120deg) brightness(0.9)}
.pulse{animation:pulse 2s infinite}
@keyframes pulse{0%,100%{opacity:1}50%{opacity:.6}}
</style>
</head>
<body>
<header>
<h2>🛡️ مركز عمليات HerSafe</h2>
<button class="logout-btn" onclick="document.cookie='auth=; Max-Age=0'; location.reload();">خروج ←</button>
</header>
<div class="main">
<div class="sidebar">
<div class="card stats-card">
<div class="tabs">
<button class="tab active" onclick="setFilter('day',this)">يومي</button>
<button class="tab" onclick="setFilter('week',this)">أسبوعي</button>
<button class="tab" onclick="setFilter('month',this)">شهري</button>
</div>
<div class="stats-num" id="totalCount">0</div>
<div class="stats-label">بلاغ في الفترة المحددة</div>
</div>
<div class="card" id="navCard">
<div id="govView">
<div class="section-title">📊 المحافظات حسب الخطورة</div>
<div id="govList"></div>
</div>
<div id="areaView" class="hidden">
<button class="back-btn" onclick="showGovs()">← العودة للمحافظات</button>
<div class="section-title" id="areaTitle"></div>
<div id="areaList"></div>
</div>
<div id="detailView" class="hidden">
<button class="back-btn" id="detailBackBtn">← العودة للمناطق</button>
<div class="section-title" id="detailTitle"></div>
<div id="detailContent"></div>
</div>
</div>
<div id="aiStatus" class="card hidden">
<div class="loading"><div class="spinner"></div><span class="pulse">جاري تحليل البيانات بالذكاء الاصطناعي...</span></div>
</div>
</div>
<div class="map-wrap"><div id="map"></div></div>
</div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"><\/script>
<script>
const map=L.map('map').setView([35.0,38.0],7);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{attribution:'© OpenStreetMap'}).addTo(map);
let allAlerts=[],aiSummary=null,currentFilter='day',currentGov=null;
const DAY=86400000;
function age(d){return Date.now()-new Date(d).getTime()}
function inRange(d,f){const a=age(d);if(f==='day')return a<=DAY;if(f==='week')return a<=7*DAY;return a<=30*DAY}
function riskColor(r){if(r>=4)return'var(--red)';if(r>=3)return'var(--orange)';return'var(--green)'}
function riskBadge(r){if(r>=4)return'badge-red';if(r>=3)return'badge-orange';return'badge-green'}
function setFilter(f,el){currentFilter=f;document.querySelectorAll('.tab').forEach(t=>t.classList.remove('active'));el.classList.add('active');render()}
function render(){
const f=allAlerts.filter(a=>inRange(a.created_at,currentFilter));
document.getElementById('totalCount').textContent=f.length;
map.eachLayer(l=>{if(l instanceof L.Marker)map.removeLayer(l)});
f.forEach(a=>{
const t=a.area_detail||'بلاغ';
L.marker([a.latitude,a.longitude]).addTo(map).bindPopup('<div style="direction:rtl;min-width:180px"><b style="color:var(--primary)">'+t+'</b><br><b>📍</b> '+(a.governorate||'-')+'<br><b>📱</b> '+(a.phone_number||'-')+'<br><b>📅</b> '+new Date(a.created_at).toLocaleDateString('ar-SA')+'</div>');
});
if(aiSummary){renderGovs()}else{renderGovsFromData(f)}
}
function renderGovsFromData(f){
const gc={};f.forEach(a=>{const g=a.governorate||'غير محدد';gc[g]=(gc[g]||0)+1});
const s=Object.entries(gc).sort((a,b)=>b[1]-a[1]);
const el=document.getElementById('govList');el.innerHTML='';
s.forEach(([g,c])=>{el.innerHTML+='<div class="list-item" onclick="showAreasData(\\''+g+'\\')"><span>'+g+'</span><span class="badge badge-accent">'+c+' بلاغ</span></div>'});
showPanel('govView');
}
function renderGovs(){
const el=document.getElementById('govList');el.innerHTML='';
const govs=Object.entries(aiSummary).sort((a,b)=>(b[1].risk_level||0)-(a[1].risk_level||0));
govs.forEach(([g,d])=>{
const r=d.risk_level||1;
el.innerHTML+='<div class="list-item" onclick="showAreas(\\''+g+'\\')"><div><div style="font-weight:600">'+g+'</div><div class="risk-bar" style="width:120px"><div class="risk-fill" style="width:'+r*20+'%;background:'+riskColor(r)+'"></div></div></div><div style="text-align:left"><span class="badge '+riskBadge(r)+'">'+(d.risk_label||r)+' </span><div style="font-size:.75rem;color:var(--muted);margin-top:2px">'+d.total+' بلاغ</div></div></div>'});
showPanel('govView');
}
function showAreas(g){
currentGov=g;
const gd=aiSummary[g];if(!gd)return;
document.getElementById('areaTitle').innerHTML='📍 '+g+' - المناطق';
const el=document.getElementById('areaList');el.innerHTML='';
const areas=Object.entries(gd.areas||{}).sort((a,b)=>(b[1].risk||0)-(a[1].risk||0));
areas.forEach(([a,d])=>{
const r=d.risk||1;
el.innerHTML+='<div class="list-item" onclick="showDetail(\\''+g+'\\',\\''+a+'\\')"><div><div style="font-weight:500">'+a+'</div></div><div style="text-align:left"><span class="badge '+riskBadge(r)+'">'+d.count+' بلاغ</span></div></div>'});
showPanel('areaView');
}
function showAreasData(g){
currentGov=g;
const f=allAlerts.filter(a=>inRange(a.created_at,currentFilter)&&a.governorate===g);
const ac={};f.forEach(a=>{const ar=a.area_detail||'غير محدد';ac[ar]=(ac[ar]||0)+1});
document.getElementById('areaTitle').innerHTML='📍 '+g+' - المناطق';
const el=document.getElementById('areaList');el.innerHTML='';
Object.entries(ac).sort((a,b)=>b[1]-a[1]).forEach(([a,c])=>{
el.innerHTML+='<div class="list-item"><span>'+a+'</span><span class="badge badge-accent">'+c+' بلاغ</span></div>'});
showPanel('areaView');
}
function showDetail(g,a){
const gd=aiSummary[g];if(!gd||!gd.areas||!gd.areas[a])return;
const d=gd.areas[a];const r=d.risk||1;
document.getElementById('detailTitle').innerHTML='🔍 '+a;
document.getElementById('detailBackBtn').onclick=()=>showAreas(g);
document.getElementById('detailContent').innerHTML=
'<div style="display:flex;gap:10px;margin-bottom:12px"><div class="card" style="flex:1;text-align:center;padding:12px"><div style="font-size:2rem;font-weight:800;color:var(--accent)">'+d.count+'</div><div style="font-size:.75rem;color:var(--muted)">بلاغ</div></div><div class="card" style="flex:1;text-align:center;padding:12px"><div style="font-size:2rem;font-weight:800;color:'+riskColor(r)+'">'+r+'/5</div><div style="font-size:.75rem;color:var(--muted)">'+(d.risk_label||'مستوى الخطورة')+'</div></div></div>'+
'<div class="detail-card"><div class="detail-label">⚠️ الأسباب المحتملة</div><div class="detail-value">'+(d.causes||'لا تتوفر بيانات')+'</div></div>'+
'<div class="detail-card" style="margin-top:8px"><div class="detail-label">✅ التوصيات الأمنية</div><div class="detail-value">'+(d.recommendations||'لا تتوفر بيانات')+'</div></div>';
showPanel('detailView');
}
function showGovs(){if(aiSummary)renderGovs();else render();showPanel('govView')}
function showPanel(id){['govView','areaView','detailView'].forEach(v=>{document.getElementById(v).classList.add('hidden')});document.getElementById(id).classList.remove('hidden')}
async function fetchData(){
const r=await fetch('/api/public/alerts');if(!r.ok)return;allAlerts=await r.json();render()}
async function fetchAI(){
const r=await fetch('/api/ai/analysis');if(!r.ok)return;
const d=await r.json();
if(d.summary){aiSummary=d.summary;document.getElementById('aiStatus').classList.add('hidden');render()}
else if(d.status==='pending'){document.getElementById('aiStatus').classList.remove('hidden');setTimeout(fetchAI,5000)}
}
fetchData();fetchAI();setInterval(fetchData,30000);
<\/script>
</body>
</html>`;
}

async function handleHtmlRequest(request) {
    if (await isAuthenticated(request)) {
        return new Response(dashboardHtml(), { headers: { "Content-Type": "text/html;charset=UTF-8" } });
    } else {
        return new Response(`<!DOCTYPE html>
<html dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>تسجيل الدخول - HerSafe</title>
    <style>
        body { font-family: 'Segoe UI', sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; background: #f5f5f5; margin: 0; }
        .login-box { background: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); text-align: center; width: 300px; }
        input { width: 100%; padding: 10px; margin: 10px 0; border: 1px solid #ddd; border-radius: 6px; box-sizing: border-box; }
        button { width: 100%; padding: 10px; background: #9c27b0; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; }
        button:hover { background: #7b1fa2; }
    </style>
</head>
<body>
    <div class="login-box">
        <h2 style="color: #9c27b0;">HerSafe Admin 🔒</h2>
        <input type="password" id="password" placeholder="كلمة المرور">
        <button onclick="login()">دخول</button>
        <p id="error" style="color: red; display: none; margin-top: 10px;">كلمة المرور غير صحيحة</p>
    </div>
    <script>
        async function login() {
            const pass = document.getElementById('password').value;
            const res = await fetch('/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ password: pass })
            });
            if (res.ok) location.reload();
            else document.getElementById('error').style.display = 'block';
        }
    </script>
</body>
</html>`, { headers: { "Content-Type": "text/html;charset=UTF-8" } });
    }
}

async function handleTelegramWebhook(request, env, corsHeaders) {
    try {
        const payload = await request.json();

        // Ensure it's a message with text
        if (payload && payload.message && payload.message.text) {
            const text = payload.message.text;
            const chatId = payload.message.chat.id;

            if (text.startsWith('/start')) {
                // The bot token provided by the user
                const botToken = "8503140381:AAGtXY0pX8OsHvEKS92t3th8cz1KPNiPPbw"; 

                // Generate QR Code URL with Chat ID
                const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=${chatId}`;
                
                // Welcome message containing the Chat ID explicitly
                const messageText = `مرحباً بك في نظام HerSafe التنبيهي.\n\nيرجى مسح رمز الـ QR أدناه عبر كاميرا التطبيق الأساسي في هاتف الابنة لإتمام عملية الربط بشكل آمن.\n\nمعرف المحادثة الخاص بك (Chat ID): ${chatId}`;

                // Call Telegram API directly from Cloudflare Worker
                await fetch(`https://api.telegram.org/bot${botToken}/sendPhoto`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        chat_id: chatId,
                        photo: qrUrl,
                        caption: messageText
                    })
                });
            }
        }
    } catch (e) {
        // Log locally if possible, but don't crash
        console.error("Telegram Webhook processing error:", e);
    }
    
    // Always return 200 OK to Telegram so it stops retrying the webhook Delivery
    return new Response("OK", { status: 200, headers: corsHeaders });
}

async function handleTelegramConfirm(request, env, corsHeaders) {
    try {
        const data = await request.json();
        const chatId = data.chat_id;

        if (!chatId) {
            return new Response(JSON.stringify({ success: false, error: "chat_id is required" }), {
                status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" }
            });
        }

        const botToken = "8503140381:AAGtXY0pX8OsHvEKS92t3th8cz1KPNiPPbw";
        const messageText = "✅ تم ربط تطبيق HerSafe بجهاز الابنة بنجاح! أنت الآن جاهز لاستقبال تنبيهات الطوارئ والبث المباشر.";

        const response = await fetch(`https://api.telegram.org/bot${botToken}/sendMessage`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                chat_id: chatId,
                text: messageText
            })
        });

        const tgResult = await response.json();
        if (tgResult.ok) {
            return new Response(JSON.stringify({ success: true }), { headers: { ...corsHeaders, "Content-Type": "application/json" } });
        } else {
            return new Response(JSON.stringify({ success: false, error: "Telegram API failed", details: tgResult }), {
                status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" }
            });
        }
    } catch (e) {
        return new Response(JSON.stringify({ success: false, error: e.message }), {
            status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
    }
}

