import { Router, Request, Response } from 'express';
import jwt from 'jsonwebtoken';
import { db } from '../database/db';
import { KeyService } from '../services/keyService';
import { authenticateToken, AuthenticatedRequest } from '../middleware/auth';
import { rateLimiter } from '../middleware/rateLimit';

const router = Router();

/**
 * GET /api/access/verify
 * This is the monetization landing page. User reaches this after solving the Link4m shortener.
 * It marks the session as `used = 1` allowing activation.
 */
router.get('/verify', (req: Request, res: Response) => {
  const { sessionId } = req.query;

  if (!sessionId || typeof sessionId !== 'string') {
    return res.status(400).send(`
      <html>
        <head>
          <title>Lỗi Xác Minh</title>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <style>
            body { font-family: sans-serif; background-color: #0b132b; color: #ffffff; text-align: center; padding: 50px 20px; }
            .card { background-color: #1c2541; padding: 30px; border-radius: 12px; max-width: 450px; margin: 0 auto; border: 1px solid #3a506b; }
            h1 { color: #ff5a5f; }
          </style>
        </head>
        <body>
          <div class="card">
            <h1>Lỗi Yêu Cầu</h1>
            <p>Mã phiên xác thực (sessionId) không hợp lệ hoặc không tồn tại.</p>
          </div>
        </body>
      </html>
    `);
  }

  const session = db.getSession(sessionId);

  if (!session) {
    return res.status(404).send(`
      <html>
        <head>
          <title>Phiên Không Tồn Tại</title>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <style>
            body { font-family: sans-serif; background-color: #0b132b; color: #ffffff; text-align: center; padding: 50px 20px; }
            .card { background-color: #1c2541; padding: 30px; border-radius: 12px; max-width: 450px; margin: 0 auto; border: 1px solid #3a506b; }
            h1 { color: #ff5a5f; }
          </style>
        </head>
        <body>
          <div class="card">
            <h1>Lỗi Xác Minh</h1>
            <p>Mã phiên truy cập không khớp với dữ liệu máy chủ.</p>
          </div>
        </body>
      </html>
    `);
  }

  const now = new Date();
  if (now.getTime() > new Date(session.expires_at).getTime()) {
    return res.status(400).send(`
      <html>
        <head>
          <title>Phiên Hết Hạn</title>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <style>
            body { font-family: sans-serif; background-color: #0b132b; color: #ffffff; text-align: center; padding: 50px 20px; }
            .card { background-color: #1c2541; padding: 30px; border-radius: 12px; max-width: 450px; margin: 0 auto; border: 1px solid #3a506b; }
            h1 { color: #ff5a5f; }
          </style>
        </head>
        <body>
          <div class="card">
            <h1>Phiên Đã Hết Hạn</h1>
            <p>Phiên xác thực này đã quá hạn 2 giờ. Vui lòng mở lại ứng dụng để lấy mã mới.</p>
          </div>
        </body>
      </html>
    `);
  }

  // Mark session as bypassed successfully
  db.updateSession(sessionId, { used: 1 });

  return res.status(200).send(`
    <html>
      <head>
        <title>Vượt Link Thành Công</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0d1b2a; color: #e0e1dd; text-align: center; padding: 40px 15px; margin: 0; }
          .container { max-width: 500px; margin: 0 auto; background-color: #1b263b; border-radius: 16px; padding: 40px 30px; box-shadow: 0 8px 32px rgba(0,0,0,0.3); border: 1px solid #415a77; }
          .icon { font-size: 64px; color: #00ebc7; margin-bottom: 20px; }
          h1 { color: #ffffff; margin-bottom: 10px; font-size: 24px; font-weight: 800; text-transform: uppercase; letter-spacing: 1px; }
          p { font-size: 15px; color: #a2aebb; line-height: 1.6; margin-bottom: 25px; }
          .btn { display: inline-block; background-color: #00ebc7; color: #0d1b2a; text-decoration: none; font-weight: bold; padding: 12px 30px; border-radius: 8px; font-size: 14px; text-transform: uppercase; box-shadow: 0 4px 15px rgba(0, 235, 199, 0.4); transition: transform 0.2s; }
          .btn:active { transform: scale(0.98); }
          .footer { margin-top: 30px; font-size: 11px; color: #778da9; }
        </style>
      </head>
      <body>
        <div class="container">
          <div class="icon">✓</div>
          <h1>Vượt Link Thành Công!</h1>
          <p>Hệ thống đã ghi nhận trạng thái hoàn tất của thiết bị. Bây giờ bạn có thể quay lại ứng dụng <b>AGM MD5 Analyzer</b>, nhập Key của bạn và nhấn nút <b>XÁC MINH KÍCH HOẠT</b> để mở khóa.</p>
          <a href="#" class="btn" onclick="window.close(); return false;">ĐÓNG TRANG NÀY</a>
          <div class="footer">AGM Network © 2026 - Bảo mật và chính xác</div>
        </div>
      </body>
    </html>
  `);
});

