var actionTypes = [
  "GatherFood",
  "GatherMaterials",
  "Scout",
  "Recover",
  "BuildDefenses",
  "LookForPlayer",
  "LookForPlayers",
  "Invent",
  "Craft",
  "Steal",
  "AttackAPlayer",
  "Study",
  "ReduceEmissions"
];

var queuedActions = [];

console.log("actions");

function onQueuedActionsUpdated() {
  console.log("populating queuad actions, nr: " + queuedActions.length);
  qaList = document.getElementById("QueuedActions");
  qaList.innerHTML = "";
  for (actionIndex in queuedActions) {
    var action = queuedActions[actionIndex];
    console.log("action: " + action);
    var queuedActionButton = document.createElement("li");
    queuedActionButton.className = 'vertical';
    queuedActionButton.onclick = onQueuedActionClicked;
    queuedActionButton.appendChild(document.createTextNode(action));
    qaList.appendChild(queuedActionButton);
  }
}
function onQueuedActionClicked(ev) {
  var actionClicked = ev.target.innerHTML
  console.log("Click removed! "+actionClicked);
  var index = queuedActions.indexOf(actionClicked);
  queuedActions.splice(index, 1);
  onQueuedActionsUpdated();
}

function populateActionTypes() {
  console.log("populating queuable actions, nr: " + actionTypes.length);
  qaList = document.getElementById("QueueableActions");
  for (actionTypeIndex in actionTypes) {
    var actionType = actionTypes[actionTypeIndex];
    var newQaTypeButton = document.createElement("li");
    newQaTypeButton.className = 'vertical';
    newQaTypeButton.onclick = onActionTypeClicked;
    newQaTypeButton.appendChild(document.createTextNode(actionType));
    qaList.appendChild(newQaTypeButton);
  }
}

function onActionTypeClicked(ev) {
  var actionTypeClicked = ev.target.innerHTML;
  console.log("Clicked! " + actionTypeClicked);
  if (queuedActions.indexOf(actionTypeClicked) >= 0) {
    alert("No use queueing up the same action twice!");
    return;
  }
  if (queuedActions.length >= 5) {
    alert("You will be less efficient doing too many things!");
    return;
  }
  queuedActions.push(actionTypeClicked);
  onQueuedActionsUpdated();
}

setTimeout(() => {
  populateActionTypes(actionTypes);
}, 100);
