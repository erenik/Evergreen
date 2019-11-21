var express = require('express');
var request = require('request');
var router = express.Router();

/* GET home page. */
router.get('/', function (req, res, next) {
  res.render('game/game', { title: 'Evergreen' });
});
router.get('/login', function (req, res, next) {
  res.render('game/login', { title: 'Evergreen' });
});
router.get('/character', function (req, res, next) {
  res.render('game/character', { title: 'Evergreen' });
});
router.get('/actions', function (req, res, next) {
  res.render('game/actions', { title: 'Evergreen' });
});
router.get('/log', function (req, res, next) {
  res.render('game/log', { title: 'Evergreen' });
});

var backendServerUri = process.env.BACKEND_URI

console.log('Using backend server Uri: '+backendServerUri)

var getEndPoints = ['/status', '/register', '/character']
var session_controller = require('../controllers/session_controller');
session_controller.backendServerUri = backendServerUri

router.post("/register", session_controller.register)

router.get("/status", function (req, res, next) { request(backendServerUri + "/status", function (error, response, body) { res.send(body); }); })
router.get("/character", function (req, res, next) { request(backendServerUri + "/character", function (error, response, body) { res.send(body); }); })



module.exports = router;
