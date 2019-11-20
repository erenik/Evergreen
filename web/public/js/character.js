console.log("getting started");

var authToken = "1234567";

fetch("/game/character", {
  method: "GET", // *GET, POST, PUT, DELETE, etc.
  mode: "cors", // no-cors, *cors, same-origin
  cache: "no-cache", // *default, no-cache, reload, force-cache, only-if-cached
  credentials: "same-origin", // include, *same-origin, omit
  headers: {
    "Content-Type": "application/json"
    // 'Content-Type': 'application/x-www-form-urlencoded',
  },
  redirect: "follow", // manual, *follow, error
  referrer: "no-referrer", // no-referrer, *client
  body: JSON.stringify(authToken) // body data type must match "Content-Type" header
})
  .then(response => {
    return response.json();
  })
  .then(data => {
    onCharacterDataReceived(data);
    console.log(JSON.stringify(data));
  });

var secondsUntilNextDay = 10000;

function onCharacterDataReceived(data) {
  console.log("onCharacterDataReceived");
  //  document.getElementById("food").textContent = data.food;
  //document.getElementById("materials").textContent = data.materials;

  textStats = ["hp", "maxHp", "food", "materials", "attack", "defense", "experience"];
  for (var statIndex in textStats) {
    var statName = textStats[statIndex];
    console.log("updating item: " + statName + ", value: " + data[statName]);
    document.getElementById(statName).textContent = data[statName];
  }
  //  document.getElementById("attack").textContent = data.attack;
}

testData = {
  hp: 10,
  maxHp: 10,
  daysSurvived: 10,
  food: 5,
  materials: 3,
  attack: 10,
  defense: 15,
  experience: 20,
  level: 5
};

setTimeout(() => {
  onCharacterDataReceived(testData);
}, 100);
