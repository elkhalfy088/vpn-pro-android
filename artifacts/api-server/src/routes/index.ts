import { Router } from "express";
import healthRouter from "./health.js";
import serversRouter from "./servers.js";

const router = Router();

router.use("/healthz", healthRouter);
router.use("/servers", serversRouter);

export default router;