/**
 * POST /api/access/check
 * App calls this to activate. We look up session by key hash and installationId.
 */
router.post('/check', rateLimiter, (req: Request, res: Response) => {
  const { installationId, key } = req.body;

  if (!installationId || !key) {
    return res.status(400).json({
      success: false,
      message: 'Vui lòng cung cấp đầy đủ mã thiết bị (installationId) và Key kích hoạt.'
    });
  }

  try {
    const keyHash = KeyService.hashKey(key);

    // Load database and lookup session
    const matchedSession = db.findSessionByKeyAndDevice(keyHash, installationId);

    if (!matchedSession) {
      return res.status(400).json({
        success: false,
        message: 'Key không hợp lệ hoặc không thuộc về thiết bị này.'
      });
    }

    // Check wrong key entry locks
    if (matchedSession.attempts >= 5) {
      return res.status(403).json({
        success: false,
        message: 'Key này đã bị khóa vĩnh viễn do nhập sai quá 5 lần.'
      });
    }

    // Check expiration
    const now = new Date();
    if (now.getTime() > new Date(matchedSession.expires_at).getTime()) {
      return res.status(403).json({
        success: false,
        message: 'Key kích hoạt này đã hết hạn 2 giờ. Vui lòng tạo key mới.'
      });
    }

    // Check if they bypassed the link
    if (matchedSession.used !== 1) {
      // Increment wrong attempts as penalty
      const newAttempts = matchedSession.attempts + 1;
      db.updateSession(matchedSession.id, { attempts: newAttempts });

      return res.status(400).json({
        success: false,
        message: `Bạn chưa vượt link rút gọn Link4m để kích hoạt! (Sai sót lần ${newAttempts}/5)`
      });
    }

    // Generate JWT token
    const tokenSecret = process.env.JWT_SECRET || 'agm_md5_default_jwt_secret_2026';
    const remainingTimeSeconds = Math.max(
      60,
      Math.floor((new Date(matchedSession.expires_at).getTime() - now.getTime()) / 1000)
    );

    const token = jwt.sign(
      {
        installationId: matchedSession.installation_id,
        sessionId: matchedSession.id
      },
      tokenSecret,
      { expiresIn: remainingTimeSeconds } // Matches remaining session limit perfectly
    );

    return res.status(200).json({
      success: true,
      message: 'Kích hoạt thành công! Thiết bị đã được mở khóa.',
      token,
      expiresAt: matchedSession.expires_at
    });

  } catch (error: any) {
    console.error('Error verifying key:', error);
    return res.status(500).json({
      success: false,
      message: 'Đã xảy ra lỗi hệ thống khi kích hoạt.'
    });
  }
});

/**
 * POST /api/access/check-token
 * Verifies JWT token and checks if still active.
 */
router.post('/check-token', authenticateToken, (req: AuthenticatedRequest, res: Response) => {
  if (!req.user) {
    return res.status(401).json({ success: false, message: 'Xác thực thất bại.' });
  }

  // Retrieve matching session
  const session = db.getSession(req.user.sessionId);
  if (!session) {
    return res.status(401).json({ success: false, message: 'Mã phiên không tồn tại.' });
  }

  const now = new Date();
  if (now.getTime() > new Date(session.expires_at).getTime()) {
    return res.status(401).json({ success: false, message: 'Phiên sử dụng của key này đã hết hạn.' });
  }

  return res.status(200).json({
    success: true,
    message: 'Phiên kích hoạt vẫn hoạt động bình thường.',
    expiresAt: session.expires_at
  });
});

/**
 * POST /api/access/logout
 * Deactivates session immediately
 */
router.post('/logout', authenticateToken, (req: AuthenticatedRequest, res: Response) => {
  if (req.user) {
    db.updateSession(req.user.sessionId, { expires_at: new Date(0).toISOString() }); // Expirate immediately
  }
  return res.status(200).json({
    success: true,
    message: 'Đã đăng xuất và hủy phiên làm việc thành công.'
  });
});

export default router;
