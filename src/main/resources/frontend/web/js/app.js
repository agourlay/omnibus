window.App = Ember.Application.createWithMixins({
  debugMode: true,
  LOG_BINDINGS: true,
  LOG_VIEW_LOOKUPS: true,
  LOG_TRANSITIONS: true,
  LOG_STACKTRACE_ON_DEPRECATION: true,
  LOG_VERSION: true
});


App.ApplicationView = Em.View.extend({
  tagName : 'div',
  elementId: 'app'
});