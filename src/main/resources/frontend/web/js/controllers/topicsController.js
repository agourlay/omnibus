App.TopicsController = Ember.ArrayController.extend({
  content: [],	
  sortProperties: ['name'],
  sortAscending: true,

  actions: {  
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
	  } 
  }
})