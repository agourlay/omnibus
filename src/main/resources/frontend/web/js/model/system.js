App.System = Em.Object.extend({
	counters : [],
	meters : [],
	timers : []
});

App.Counter = Em.Object.extend({
	name : null,
	count : null
});

App.Timer = Em.Object.extend({
	name : null,
	count : null,
	max: null,
	min: null,
	mean: null,
	stdDev: null,
	fifteenMinuteRate : null,
	fiveMinuteRate : null,
	meanRate : null,
	oneMinuteRate : null
});

App.Meter = Em.Object.extend({
	name : null,
	count : null,
	fifteenMinuteRate : null,
	fiveMinuteRate : null,
	meanRate : null,
	oneMinuteRate : null
});