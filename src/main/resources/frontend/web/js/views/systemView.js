App.SystemView = Em.View.extend({
    tagName : 'div',
    elementId: 'system',
    contentBinding: 'controller.content'      
});

App.DurationFormatHelper = Ember.HTMLBars.makeBoundHelper(function(params, hash, options, env) {
    var number = params[0];
    var digits = params[1];
	var value = number;
	if (value > 1000) {
		return (value / 1000).toFixed(digits) + " s";
	} else {
		return value.toFixed(digits) + " ms";
	}
});

App.ToFixedHelper = Ember.HTMLBars.makeBoundHelper(function(params, hash, options, env) {
  var number = params[0];
  var digits = params[1];
  return number.toFixed(digits);
});