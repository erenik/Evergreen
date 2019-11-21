
console.log('getting started with registuering')
var result = document.getElementById("termsOfUse")
console.log("termsofuse: " + result)
/*addEventListener("click", event => {
	console.log('something clicketiclicked');
	document.getElementById("termsOfUse")
})*/



function termsAccepted() {
	registerButton.disabled = !termsOfUse.checked
	registerButton.onclick = register
}


function getText(elementId) {
	var element = document.getElementById(elementId)
	//console.log('element ['+elementId+']: '+element.value)
	return document.getElementById(elementId).value
}

/**
 * 	RegisterData struct:
 	name: String,
	difficulty: int,
	avatar: int,
	startingBonus: int
 */
function register() {
	var registerData = {
		name: getText('name'),
		password: getText('password'),
		difficulty: getText('difficulty'),
		avatar: 0,
		startingBonus: getText('startingBonus'),
	}
	console.log('registering with data: ' + JSON.stringify(registerData))
	fetch("/game/register", {
		headers: {
			'Accept': 'application/json',
			'Content-Type': 'application/json'
		},
		method: "POST",
		body: JSON.stringify(registerData)
	})
		.then(response => {
			return response.json()
		})
		.then(function (res) {
			if (res.body.success) {
				// Save in cookie the name of the player.
				// Go to character screen.
			}
			else {
				alert('Failed to register! Why? ' + res.body)
			}
		})
		.catch(function (res) {
			console.log(res)
		})
}



fetch('/game/status')
	.then(response => {
		return response.json()
	})
	.then(data => {
		document.getElementById("players").textContent = data.players;
		document.getElementById("day").textContent = data.day;
		console.log(JSON.stringify(data))
	})


