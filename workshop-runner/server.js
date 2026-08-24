#!/usr/bin/env node

const http = require('http');
const { WebSocketServer } = require('ws');
const { spawn } = require('child_process');
const path = require('path');

const PORT = process.env.WS_PORT || 3001;
const CWD = process.argv[2] ? path.resolve(process.argv[2]) : process.cwd();

const server = http.createServer((req, res) => {
  res.writeHead(200);
  res.end('Workshop runner active');
});

const wss = new WebSocketServer({ server });

wss.on('connection', (ws) => {
  ws.on('message', (msg) => {
    const { id, command } = JSON.parse(msg);
    const shell = process.env.SHELL || '/bin/zsh';
    const proc = spawn(shell, ['-c', command], {
      cwd: CWD,
      env: { ...process.env, TERM: 'dumb' },
    });

    proc.stdout.on('data', (data) => {
      ws.send(JSON.stringify({ id, stream: 'stdout', data: data.toString() }));
    });
    proc.stderr.on('data', (data) => {
      ws.send(JSON.stringify({ id, stream: 'stderr', data: data.toString() }));
    });
    proc.on('close', (code) => {
      ws.send(JSON.stringify({ id, stream: 'exit', code }));
    });
    proc.on('error', (err) => {
      ws.send(JSON.stringify({ id, stream: 'error', data: err.message }));
    });
  });
});

server.listen(PORT, '127.0.0.1', () => {
  console.log(`Workshop runner listening on ws://localhost:${PORT}`);
  console.log(`Commands execute in: ${CWD}`);
});
