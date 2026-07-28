import { eq } from "drizzle-orm";
import { Router } from "express";
import { db, vpnServersTable, insertVpnServerSchema, updateVpnServerSchema } from "@workspace/db";

const router = Router();

// GET /api/servers
router.get("/", async (req, res) => {
  try {
    const servers = await db
      .select()
      .from(vpnServersTable)
      .orderBy(vpnServersTable.createdAt);
    res.json(servers);
  } catch (err) {
    req.log.error({ err }, "Failed to list servers");
    res.status(500).json({ error: "Internal server error" });
  }
});

// GET /api/servers/active  (must be before /:id)
router.get("/active", async (req, res) => {
  try {
    const results = await db
      .select()
      .from(vpnServersTable)
      .where(eq(vpnServersTable.isActive, true))
      .limit(1);
    res.json({ server: results[0] ?? null });
  } catch (err) {
    req.log.error({ err }, "Failed to get active server");
    res.status(500).json({ error: "Internal server error" });
  }
});

// GET /api/servers/:id
router.get("/:id", async (req, res) => {
  try {
    const results = await db
      .select()
      .from(vpnServersTable)
      .where(eq(vpnServersTable.id, req.params.id))
      .limit(1);
    if (results.length === 0) {
      res.status(404).json({ error: "Server not found" });
      return;
    }
    res.json(results[0]);
  } catch (err) {
    req.log.error({ err }, "Failed to get server");
    res.status(500).json({ error: "Internal server error" });
  }
});

// POST /api/servers
router.post("/", async (req, res) => {
  const parsed = insertVpnServerSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.message });
    return;
  }
  try {
    const inserted = await db
      .insert(vpnServersTable)
      .values(parsed.data)
      .returning();
    res.status(201).json(inserted[0]);
  } catch (err) {
    req.log.error({ err }, "Failed to create server");
    res.status(500).json({ error: "Internal server error" });
  }
});

// PUT /api/servers/:id
router.put("/:id", async (req, res) => {
  const parsed = updateVpnServerSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: parsed.error.message });
    return;
  }
  try {
    const updated = await db
      .update(vpnServersTable)
      .set({ ...parsed.data, updatedAt: new Date() })
      .where(eq(vpnServersTable.id, req.params.id))
      .returning();
    if (updated.length === 0) {
      res.status(404).json({ error: "Server not found" });
      return;
    }
    res.json(updated[0]);
  } catch (err) {
    req.log.error({ err }, "Failed to update server");
    res.status(500).json({ error: "Internal server error" });
  }
});

// DELETE /api/servers/:id
router.delete("/:id", async (req, res) => {
  try {
    const deleted = await db
      .delete(vpnServersTable)
      .where(eq(vpnServersTable.id, req.params.id))
      .returning();
    if (deleted.length === 0) {
      res.status(404).json({ error: "Server not found" });
      return;
    }
    res.status(204).send();
  } catch (err) {
    req.log.error({ err }, "Failed to delete server");
    res.status(500).json({ error: "Internal server error" });
  }
});

// POST /api/servers/:id/activate
router.post("/:id/activate", async (req, res) => {
  try {
    await db.update(vpnServersTable).set({ isActive: false });
    const activated = await db
      .update(vpnServersTable)
      .set({ isActive: true, updatedAt: new Date() })
      .where(eq(vpnServersTable.id, req.params.id))
      .returning();
    if (activated.length === 0) {
      res.status(404).json({ error: "Server not found" });
      return;
    }
    res.json(activated[0]);
  } catch (err) {
    req.log.error({ err }, "Failed to activate server");
    res.status(500).json({ error: "Internal server error" });
  }
});

export default router;
