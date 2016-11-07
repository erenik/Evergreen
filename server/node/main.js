
// Libraries
var http = require('http');
var express = require('express');
var app = express();

app.get('/', function(req, res) {
   res.send('Hello World');
})

app.get('/test', function(req, res) {
   res.send('Hello test');
})

var port = 80;

// Command-line args?
var server = app.listen(port, function () {
   var host = server.address().address
   var port = server.address().port
   
   console.log("Example app listening at http://%s:%s", host, port)
})

/**
var server = http.createServer(function (request, response) {
        response.writeHead(200, {"Content-Type": "text/plain"});
        response.end("Hello world\nVery awesome");
});
server.listen(port);
*/


console.log("Server running at http://127.0.0.1:"+port);

