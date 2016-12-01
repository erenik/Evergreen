var express = require('express');
var router = express.Router();

/* GET home page. */
router.get('/', function(req, res, next) {
  res.render('index', { title: 'Erenik' });
});

router.get('/evergreen', function(req, res, next) {
  res.render('evergreen/index', { title: 'Evergreen' });
});

router.get('/json', function (req, res, next){
	res.send('Yoyoyoy');
});

module.exports = router;
