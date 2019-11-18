
console.log('getting started')
fetch('/game/status')
	.then(response => {
		return response.json()
	})
	.then(data => {
		document.getElementById("players").textContent=data.players;
		document.getElementById("day").textContent=data.day;
		console.log(JSON.stringify(data))
	})


