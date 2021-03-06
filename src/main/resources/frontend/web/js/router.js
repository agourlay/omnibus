App.Router.map(function() {
    this.resource('index', { path:'/'}); 
    this.resource('settings');
    this.resource('subscriptions');
    this.resource('topics');
    this.resource('topic', { path:'/topic/*topic_id' });
    this.resource('system');
});

App.IndexRoute = Ember.Route.extend({
  model: function() {
    return App.Dao.summary();
  }
});

App.SystemRoute = Ember.Route.extend({ 
  model: function() {
    return App.Dao.system();
  }
});

App.TopicRoute = Ember.Route.extend({
  deactivate: function(transition) {
    App.Dao.closeOpenStream();  
  },

  model: function(params) {
    return params.topic_id;
  },

  afterModel: function(topicName) {
    App.Dao.setupStream("stats/topics/"+topicName+"?mode=streaming");
  } 
});

App.TopicsRoute = Ember.Route.extend({
	model: function() {
		return App.Dao.topics();
  }
});

App.SubscriptionsRoute = Ember.Route.extend({
  model: function() {
    return App.Dao.subscribers();
  }
});