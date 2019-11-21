var request = require('request');
var fetch = require('node-fetch')

var backendServerUri = ''
console.log('session_controller!')

exports.index = function (req, res) {
	res.send('NOT IMPLEMENTED: Site Home Page');
};


/*
const RegisterPlayer = {
	name: String,
	difficulty: int,
	avatar: int,
	startingBonus: int
}*/


var backendServerUri = process.env.BACKEND_URI

// POST Register
exports.register = function (req, res) {
	console.log('hello')
	var body = req.body;
	var fullUri = backendServerUri + 'register/';
	console.log('Forwarding register request to game server with body ' + JSON.stringify(body) + ' to ' + fullUri);
	(async () => {
		const rawResponse = await fetch(fullUri, {
			headers: {
				'Content-Type': 'application/json'
			},
			method: "POST",
			body: JSON.stringify(req.body)
		});
		const content = await rawResponse.json();
		console.log(content);
		res.send(content)
	})();


};

// Handle book update on POST.
exports.book_update_post = function (req, res) {
	res.send('NOT IMPLEMENTED: Book update POST');
};


/*
fetch(fullUri,
{
  method: "POST",
  body: JSON.stringify(req.body)
})
.then(function(res){ return res.json(); })
.then(function(data){ alert( JSON.stringify( data ) ) })
*/

/*
fetch(fullUri, params)
	.then((response) => {
		if (response.ok) {
			return response.json();
		} else {
			throw new Error('Something went wrong, but what?');
		}
	})
	.then(function (jsonResponse) {
		console.log('response received' + jsonResponse)
		if (jsonResponse.success) {
			// Save in cookie the name of the player.
			// Go to character screen.
		}
		else {
			alert('Failed to register! Why? ' + jsonResponse)
		}
	})
	.catch(function (res) {
		console.log(res)
	})*/
