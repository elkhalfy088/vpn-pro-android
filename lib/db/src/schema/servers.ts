import { boolean, pgTable, text, timestamp, uuid, real } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod/v4";

export const serverTypeEnum = [
  "v2ray",
  "proxy",
  "socks5",
  "shadowsocks",
  "trojan",
  "hysteria2",
  "vless",
  "custom",
] as const;

export const vpnServersTable = pgTable("vpn_servers", {
  id: uuid("id").primaryKey().defaultRandom(),
  name: text("name").notNull(),
  type: text("type", { enum: serverTypeEnum }).notNull(),
  config: text("config").notNull(),
  country: text("country").notNull(),
  flag: text("flag").notNull(),
  ping: real("ping"),
  isActive: boolean("is_active").notNull().default(false),
  note: text("note"),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
  updatedAt: timestamp("updated_at", { withTimezone: true }).notNull().defaultNow(),
});

export const insertVpnServerSchema = createInsertSchema(vpnServersTable).omit({
  id: true,
  createdAt: true,
  updatedAt: true,
  isActive: true,
  ping: true,
});

export const updateVpnServerSchema = insertVpnServerSchema.partial();

export type InsertVpnServer = z.infer<typeof insertVpnServerSchema>;
export type UpdateVpnServer = z.infer<typeof updateVpnServerSchema>;
export type VpnServer = typeof vpnServersTable.$inferSelect;
