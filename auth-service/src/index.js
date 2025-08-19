import express from 'express';
import bodyParser from 'body-parser';
import jwt from 'jsonwebtoken';
import bcrypt from 'bcryptjs';
import pg from 'pg';
import dotenv from 'dotenv';
import cors from 'cors';

dotenv.config();

const app = express();
app.use(cors());
app.use(bodyParser.json());

const {
  AUTH_SERVICE_PORT = 8081,
  DB_URL = 'postgres://postgres:postgres@postgres:5432/shecancode',
  JWT_SECRET = 'ZmFrZVN1cGVyU2VjcmV0S2V5Rm9yRGVtby0xMjM0NTY3ODkw',
  JWT_EXP_MINUTES = 240,
} = process.env;

const pool = new pg.Pool({ connectionString: DB_URL });

function signToken(user) {
  const payload = { role: user.role };
  const opts = { expiresIn: `${JWT_EXP_MINUTES}m` };
  // IMPORTANT: subject is the email (to match leave-service JwtService change)
  return jwt.sign(payload, Buffer.from(JWT_SECRET, 'base64'), { ...opts, subject: user.email });
}

app.post('/api/auth/login', async (req, res) => {
  const { email, password } = req.body || {};
  if (!email || !password) return res.status(400).json({ error: 'Email and password required' });
  try {
    const { rows } = await pool.query('SELECT id, email, password_hash, full_name, role, google_avatar_url FROM users WHERE email=$1', [email]);
    const user = rows[0];
    if (!user) return res.status(401).json({ error: 'Invalid credentials' });
    const ok = await bcrypt.compare(password, user.password_hash);
    if (!ok) return res.status(401).json({ error: 'Invalid credentials' });
    const token = signToken({ email: user.email, role: user.role });
    return res.json({
      token,
      role: user.role,
      fullName: user.full_name,
      email: user.email,
      avatarUrl: user.google_avatar_url,
    });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

app.post('/api/auth/register', async (req, res) => {
  const { fullName, email, password } = req.body || {};
  if (!fullName || !email || !password) return res.status(400).json({ error: 'Missing fields' });
  try {
    const { rows: existing } = await pool.query('SELECT 1 FROM users WHERE email=$1', [email]);
    if (existing.length) return res.status(400).json({ error: 'Email already registered' });
    const hash = await bcrypt.hash(password, 10);
    const avatar = `https://www.gravatar.com/avatar/${(email).split('').reduce((a,c)=>a+c.charCodeAt(0),0).toString(16)}`;
    const role = 'STAFF';
    await pool.query(
      'INSERT INTO users (email, password_hash, full_name, role, google_avatar_url, created_at, two_factor_enabled) VALUES ($1,$2,$3,$4,$5, now(), false)',
      [email, hash, fullName, role, avatar]
    );
    const token = signToken({ email, role });
    return res.json({ token, role, fullName, email, avatarUrl: avatar });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Server error' });
  }
});

app.get('/api/auth/health', (_req, res) => res.json({ status: 'ok' }));

app.listen(AUTH_SERVICE_PORT, () => {
  console.log(`auth-service listening on ${AUTH_SERVICE_PORT}`);
});
