
console.log('getting started with registuering')
var result = document.getElementById("termsOfUse")
console.log("termsofuse: "+result)
/*addEventListener("click", event => {
	console.log('something clicketiclicked');
	document.getElementById("termsOfUse")
})*/

function termsAccepted(){
	registerButton.disabled = !termsOfUse.checked
}

fetch('/game/status')
	.then(response => {
		return response.json()
	})
	.then(data => {
		document.getElementById("players").textContent=data.players;
		document.getElementById("day").textContent=data.day;
		console.log(JSON.stringify(data))
	})


