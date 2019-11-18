var express = require('express');
var request = require('request');
var router = express.Router();

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('game/game', { title: 'Evergreen' });
});
router.get('/login', function(req, res, next) {
  res.render('game/login', { title: 'Evergreen' });
});
router.get('/character', function(req, res, next) {
  res.render('game/character', { title: 'Evergreen' });
});
router.get('/actions', function(req, res, next) {
  res.render('game/actions', { title: 'Evergreen' });
});
router.get('/log', function(req, res, next) {
  res.render('game/log', { title: 'Evergreen' });
});

router.get("/status", function(req, res, next) {
  request("http://localhost:8080/status", function (error, response, body){
    console.log('Status of game server: '+body);
    res.send(body);
  });
})

module.exports = router;
