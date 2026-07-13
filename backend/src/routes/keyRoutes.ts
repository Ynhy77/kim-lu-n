import { Router, Response } from 'express';
import { db } from '../database/db';
import { KeyService } from '../services/keyService';
import { rateLimiter } from '../middleware/rateLimit';

const router = Router();

/**
 * POST /api/key/create
 * Generates a license key (AGM-XXXXXX) and a Link4m shortened validation link.
 * Limit: Max 10 keys per day per installation ID.
 */
router.post('/create', rateLimiter, async (req: any, res: Response) => {
  const { installationId } = req.body;

  if (!installationId || typeof installationId !== 'string') {
    return res.status(400).json({
      success: false,
      message: 'Thiếu hoặc sai định dạng mã thiết bị (installationId).'
    });
  }

  try {
    // Enforce rate limit: max 10 links per day per installation ID
    const generationsToday = db.countLinkGenerationsToday(installationId);
    if (generationsToday >= 10) {
      return res.status(429).json({
        success: false,
        message: 'Bạn đã đạt giới hạn tạo 10 key trong ngày hôm nay. Vui lòng quay lại vào ngày mai!'
      });
    }

    // Create session and link
    const sessionData = await KeyService.createKeySession(installationId);

    return res.status(200).json({
      success: true,
      key: sessionData.key,
      directUrl: sessionData.directUrl,
      bypassUrl: sessionData.bypassUrl,
      expiresAt: sessionData.expiresAt
    });

  } catch (error: any) {
    console.error('Error creating key session:', error);
    return res.status(500).json({
      success: false,
      message: 'Đã xảy ra lỗi hệ thống trong quá trình tạo key.'
    });
  }
});

export default router;
