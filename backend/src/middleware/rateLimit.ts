import { Request, Response, NextFunction } from 'express';

interface RateLimitInfo {
  count: number;
  resetTime: number;
}

const ipCache = new Map<string, RateLimitInfo>();

export const rateLimiter = (req: Request, res: Response, next: NextFunction) => {
  const ip = req.ip || req.headers['x-forwarded-for'] as string || 'unknown';
  const now = Date.now();
  const WINDOW_MS = 60 * 1000; // 1 minute window
  const MAX_LIMIT = 5;

  const record = ipCache.get(ip);

  if (!record || now > record.resetTime) {
    // Initialize or reset limit
    ipCache.set(ip, {
      count: 1,
      resetTime: now + WINDOW_MS
    });
    return next();
  }

  if (record.count >= MAX_LIMIT) {
    return res.status(429).json({
      success: false,
      message: 'Quá nhiều yêu cầu. Vui lòng thử lại sau 1 phút (Tối đa 5 yêu cầu/phút).'
    });
  }

  record.count += 1;
  next();
};
