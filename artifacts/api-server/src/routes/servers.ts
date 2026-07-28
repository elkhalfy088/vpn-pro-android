import { Router } from "express";
import { db } from "@workspace/db";
import { serversTable } from "@workspace/db";
import { eq } from "drizzle-orm";

const router = Router();

// GET /servers - List all servers
router.get("/servers", async (req, res) => {
  try {
    const servers = await db
      .select()
      .from(serversTable)
      .orderBy(serversTable.createdAt);
    res.json(servers);
  } catch (err) {
    req.log.error({ err }, "Failed to get servers");
    res.status(500).json({ error: "Failed to fetch servers" });
  }
});

// GET /servers/active - Get the active server
router.get("/servers/active", async (req, res) => {
  try {
    const rows = await db
      .select()
      .from(serversTable)
      .where(eq(serversTable.isActive, true))
      .limit(1);
    res.json({ server: rows[0] ?? null });
  } catch (err) {
    req.log.error({ err }, "Failed to get active server");
    res.status(500).json({ error: "Failed to fetch active server" });
  }
});

// GET /servers/:id - Get single server
router.get("/servers/:id", async (req, res) => {
  try {
    const id = parseInt(req.params.id);
    if (isNaN(id)) return res.status(400).json({ error: "Invalid server id" });

    const rows = await db
      .select()
      .from(serversTable)
      .where(eq(serversTable.id, id))
      .limit(1);

    if (!rows[0]) return res.status(404).json({ error: "Server not found" });
    res.json(rows[0]);
  } catch (err) {
    req.log.error({ err }, "Failed to get server");
    res.status(500).json({ error: "Failed to fetch server" });
  }
});

// POST /servers - Create server
router.post("/servers", async (req, res) => {
  try {
    const { name, type, host, port, username, password, config, flag, country } = req.body;

    if (!name || !type) {
      return res.status(400).json({ error: "name and type are required" });
    }

    const validTypes = ["v2ray", "proxy", "vmess", "vless", "trojan", "shadowsocks"];
    if (!validTypes.includes(type)) {
      return res.status(400).json({ error: "Invalid server type" });
    }

    const rows = await db
      .insert(serversTable)
      .values({
        name,
        type,
        host: host ?? null,
        port: port ?? null,
        username: username ?? null,
        password: password ?? null,
        config: config ?? null,
        flag: flag ?? null,
        country: country ?? null,
        isActive: false,
      })
      .returning();

    res.status(201).json(rows[0]);
  } catch (err) {
    req.log.error({ err }, "Failed to create server");
    res.status(500).json({ error: "Failed to create server" });
  }
});

// PATCH /servers/:id - Update server
router.patch("/servers/:id", async (req, res) => {
  try {
    const id = parseInt(req.params.id);
    if (isNaN(id)) return res.status(400).json({ error: "Invalid server id" });

    const { name, type, host, port, username, password, config, flag, country } = req.body;

    const existing = await db
      .select()
      .from(serversTable)
      .where(eq(serversTable.id, id))
      .limit(1);

    if (!existing[0]) return res.status(404).json({ error: "Server not found" });

    const updates: Record<string, unknown> = {};
    if (name !== undefined) updates.name = name;
    if (type !== undefined) updates.type = type;
    if (host !== undefined) updates.host = host;
    if (port !== undefined) updates.port = port;
    if (username !== undefined) updates.username = username;
    if (password !== undefined) updates.password = password;
    if (config !== undefined) updates.config = config;
    if (flag !== undefined) updates.flag = flag;
    if (country !== undefined) updates.country = country;

    const rows = await db
      .update(serversTable)
      .set(updates)
      .where(eq(serversTable.id, id))
      .returning();

    res.json(rows[0]);
  } catch (err) {
    req.log.error({ err }, "Failed to update server");
    res.status(500).json({ error: "Failed to update server" });
  }
});

// DELETE /servers/:id - Delete server
router.delete("/servers/:id", async (req, res) => {
  try {
    const id = parseInt(req.params.id);
    if (isNaN(id)) return res.status(400).json({ error: "Invalid server id" });

    const rows = await db
      .select()
      .from(serversTable)
      .where(eq(serversTable.id, id))
      .limit(1);

    if (!rows[0]) return res.status(404).json({ error: "Server not found" });

    await db.delete(serversTable).where(eq(serversTable.id, id));
    res.status(204).send();
  } catch (err) {
    req.log.error({ err }, "Failed to delete server");
    res.status(500).json({ error: "Failed to delete server" });
  }
});

// POST /servers/:id/activate - Activate a server
router.post("/servers/:id/activate", async (req, res) => {
  try {
    const id = parseInt(req.params.id);
    if (isNaN(id)) return res.status(400).json({ error: "Invalid server id" });

    const existing = await db
      .select()
      .from(serversTable)
      .where(eq(serversTable.id, id))
      .limit(1);

    if (!existing[0]) return res.status(404).json({ error: "Server not found" });

    // Deactivate all others
    await db.update(serversTable).set({ isActive: false });

    // Activate this one
    const rows = await db
      .update(serversTable)
      .set({ isActive: true })
      .where(eq(serversTable.id, id))
      .returning();

    res.json(rows[0]);
  } catch (err) {
    req.log.error({ err }, "Failed to activate server");
    res.status(500).json({ error: "Failed to activate server" });
  }
});

export default router;
