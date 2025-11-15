const net = require('net');

if (process.argv.length < 5) {
  console.log('Usage: node tcp_tuning.js <host> <port> <message>');
  process.exit(0);
}

const host = process.argv[2];
const port = parseInt(process.argv[3], 10);
const message = process.argv[4];

const socket = net.connect({ host, port }, () => {
  socket.setNoDelay(true);
  socket.setKeepAlive(true, 30000);
  socket.setTimeout(10000);
  const start = Date.now();
  socket._startTime = start;
  socket.write(message + '\n');
});

socket.on('data', (data) => {
  const rtt = Date.now() - (socket._startTime || Date.now());
  console.log('Echo:', data.toString());
  console.log(`Approx RTT: ${rtt.toFixed(2)} ms`);
  socket.end();
});

socket.on('timeout', () => {
  console.error('Read timeout');
  socket.destroy();
});

socket.on('error', (err) => {
  console.error('Socket error:', err.message);
});
