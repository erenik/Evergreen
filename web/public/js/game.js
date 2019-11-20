 

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

var secondsUntilNextDay = 10000

function onUpdate(){
	var date = new Date(null);
	date.setSeconds(secondsUntilNextDay); // specify value for SECONDS here
	var timeUntilNextDay = date.toISOString().substr(11, 8);	
	document.getElementById("timeUntilNextDay").textContent = timeUntilNextDay
	secondsUntilNextDay -= 1

	setTimeout(() => {
		onUpdate();
	  }, 1000);	
}



const timeoutScheduled = Date.now();

setTimeout(() => {
	onUpdate();
  }, 100);
