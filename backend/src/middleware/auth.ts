import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import dotenv from 'dotenv';

dotenv.config();

export interface AuthenticatedRequest extends Request {
  user?: {
    installationId: string;
    sessionId: string;
  };
}

export const authenticateToken = (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({
      success: false,
      message: 'Thiếu mã xác thực (Token). Vui lòng đăng nhập lại.'
    });
  }

  const secret = process.env.JWT_SECRET || 'agm_md5_default_jwt_secret_2026';

  jwt.verify(token, secret, (err, decoded: any) => {
    if (err) {
      return res.status(403).json({
        success: false,
        message: 'Mã xác thực không hợp lệ hoặc đã hết hạn.'
      });
    }

    req.user = {
      installationId: decoded.installationId,
      sessionId: decoded.sessionId
    };
    next();
  });
};
