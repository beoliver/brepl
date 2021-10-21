var preplPort = 8888;
window.addEventListener("load", function (evt) {
  var output = document.getElementById("output");
  var input = document.getElementById("input");
  var ws;
  var print = function (message) {
    var d = document.createElement("pre");
    d.textContent = message;
    output.appendChild(d);
    output.scroll(0, output.scrollHeight);
  };
  document.getElementById("open").onclick = function (evt) {
    if (ws) {
      return false;
    }
    ws = new WebSocket(`ws://${window.location.host}/prepl/${preplPort}`);
    ws.onopen = function (evt) {
      print("OPEN");
    };
    ws.onclose = function (evt) {
      print("CLOSE");
      ws = null;
    };
    ws.onmessage = function (evt) {
      print("RESPONSE: " + evt.data);
    };
    ws.onerror = function (evt) {
      print("ERROR: " + evt.data);
    };
    return false;
  };
  document.getElementById("send").onclick = function (evt) {
    if (!ws) {
      return false;
    }
    print("SEND: " + input.value);
    ws.send(input.value);
    return false;
  };
  document.getElementById("close").onclick = function (evt) {
    if (!ws) {
      return false;
    }
    ws.close();
    return false;
  };
});
