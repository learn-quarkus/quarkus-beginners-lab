(function () {
  var WS_URL = 'ws://localhost:3001';
  var ws = null;
  var cmdId = 0;

  function connect() {
    ws = new WebSocket(WS_URL);
    ws.onmessage = function (event) {
      var msg = JSON.parse(event.data);
      var output = document.getElementById('wr-output-' + msg.id);
      var btn = document.getElementById('wr-btn-' + msg.id);
      if (!output) return;

      if (msg.stream === 'stdout' || msg.stream === 'stderr') {
        output.style.display = 'block';
        var span = document.createElement('span');
        span.className = 'wr-' + msg.stream;
        span.textContent = msg.data;
        output.appendChild(span);
        output.scrollTop = output.scrollHeight;
      } else if (msg.stream === 'exit') {
        if (btn) {
          btn.disabled = false;
          btn.textContent = msg.code === 0 ? '✓ Done' : '✗ Failed';
          btn.className = 'wr-btn ' + (msg.code === 0 ? 'wr-success' : 'wr-error');
        }
      } else if (msg.stream === 'error') {
        output.style.display = 'block';
        var span = document.createElement('span');
        span.className = 'wr-stderr';
        span.textContent = msg.data;
        output.appendChild(span);
        if (btn) {
          btn.disabled = false;
          btn.textContent = '✗ Failed';
          btn.className = 'wr-btn wr-error';
        }
      }
    };
    ws.onclose = function () {
      setTimeout(connect, 2000);
    };
    ws.onerror = function () {};
  }

  function runCommand(id, command) {
    var btn = document.getElementById('wr-btn-' + id);
    var output = document.getElementById('wr-output-' + id);
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      output.style.display = 'block';
      output.innerHTML = '<span class="wr-stderr">Workshop runner not connected. Start it with: node workshop-runner/server.js</span>';
      return;
    }
    btn.disabled = true;
    btn.textContent = '⏳ Running...';
    btn.className = 'wr-btn wr-running';
    output.innerHTML = '';
    output.style.display = 'block';
    ws.send(JSON.stringify({ id: id, command: command }));
  }

  function addRunButtons() {
    // MkDocs Material renders ```bash as: <div class="language-bash highlight"><pre><code>...</code></pre></div>
    var wrappers = document.querySelectorAll(
      '.language-bash.highlight, .language-shell.highlight, .language-sh.highlight'
    );

    wrappers.forEach(function (wrapper) {
      if (wrapper.querySelector('.wr-bar')) return;

      var codeEl = wrapper.querySelector('pre code');
      if (!codeEl) return;

      var command = codeEl.textContent.trim();
      var id = cmdId++;

      var btnBar = document.createElement('div');
      btnBar.className = 'wr-bar';

      var btn = document.createElement('button');
      btn.id = 'wr-btn-' + id;
      btn.className = 'wr-btn';
      btn.textContent = '▶ Run';
      btn.onclick = function () { runCommand(id, command); };
      btnBar.appendChild(btn);

      var output = document.createElement('div');
      output.id = 'wr-output-' + id;
      output.className = 'wr-output';

      wrapper.style.position = 'relative';
      wrapper.appendChild(btnBar);
      wrapper.parentElement.insertBefore(output, wrapper.nextSibling);
    });
  }

  connect();

  // MkDocs Material uses instant loading — re-run on navigation
  if (typeof document$ !== 'undefined') {
    document$.subscribe(function () { cmdId = 0; addRunButtons(); });
  } else {
    document.addEventListener('DOMContentLoaded', addRunButtons);
  }
})();
