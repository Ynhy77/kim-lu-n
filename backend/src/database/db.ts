import fs from 'fs';
import path from 'path';

export interface AccessSession {
  id: string;
  installation_id: string;
  key_hash: string;
  attempts: number;
  used: number; // 0 or 1
  created_at: string;
  expires_at: string;
}

export interface LinkLog {
  id: string;
  installation_id: string;
  created_at: string;
}

const DB_FILE = path.join(__dirname, 'database.json');

class Database {
  private sessions: AccessSession[] = [];
  private logs: LinkLog[] = [];

  constructor() {
    this.load();
  }

  private load() {
    try {
      const dbDir = path.dirname(DB_FILE);
      if (!fs.existsSync(dbDir)) {
        fs.mkdirSync(dbDir, { recursive: true });
      }

      if (fs.existsSync(DB_FILE)) {
        const raw = fs.readFileSync(DB_FILE, 'utf-8');
        const data = JSON.parse(raw);
        this.sessions = data.sessions || [];
        this.logs = data.logs || [];
      } else {
        this.save();
      }
    } catch (e) {
      console.error('Failed to load database, initializing empty', e);
      this.sessions = [];
      this.logs = [];
    }
  }

  private save() {
    try {
      fs.writeFileSync(DB_FILE, JSON.stringify({
        sessions: this.sessions,
        logs: this.logs
      }, null, 2), 'utf-8');
    } catch (e) {
      console.error('Failed to save database', e);
    }
  }

  public getSession(id: string): AccessSession | undefined {
    this.load();
    return this.sessions.find(s => s.id === id);
  }

  public findSessionByKeyAndDevice(keyHash: string, installationId: string): AccessSession | undefined {
    this.load();
    return this.sessions.find(s => s.key_hash === keyHash && s.installation_id === installationId);
  }

  public createSession(session: AccessSession): void {
    this.load();
    this.sessions.push(session);
    this.save();
  }

  public updateSession(id: string, updates: Partial<AccessSession>): boolean {
    this.load();
    const idx = this.sessions.findIndex(s => s.id === id);
    if (idx !== -1) {
      this.sessions[idx] = { ...this.sessions[idx], ...updates };
      this.save();
      return true;
    }
    return false;
  }

  public logLinkGeneration(installationId: string): void {
    this.load();
    this.logs.push({
      id: Math.random().toString(36).substring(2, 11),
      installation_id: installationId,
      created_at: new Date().toISOString()
    });
    this.save();
  }

  public countLinkGenerationsToday(installationId: string): number {
    this.load();
    const startOfToday = new Date();
    startOfToday.setHours(0, 0, 0, 0);

    return this.logs.filter(l => 
      l.installation_id === installationId && 
      new Date(l.created_at).getTime() >= startOfToday.getTime()
    ).length;
  }
}

export const db = new Database();
