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
    return App.Dao.systemStats();
  },
    
  afterModel: function(data) {
    App.Dao.setupStream("stats/system?mode=streaming");
  }  
});

App.TopicRoute = Ember.Route.extend({
	model: function(params) {
		return App.Dao.topicStats(params.topic_id);
  },

  afterModel: function(container) {
    App.Dao.setupStream("stats/topics/"+container.topic+"?mode=streaming");
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