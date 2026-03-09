
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

            if (path === "/login" && request.method === "POST") {
                return handleLogin(request);
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
    // Migration: add new columns to existing tables (safe if already exist)
    const newCols = ['birthdate TEXT', 'residence TEXT', 'father_name TEXT', 'mother_name TEXT'];
    for (const col of newCols) {
        try { await env.DB.prepare(`ALTER TABLE users ADD COLUMN ${col}`).run(); } catch (e) { /* column exists */ }
    }
    return true;
}

// Handlers
async function handleLogin(request) {
    const { password } = await request.json();
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

async function handlePublicAlerts(env, corsHeaders) {
    const result = await env.DB.prepare("SELECT id, phone_number, latitude, longitude, created_at, governorate, area_detail, telegram_video_id FROM alerts ORDER BY created_at DESC LIMIT 500").all();
    return new Response(JSON.stringify(result.results), {
        headers: { ...corsHeaders, "Content-Type": "application/json" }
    });
}


function dashboardHtml() {
    return `<!DOCTYPE html>
<html dir="rtl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>لوحة التحكم الأمنية HerSafe</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        :root { --primary: #9c27b0; --dark: #1a102e; --bg: #f5f5f5; --text: #333; }
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; background: var(--bg); display: flex; flex-direction: column; height: 100vh; }
        header { background: var(--dark); color: white; padding: 0.8rem 2rem; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 2px 10px rgba(0,0,0,0.2); }
        .main-content { display: flex; flex: 1; overflow: hidden; padding: 15px; gap: 15px; }
        .sidebar { width: 400px; display: flex; flex-direction: column; gap: 15px; overflow-y: auto; }
        .map-container { flex: 1; position: relative; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
        #map { height: 100%; width: 100%; }
        .card { background: white; border-radius: 12px; padding: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
        .stats-item { text-align: center; }
        .stats-count { font-size: 2.5rem; font-weight: bold; color: #e91e63; }
        .filter-tabs { display: flex; gap: 10px; justify-content: center; margin: 15px 0; }
        .filter-tab { padding: 8px 20px; background: #eee; border: none; border-radius: 20px; cursor: pointer; transition: 0.3s; font-weight: 500; }
        .filter-tab.active { background: var(--primary); color: white; }
        .gov-list { max-height: 400px; overflow-y: auto; }
        .gov-item { padding: 12px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; cursor: pointer; transition: 0.2s; }
        .gov-item:hover { background: #f9f9f9; }
        .gov-badge { background: #e91e63; color: white; padding: 4px 12px; border-radius: 12px; font-size: 0.85rem; font-weight: bold; }
        .area-detail { display: none; }
        .area-detail.active { display: block; }
        .area-item { padding: 10px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; font-size: 0.9rem; }
        .back-btn { background: var(--primary); color: white; border: none; padding: 8px 16px; border-radius: 8px; cursor: pointer; margin-bottom: 10px; }
        ::-webkit-scrollbar { width: 6px; }
        ::-webkit-scrollbar-thumb { background: #ccc; border-radius: 10px; }
        /* Change default blue marker to dark pink using hue-rotate */
        img.leaflet-marker-icon {
            filter: hue-rotate(120deg) brightness(0.9);
        }
    </style>
</head>
<body>
    <header>
        <h2 style="margin:0">مركز عمليات HerSafe 🛰️</h2>
        <button onclick="document.cookie='auth=; Max-Age=0'; location.reload();" style="background:none; border:1px solid #fff; color:#fff; padding:6px 15px; border-radius:20px; cursor:pointer;">خروج</button>
    </header>

    <div class="main-content">
        <div class="sidebar">
            <div class="card stats-item">
                <h3 style="margin-top:0">التقارير حسب الفترة</h3>
                <div class="filter-tabs">
                    <button class="filter-tab active" onclick="setFilter('day')">يومي</button>
                    <button class="filter-tab" onclick="setFilter('week')">أسبوعي</button>
                    <button class="filter-tab" onclick="setFilter('month')">شهري</button>
                </div>
                <div id="filterCount" class="stats-count">0</div>
                <p style="color: #666; margin: 5px 0 0 0;">بلاغ في الفترة المحددة</p>
            </div>
            
            <div class="card">
                <div id="govView">
                    <h3>المحافظات حسب الخطورة</h3>
                    <div id="govList" class="gov-list"></div>
                </div>
                <div id="areaView" class="area-detail">
                    <button class="back-btn" onclick="showGovView()">← العودة للمحافظات</button>
                    <h3 id="areaTitle"></h3>
                    <div id="areaList"></div>
                </div>
            </div>
        </div>

        <div class="map-container">
            <div id="map"></div>
        </div>
    </div>

    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script>
        const map = L.map('map').setView([35.0, 38.0], 7);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap contributors'
        }).addTo(map);

        let allAlerts = [];
        let currentFilter = 'day';
        let currentGov = null;

        function isWithinTimeRange(dateStr, filter) {
            const date = new Date(dateStr);
            const now = new Date();
            const diff = now - date;
            const day = 24 * 60 * 60 * 1000;
            
            if (filter === 'day') return diff <= day;
            if (filter === 'week') return diff <= 7 * day;
            if (filter === 'month') return diff <= 30 * day;
            return true;
        }

        function setFilter(filter) {
            currentFilter = filter;
            document.querySelectorAll('.filter-tab').forEach(tab => tab.classList.remove('active'));
            event.target.classList.add('active');
            updateDisplay();
        }

        function updateDisplay() {
            const filtered = allAlerts.filter(a => isWithinTimeRange(a.created_at, currentFilter));
            document.getElementById('filterCount').innerText = filtered.length;

            const govCounts = {};
            filtered.forEach(alert => {
                const gov = alert.governorate || "غير محدد";
                govCounts[gov] = (govCounts[gov] || 0) + 1;
            });

            const sorted = Object.entries(govCounts).sort((a,b) => b[1] - a[1]);
            const govList = document.getElementById('govList');
            govList.innerHTML = '';
            sorted.forEach(([gov, count]) => {
                const div = document.createElement('div');
                div.className = 'gov-item';
                div.innerHTML = \`<span>\${gov}</span><span class="gov-badge">\${count} بلاغ</span>\`;
                div.onclick = () => showAreaDetail(gov, filtered);
                govList.appendChild(div);
            });

            // Update map
            map.eachLayer(layer => { if (layer instanceof L.Marker) map.removeLayer(layer); });
            filtered.forEach(alert => {
                const title = alert.area_detail || "موقع بلاغ";
                const videoInfo = alert.telegram_video_id 
                    ? \`<br><b>🎥 فيديو ID:</b> \${alert.telegram_video_id}\` 
                    : "";
                const googleMapsUrl = \`https://www.google.com/maps?q=\${alert.latitude},\${alert.longitude}\`;
                
                L.marker([alert.latitude, alert.longitude])
                 .addTo(map)
                 .bindPopup(\`
                    <div style="direction: rtl; min-width: 200px;">
                        <b style="color: #9c27b0;">\${title}</b><br>
                        <hr style="margin: 8px 0; border: none; border-top: 1px solid #eee;">
                        <b>📍 المحافظة:</b> \${alert.governorate || 'غير محدد'}<br>
                        <b>📱 الهاتف:</b> \${alert.phone_number || 'غير متوفر'}<br>
                        <b>📅 التاريخ:</b> \${new Date(alert.created_at).toLocaleDateString('ar-SA')}<br>
                        <b>⏰ الوقت:</b> \${new Date(alert.created_at).toLocaleTimeString('ar-SA')}\${videoInfo}
                        <hr style="margin: 8px 0; border: none; border-top: 1px solid #eee;">
                        <a href="\${googleMapsUrl}" target="_blank" style="display: block; text-align: center; background: #4285F4; color: white; padding: 8px; border-radius: 6px; text-decoration: none; margin-top: 5px;">🗺️ فتح في خرائط جوجل</a>
                    </div>
                \`);
            });
        }

        function showAreaDetail(gov, filtered) {
            currentGov = gov;
            const areaCounts = {};
            filtered.forEach(alert => {
                if ((alert.governorate || "غير محدد") === gov) {
                    const area = alert.area_detail || "منطقة غير محددة";
                    areaCounts[area] = (areaCounts[area] || 0) + 1;
                }
            });

            const sorted = Object.entries(areaCounts).sort((a,b) => b[1] - a[1]);
            document.getElementById('areaTitle').innerText = \`محافظة \${gov} - المناطق الأكثر خطراً\`;
            const areaList = document.getElementById('areaList');
            areaList.innerHTML = '';
            sorted.forEach(([area, count]) => {
                const div = document.createElement('div');
                div.className = 'area-item';
                div.innerHTML = \`<span>\${area}</span><span class="gov-badge">\${count} بلاغ</span>\`;
                areaList.appendChild(div);
            });

            document.getElementById('govView').style.display = 'none';
            document.getElementById('areaView').classList.add('active');
        }

        function showGovView() {
            document.getElementById('govView').style.display = 'block';
            document.getElementById('areaView').classList.remove('active');
        }

        async function fetchData() {
            const response = await fetch('/api/public/alerts');
            if (!response.ok) return;
            allAlerts = await response.json();
            updateDisplay();
        }

        fetchData();
        setInterval(fetchData, 15000);
    </script>
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
