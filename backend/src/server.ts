import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import path from 'path';
import keyRoutes from './routes/keyRoutes';
import accessRoutes from './routes/accessRoutes';

dotenv.config();

const app = express();
const port = process.env.PORT || 3000;

// Enable CORS so the app can talk to it
app.use(cors());

// Parse JSON request bodies
app.use(express.json());

// Serve static assets (landing page/dashboard)
app.use(express.static(path.join(__dirname, '../public')));

// Set up Router Mounts
app.use('/api/key', keyRoutes);
app.use('/api/access', accessRoutes);

/**
 * GET /api/health
 * Simple health status checker
 */
app.get('/api/health', (req, res) => {
  res.status(200).json({
    status: 'ok',
    service: 'agm-md5-analyzer-backend',
    timestamp: new Date().toISOString()
  });
});

// Start listening for incoming traffic
app.listen(port, () => {
  console.log(`=================================================`);
  console.log(` AGM MD5 FLOATING ANALYZER BACKEND STARTED       `);
  console.log(` Listening on port: ${port}                          `);
  console.log(` BASE_URL: ${process.env.BASE_URL || 'http://localhost:3000'} `);
  console.log(`=================================================`);
});
