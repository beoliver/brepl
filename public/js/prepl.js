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
  document.getElementById("connect").onclick = function (evt) {
    if (ws) {
      return false;
    }
    // don't try to create a websocket if we know there is no port
    if (port.value === "") {
      return false;
    }

    ws = new WebSocket(`ws://${window.location.host}/prepl/${port.value}`);

    ws.onopen = function (evt) {
      print("OPEN");
    };
    ws.onclose = function (evt) {
      print("CLOSE");
      ws = null;
    };
    ws.onmessage = function (evt) {
      console.log(evt.data);
      console.log(typeof evt.data);
      const json = JSON.parse(evt.data);
      console.log(typeof json);
      print("RESPONSE: " + JSON.stringify(json["val"], null, 2));
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
