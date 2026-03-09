const fs = require('fs');

const centers = {
    // Heavy Traffic
    "Hama": { lat: 35.1318, lng: 36.7578, count: 40, spread: 0.05 }, // ~5km radius
    "Damascus": { lat: 33.5138, lng: 36.2765, count: 25, spread: 0.08 },
    "Aleppo": { lat: 36.2021, lng: 37.1343, count: 20, spread: 0.06 },
    "Homs": { lat: 34.7324, lng: 36.7137, count: 15, spread: 0.05 },

    // Moderate
    "Latakia": { lat: 35.5317, lng: 35.7901, count: 10, spread: 0.04 },
    "Tartus": { lat: 34.8890, lng: 35.8866, count: 10, spread: 0.04 },
    "Idlib": { lat: 35.9306, lng: 36.6339, count: 10, spread: 0.04 },

    // Light
    "Daraa": { lat: 32.6184, lng: 36.1014, count: 5, spread: 0.03 },
    "Suwayda": { lat: 32.7090, lng: 36.5658, count: 5, spread: 0.03 },
    "DeirEzZor": { lat: 35.3364, lng: 40.1458, count: 5, spread: 0.05 },
    "Hasakah": { lat: 36.5074, lng: 40.7380, count: 5, spread: 0.05 },
    "Raqqa": { lat: 35.9525, lng: 39.0089, count: 5, spread: 0.05 },
    "Quneitra": { lat: 33.1264, lng: 35.8239, count: 2, spread: 0.02 }
};

function randomDate() {
    const now = new Date();
    const r = Math.random();
    let past = new Date();

    if (r < 0.35) {
        // Today (Last 24h)
        past.setHours(now.getHours() - Math.floor(Math.random() * 24));
    } else if (r < 0.65) {
        // This Week (1-7 days ago)
        past.setDate(now.getDate() - Math.floor(Math.random() * 6) - 1);
    } else {
        // This Month (7-30 days ago)
        past.setDate(now.getDate() - Math.floor(Math.random() * 23) - 7);
    }
    return past.toISOString().slice(0, 19).replace('T', ' '); // YYYY-MM-DD HH:MM:SS
}

function generateSQL() {
    let sql = "DELETE FROM alerts;\n"; // Clear old data for clean state
    sql += "INSERT INTO alerts (phone_number, latitude, longitude, created_at) VALUES\n";

    const values = [];

    for (const [city, data] of Object.entries(centers)) {
        for (let i = 0; i < data.count; i++) {
            // Random Jitter
            const lat = data.lat + (Math.random() - 0.5) * data.spread;
            const lng = data.lng + (Math.random() - 0.5) * data.spread;
            const time = randomDate();
            const phone = '+9639' + Math.floor(Math.random() * 100000000).toString().padStart(8, '0');

            values.push(`('${phone}', ${lat.toFixed(6)}, ${lng.toFixed(6)}, '${time}')`);
        }
    }

    sql += values.join(",\n") + ";";
    return sql;
}

fs.writeFileSync('seed.sql', generateSQL());
console.log("seed.sql generated!");
