var express = require('express');
var router = express.Router();

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'Erenik' });
});

router.get('/dev', function(req, res, next) {
  res.render('dev', { title: 'Author' });
});


router.get('/', function(req, res, next) {
  res.render('index', { title: 'Evergreen' });
});

router.get('/contact', function(req, res, next) {
  res.render('contact', { title: 'Evergreen' });
});

router.get('/science', function(req, res, next) {
  res.render('science', { title: 'Evergreen' });
});

module.exports = router;
