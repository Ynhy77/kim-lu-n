import crypto from 'crypto';
import { v4 as uuidv4 } from 'uuid';
import dotenv from 'dotenv';
import { db, AccessSession } from '../database/db';
import { Link4mService } from './link4mService';

dotenv.config();

export class KeyService {
  /**
   * Generate an HMAC-SHA256 hash of a key using the server secret
   */
  public static hashKey(key: string): string {
    const secret = process.env.KEY_HASH_SECRET || 'agm_md5_default_hash_secret_key_2026';
    return crypto.createHmac('sha256', secret).update(key).digest('hex');
  }

  /**
   * Generate a unique license key in format: AGM-XXXXXX
   * (6 random uppercase alphanumeric characters)
   */
  public static generateLicenseKey(): string {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let suffix = '';
    for (let i = 0; i < 6; i++) {
      const idx = crypto.randomInt(chars.length);
      suffix += chars.charAt(idx);
    }
    return `AGM-${suffix}`;
  }

  /**
   * Create a new activation key and its associated access session
   */
  public static async createKeySession(installationId: string): Promise<{
    key: string;
    sessionId: string;
    directUrl: string;
    bypassUrl: string;
    expiresAt: string;
  }> {
    const key = this.generateLicenseKey();
    const sessionId = uuidv4();
    const keyHash = this.hashKey(key);

    const now = new Date();
    const expiresAtDate = new Date(now.getTime() + 2 * 60 * 60 * 1000); // 2 hours validity

    const session: AccessSession = {
      id: sessionId,
      installation_id: installationId,
      key_hash: keyHash,
      attempts: 0,
      used: 0,
      created_at: now.toISOString(),
      expires_at: expiresAtDate.toISOString()
    };

    // Save session in db
    db.createSession(session);

    // Log link generation for rate limiting
    db.logLinkGeneration(installationId);

    // Build bypass urls
    const baseUrl = process.env.BASE_URL || 'http://localhost:3000';
    const directUrl = `${baseUrl}/api/access/verify?sessionId=${sessionId}`;

    // Shorten with Link4m shortener API
    const bypassUrl = await Link4mService.shortenUrl(directUrl);

    return {
      key,
      sessionId,
      directUrl,
      bypassUrl,
      expiresAt: expiresAtDate.toISOString()
    };
  }
}
