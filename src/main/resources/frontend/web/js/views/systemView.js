App.SystemView = Em.View.extend({
    tagName : 'div',
    elementId: 'system',
    contentBinding: 'controller.content'      
});

Handlebars.registerHelper('toFixed', function(number, digits) {
  return Ember.get(this,number).toFixed(digits);
});