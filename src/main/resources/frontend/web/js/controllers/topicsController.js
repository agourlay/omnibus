App.TopicsController = Ember.ArrayController.extend({
  content: [],	
  sortProperties: ['name'],
  sortAscending: true,

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