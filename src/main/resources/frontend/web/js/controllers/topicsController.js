App.TopicsController = Ember.ArrayController.extend({
  content: [],	
  sortProperties: ['name'],
  sortAscending: true,

  newTopicName : null,

  createTopic : function(newTopic) {
    var controller = this;
    if (newTopic) {
      App.Dao.postTopic(newTopic).done(function() {
        controller.reloadContent();
      });
    }  
  },

  deleteTopic : function(topicName) {
    var controller = this;
    if (topicName) {
      App.Dao.deleteTopic(topicName).done(function() {
        controller.reloadContent();
      });
    }  
  },

  reloadContent : function() {
    var controller = this;
    App.Dao.topics().then(function(topics){
      controller.content.clear();
      controller.content.pushObjects(topics);  
    });
  },

  expandTopic: function(topicsName){
  	var controller = this;
    $.each(topicsName, function(i, topic){
    	var exist = controller.findProperty('name', topic);
    	if (!exist) {
    		App.Dao.topic(topic).then(function (sub) {
    			controller.content.pushObject(sub); 
    		});
    	}
    });
  },

  collapseTopic: function(topicName, subTopicsName){
  	var controller = this;
    var toDelete = [];
    $.each(controller.content, function(i, model){
    	$.each(subTopicsName, function(i, sub){
    		if (model.name.indexOf(sub) == 0 ) {
    			toDelete.push(model);
    		}
    	});	
    });	
    controller.content.removeObjects(toDelete); 
  }
})