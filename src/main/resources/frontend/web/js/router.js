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
    App.Dao.setupStream("stats/system?mode=streaming");
    return App.Dao.systemStats().then(null, function() { return Ember.A([]) ;});
    }
});

App.TopicRoute = Ember.Route.extend({
	model: function(params) {
    App.Dao.setupStream("stats/topics/"+params.topic_id+"?mode=streaming");
		return App.Dao.topicStats(params.topic_id).then(null, function() { return Ember.A([]) ;});
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