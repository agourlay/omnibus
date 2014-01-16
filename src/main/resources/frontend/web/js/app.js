window.App = Ember.Application.createWithMixins({
    
  debugMode: false,
  LOG_BINDINGS: false,
  LOG_VIEW_LOOKUPS: false,
  LOG_TRANSITIONS: true,
  LOG_STACKTRACE_ON_DEPRECATION: true,
  LOG_VERSION: true
});

App.ApplicationView = Em.View.extend({
  tagName : 'div',
  elementId: 'app'
});